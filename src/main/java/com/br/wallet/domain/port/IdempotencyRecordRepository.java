package com.br.wallet.domain.port;

import com.br.wallet.domain.model.IdempotencyRecord;

import java.util.Optional;

public interface IdempotencyRecordRepository {
    IdempotencyRecord save(IdempotencyRecord record);
    Optional<IdempotencyRecord> findByScopeAndIdempotencyKey(String scope, String key);
}
