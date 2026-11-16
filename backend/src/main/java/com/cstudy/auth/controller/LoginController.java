package com.cstudy.auth.controller;

import com.cstudy.auth.dto.LoginRequest;
import com.cstudy.auth.dto.LoginResponse;
import com.cstudy.auth.service.LoginService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class LoginController {

    private final LoginService loginService;

    public LoginController(LoginService loginService) {
        this.loginService = loginService;
    }

    /** 운영자 로그인. 성공하면 JWT Access Token 을 돌려준다. (UC-003, TC-006) */
    @PostMapping("/api/auth/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return loginService.login(request.id(), request.password());
    }
}
