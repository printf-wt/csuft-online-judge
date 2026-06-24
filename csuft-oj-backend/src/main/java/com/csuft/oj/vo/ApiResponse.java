package com.csuft.oj.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Common API response wrapper.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {

    /**
     * Whether the request succeeded.
     */
    private Boolean success;

    /**
     * Business status code.
     */
    private Integer code;

    /**
     * Response message.
     */
    private String message;

    /**
     * Response data.
     */
    private T data;

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, 200, "success", data);
    }

    public static <T> ApiResponse<T> fail(Integer code, String message) {
        return new ApiResponse<>(false, code, message, null);
    }
}
