package com.br.wallet.application.usecase.wallet;

import com.br.wallet.domain.model.Wallet;
import com.br.wallet.domain.port.WalletRepository;
import com.br.wallet.infrastructure.metrics.WalletMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CreateWalletUseCase {

    private static final Logger log = LoggerFactory.getLogger(CreateWalletUseCase.class);

    private final WalletRepository walletRepository;
    private final WalletMetrics walletMetrics;

    public CreateWalletUseCase(
            WalletRepository walletRepository,
            WalletMetrics walletMetrics
    ) {
        this.walletRepository = walletRepository;
        this.walletMetrics = walletMetrics;
    }

    public Wallet execute(String ownerId) {

        long startNs = System.nanoTime();
        String result = "success";

        log.info("CreateWalletUseCase - creating wallet for ownerId={}", ownerId);

        try {
            Wallet wallet = Wallet.newWallet(ownerId);
            Wallet saved = walletRepository.save(wallet);

            log.info(
                    "CreateWalletUseCase - wallet created walletId={}, ownerId={}, createdAt={}",
                    saved.id(), saved.ownerId(), saved.createdAt()
            );

            walletMetrics.recordWalletCreated();
            return saved;

        } catch (Exception e) {
            result = "error";
            walletMetrics.recordWalletError(e.getClass().getSimpleName());
            log.error("CreateWalletUseCase - error creating wallet ownerId={}", ownerId, e);
            throw e;
        } finally {
            walletMetrics.recordDuration(System.nanoTime() - startNs, result);
        }
    }
}

