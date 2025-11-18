package com.br.wallet.infrastructure.metrics;

import com.br.wallet.domain.enums.PixEventType;
import com.br.wallet.domain.enums.PixTransferStatus;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Component
public class PixWebhookMetrics {

    private final MeterRegistry meterRegistry;

    public PixWebhookMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void recordWebhookReceived(PixEventType type) {
        meterRegistry.counter(
                "wallet.pix.webhook.events.total",
                "event_type", type.name()
        ).increment();
    }

    public void recordDuplicateEvent(PixEventType type) {
        meterRegistry.counter(
                "wallet.pix.webhook.duplicates.total",
                "event_type", type.name()
        ).increment();

        meterRegistry.counter(
                "wallet.pix.webhook.events.total",
                "event_type", type.name(),
                "result", "duplicate"
        ).increment();
    }

    public void recordTransferNotFound(PixEventType type) {
        meterRegistry.counter(
                "wallet.pix.webhook.transfer_not_found.total",
                "event_type", type.name()
        ).increment();
    }

    public void recordIgnoredFinalized(PixEventType type, PixTransferStatus currentStatus) {
        meterRegistry.counter(
                "wallet.pix.webhook.ignored_finalized.total",
                "event_type", type.name(),
                "current_status", currentStatus.name()
        ).increment();

        meterRegistry.counter(
                "wallet.pix.webhook.events.total",
                "event_type", type.name(),
                "result", "ignored_finalized"
        ).increment();
    }

    public void recordUnsupportedType(PixEventType type) {
        meterRegistry.counter(
                "wallet.pix.webhook.events.total",
                "event_type", type.name(),
                "result", "unsupported_type"
        ).increment();
    }

    public void recordError(PixEventType type) {
        meterRegistry.counter(
                "wallet.pix.webhook.errors.total",
                "event_type", type.name()
        ).increment();
    }

    public void recordWebhookLatency(PixEventType type, Duration latency) {
        meterRegistry.timer(
                "wallet.pix.webhook.latency.seconds",
                "event_type", type.name()
        ).record(latency);
    }

    public void recordSettlementDuration(PixTransferStatus status, Duration duration) {
        meterRegistry.timer(
                "wallet.pix.transfer.settlement_duration.seconds",
                "status", status.name()
        ).record(duration);
    }

    public void recordAmount(PixTransferStatus status, BigDecimal amount) {
        meterRegistry.summary(
                "wallet.pix.transfer.amount.brl",
                "status", status.name()
        ).record(amount.doubleValue());
    }

    public void recordProcessingDuration(PixEventType type, String result, long nanos) {
        Timer.builder("wallet.pix.webhook.processing_duration.seconds")
                .tag("event_type", type.name())
                .tag("result", result)
                .register(meterRegistry)
                .record(nanos, TimeUnit.NANOSECONDS);
    }
}
