package com.csuft.oj.audit;

import com.csuft.oj.mapper.AuditLogMapper;
import com.csuft.oj.security.ClientIpResolver;
import com.csuft.oj.security.SecurityUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Persists redacted audit records without allowing audit failures to break business requests.
 */
@Slf4j
@Aspect
@Component
public class AuditLogAspect {

    private static final Set<String> SENSITIVE_FIELD_PARTS =
            Set.of("password", "code", "token", "secret", "authorization", "cookie");
    private static final int MAX_TEXT_LENGTH = 512;

    private final AuditLogMapper auditLogMapper;
    private final ObjectMapper objectMapper;

    public AuditLogAspect(AuditLogMapper auditLogMapper, ObjectMapper objectMapper) {
        this.auditLogMapper = auditLogMapper;
        this.objectMapper = objectMapper;
    }

    @AfterReturning("@annotation(auditLog)")
    public void saveSuccessfulAuditLog(JoinPoint joinPoint, AuditLog auditLog) {
        saveAuditLog(joinPoint, auditLog, "SUCCESS", null);
    }

    @AfterThrowing(pointcut = "@annotation(auditLog)", throwing = "error")
    public void saveFailedAuditLog(JoinPoint joinPoint, AuditLog auditLog, Throwable error) {
        saveAuditLog(joinPoint, auditLog, "FAILED", error);
    }

    private void saveAuditLog(JoinPoint joinPoint, AuditLog auditLog, String outcome, Throwable error) {
        try {
            HttpServletRequest request = currentRequest();
            com.csuft.oj.entity.AuditLog entity = new com.csuft.oj.entity.AuditLog();
            entity.setOperatorId(SecurityUtils.currentUserIdOrNull());
            entity.setAction(auditLog.action());
            entity.setTargetType(joinPoint.getSignature().getDeclaringType().getSimpleName());
            entity.setTargetId(firstLongArgument(joinPoint.getArgs()));
            entity.setIpAddress(ClientIpResolver.resolve(request));
            entity.setUserAgent(truncate(request == null ? null : request.getHeader("User-Agent")));
            entity.setDetail(buildDetail(joinPoint, outcome, error));
            entity.setCreatedAt(LocalDateTime.now());
            auditLogMapper.insert(entity);
        } catch (Exception ex) {
            log.error("Failed to persist audit log for action {}", auditLog.action(), ex);
        }
    }

    private HttpServletRequest currentRequest() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            return attributes.getRequest();
        }
        return null;
    }

    private Long firstLongArgument(Object[] args) {
        for (Object arg : args) {
            if (arg instanceof Long value) {
                return value;
            }
        }
        return null;
    }

    private String buildDetail(JoinPoint joinPoint, String outcome, Throwable error)
            throws JsonProcessingException {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String[] names = signature.getParameterNames();
        Object[] args = joinPoint.getArgs();
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("method", signature.getMethod().getName());
        detail.put("className", signature.getDeclaringTypeName());
        detail.put("outcome", outcome);
        if (error != null) {
            detail.put("errorType", error.getClass().getSimpleName());
            detail.put("errorMessage", truncate(error.getMessage()));
        }

        Map<String, JsonNode> params = new LinkedHashMap<>();
        for (int i = 0; i < args.length; i++) {
            String name = names != null && i < names.length ? names[i] : "arg" + i;
            JsonNode value = sanitizeValue(name, args[i]);
            if (value != null) {
                params.put(name, value);
            }
        }
        detail.put("params", params);
        return objectMapper.writeValueAsString(detail);
    }

    JsonNode sanitizeValue(String name, Object value) {
        if (isSensitive(name)) {
            return objectMapper.getNodeFactory().textNode("[REDACTED]");
        }
        if (value == null) {
            return objectMapper.getNodeFactory().nullNode();
        }
        if (value instanceof ServletRequest || value instanceof ServletResponse) {
            return null;
        }
        if (value instanceof MultipartFile file) {
            ObjectNode fileNode = objectMapper.createObjectNode();
            fileNode.put("filename", truncate(file.getOriginalFilename()));
            fileNode.put("size", file.getSize());
            return fileNode;
        }
        JsonNode node = objectMapper.valueToTree(value);
        if (node.isTextual() && node.textValue().length() > MAX_TEXT_LENGTH) {
            return objectMapper.getNodeFactory().textNode(truncate(node.textValue()));
        }
        redactAndTruncate(node);
        return node;
    }

    private void redactAndTruncate(JsonNode node) {
        if (node instanceof ObjectNode objectNode) {
            Iterator<Map.Entry<String, JsonNode>> fields = objectNode.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                if (isSensitive(field.getKey())) {
                    objectNode.put(field.getKey(), "[REDACTED]");
                } else if (field.getValue().isTextual()
                        && field.getValue().textValue().length() > MAX_TEXT_LENGTH) {
                    objectNode.put(field.getKey(), truncate(field.getValue().textValue()));
                } else {
                    redactAndTruncate(field.getValue());
                }
            }
        } else if (node instanceof ArrayNode arrayNode) {
            for (int i = 0; i < arrayNode.size(); i++) {
                JsonNode item = arrayNode.get(i);
                if (item.isTextual() && item.textValue().length() > MAX_TEXT_LENGTH) {
                    arrayNode.set(i, objectMapper.getNodeFactory().textNode(truncate(item.textValue())));
                } else {
                    redactAndTruncate(item);
                }
            }
        }
    }

    private boolean isSensitive(String fieldName) {
        String normalized = fieldName == null ? "" : fieldName.toLowerCase(Locale.ROOT);
        return SENSITIVE_FIELD_PARTS.stream().anyMatch(normalized::contains);
    }

    private String truncate(String value) {
        if (value == null || value.length() <= MAX_TEXT_LENGTH) {
            return value;
        }
        return value.substring(0, MAX_TEXT_LENGTH);
    }
}
