package com.ganesh.society;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "loan_repayments")
public class LoanRepayment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    @Column(name = "loan_id")
    public Long loanId;
    @Column(name = "paid_on")
    public LocalDate paidOn;
    @Column(name = "received_amount")
    public BigDecimal receivedAmount;
    @Column(name = "principal_component")
    public BigDecimal principalComponent;
    @Column(name = "interest_component")
    public BigDecimal interestComponent;
    public String notes;
}
