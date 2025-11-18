package com.br.wallet.infrastructure.persistence.repository;

import com.br.wallet.infrastructure.persistence.entity.IdempotencyRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface IdempotencyRecordJpaRepository extends JpaRepository<IdempotencyRecordEntity, UUID> {

    Optional<IdempotencyRecordEntity> findByScopeAndIdempotencyKey(String scope, String idempotencyKey);
}
