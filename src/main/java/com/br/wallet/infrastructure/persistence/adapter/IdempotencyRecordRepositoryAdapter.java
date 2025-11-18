package com.br.wallet.infrastructure.persistence.adapter;

import com.br.wallet.domain.model.IdempotencyRecord;
import com.br.wallet.domain.port.IdempotencyRecordRepository;
import com.br.wallet.infrastructure.persistence.entity.IdempotencyRecordEntity;
import com.br.wallet.infrastructure.persistence.repository.IdempotencyRecordJpaRepository;
import org.springframework.stereotype.Component;
import java.util.Optional;

@Component
public class IdempotencyRecordRepositoryAdapter implements IdempotencyRecordRepository {

    private final IdempotencyRecordJpaRepository idempotencyRecordJpaRepository;

    public IdempotencyRecordRepositoryAdapter(IdempotencyRecordJpaRepository idempotencyRecordJpaRepository) {
        this.idempotencyRecordJpaRepository = idempotencyRecordJpaRepository;
    }

    @Override
    public IdempotencyRecord save(IdempotencyRecord record) {
        return idempotencyRecordJpaRepository.save(IdempotencyRecordEntity.fromDomain(record)).toDomain();
    }

    @Override
    public Optional<IdempotencyRecord> findByScopeAndIdempotencyKey(String scope, String key) {
        return idempotencyRecordJpaRepository.findByScopeAndIdempotencyKey(scope, key).map(IdempotencyRecordEntity::toDomain);
    }
}

