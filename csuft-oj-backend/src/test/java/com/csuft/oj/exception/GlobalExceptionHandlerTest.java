package com.csuft.oj.exception;

import com.csuft.oj.vo.ApiResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void preservesBusinessHttpStatus() {
        ResponseEntity<ApiResponse<Void>> response =
                handler.handleBusinessException(new BusinessException(403, "Forbidden"));

        assertEquals(403, response.getStatusCode().value());
        assertEquals(403, response.getBody().getCode());
        assertEquals("Forbidden", response.getBody().getMessage());
    }

    @Test
    void unknownBusinessCodeFallsBackToBadRequestStatus() {
        ResponseEntity<ApiResponse<Void>> response =
                handler.handleBusinessException(new BusinessException(10001, "Invalid"));

        assertEquals(400, response.getStatusCode().value());
        assertEquals(10001, response.getBody().getCode());
    }
}
