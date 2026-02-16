package pi.db.piversionbd.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ClaimResponse(
        Long id,
        String claimNumber,
        BigDecimal amountRequested,
        BigDecimal amountApproved,
        BigDecimal finalScoreSnapshot,
        String status,
        String decisionReason,
        boolean excludedConditionDetected,
        String decisionComment,
        LocalDateTime decisionAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        Long memberId,
        Long groupId
) {}
