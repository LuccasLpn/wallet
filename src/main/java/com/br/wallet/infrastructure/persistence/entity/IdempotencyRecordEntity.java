package com.br.wallet.infrastructure.persistence.entity;

import com.br.wallet.domain.model.IdempotencyRecord;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "idempotency_records",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_idempotency_scope_key",
                        columnNames = {"scope", "idempotency_key"}
                )
        }
)
@Getter
@Setter
public class IdempotencyRecordEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "scope", nullable = false, length = 50)
    private String scope;

    @Column(name = "idempotency_key", nullable = false, length = 100)
    private String idempotencyKey;

    @Lob
    @Column(name = "response_payload", nullable = false)
    private String responsePayload;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public IdempotencyRecordEntity() {
    }

    public IdempotencyRecordEntity(UUID id, String scope, String idempotencyKey, String responsePayload, Instant createdAt) {
        this.id = id;
        this.scope = scope;
        this.idempotencyKey = idempotencyKey;
        this.responsePayload = responsePayload;
        this.createdAt = createdAt;
    }

    public static IdempotencyRecordEntity fromDomain(IdempotencyRecord record) {
        return new IdempotencyRecordEntity(
                record.id(),
                record.scope(),
                record.idempotencyKey(),
                record.responsePayload(),
                record.createdAt()
        );
    }

    public IdempotencyRecord toDomain() {
        return new IdempotencyRecord(
                this.id,
                this.scope,
                this.idempotencyKey,
                this.responsePayload,
                this.createdAt
        );
    }
}
