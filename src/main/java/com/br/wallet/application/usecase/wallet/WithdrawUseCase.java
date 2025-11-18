package com.br.wallet.application.usecase.wallet;

import com.br.wallet.domain.enums.LedgerEntryDirection;
import com.br.wallet.domain.enums.LedgerOperationType;
import com.br.wallet.domain.model.LedgerEntry;
import com.br.wallet.domain.model.Wallet;
import com.br.wallet.domain.port.LedgerEntryRepository;
import com.br.wallet.domain.port.WalletRepository;
import com.br.wallet.infrastructure.metrics.WithdrawMetrics;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class WithdrawUseCase {

    private static final Logger log = LoggerFactory.getLogger(WithdrawUseCase.class);

    private final WalletRepository walletRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final WithdrawMetrics withdrawMetrics;

    public WithdrawUseCase(
            WalletRepository walletRepository,
            LedgerEntryRepository ledgerEntryRepository,
            WithdrawMetrics withdrawMetrics
    ) {
        this.walletRepository = walletRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.withdrawMetrics = withdrawMetrics;
    }

    @Transactional
    public LedgerEntry execute(UUID walletId, BigDecimal amount) {
        long startNs = System.nanoTime();
        String result = "success";
        log.info(
                "WithdrawUseCase - request received walletId={}, amount={}",
                walletId, amount
        );
        try {
            Wallet wallet = walletRepository.findByIdForUpdate(walletId)
                    .orElseThrow(() -> {
                        log.warn(
                                "WithdrawUseCase - wallet not found walletId={}",
                                walletId
                        );
                        return new IllegalArgumentException("Wallet not found");
                    });
            BigDecimal balance = ledgerEntryRepository.calculateCurrentBalance(wallet.id());
            if (balance.compareTo(amount) < 0) {
                log.warn(
                        "WithdrawUseCase - insufficient funds walletId={}, balance={}, requestedAmount={}",
                        wallet.id(), balance, amount
                );
                withdrawMetrics.recordInsufficientFunds();
                withdrawMetrics.recordWithdrawError("insufficient_funds");
                result = "error";
                throw new IllegalStateException("Insufficient funds");
            }
            LedgerEntry entry = LedgerEntry.newEntry(
                    wallet.id(),
                    LedgerOperationType.WITHDRAW,
                    LedgerEntryDirection.DEBIT,
                    amount,
                    null,
                    null,
                    Instant.now()
            );
            ledgerEntryRepository.save(entry);
            log.info(
                    "WithdrawUseCase - withdraw ledger entry created walletId={}, amount={}, ledgerEntryId={}",
                    wallet.id(), amount, entry.id()
            );
            withdrawMetrics.recordWithdrawSuccess(amount);
            return entry;
        } catch (IllegalArgumentException e) {
            withdrawMetrics.recordWithdrawError("wallet_not_found");
            result = "error";
            throw e;
        } catch (IllegalStateException e) {
            if (!"Insufficient funds".equals(e.getMessage())) {
                withdrawMetrics.recordWithdrawError("illegal_state");
            }
            result = "error";
            throw e;
        } catch (Exception e) {
            withdrawMetrics.recordWithdrawError(e.getClass().getSimpleName());
            result = "error";
            log.error("WithdrawUseCase - unexpected error walletId={}, amount={}", walletId, amount, e);
            throw e;
        } finally {
            withdrawMetrics.recordDuration(System.nanoTime() - startNs, result);
        }
    }
}
