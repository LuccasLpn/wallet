package com.br.wallet.infrastructure.persistence.repository;

import com.br.wallet.infrastructure.persistence.entity.PixKeyEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PixKeyJpaRepository extends JpaRepository<PixKeyEntity, UUID> {

    Optional<PixKeyEntity> findByKeyValue(String keyValue);
}
