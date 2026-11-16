package com.cstudy.auth.repository;

import com.cstudy.auth.domain.AdminModel;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoginRepository extends JpaRepository<AdminModel, String> {
}
