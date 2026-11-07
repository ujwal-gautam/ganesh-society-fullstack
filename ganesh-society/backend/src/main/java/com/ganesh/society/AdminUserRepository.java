package com.ganesh.society;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.*;

public interface AdminUserRepository extends JpaRepository<AdminUser, Long> {
    boolean existsByMobileNumber(String mobileNumber);
    boolean existsByMobileNumberAndIdNot(String mobileNumber, Long id);
    Optional<AdminUser> findByEmail(String email);
}
