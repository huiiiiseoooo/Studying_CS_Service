package com.cstudy.auth.service;

import com.cstudy.auth.domain.AdminModel;
import com.cstudy.auth.repository.LoginRepository;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 요청에 실린 토큰이 유효한 운영자의 것인지 확인한다. (TC-006: 서명 → 만료 → 권한 순) */
@Service
public class AuthService {

    private final JwtTokenProvider jwtTokenProvider;
    private final LoginRepository loginRepository;

    public AuthService(JwtTokenProvider jwtTokenProvider, LoginRepository loginRepository) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.loginRepository = loginRepository;
    }

    /** 토큰이 유효하고 운영자 권한이 확인되면 운영자 ID를 돌려준다. */
    @Transactional(readOnly = true)
    public Optional<String> authenticate(String token) {
        return jwtTokenProvider.parse(token)                                   // 서명·만료
                .filter(claims -> AdminModel.ROLE_ADMIN.equals(claims.role())) // 권한
                .map(JwtTokenProvider.TokenClaims::adminId)
                .filter(this::isAuthenticated);
    }

    /** 계정이 지금도 존재하고 운영자 권한을 가지고 있는지 확인한다. (삭제된 계정의 토큰 차단) */
    public boolean isAuthenticated(String adminId) {
        return loginRepository.findById(adminId)
                .map(admin -> AdminModel.ROLE_ADMIN.equals(admin.getRole()))
                .orElse(false);
    }
}
