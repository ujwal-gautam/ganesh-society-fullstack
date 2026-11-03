package com.ganesh.society;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "loans")
public class Loan {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    @Column(name = "member_id")
    public Long memberId;
    public BigDecimal principal;
    @Column(name = "term_months")
    public int termMonths;
    @Column(name = "issued_on")
    public LocalDate issuedOn;
    @Column(name = "needs_review")
    public boolean needsReview;
    public String notes;
}
