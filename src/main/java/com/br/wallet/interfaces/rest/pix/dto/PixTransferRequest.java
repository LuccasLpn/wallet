package com.br.wallet.interfaces.rest.pix.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record PixTransferRequest(
        UUID fromWalletId,
        String toPixKey,
        BigDecimal amount
) {}
