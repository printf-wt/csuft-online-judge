package com.csuft.oj.service;

import com.csuft.oj.vo.AuthLoginVO;

public record AuthSession(AuthLoginVO response, String refreshToken, long refreshExpiresInSeconds) {
}
