package com.cstudy.auth.service;

import com.cstudy.auth.domain.AdminModel;
import com.cstudy.auth.repository.LoginRepository;
import com.cstudy.common.config.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 환경 변수 ADMIN_ID / ADMIN_PASSWORD 가 있으면 시작할 때 운영자 계정을 한 번 만든다.
 * 비밀번호를 소스나 SQL 파일에 적지 않기 위한 장치이며, 이미 있는 계정은 건드리지 않는다.
 */
@Component
public class AdminInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminInitializer.class);

    private final AppProperties properties;
    private final LoginRepository loginRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminInitializer(AppProperties properties, LoginRepository loginRepository,
                            PasswordEncoder passwordEncoder) {
        this.properties = properties;
        this.loginRepository = loginRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        String id = properties.admin().initialId();
        String password = properties.admin().initialPassword();
        if (id == null || id.isBlank() || password == null || password.isBlank()) {
            return;
        }
        if (loginRepository.existsById(id)) {
            return;
        }
        loginRepository.save(new AdminModel(id, passwordEncoder.encode(password), AdminModel.ROLE_ADMIN));
        log.info("운영자 계정을 생성했습니다: {}", id);
    }
}
