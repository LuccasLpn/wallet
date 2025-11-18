package com.br.wallet.infrastructure.persistence.adapter;

import com.br.wallet.domain.model.Wallet;
import com.br.wallet.domain.port.WalletRepository;
import com.br.wallet.infrastructure.persistence.entity.WalletEntity;
import com.br.wallet.infrastructure.persistence.repository.WalletJpaRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class WalletRepositoryAdapter implements WalletRepository {

    private final WalletJpaRepository walletRepository;

    public WalletRepositoryAdapter(WalletJpaRepository walletRepository) {
        this.walletRepository = walletRepository;
    }

    @Override
    @Transactional
    public Wallet save(Wallet wallet) {
        WalletEntity entity = WalletEntity.fromDomain(wallet);
        WalletEntity saved = walletRepository.save(entity);
        return saved.toDomain();
    }

    @Override
    public Optional<Wallet> findById(UUID id) {
        return walletRepository.findById(id).map(WalletEntity::toDomain);
    }

    @Override
    public Optional<Wallet> findByIdForUpdate(UUID id) {
        return walletRepository.findByIdForUpdate(id).map(WalletEntity::toDomain);
    }
}
