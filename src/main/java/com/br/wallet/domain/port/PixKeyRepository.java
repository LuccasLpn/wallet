package com.br.wallet.domain.port;

import com.br.wallet.domain.model.PixKey;

import java.util.Optional;

public interface PixKeyRepository {
    PixKey save(PixKey pixKey);
    Optional<PixKey> findByKeyValue(String keyValue);
}
