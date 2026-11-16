package com.cstudy.auth.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** 운영자 계정. 비밀번호는 BCrypt 해시로만 보관한다. (QC-004) */
@Entity
@Table(name = "admin")
public class AdminModel {

    public static final String ROLE_ADMIN = "ADMIN";

    @Id
    @Column(length = 50)
    private String id;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(nullable = false, length = 20)
    private String role;

    protected AdminModel() {
    }

    public AdminModel(String id, String passwordHash, String role) {
        this.id = id;
        this.passwordHash = passwordHash;
        this.role = role;
    }

    public String getId() {
        return id;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getRole() {
        return role;
    }
}
