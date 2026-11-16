package com.cstudy.auth.dto;

/**
 * @param accessToken      이후 요청의 Authorization: Bearer 헤더에 넣을 JWT
 * @param expiresInSeconds 토큰이 유효한 시간(초)
 */
public record LoginResponse(String accessToken, String tokenType, long expiresInSeconds) {
}
