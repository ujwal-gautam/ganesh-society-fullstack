package com.ganesh.society;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "contribution_changes")
public class ContributionChange {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    @Column(name = "contribution_id", nullable = false)
    public Long contributionId;
    @Column(name = "member_id", nullable = false)
    public Long memberId;
    @Column(name = "old_month", nullable = false)
    public String oldMonth;
    @Column(name = "new_month", nullable = false)
    public String newMonth;
    @Column(name = "old_amount", nullable = false)
    public BigDecimal oldAmount;
    @Column(name = "new_amount", nullable = false)
    public BigDecimal newAmount;
    @Column(name = "changed_by", nullable = false)
    public String changedBy;
    @Column(name = "reason", nullable = false)
    public String reason;
    @Column(name = "changed_at", insertable = false, updatable = false)
    public Instant changedAt;
}
