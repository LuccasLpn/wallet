package com.br.wallet.infrastructure.persistence.adapter;

import com.br.wallet.domain.model.PixTransfer;
import com.br.wallet.domain.port.PixTransferRepository;
import com.br.wallet.infrastructure.persistence.entity.PixTransferEntity;
import com.br.wallet.infrastructure.persistence.repository.PixTransferJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class PixTransferRepositoryAdapter implements PixTransferRepository {

    private final PixTransferJpaRepository pixTransferJpaRepository;

    public PixTransferRepositoryAdapter(PixTransferJpaRepository pixTransferJpaRepository) {
        this.pixTransferJpaRepository = pixTransferJpaRepository;
    }

    @Override
    public PixTransfer save(PixTransfer transfer) {
        return pixTransferJpaRepository.save(PixTransferEntity.fromDomain(transfer)).toDomain();
    }

    @Override
    public Optional<PixTransfer> findByEndToEndId(String endToEndId) {
        return pixTransferJpaRepository.findByEndToEndId(endToEndId).map(PixTransferEntity::toDomain);
    }

    @Override
    public Optional<PixTransfer> findByFromWalletIdAndIdempotencyKey(UUID fromWalletId, String key) {
        return pixTransferJpaRepository.findByFromWalletIdAndIdempotencyKey(fromWalletId, key)
                .map(PixTransferEntity::toDomain);
    }
}

