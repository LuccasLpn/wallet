package com.br.wallet.domain.model;

import java.time.Instant;
import java.util.UUID;

public record Wallet(
        UUID id,
        String ownerId,
        Instant createdAt
) {

    public static Wallet newWallet(String ownerId) {
        return new Wallet(
                UUID.randomUUID(),
                ownerId,
                Instant.now()
        );
    }
}
