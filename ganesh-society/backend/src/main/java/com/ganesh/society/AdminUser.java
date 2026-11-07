package com.ganesh.society;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.*;
@Entity @Table(name="admin_users") public class AdminUser {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 @Column(unique=true,nullable=false) public String email;
 @Column(name="password_hash",nullable=false) public String passwordHash;
 @Column(name="full_name") public String fullName;
 @Column(name="mobile_number",length=10,unique=true) public String mobileNumber;
 // Legacy column retained for backward-compatible database migration.
 public String role="ADMIN";
 public boolean enabled=true;
 @Column(name="token_version",nullable=false) public Long tokenVersion=0L;
 @Column(name="created_at",insertable=false,updatable=false) public Instant createdAt;
 @Column(name="updated_at",insertable=false,updatable=false) public Instant updatedAt;
 @Column(name="last_login_at") public Instant lastLoginAt;
 @ManyToMany(fetch=FetchType.EAGER) @JoinTable(name="user_roles",joinColumns=@JoinColumn(name="user_id"),inverseJoinColumns=@JoinColumn(name="role_id")) public Set<Role> roles=new HashSet<>();
}
