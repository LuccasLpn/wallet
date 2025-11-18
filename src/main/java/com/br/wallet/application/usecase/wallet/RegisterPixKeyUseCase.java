package com.br.wallet.application.usecase.wallet;

import com.br.wallet.domain.enums.PixKeyType;
import com.br.wallet.domain.model.PixKey;
import com.br.wallet.domain.model.Wallet;
import com.br.wallet.domain.port.PixKeyRepository;
import com.br.wallet.domain.port.WalletRepository;
import com.br.wallet.infrastructure.metrics.PixKeyMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

public class RegisterPixKeyUseCase {

    private static final Logger log = LoggerFactory.getLogger(RegisterPixKeyUseCase.class);

    private final WalletRepository walletRepository;
    private final PixKeyRepository pixKeyRepository;
    private final PixKeyMetrics pixKeyMetrics;

    public RegisterPixKeyUseCase(
            WalletRepository walletRepository,
            PixKeyRepository pixKeyRepository,
            PixKeyMetrics pixKeyMetrics
    ) {
        this.walletRepository = walletRepository;
        this.pixKeyRepository = pixKeyRepository;
        this.pixKeyMetrics = pixKeyMetrics;
    }

    public PixKey execute(UUID walletId, PixKeyType type, String keyValue) {

        long startNs = System.nanoTime();
        String result = "success";
        log.info(
                "RegisterPixKeyUseCase - received request walletId={}, type={}, keyValue={}",
                walletId, type, keyValue
        );
        try {
            Wallet wallet = walletRepository.findById(walletId)
                    .orElseThrow(() -> {
                        log.warn(
                                "RegisterPixKeyUseCase - wallet not found walletId={}",
                                walletId
                        );
                        return new IllegalArgumentException("Wallet not found");
                    });
            pixKeyRepository.findByKeyValue(keyValue).ifPresent(existing -> {
                log.warn(
                        "RegisterPixKeyUseCase - pix key already in use keyValue={}, existingWalletId={}",
                        keyValue, existing.walletId()
                );
                pixKeyMetrics.recordAlreadyInUse(type);
                pixKeyMetrics.recordRegisterError(type, "key_in_use");
                throw new IllegalStateException("Pix key already in use");
            });
            PixKey newKey = PixKey.newKey(wallet.id(), type, keyValue);
            PixKey saved = pixKeyRepository.save(newKey);
            log.info(
                    "RegisterPixKeyUseCase - pix key registered successfully pixKeyId={}, walletId={}, type={}, keyValue={}",
                    saved.id(), saved.walletId(), saved.keyType(), saved.keyValue()
            );

            pixKeyMetrics.recordRegisterSuccess(type);
            return saved;

        } catch (IllegalArgumentException e) {
            pixKeyMetrics.recordRegisterError(type, "wallet_not_found");
            result = "error";
            throw e;
        } catch (IllegalStateException e) {
            if (!"Pix key already in use".equals(e.getMessage())) {
                pixKeyMetrics.recordRegisterError(type, "illegal_state");
            }
            result = "error";
            throw e;
        } catch (Exception e) {
            pixKeyMetrics.recordRegisterError(type, e.getClass().getSimpleName());
            result = "error";
            log.error(
                    "RegisterPixKeyUseCase - unexpected error walletId={}, type={}, keyValue={}",
                    walletId, type, keyValue, e
            );
            throw e;
        } finally {
            pixKeyMetrics.recordDuration(System.nanoTime() - startNs, type, result);
        }
    }
}
