package com.br.wallet.domain.model;

import com.br.wallet.domain.enums.LedgerEntryDirection;
import com.br.wallet.domain.enums.LedgerOperationType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record LedgerEntry(
        UUID id,
        UUID walletId,
        LedgerOperationType operationType,
        LedgerEntryDirection direction,
        BigDecimal amount,
        String referenceId,
        String endToEndId,
        Instant occurredAt,
        Instant createdAt
) {

    public static LedgerEntry newEntry(
            UUID walletId,
            LedgerOperationType operationType,
            LedgerEntryDirection direction,
            BigDecimal amount,
            String referenceId,
            String endToEndId,
            Instant occurredAt
    ) {
        return new LedgerEntry(
                UUID.randomUUID(),
                walletId,
                operationType,
                direction,
                amount,
                referenceId,
                endToEndId,
                occurredAt,
                Instant.now()
        );
    }
}
