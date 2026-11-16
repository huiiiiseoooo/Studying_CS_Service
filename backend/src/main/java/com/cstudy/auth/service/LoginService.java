package com.cstudy.auth.service;

import com.cstudy.auth.domain.AdminModel;
import com.cstudy.auth.dto.LoginResponse;
import com.cstudy.auth.repository.LoginRepository;
import com.cstudy.common.error.ApiException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 운영자 로그인. (UC-003) */
@Service
public class LoginService {

    private final LoginRepository loginRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    /** 없는 아이디일 때도 해시 비교를 한 번 수행해, 응답 시간으로 아이디 존재 여부가 드러나지 않게 한다. */
    private final String dummyHash;

    public LoginService(LoginRepository loginRepository, PasswordEncoder passwordEncoder,
                        JwtTokenProvider jwtTokenProvider) {
        this.loginRepository = loginRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.dummyHash = passwordEncoder.encode("not-a-real-password");
    }

    /** 인증에 성공하면 Access Token 을 발급한다. (TC-006) */
    @Transactional(readOnly = true)
    public LoginResponse login(String id, String password) {
        AdminModel admin = authLogin(id, password);
        String token = jwtTokenProvider.createToken(admin.getId(), admin.getRole());
        return new LoginResponse(token, "Bearer", jwtTokenProvider.validitySeconds());
    }

    /** 아이디·비밀번호를 DB의 운영자 계정과 대조한다. 어느 쪽이 틀렸는지는 알리지 않는다. (UC-003 2a) */
    private AdminModel authLogin(String id, String password) {
        AdminModel admin = loginRepository.findById(id).orElse(null);
        String hash = admin == null ? dummyHash : admin.getPasswordHash();
        boolean matched = matchesPassword(password, hash);
        if (admin == null || !matched || !AdminModel.ROLE_ADMIN.equals(admin.getRole())) {
            throw ApiException.unauthorized("LOGIN_FAILED", "계정이 존재하지 않거나 비밀번호가 잘못되었습니다.");
        }
        return admin;
    }

    private boolean matchesPassword(String raw, String hash) {
        return passwordEncoder.matches(raw, hash);
    }
}
