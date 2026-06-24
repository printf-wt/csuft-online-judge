package com.csuft.oj.audit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks controller methods that should be persisted to tb_audit_log.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuditLog {

    /**
     * Business action name, such as CREATE_PROBLEM or CREATE_CONTEST.
     */
    String action();
}
