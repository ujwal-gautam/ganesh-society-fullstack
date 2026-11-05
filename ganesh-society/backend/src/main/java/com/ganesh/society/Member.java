package com.ganesh.society;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "members")
public class Member {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    @Column(name = "full_name", nullable = false)
    public String fullName;
    @Column(name = "mobile_number", length = 10, unique = true)
    public String mobileNumber;
    public int shares;
    public boolean active = true;
}
