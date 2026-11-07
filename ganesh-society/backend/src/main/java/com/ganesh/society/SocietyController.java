package com.ganesh.society;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import java.util.*;

@RestController
public class SocietyController {
    final MemberRepository members;
    final ContributionRepository contributions;
    final LoanRepository loans;
    final LoanRepaymentRepository repayments;
    final SocietySettingsRepository settings;
    final ContributionChangeRepository changes;

    public SocietyController(MemberRepository m, ContributionRepository c, LoanRepository l, LoanRepaymentRepository r, SocietySettingsRepository s, ContributionChangeRepository changes) {
        members = m;
        contributions = c;
        loans = l;
        repayments = r;
        settings = s;
        this.changes = changes;
    }

    record Snapshot(List<Member> members, List<Contribution> contributions, List<Loan> loans,
                    List<LoanRepayment> repayments, SocietySettings settings) {
    }

    @GetMapping("/api/public/snapshot")
    @Transactional(readOnly = true)
    public Snapshot snapshot() {
        List<Member> publicMembers = members.findAll().stream().map(member -> {
            Member safe = new Member();
            safe.id = member.id;
            safe.fullName = member.fullName;
            safe.shares = member.shares;
            safe.active = member.active;
            return safe;
        }).toList();
        return new Snapshot(publicMembers, contributions.findByDeletedAtIsNull(), loans.findAll(), repayments.findAll(), settings.findAll().stream().findFirst().orElseThrow());
    }

    @GetMapping("/api/admin/me")
    public Map<String, Object> me(org.springframework.security.core.Authentication auth) {
        return Map.of("email", auth.getName(), "permissions", auth.getAuthorities().stream().map(a -> a.getAuthority()).toList());
    }

    private String normalizeMobile(String value) {
        if (value == null || value.isBlank()) return null;
        String normalized = value.trim().replaceAll("[\\s-]", "");
        if (normalized.startsWith("+91")) normalized = normalized.substring(3);
        else if (normalized.startsWith("91") && normalized.length() == 12) normalized = normalized.substring(2);
        if (!normalized.matches("[6-9][0-9]{9}"))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid Indian mobile number");
        return normalized;
    }

    @GetMapping("/api/admin/members")
    @PreAuthorize("hasAuthority('MEMBER_VIEW')")
    public List<Member> adminMembers() { return members.findAll(); }

    @GetMapping("/api/admin/members/{id}/contributions")
    @PreAuthorize("hasAuthority('CONTRIBUTION_VIEW')")
    public List<Contribution> memberContributions(@PathVariable Long id) {
        if (!members.existsById(id)) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        return contributions.findByMemberIdAndDeletedAtIsNullOrderByContributionMonthDescPaidOnDesc(id);
    }

    @GetMapping("/api/admin/members/{id}/contribution-changes")
    @PreAuthorize("hasAuthority('CONTRIBUTION_VIEW')")
    public List<ContributionChange> memberChanges(@PathVariable Long id) {
        if (!members.existsById(id)) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        return changes.findByMemberIdOrderByIdDesc(id);
    }

    @GetMapping("/api/admin/contributions/{id}/history")
    @PreAuthorize("hasAuthority('CONTRIBUTION_VIEW')")
    public List<ContributionChange> contributionHistory(@PathVariable Long id) {
        if (contributions.findById(id).filter(c -> c.deletedAt == null).isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        return changes.findByContributionIdOrderByIdDesc(id);
    }

    record ContributionCorrection(String contributionMonth, BigDecimal amount, String reason) {}

    @PutMapping("/api/admin/contributions/{id}")
    @PreAuthorize("hasAuthority('CONTRIBUTION_UPDATE')")
    @Transactional
    public Contribution updateContribution(@PathVariable Long id,
                                           @RequestBody ContributionCorrection body,
                                           Authentication auth) {
        Contribution old = contributions.findById(id).filter(c -> c.deletedAt == null)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!month(body.contributionMonth()) || body.amount() == null ||
                body.amount().signum() <= 0 || body.amount().scale() > 2 ||
                body.reason() == null || body.reason().isBlank() || body.reason().length() > 500)
            throw bad();
        if (Objects.equals(old.contributionMonth, body.contributionMonth()) &&
                old.amount.compareTo(body.amount()) == 0) return old;
        ContributionChange change = new ContributionChange();
        change.contributionId = old.id;
        change.memberId = old.memberId;
        change.oldMonth = old.contributionMonth;
        change.newMonth = body.contributionMonth();
        change.oldAmount = old.amount;
        change.newAmount = body.amount();
        change.changedBy = auth.getName();
        change.reason = body.reason().trim();
        changes.save(change);
        old.contributionMonth = body.contributionMonth();
        old.amount = body.amount();
        return contributions.save(old);
    }

    @DeleteMapping("/api/admin/contributions/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_SUPER_ADMIN') and hasAuthority('CONTRIBUTION_DELETE')")
    @Transactional
    public ResponseEntity<Void> deleteContribution(@PathVariable Long id, Authentication auth) {
        Contribution contribution = contributions.findById(id)
                .filter(c -> c.deletedAt == null)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Contribution not found"));
        contribution.deletedAt = java.time.Instant.now();
        contribution.deletedBy = auth.getName();
        contributions.save(contribution);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/api/admin/members")
    public Member member(@RequestBody Member m) {
        if (m.id != null || m.fullName == null || m.fullName.isBlank() || m.shares < 1) throw bad();
        m.mobileNumber = normalizeMobile(m.mobileNumber);
        if (m.mobileNumber == null) throw bad();
        if (members.existsByMobileNumber(m.mobileNumber))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Mobile number already registered");
        m.active = true;
        return members.save(m);
    }

    @PutMapping("/api/admin/members/{id}")
    public Member memberUpdate(@PathVariable Long id, @RequestBody Member m) {
        Member old = members.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (m.fullName == null || m.fullName.isBlank() || m.shares < 1) throw bad();
        String mobile = normalizeMobile(m.mobileNumber);
        if (mobile == null) throw bad();
        if (members.existsByMobileNumberAndIdNot(mobile, id))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Mobile number already registered");
        old.mobileNumber = mobile;
        old.fullName = m.fullName;
        old.shares = m.shares;
        old.active = m.active;
        return members.save(old);
    }

    @PostMapping("/api/admin/contributions")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_SUPER_ADMIN') and hasAuthority('CONTRIBUTION_CREATE')")
    public Contribution contribution(@RequestBody Contribution c) {
        if (c.id != null || c.memberId == null || !members.existsById(c.memberId) || c.amount == null || c.amount.signum() <= 0 || c.paidOn == null || !month(c.contributionMonth))
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
