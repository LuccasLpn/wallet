package com.br.wallet.infrastructure.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

@Component
public class PixTransferMetrics {

    private final MeterRegistry meterRegistry;

    public PixTransferMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void recordCreateRequest(String idempotencyType) {
        meterRegistry.counter(
                "wallet.pix.transfer.create.requests.total",
                "idempotency", idempotencyType
        ).increment();
    }

    public void recordCreateResult(String idempotencyType, String result) {
        meterRegistry.counter(
                "wallet.pix.transfer.create.requests.total",
                "idempotency", idempotencyType,
                "result", result
        ).increment();
    }

    public void recordIdempotencyHit() {
        meterRegistry.counter(
                "wallet.pix.transfer.idempotency.hit.total",
                "scope", "PIX_TRANSFER"
        ).increment();
    }

    public void recordIdempotencyMiss() {
        meterRegistry.counter(
                "wallet.pix.transfer.idempotency.miss.total",
                "scope", "PIX_TRANSFER"
        ).increment();
    }

    public void recordSerializationError(String operation) {
        meterRegistry.counter(
                "wallet.pix.transfer.idempotency.serialization_error.total",
                "scope", "PIX_TRANSFER",
                "operation", operation
        ).increment();
    }

    public void recordAmount(BigDecimal amount, String idempotencyType) {
        meterRegistry.summary(
                "wallet.pix.transfer.create.amount.brl",
                "idempotency", idempotencyType
        ).record(amount.doubleValue());
    }

    public void recordCreateDuration(String idempotencyType, String result, long nanos) {
        Timer.builder("wallet.pix.transfer.create.duration.seconds")
                .tag("idempotency", idempotencyType)
                .tag("result", result)
                .register(meterRegistry)
                .record(nanos, TimeUnit.NANOSECONDS);
    }
}
