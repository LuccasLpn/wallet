package com.br.wallet.domain.model;

import java.time.Instant;
import java.util.UUID;

public record IdempotencyRecord(
        UUID id,
        String scope,
        String idempotencyKey,
        String responsePayload,
        Instant createdAt
) {

    public static IdempotencyRecord newRecord(
            String scope,
            String idempotencyKey,
            String responsePayload
    ) {
        return new IdempotencyRecord(
                UUID.randomUUID(),
                scope,
                idempotencyKey,
                responsePayload,
                Instant.now()
        );
    }
}
