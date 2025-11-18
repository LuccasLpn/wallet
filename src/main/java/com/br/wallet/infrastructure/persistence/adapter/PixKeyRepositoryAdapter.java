package com.br.wallet.infrastructure.persistence.adapter;

import com.br.wallet.domain.model.PixKey;
import com.br.wallet.domain.port.PixKeyRepository;
import com.br.wallet.infrastructure.persistence.entity.PixKeyEntity;
import com.br.wallet.infrastructure.persistence.repository.PixKeyJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class PixKeyRepositoryAdapter implements PixKeyRepository {

    private final PixKeyJpaRepository pixKeyJpaRepository;

    public PixKeyRepositoryAdapter(PixKeyJpaRepository pixKeyJpaRepository) {
        this.pixKeyJpaRepository = pixKeyJpaRepository;
    }

    @Override
    public PixKey save(PixKey pixKey) {
        return pixKeyJpaRepository.save(PixKeyEntity.fromDomain(pixKey)).toDomain();
    }

    @Override
    public Optional<PixKey> findByKeyValue(String keyValue) {
        return pixKeyJpaRepository.findByKeyValue(keyValue).map(PixKeyEntity::toDomain);
    }
}

