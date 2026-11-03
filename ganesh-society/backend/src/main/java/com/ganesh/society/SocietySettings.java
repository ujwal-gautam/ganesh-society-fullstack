package com.ganesh.society;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "society_settings")
public class SocietySettings {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    @Column(name = "group_name")
    public String groupName;
    @Column(name = "start_month")
    public String startMonth;
    @Column(name = "cycle_months")
    public int cycleMonths;
    @Column(name = "monthly_amount_per_share")
    public BigDecimal monthlyAmountPerShare;
    @Column(name = "monthly_interest_rate")
    public BigDecimal monthlyInterestRate;
}
