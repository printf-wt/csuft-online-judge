package com.csuft.oj.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileVO {
    private Long id;
    private String username;
    private String nickname;
    private String email;
    private String role;
    private Integer globalAcCount;
    private Integer submitCount;
    private LocalDateTime createdAt;
    private List<SubmissionActivityVO> submissionActivity;
}
