package com.br.wallet.application.usecase.pix;

import com.br.wallet.domain.enums.LedgerEntryDirection;
import com.br.wallet.domain.enums.LedgerOperationType;
import com.br.wallet.domain.enums.PixEventType;
import com.br.wallet.domain.enums.PixTransferStatus;
import com.br.wallet.domain.model.LedgerEntry;
import com.br.wallet.domain.model.PixEvent;
import com.br.wallet.domain.model.PixTransfer;
import com.br.wallet.domain.port.LedgerEntryRepository;
import com.br.wallet.domain.port.PixEventRepository;
import com.br.wallet.domain.port.PixTransferRepository;
import com.br.wallet.infrastructure.metrics.PixWebhookMetrics;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;

public class HandlePixWebhookUseCase {

    private static final Logger log = LoggerFactory.getLogger(HandlePixWebhookUseCase.class);

    private final PixTransferRepository pixTransferRepository;
    private final PixEventRepository pixEventRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final PixWebhookMetrics pixWebhookMetrics;

    public HandlePixWebhookUseCase(
            PixTransferRepository pixTransferRepository,
            PixEventRepository pixEventRepository,
            LedgerEntryRepository ledgerEntryRepository,
            PixWebhookMetrics pixWebhookMetrics
    ) {
        this.pixTransferRepository = pixTransferRepository;
        this.pixEventRepository = pixEventRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.pixWebhookMetrics = pixWebhookMetrics;
    }

    @Transactional
    public void execute(String endToEndId, String eventId, PixEventType type, Instant occurredAt) {
        long startNs = System.nanoTime();
        String resultTag = "success";
        pixWebhookMetrics.recordWebhookReceived(type);
        try {
            log.info(
                    "HandlePixWebhookUseCase - received webhook endToEndId={}, eventId={}, eventType={}, occurredAt={}",
                    endToEndId, eventId, type, occurredAt
            );
            if (pixEventRepository.findByEventId(eventId).isPresent()) {
                log.warn(
                        "HandlePixWebhookUseCase - duplicate event ignored endToEndId={}, eventId={}, eventType={}",
                        endToEndId, eventId, type
                );
                pixWebhookMetrics.recordDuplicateEvent(type);
                return;
            }
            PixTransfer transfer = pixTransferRepository.findByEndToEndId(endToEndId)
                    .orElseThrow(() -> {
                        log.warn(
                                "HandlePixWebhookUseCase - transfer not found for endToEndId={}, eventId={}, eventType={}",
                                endToEndId, eventId, type
                        );
                        pixWebhookMetrics.recordTransferNotFound(type);
                        return new IllegalArgumentException("Transfer not found");
                    });
            pixWebhookMetrics.recordWebhookLatency(
                    type,
                    Duration.between(occurredAt, Instant.now())
            );
            PixEvent event = PixEvent.newEvent(eventId, endToEndId, type, occurredAt);
            pixEventRepository.save(event);
            log.info(
                    "HandlePixWebhookUseCase - event persisted endToEndId={}, eventId={}, eventType={}",
                    endToEndId, eventId, type
            );
            if (transfer.status() == PixTransferStatus.CONFIRMED
                    || transfer.status() == PixTransferStatus.REJECTED) {
                log.info(
                        "HandlePixWebhookUseCase - transfer already finalized, ignoring event endToEndId={}, currentStatus={}, eventId={}, eventType={}",
                        endToEndId, transfer.status(), eventId, type
                );
                pixWebhookMetrics.recordIgnoredFinalized(type, transfer.status());
                return;
            }
            if (type == PixEventType.CONFIRMED) {
                LedgerEntry credit = LedgerEntry.newEntry(
                        transfer.toWalletId(),
                        LedgerOperationType.PIX_CREDIT,
                        LedgerEntryDirection.CREDIT,
                        transfer.amount(),
                        transfer.id().toString(),
                        transfer.endToEndId(),
                        occurredAt
                );
                ledgerEntryRepository.save(credit);
                log.info(
                        "HandlePixWebhookUseCase - pix credit applied walletId={}, amount={}, endToEndId={}, eventId={}, ledgerEntryId={}",
                        transfer.toWalletId(), transfer.amount(), endToEndId, eventId, credit.id()
                );
                PixTransfer updated = transfer.markConfirmed();
                pixTransferRepository.save(updated);

                log.info(
                        "HandlePixWebhookUseCase - transfer marked as CONFIRMED endToEndId={}, transferId={}",
                        endToEndId, updated.id()
                );
                pixWebhookMetrics.recordAmount(PixTransferStatus.CONFIRMED, transfer.amount());
                if (transfer.createdAt() != null) {
                    pixWebhookMetrics.recordSettlementDuration(
                            PixTransferStatus.CONFIRMED,
                            Duration.between(transfer.createdAt(), occurredAt)
                    );
                }
            } else if (type == PixEventType.REJECTED) {
                LedgerEntry reversal = LedgerEntry.newEntry(
                        transfer.fromWalletId(),
                        LedgerOperationType.PIX_REVERSAL,
                        LedgerEntryDirection.CREDIT,
                        transfer.amount(),
                        transfer.id().toString(),
                        transfer.endToEndId(),
                        occurredAt
                );
                ledgerEntryRepository.save(reversal);
                log.info(
                        "HandlePixWebhookUseCase - pix reversal applied walletId={}, amount={}, endToEndId={}, eventId={}, ledgerEntryId={}",
                        transfer.fromWalletId(), transfer.amount(), endToEndId, eventId, reversal.id()
                );
                PixTransfer updated = transfer.markRejected();
                pixTransferRepository.save(updated);
                log.info(
                        "HandlePixWebhookUseCase - transfer marked as REJECTED endToEndId={}, transferId={}",
                        endToEndId, updated.id()
                );
                pixWebhookMetrics.recordAmount(PixTransferStatus.REJECTED, transfer.amount());
                if (transfer.createdAt() != null) {
                    pixWebhookMetrics.recordSettlementDuration(
                            PixTransferStatus.REJECTED,
                            Duration.between(transfer.createdAt(), occurredAt)
                    );
                }

            } else {
                log.warn(
                        "HandlePixWebhookUseCase - unsupported event type received endToEndId={}, eventId={}, eventType={}",
                        endToEndId, eventId, type
                );
                pixWebhookMetrics.recordUnsupportedType(type);
            }
        } catch (Exception e) {
            resultTag = "error";
            pixWebhookMetrics.recordError(type);
            throw e;
        } finally {
            pixWebhookMetrics.recordProcessingDuration(type, resultTag, System.nanoTime() - startNs);
        }
    }

}

