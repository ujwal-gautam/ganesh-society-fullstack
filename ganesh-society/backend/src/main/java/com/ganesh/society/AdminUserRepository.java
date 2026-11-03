package com.ganesh.society;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.*;

public interface AdminUserRepository extends JpaRepository<AdminUser, Long> {
    Optional<AdminUser> findByEmail(String email);
}
