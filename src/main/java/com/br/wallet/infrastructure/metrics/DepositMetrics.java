package com.br.wallet.infrastructure.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

@Component
public class DepositMetrics {

    private final MeterRegistry meterRegistry;

    public DepositMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void recordDepositSuccess(BigDecimal amount) {
        meterRegistry.counter(
                "wallet.deposit.total",
                "result", "success"
        ).increment();

        meterRegistry.summary(
                "wallet.deposit.amount.brl",
                "result", "success"
        ).record(amount.doubleValue());
    }

    public void recordDepositError(String errorType) {
        meterRegistry.counter(
                "wallet.deposit.errors.total",
                "error_type", errorType
        ).increment();

        meterRegistry.counter(
                "wallet.deposit.total",
                "result", "error"
        ).increment();
    }

    public void recordDuration(long nanos, String result) {
        Timer.builder("wallet.deposit.duration.seconds")
                .tag("result", result)
                .register(meterRegistry)
                .record(nanos, TimeUnit.NANOSECONDS);
    }
}
