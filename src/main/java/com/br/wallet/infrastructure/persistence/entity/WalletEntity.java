package com.br.wallet.infrastructure.persistence.entity;

import com.br.wallet.domain.model.Wallet;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "wallets")
@Getter
@Setter
public class WalletEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "owner_id", nullable = false, unique = true)
    private String ownerId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public WalletEntity(UUID id, String ownerId, Instant createdAt) {
        this.id = id;
        this.ownerId = ownerId;
        this.createdAt = createdAt;
    }

    public WalletEntity() {
    }

    public static WalletEntity fromDomain(Wallet wallet) {
        return new WalletEntity(
                wallet.id(),
                wallet.ownerId(),
                wallet.createdAt()
        );
    }

    public Wallet toDomain() {
        return new Wallet(
                this.id,
                this.ownerId,
                this.createdAt
                );
    }
}
