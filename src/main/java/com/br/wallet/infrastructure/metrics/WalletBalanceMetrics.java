package com.br.wallet.infrastructure.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

@Component
public class WalletBalanceMetrics {

    private final MeterRegistry meterRegistry;

    public WalletBalanceMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void recordRequest(String type) {
        meterRegistry.counter(
                "wallet.balance.requests.total",
                "type", type,
                "result", "success"
        ).increment();
    }

    public void recordError(String type, String errorType) {
        meterRegistry.counter(
                "wallet.balance.requests.total",
                "type", type,
                "result", "error"
        ).increment();

        meterRegistry.counter(
                "wallet.balance.errors.total",
                "type", type,
                "error_type", errorType
        ).increment();
    }

    public void recordValue(BigDecimal balance, String type) {
        meterRegistry.summary(
                "wallet.balance.value.brl",
                "type", type
        ).record(balance.doubleValue());
    }

    public void recordDuration(long nanos, String type, String result) {
        Timer.builder("wallet.balance.duration.seconds")
                .tag("type", type)
                .tag("result", result)
                .register(meterRegistry)
                .record(nanos, TimeUnit.NANOSECONDS);
    }
}
