package com.ganesh.society;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.*;

public interface MemberRepository extends JpaRepository<Member, Long> {
    boolean existsByMobileNumber(String mobileNumber);
    boolean existsByMobileNumberAndIdNot(String mobileNumber, Long id);
}
