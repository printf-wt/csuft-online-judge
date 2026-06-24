package com.csuft.oj.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserVO {
    private Long id;
    private String username;
    private String nickname;
    private String email;
    private String role;
    private Integer status;
    private Integer globalAcCount;
    private Integer submitCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
