package com.br.wallet.domain.port;

import com.br.wallet.domain.model.LedgerEntry;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public interface LedgerEntryRepository {
    LedgerEntry save(LedgerEntry entry);
    BigDecimal calculateCurrentBalance(UUID walletId);
    BigDecimal calculateBalanceAt(UUID walletId, Instant at);
}
