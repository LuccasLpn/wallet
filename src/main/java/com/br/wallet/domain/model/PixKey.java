package com.br.wallet.domain.model;

import com.br.wallet.domain.enums.PixKeyType;

import java.time.Instant;
import java.util.UUID;

public record PixKey(
        UUID id,
        UUID walletId,
        PixKeyType keyType,
        String keyValue,
        Instant createdAt
) {

    public static PixKey newKey(UUID walletId, PixKeyType keyType, String keyValue) {
        return new PixKey(
                UUID.randomUUID(),
                walletId,
                keyType,
                keyValue,
                Instant.now()
        );
    }
}
