package com.ganesh.society;

import jakarta.persistence.*;
@Entity @Table(name="permissions") public class Permission {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 @Column(unique=true,nullable=false) public String code;
 public String module;
 public String description;
}
