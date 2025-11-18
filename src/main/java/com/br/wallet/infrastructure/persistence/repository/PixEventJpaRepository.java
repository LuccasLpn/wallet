package com.br.wallet.infrastructure.persistence.repository;

import com.br.wallet.infrastructure.persistence.entity.PixEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PixEventJpaRepository extends JpaRepository<PixEventEntity, UUID> {

    Optional<PixEventEntity> findByEventId(String eventId);
}
