package pi.db.piversionbd.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AdherenceResponse(
        Long id,
        Long memberId,
        Long claimId,
        String eventType,
        BigDecimal scoreChange,
        BigDecimal currentScore,
        String note,
        LocalDateTime createdAt
) {}
