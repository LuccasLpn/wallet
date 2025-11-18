package com.br.wallet.application.usecase.wallet;

import com.br.wallet.domain.enums.LedgerEntryDirection;
import com.br.wallet.domain.enums.LedgerOperationType;
import com.br.wallet.domain.model.LedgerEntry;
import com.br.wallet.domain.model.Wallet;
import com.br.wallet.domain.port.LedgerEntryRepository;
import com.br.wallet.domain.port.WalletRepository;
import com.br.wallet.infrastructure.metrics.DepositMetrics;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class DepositUseCase {

    private static final Logger log = LoggerFactory.getLogger(DepositUseCase.class);

    private final WalletRepository walletRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final DepositMetrics depositMetrics;

    public DepositUseCase(
            WalletRepository walletRepository,
            LedgerEntryRepository ledgerEntryRepository,
            DepositMetrics depositMetrics
    ) {
        this.walletRepository = walletRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.depositMetrics = depositMetrics;
    }

    @Transactional
    public LedgerEntry execute(UUID walletId, BigDecimal amount) {
        long startNs = System.nanoTime();
        String result = "success";

        log.info(
                "DepositUseCase - request received walletId={}, amount={}",
                walletId, amount
        );

        try {
            Wallet wallet = walletRepository.findById(walletId)
                    .orElseThrow(() -> {
                        log.warn("DepositUseCase - wallet not found walletId={}", walletId);
                        depositMetrics.recordDepositError("wallet_not_found");
                        return new IllegalArgumentException("Wallet not found");
                    });

            LedgerEntry entry = LedgerEntry.newEntry(
                    wallet.id(),
                    LedgerOperationType.DEPOSIT,
                    LedgerEntryDirection.CREDIT,
                    amount,
                    null,
                    null,
                    Instant.now()
            );
            ledgerEntryRepository.save(entry);
            log.info(
                    "DepositUseCase - deposit ledger entry created walletId={}, amount={}, ledgerEntryId={}",
                    wallet.id(), amount, entry.id()
            );
            depositMetrics.recordDepositSuccess(amount);
            return entry;
        } catch (Exception e) {
            depositMetrics.recordDepositError(e.getClass().getSimpleName());
            result = "error";
            log.error("DepositUseCase - error processing deposit walletId={}, amount={}", walletId, amount, e);
            throw e;
        } finally {
            depositMetrics.recordDuration(System.nanoTime() - startNs, result);
        }
    }
}
