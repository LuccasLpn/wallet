package com.br.wallet.infrastructure.persistence.adapter;

import com.br.wallet.domain.model.LedgerEntry;
import com.br.wallet.domain.port.LedgerEntryRepository;
import com.br.wallet.infrastructure.persistence.entity.LedgerEntryEntity;
import com.br.wallet.infrastructure.persistence.repository.LedgerEntryJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Component
public class LedgerEntryRepositoryAdapter implements LedgerEntryRepository {

    private final LedgerEntryJpaRepository ledgerEntryJpaRepository;

    public LedgerEntryRepositoryAdapter(LedgerEntryJpaRepository ledgerEntryJpaRepository) {
        this.ledgerEntryJpaRepository = ledgerEntryJpaRepository;
    }

    @Override
    public LedgerEntry save(LedgerEntry entry) {
        return ledgerEntryJpaRepository.save(LedgerEntryEntity.fromDomain(entry)).toDomain();
    }

    @Override
    public BigDecimal calculateCurrentBalance(UUID walletId) {
        return ledgerEntryJpaRepository.calculateCurrentBalance(walletId);
    }

    @Override
    public BigDecimal calculateBalanceAt(UUID walletId, Instant at) {
        return ledgerEntryJpaRepository.calculateBalanceAt(walletId, at);
    }
}

