package com.br.wallet.application.usecase.wallet;

import com.br.wallet.domain.port.LedgerEntryRepository;
import com.br.wallet.infrastructure.metrics.WalletBalanceMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class GetWalletBalanceUseCase {

    private static final Logger log = LoggerFactory.getLogger(GetWalletBalanceUseCase.class);

    private final LedgerEntryRepository ledgerEntryRepository;
    private final WalletBalanceMetrics walletBalanceMetrics;

    public GetWalletBalanceUseCase(
            LedgerEntryRepository ledgerEntryRepository,
            WalletBalanceMetrics walletBalanceMetrics
    ) {
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.walletBalanceMetrics = walletBalanceMetrics;
    }

    public BigDecimal currentBalance(UUID walletId) {

        long startNs = System.nanoTime();
        String type = "current";
        String result = "success";
        log.info("GetWalletBalanceUseCase - calculating current balance for walletId={}", walletId);
        try {
            BigDecimal balance = ledgerEntryRepository.calculateCurrentBalance(walletId);
            log.info(
                    "GetWalletBalanceUseCase - current balance calculated walletId={}, balance={}",
                    walletId, balance
            );
            walletBalanceMetrics.recordRequest(type);
            walletBalanceMetrics.recordValue(balance, type);
            return balance;
        } catch (Exception e) {
            result = "error";
            walletBalanceMetrics.recordError(type, e.getClass().getSimpleName());
            log.error("GetWalletBalanceUseCase - error calculating current balance walletId={}", walletId, e);
            throw e;
        } finally {
            walletBalanceMetrics.recordDuration(System.nanoTime() - startNs, type, result);
        }
    }

    public BigDecimal balanceAt(UUID walletId, Instant at) {
        long startNs = System.nanoTime();
        String type = "historical";
        String result = "success";
        log.info(
                "GetWalletBalanceUseCase - calculating historical balance walletId={}, at={}",
                walletId, at
        );
        try {
            BigDecimal balance = ledgerEntryRepository.calculateBalanceAt(walletId, at);

            log.info(
                    "GetWalletBalanceUseCase - historical balance calculated walletId={}, at={}, balance={}",
                    walletId, at, balance
            );
            walletBalanceMetrics.recordRequest(type);
            walletBalanceMetrics.recordValue(balance, type);
            return balance;
        } catch (Exception e) {
            result = "error";
            walletBalanceMetrics.recordError(type, e.getClass().getSimpleName());
            log.error("GetWalletBalanceUseCase - error calculating historical balance walletId={}, at={}", walletId, at, e);
            throw e;
        } finally {
            walletBalanceMetrics.recordDuration(System.nanoTime() - startNs, type, result);
        }
    }
}
