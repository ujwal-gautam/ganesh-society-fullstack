package com.ganesh.society;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "admin_users")
public class AdminUser {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    public String email;
    @Column(name = "password_hash")
    public String passwordHash;
    public String role = "ADMIN";
    public boolean enabled = true;
}
