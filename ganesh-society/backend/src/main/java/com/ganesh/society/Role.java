package com.ganesh.society;

import jakarta.persistence.*;
import java.util.*;
@Entity @Table(name="roles") public class Role {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 @Column(unique=true,nullable=false) public String name;
 public String description;
 public boolean active=true;
 @ManyToMany(fetch=FetchType.EAGER) @JoinTable(name="role_permissions",joinColumns=@JoinColumn(name="role_id"),inverseJoinColumns=@JoinColumn(name="permission_id")) public Set<Permission> permissions=new HashSet<>();
}
