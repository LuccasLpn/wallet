package com.br.wallet.infrastructure.persistence.entity;

import com.br.wallet.domain.enums.LedgerEntryDirection;
import com.br.wallet.domain.enums.LedgerOperationType;
import com.br.wallet.domain.model.LedgerEntry;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "ledger_entries",
        indexes = {
                @Index(name = "idx_ledger_entries_wallet_id", columnList = "wallet_id"),
                @Index(name = "idx_ledger_entries_wallet_id_occurred_at", columnList = "wallet_id, occurred_at"),
                @Index(name = "idx_ledger_entries_end_to_end_id", columnList = "end_to_end_id")
        }
)
@Getter
@Setter
public class LedgerEntryEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "wallet_id", nullable = false)
    private UUID walletId;

    @Enumerated(EnumType.STRING)
    @Column(name = "operation_type", nullable = false)
    private LedgerOperationType operationType;

    @Enumerated(EnumType.STRING)
    @Column(name = "direction", nullable = false)
    private LedgerEntryDirection direction;

    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "reference_id")
    private String referenceId;

    @Column(name = "end_to_end_id")
    private String endToEndId;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public LedgerEntryEntity() {
    }

    public LedgerEntryEntity(UUID id, UUID walletId, LedgerOperationType operationType, LedgerEntryDirection direction, BigDecimal amount, String referenceId, String endToEndId, Instant occurredAt, Instant createdAt) {
        this.id = id;
        this.walletId = walletId;
        this.operationType = operationType;
        this.direction = direction;
        this.amount = amount;
        this.referenceId = referenceId;
        this.endToEndId = endToEndId;
        this.occurredAt = occurredAt;
        this.createdAt = createdAt;
    }

    public static LedgerEntryEntity fromDomain(LedgerEntry entry) {
        return new LedgerEntryEntity(
                entry.id(),
                entry.walletId(),
                entry.operationType(),
                entry.direction(),
                entry.amount(),
                entry.referenceId(),
                entry.endToEndId(),
                entry.occurredAt(),
                entry.createdAt()
        );
    }

    public LedgerEntry toDomain() {
        return new LedgerEntry(
                this.id,
                this.walletId,
                this.operationType,
                this.direction,
                this.amount,
                this.referenceId,
                this.endToEndId,
                this.occurredAt,
                this.createdAt
        );
    }
}
