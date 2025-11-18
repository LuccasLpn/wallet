package com.br.wallet.interfaces.rest.wallet.dto;

import com.br.wallet.domain.enums.PixKeyType;
import java.time.Instant;
import java.util.UUID;

public record PixKeyResponse(
        UUID id,
        UUID walletId,
        PixKeyType type,
        String keyValue,
        Instant createdAt
) {}
