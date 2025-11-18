package com.br.wallet.infrastructure.persistence.entity;

import com.br.wallet.domain.enums.PixKeyType;
import com.br.wallet.domain.model.PixKey;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "pix_keys",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_pix_keys_key_value", columnNames = "key_value")
        }
)
@Getter
@Setter
public class PixKeyEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "wallet_id", nullable = false)
    private UUID walletId;

    @Enumerated(EnumType.STRING)
    @Column(name = "key_type", nullable = false)
    private PixKeyType keyType;

    @Column(name = "key_value", nullable = false)
    private String keyValue;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public PixKeyEntity() {
    }

    public PixKeyEntity(UUID id, UUID walletId, PixKeyType keyType, String keyValue, Instant createdAt) {
        this.id = id;
        this.walletId = walletId;
        this.keyType = keyType;
        this.keyValue = keyValue;
        this.createdAt = createdAt;
    }

    public static PixKeyEntity fromDomain(PixKey key) {
        return new PixKeyEntity(
                key.id(),
                key.walletId(),
                key.keyType(),
                key.keyValue(),
                key.createdAt()
        );
    }

    public PixKey toDomain() {
        return new PixKey(
                this.id,
                this.walletId,
                this.keyType,
                this.keyValue,
                this.createdAt
        );
    }
}
