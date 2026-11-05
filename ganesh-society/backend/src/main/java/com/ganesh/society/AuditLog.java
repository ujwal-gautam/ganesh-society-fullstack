package com.ganesh.society;

import jakarta.persistence.*;
import java.time.Instant;
@Entity @Table(name="audit_logs") public class AuditLog {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 @Column(name="actor_id") public Long actorId;
 public String action;
 public String target;
 @Column(name="created_at",insertable=false,updatable=false) public Instant createdAt;
}
