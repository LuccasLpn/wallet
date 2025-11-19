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
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.config.StateMachineFactory;
import org.springframework.statemachine.support.DefaultStateMachineContext;

import java.time.Duration;
import java.time.Instant;

public class HandlePixWebhookUseCase {

    private static final Logger log = LoggerFactory.getLogger(HandlePixWebhookUseCase.class);

    private final PixTransferRepository pixTransferRepository;
    private final PixEventRepository pixEventRepository;
    private final PixWebhookMetrics pixWebhookMetrics;
    private final StateMachineFactory<PixTransferStatus, PixEventType> stateMachineFactory;

    public HandlePixWebhookUseCase(
            PixTransferRepository pixTransferRepository,
            PixEventRepository pixEventRepository,
            PixWebhookMetrics pixWebhookMetrics,
            StateMachineFactory<PixTransferStatus, PixEventType> stateMachineFactory
    ) {
        this.pixTransferRepository = pixTransferRepository;
        this.pixEventRepository = pixEventRepository;
        this.pixWebhookMetrics = pixWebhookMetrics;
        this.stateMachineFactory = stateMachineFactory;
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
            StateMachine<PixTransferStatus, PixEventType> stateMachine =
                    stateMachineFactory.getStateMachine(transfer.id().toString());
            stateMachine.stop();
            stateMachine.getStateMachineAccessor()
                    .doWithAllRegions(access ->
                            access.resetStateMachine(
                                    new DefaultStateMachineContext<>(transfer.status(), null, null, null)
                            )
                    );
            stateMachine.getExtendedState().getVariables().put("transfer", transfer);
            stateMachine.getExtendedState().getVariables().put("occurredAt", occurredAt);
            stateMachine.getExtendedState().getVariables().put("eventId", eventId);
            stateMachine.start();
            boolean accepted = stateMachine.sendEvent(type);
            if (!accepted) {
                log.warn(
                        "HandlePixWebhookUseCase - unsupported event transition endToEndId={}, eventId={}, eventType={}, currentStatus={}",
                        endToEndId, eventId, type, transfer.status()
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
