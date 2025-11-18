package com.br.wallet.infrastructure.persistence.entity;

import com.br.wallet.domain.enums.PixTransferStatus;
import com.br.wallet.domain.model.PixTransfer;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "pix_transfers",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_pix_transfers_from_wallet_id_idempotency_key",
                        columnNames = {"from_wallet_id", "idempotency_key"}
                ),
                @UniqueConstraint(
                        name = "uk_pix_transfers_end_to_end_id",
                        columnNames = "end_to_end_id"
                )
        }
)
@Getter
@Setter
public class PixTransferEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "end_to_end_id", nullable = false, updatable = false)
    private String endToEndId;

    @Column(name = "from_wallet_id", nullable = false)
    private UUID fromWalletId;

    @Column(name = "to_wallet_id", nullable = false)
    private UUID toWalletId;

    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private PixTransferStatus status;

    @Column(name = "idempotency_key", nullable = false, updatable = false)
    private String idempotencyKey;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    public PixTransferEntity() {
    }

    public PixTransferEntity(UUID id, String endToEndId, UUID fromWalletId, UUID toWalletId, BigDecimal amount, PixTransferStatus status, String idempotencyKey, Instant createdAt, Instant updatedAt, Long version) {
        this.id = id;
        this.endToEndId = endToEndId;
        this.fromWalletId = fromWalletId;
        this.toWalletId = toWalletId;
        this.amount = amount;
        this.status = status;
        this.idempotencyKey = idempotencyKey;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.version = version;
    }

    public static PixTransferEntity fromDomain(PixTransfer transfer) {
        return new PixTransferEntity(
                transfer.id(),
                transfer.endToEndId(),
                transfer.fromWalletId(),
                transfer.toWalletId(),
                transfer.amount(),
                transfer.status(),
                transfer.idempotencyKey(),
                transfer.createdAt(),
                transfer.updatedAt(),
                transfer.version()
        );
    }

    public PixTransfer toDomain() {
        return new PixTransfer(
                this.id,
                this.endToEndId,
                this.fromWalletId,
                this.toWalletId,
                this.amount,
                this.idempotencyKey,
                this.createdAt,
                this.updatedAt,
                this.version,
                this.status
        );
    }
}
