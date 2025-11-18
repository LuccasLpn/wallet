package com.br.wallet.interfaces.rest.wallet.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record BalanceResponse(UUID walletId, BigDecimal balance) {}
