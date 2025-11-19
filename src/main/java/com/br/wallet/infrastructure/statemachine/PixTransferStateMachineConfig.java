package com.br.wallet.infrastructure.statemachine;

import com.br.wallet.domain.enums.PixEventType;
import com.br.wallet.domain.enums.PixTransferStatus;
import com.br.wallet.domain.enums.LedgerEntryDirection;
import com.br.wallet.domain.enums.LedgerOperationType;
import com.br.wallet.domain.model.LedgerEntry;
import com.br.wallet.domain.model.PixTransfer;
import com.br.wallet.domain.port.LedgerEntryRepository;
import com.br.wallet.domain.port.PixTransferRepository;
import com.br.wallet.infrastructure.metrics.PixWebhookMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;
import org.springframework.statemachine.config.EnableStateMachineFactory;
import org.springframework.statemachine.config.StateMachineConfigurerAdapter;
import org.springframework.statemachine.config.builders.StateMachineConfigurationConfigurer;
import org.springframework.statemachine.config.builders.StateMachineStateConfigurer;
import org.springframework.statemachine.config.builders.StateMachineTransitionConfigurer;

import java.time.Duration;
import java.time.Instant;

@Configuration
@EnableStateMachineFactory
public class PixTransferStateMachineConfig
        extends StateMachineConfigurerAdapter<PixTransferStatus, PixEventType> {

    private static final Logger log = LoggerFactory.getLogger(PixTransferStateMachineConfig.class);

    private final PixTransferRepository pixTransferRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final PixWebhookMetrics pixWebhookMetrics;

    public PixTransferStateMachineConfig(
            PixTransferRepository pixTransferRepository,
            LedgerEntryRepository ledgerEntryRepository,
            PixWebhookMetrics pixWebhookMetrics
    ) {
        this.pixTransferRepository = pixTransferRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.pixWebhookMetrics = pixWebhookMetrics;
    }

    @Override
    public void configure(StateMachineConfigurationConfigurer<PixTransferStatus, PixEventType> config)
            throws Exception {
        config
                .withConfiguration()
                .autoStartup(false);
    }

    @Override
    public void configure(StateMachineStateConfigurer<PixTransferStatus, PixEventType> states)
            throws Exception {
        states
                .withStates()
                .initial(PixTransferStatus.PENDING)
                .state(PixTransferStatus.CONFIRMED)
                .state(PixTransferStatus.REJECTED);
    }

    @Override
    public void configure(StateMachineTransitionConfigurer<PixTransferStatus, PixEventType> transitions)
            throws Exception {
        transitions
                .withExternal()
                .source(PixTransferStatus.PENDING)
                .target(PixTransferStatus.CONFIRMED)
                .event(PixEventType.CONFIRMED)
                .action(confirmPixAction())
                .and()
                .withExternal()
                .source(PixTransferStatus.PENDING)
                .target(PixTransferStatus.REJECTED)
                .event(PixEventType.REJECTED)
                .action(rejectPixAction());
    }

    @Bean
    public Action<PixTransferStatus, PixEventType> confirmPixAction() {
        return context -> {
            PixTransfer transfer = getTransferFromContext(context);
            Instant occurredAt = getOccurredAtFromContext(context);
            String eventId = getEventIdFromContext(context);

            log.info(
                    "PixStateMachine - CONFIRM action started endToEndId={}, transferId={}, eventId={}",
                    transfer.endToEndId(), transfer.id(), eventId
            );

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
                    "PixStateMachine - pix credit applied walletId={}, amount={}, endToEndId={}, eventId={}, ledgerEntryId={}",
                    transfer.toWalletId(), transfer.amount(), transfer.endToEndId(), eventId, credit.id()
            );

            PixTransfer updated = transfer.markConfirmed();
            pixTransferRepository.save(updated);

            log.info(
                    "PixStateMachine - transfer marked as CONFIRMED endToEndId={}, transferId={}",
                    transfer.endToEndId(), updated.id()
            );

            pixWebhookMetrics.recordAmount(PixTransferStatus.CONFIRMED, transfer.amount());
            if (transfer.createdAt() != null && occurredAt != null) {
                pixWebhookMetrics.recordSettlementDuration(
                        PixTransferStatus.CONFIRMED,
                        Duration.between(transfer.createdAt(), occurredAt)
                );
            }
        };
    }

    @Bean
    public Action<PixTransferStatus, PixEventType> rejectPixAction() {
        return context -> {
            PixTransfer transfer = getTransferFromContext(context);
            Instant occurredAt = getOccurredAtFromContext(context);
            String eventId = getEventIdFromContext(context);

            log.info(
                    "PixStateMachine - REJECT action started endToEndId={}, transferId={}, eventId={}",
                    transfer.endToEndId(), transfer.id(), eventId
            );

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
                    "PixStateMachine - pix reversal applied walletId={}, amount={}, endToEndId={}, eventId={}, ledgerEntryId={}",
                    transfer.fromWalletId(), transfer.amount(), transfer.endToEndId(), eventId, reversal.id()
            );

            PixTransfer updated = transfer.markRejected();
            pixTransferRepository.save(updated);

            log.info(
                    "PixStateMachine - transfer marked as REJECTED endToEndId={}, transferId={}",
                    transfer.endToEndId(), updated.id()
            );

            pixWebhookMetrics.recordAmount(PixTransferStatus.REJECTED, transfer.amount());
            if (transfer.createdAt() != null && occurredAt != null) {
                pixWebhookMetrics.recordSettlementDuration(
                        PixTransferStatus.REJECTED,
                        Duration.between(transfer.createdAt(), occurredAt)
                );
            }
        };
    }

    private PixTransfer getTransferFromContext(StateContext<PixTransferStatus, PixEventType> context) {
        Object value = context.getExtendedState().getVariables().get("transfer");
        if (!(value instanceof PixTransfer transfer)) {
            throw new IllegalStateException("PixTransfer not found in state machine context");
        }
        return transfer;
    }

    private Instant getOccurredAtFromContext(StateContext<PixTransferStatus, PixEventType> context) {
        Object value = context.getExtendedState().getVariables().get("occurredAt");
        if (value == null) return null;
        if (!(value instanceof Instant instant)) {
            throw new IllegalStateException("occurredAt is not an Instant in state machine context");
        }
        return instant;
    }

    private String getEventIdFromContext(StateContext<PixTransferStatus, PixEventType> context) {
        Object value = context.getExtendedState().getVariables().get("eventId");
        return value != null ? value.toString() : null;
    }
}
