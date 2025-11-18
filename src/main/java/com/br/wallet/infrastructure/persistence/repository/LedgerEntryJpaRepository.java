package com.br.wallet.infrastructure.persistence.repository;

import com.br.wallet.infrastructure.persistence.entity.LedgerEntryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface LedgerEntryJpaRepository extends JpaRepository<LedgerEntryEntity, UUID> {

    List<LedgerEntryEntity> findByWalletId(UUID walletId);

    List<LedgerEntryEntity> findByWalletIdAndOccurredAtLessThanEqual(UUID walletId, Instant at);

    @Query("""
            select coalesce(
              sum(
                case when e.direction = 'CREDIT' then e.amount else -e.amount end
              ),
              0
            )
            from LedgerEntryEntity e
            where e.walletId = :walletId
            """)
    BigDecimal calculateCurrentBalance(UUID walletId);

    @Query("""
            select coalesce(
              sum(
                case when e.direction = 'CREDIT' then e.amount else -e.amount end
              ),
              0
            )
            from LedgerEntryEntity e
            where e.walletId = :walletId
              and e.occurredAt <= :at
            """)
    BigDecimal calculateBalanceAt(UUID walletId, Instant at);
}

