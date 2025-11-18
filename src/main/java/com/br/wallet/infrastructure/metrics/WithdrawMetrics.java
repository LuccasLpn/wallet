package com.br.wallet.infrastructure.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

@Component
public class WithdrawMetrics {

    private final MeterRegistry meterRegistry;

    public WithdrawMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void recordWithdrawSuccess(BigDecimal amount) {
        meterRegistry.counter(
                "wallet.withdraw.total",
                "result", "success"
        ).increment();

        meterRegistry.summary(
                "wallet.withdraw.amount.brl",
                "result", "success"
        ).record(amount.doubleValue());
    }

    public void recordWithdrawError(String errorType) {
        meterRegistry.counter(
                "wallet.withdraw.errors.total",
                "error_type", errorType
        ).increment();

        meterRegistry.counter(
                "wallet.withdraw.total",
                "result", "error"
        ).increment();
    }

    public void recordInsufficientFunds() {
        meterRegistry.counter(
                "wallet.withdraw.insufficient_funds.total"
        ).increment();
    }

    public void recordDuration(long nanos, String result) {
        Timer.builder("wallet.withdraw.duration.seconds")
                .tag("result", result)
                .register(meterRegistry)
                .record(nanos, TimeUnit.NANOSECONDS);
    }
}
