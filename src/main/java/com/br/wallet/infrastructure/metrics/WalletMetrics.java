package com.br.wallet.infrastructure.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class WalletMetrics {

    private final MeterRegistry meterRegistry;

    public WalletMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void recordWalletCreated() {
        meterRegistry.counter(
                "wallet.create.total",
                "result", "success"
        ).increment();
    }

    public void recordWalletError(String errorType) {
        meterRegistry.counter(
                "wallet.create.errors.total",
                "error_type", errorType
        ).increment();

        meterRegistry.counter(
                "wallet.create.total",
                "result", "error"
        ).increment();
    }

    public void recordDuration(long nanos, String result) {
        Timer.builder("wallet.create.duration.seconds")
                .tag("result", result)
                .register(meterRegistry)
                .record(nanos, TimeUnit.NANOSECONDS);
    }
}
