package com.br.wallet.application.usecase.pix;

import com.br.wallet.domain.enums.LedgerEntryDirection;
import com.br.wallet.domain.enums.LedgerOperationType;
import com.br.wallet.domain.model.LedgerEntry;
import com.br.wallet.domain.model.PixKey;
import com.br.wallet.domain.model.PixTransfer;
import com.br.wallet.domain.model.Wallet;
import com.br.wallet.domain.port.LedgerEntryRepository;
import com.br.wallet.domain.port.PixKeyRepository;
import com.br.wallet.domain.port.PixTransferRepository;
import com.br.wallet.domain.port.WalletRepository;
import com.br.wallet.infrastructure.metrics.PixMetrics;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class CreatePixTransferUseCase {

    private static final Logger log = LoggerFactory.getLogger(CreatePixTransferUseCase.class);

    private final WalletRepository walletRepository;
    private final PixKeyRepository pixKeyRepository;
    private final PixTransferRepository pixTransferRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final PixMetrics pixMetrics;

    public CreatePixTransferUseCase(
            WalletRepository walletRepository,
            PixKeyRepository pixKeyRepository,
            PixTransferRepository pixTransferRepository,
            LedgerEntryRepository ledgerEntryRepository,
            PixMetrics pixMetrics
    ) {
        this.walletRepository = walletRepository;
        this.pixKeyRepository = pixKeyRepository;
        this.pixTransferRepository = pixTransferRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.pixMetrics = pixMetrics;
    }

    @Transactional
    public PixTransfer execute(UUID fromWalletId, String toPixKey, BigDecimal amount, String idempotencyKey) {
        long startNanos = System.nanoTime();
        pixMetrics.onPixTransferRequested();

        log.info(
                "CreatePixTransferUseCase - starting PIX transfer fromWalletId={}, toPixKey={}, amount={}, idempotencyKey={}",
                fromWalletId, toPixKey, amount, idempotencyKey
        );

        try {
            PixKey key = pixKeyRepository.findByKeyValue(toPixKey)
                    .orElseThrow(() -> {
                        log.warn("CreatePixTransferUseCase - pix key not found toPixKey={}", toPixKey);
                        return new IllegalArgumentException("Pix key not found");
                    });

            UUID toWalletId = key.walletId();

            log.info(
                    "CreatePixTransferUseCase - pix key found toPixKey={}, toWalletId={}",
                    toPixKey, toWalletId
            );

            var existing = pixTransferRepository.findByFromWalletIdAndIdempotencyKey(fromWalletId, idempotencyKey);
            if (existing.isPresent()) {
                PixTransfer found = existing.get();
                pixMetrics.onIdempotencyHit();
                log.info(
                        "CreatePixTransferUseCase - idempotency hit, returning existing transfer transferId={}, endToEndId={}, fromWalletId={}, toWalletId={}",
                        found.id(), found.endToEndId(), found.fromWalletId(), found.toWalletId()
                );
                return found;
            }
            pixMetrics.onIdempotencyMiss();
            Wallet fromWallet = walletRepository.findByIdForUpdate(fromWalletId)
                    .orElseThrow(() -> {
                        log.warn("CreatePixTransferUseCase - wallet not found fromWalletId={}", fromWalletId);
                        return new IllegalArgumentException("Wallet not found");
                    });
            BigDecimal balance = ledgerEntryRepository.calculateCurrentBalance(fromWallet.id());
            log.info(
                    "CreatePixTransferUseCase - wallet loaded walletId={}, currentBalance={}, requestedAmount={}",
                    fromWallet.id(), balance, amount
            );
            if (balance.compareTo(amount) < 0) {
                pixMetrics.onInsufficientFunds();
                log.warn(
                        "CreatePixTransferUseCase - insufficient funds walletId={}, balance={}, requestedAmount={}",
                        fromWallet.id(), balance, amount
                );
                throw new IllegalStateException("Insufficient funds");
            }
            String endToEndId = "E2E-" + UUID.randomUUID();
            log.info(
                    "CreatePixTransferUseCase - creating pending transfer endToEndId={}, fromWalletId={}, toWalletId={}, amount={}",
                    endToEndId, fromWalletId, toWalletId, amount
            );
            PixTransfer transfer = PixTransfer.newPending(
                    fromWalletId,
                    toWalletId,
                    amount,
                    endToEndId,
                    idempotencyKey
            );
            PixTransfer saved = pixTransferRepository.save(transfer);
            pixMetrics.onPixTransferCreated();
            log.info(
                    "CreatePixTransferUseCase - pending transfer created transferId={}, endToEndId={}",
                    saved.id(), saved.endToEndId()
            );
            LedgerEntry debit = LedgerEntry.newEntry(
                    fromWallet.id(),
                    LedgerOperationType.PIX_DEBIT,
                    LedgerEntryDirection.DEBIT,
                    amount,
                    saved.id().toString(),
                    saved.endToEndId(),
                    Instant.now()
            );
            ledgerEntryRepository.save(debit);
            log.info(
                    "CreatePixTransferUseCase - ledger debit entry created walletId={}, amount={}, transferId={}, endToEndId={}",
                    fromWallet.id(), amount, saved.id(), saved.endToEndId()
            );
            log.info(
                    "CreatePixTransferUseCase - PIX transfer completed successfully transferId={}, endToEndId={}",
                    saved.id(), saved.endToEndId()
            );
            return saved;
        } finally {
            pixMetrics.recordProcessingTime(startNanos);
        }
    }
}
