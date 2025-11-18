package com.br.wallet.infrastructure.persistence.adapter;

import com.br.wallet.domain.model.PixEvent;
import com.br.wallet.domain.port.PixEventRepository;
import com.br.wallet.infrastructure.persistence.entity.PixEventEntity;
import com.br.wallet.infrastructure.persistence.repository.PixEventJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class PixEventRepositoryAdapter implements PixEventRepository {

    private final PixEventJpaRepository pixEventJpaRepository;

    public PixEventRepositoryAdapter(PixEventJpaRepository pixEventJpaRepository) {
        this.pixEventJpaRepository = pixEventJpaRepository;
    }

    @Override
    public PixEvent save(PixEvent event) {
        return pixEventJpaRepository.save(PixEventEntity.fromDomain(event)).toDomain();
    }

    @Override
    public Optional<PixEvent> findByEventId(String eventId) {
        return pixEventJpaRepository.findByEventId(eventId).map(PixEventEntity::toDomain);
    }
}

