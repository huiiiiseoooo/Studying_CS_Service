package com.cstudy.auth.filter;

import com.cstudy.auth.service.AuthService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Authorization: Bearer 헤더의 JWT 를 검사해 운영자로 인증한다. (TC-006)
 * 토큰이 없거나 유효하지 않으면 인증 없이 통과시키고, 운영자 API 라면 뒤에서 401 로 거부된다.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final AuthService authService;

    public JwtAuthenticationFilter(AuthService authService) {
        this.authService = authService;
    }

    /** 운영자 API 가 아닌 요청은 토큰을 볼 필요가 없다. */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith(request.getContextPath() + "/api/admin/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            authService.authenticate(header.substring(BEARER_PREFIX.length()).trim())
                    .ifPresent(adminId -> SecurityContextHolder.getContext().setAuthentication(
                            new UsernamePasswordAuthenticationToken(adminId, null,
                                    List.of(new SimpleGrantedAuthority("ROLE_ADMIN")))));
        }
        chain.doFilter(request, response);
    }
}
