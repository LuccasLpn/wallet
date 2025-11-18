package com.br.wallet.infrastructure.persistence.repository;

import com.br.wallet.infrastructure.persistence.entity.PixTransferEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PixTransferJpaRepository extends JpaRepository<PixTransferEntity, UUID> {

    Optional<PixTransferEntity> findByEndToEndId(String endToEndId);

    Optional<PixTransferEntity> findByFromWalletIdAndIdempotencyKey(UUID fromWalletId, String idempotencyKey);
}
