package com.ganesh.society;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ContributionChangeRepository extends JpaRepository<ContributionChange, Long> {
    List<ContributionChange> findByContributionIdOrderByIdDesc(Long contributionId);
    List<ContributionChange> findByMemberIdOrderByIdDesc(Long memberId);
}
