package com.br.wallet.domain.port;

import com.br.wallet.domain.model.Wallet;
import java.util.Optional;
import java.util.UUID;

public interface WalletRepository {
    Wallet save(Wallet wallet);
    Optional<Wallet> findById(UUID id);
    Optional<Wallet> findByIdForUpdate(UUID id);
}
