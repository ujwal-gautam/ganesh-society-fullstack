package com.ganesh.society;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.*;

@RestController
public class SocietyController {
    final MemberRepository members;
    final ContributionRepository contributions;
    final LoanRepository loans;
    final LoanRepaymentRepository repayments;
    final SocietySettingsRepository settings;

    public SocietyController(MemberRepository m, ContributionRepository c, LoanRepository l, LoanRepaymentRepository r, SocietySettingsRepository s) {
        members = m;
        contributions = c;
        loans = l;
        repayments = r;
        settings = s;
    }

    record Snapshot(List<Member> members, List<Contribution> contributions, List<Loan> loans,
                    List<LoanRepayment> repayments, SocietySettings settings) {
    }

    @GetMapping("/api/public/snapshot")
    @Transactional(readOnly = true)
    public Snapshot snapshot() {
        return new Snapshot(members.findAll(), contributions.findAll(), loans.findAll(), repayments.findAll(), settings.findAll().stream().findFirst().orElseThrow());
    }

    @GetMapping("/api/admin/me")
    public Map<String, Object> me(org.springframework.security.core.Authentication auth) {
        return Map.of("email", auth.getName(), "role", "ADMIN");
    }

    @PostMapping("/api/admin/members")
    public Member member(@RequestBody Member m) {
        if (m.id != null || m.fullName == null || m.fullName.isBlank() || m.shares < 1) throw bad();
        m.active = true;
        return members.save(m);
    }

    @PutMapping("/api/admin/members/{id}")
    public Member memberUpdate(@PathVariable Long id, @RequestBody Member m) {
        Member old = members.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (m.fullName == null || m.fullName.isBlank() || m.shares < 1) throw bad();
        old.fullName = m.fullName;
        old.shares = m.shares;
        old.active = m.active;
        return members.save(old);
    }

    @PostMapping("/api/admin/contributions")
    public Contribution contribution(@RequestBody Contribution c) {
        if (c.id != null || !members.existsById(c.memberId) || c.amount == null || c.amount.signum() <= 0 || c.paidOn == null || !month(c.contributionMonth))
            throw bad();
        return contributions.save(c);
    }

    @PostMapping("/api/admin/loans")
    public Loan loan(@RequestBody Loan l) {
        if (l.id != null || !members.existsById(l.memberId) || l.principal == null || l.principal.signum() <= 0 || l.termMonths <= 0 || l.issuedOn == null)
            throw bad();
        return loans.save(l);
    }

    @PostMapping("/api/admin/loans/{id}/repayments")
    @Transactional
    public LoanRepayment repayment(@PathVariable Long id, @RequestBody LoanRepayment r) {
        Loan l = loans.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (r.id != null || r.receivedAmount == null || r.principalComponent == null || r.interestComponent == null || r.paidOn == null || r.receivedAmount.signum() <= 0 || r.principalComponent.signum() < 0 || r.interestComponent.signum() < 0 || r.principalComponent.add(r.interestComponent).compareTo(r.receivedAmount) > 0)
            throw bad();
        BigDecimal paid = repayments.findAll().stream().filter(x -> x.loanId.equals(id)).map(x -> x.principalComponent).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (paid.add(r.principalComponent).compareTo(l.principal) > 0) throw bad();
        r.loanId = id;
        return repayments.save(r);
    }

    @PutMapping("/api/admin/settings")
    public SocietySettings updateSettings(@RequestBody SocietySettings s) {
        SocietySettings old = settings.findAll().stream().findFirst().orElseThrow();
        if (s.groupName == null || s.groupName.isBlank() || !month(s.startMonth) || s.cycleMonths < 1 || s.cycleMonths > 120 || s.monthlyAmountPerShare == null || s.monthlyAmountPerShare.signum() <= 0 || s.monthlyInterestRate == null || s.monthlyInterestRate.signum() < 0 || s.monthlyInterestRate.compareTo(new BigDecimal("100")) > 0)
            throw bad();
        old.groupName = s.groupName;
        old.startMonth = s.startMonth;
        old.cycleMonths = s.cycleMonths;
        old.monthlyAmountPerShare = s.monthlyAmountPerShare;
        old.monthlyInterestRate = s.monthlyInterestRate;
        return settings.save(old);
    }

    private boolean month(String s) {
        return s != null && s.matches("\\d{4}-(0[1-9]|1[0-2])");
    }

    private ResponseStatusException bad() {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid request data");
    }
}
