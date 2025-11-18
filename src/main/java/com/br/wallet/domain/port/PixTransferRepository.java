package com.br.wallet.domain.port;

import com.br.wallet.domain.model.PixTransfer;

import java.util.Optional;
import java.util.UUID;

public interface PixTransferRepository {
    PixTransfer save(PixTransfer transfer);
    Optional<PixTransfer> findByEndToEndId(String endToEndId);
    Optional<PixTransfer> findByFromWalletIdAndIdempotencyKey(UUID fromWalletId, String idempotencyKey);
}
