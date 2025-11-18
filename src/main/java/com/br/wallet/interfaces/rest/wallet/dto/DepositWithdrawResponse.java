package com.br.wallet.interfaces.rest.wallet.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record DepositWithdrawResponse(
        UUID walletId,
        UUID ledgerEntryId,
        BigDecimal amount,
        BigDecimal balanceAfter,
        String operationType,
        String status,
        Instant processedAt
) {}
