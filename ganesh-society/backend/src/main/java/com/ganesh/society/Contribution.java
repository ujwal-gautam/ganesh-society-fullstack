package com.ganesh.society;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Instant;

@Entity
@Table(name = "contributions")
public class Contribution {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    @Column(name = "member_id", nullable = false)
    public Long memberId;
    @Column(name = "contribution_month", nullable = false)
    public String contributionMonth;
    public BigDecimal amount;
    @Column(name = "paid_on")
    public LocalDate paidOn;
    public String notes;
    @Column(name = "deleted_at")
    public Instant deletedAt;
    @Column(name = "deleted_by")
    public String deletedBy;
}
