package com.ganesh.society;

import java.math.BigDecimal;

public record MemberLoanSummary(BigDecimal issued, BigDecimal received, BigDecimal outstanding, BigDecimal interest) {}
