package com.br.wallet.infrastructure.metrics;

import com.br.wallet.domain.enums.PixKeyType;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class PixKeyMetrics {

    private final MeterRegistry meterRegistry;

    public PixKeyMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void recordRegisterSuccess(PixKeyType type) {
        meterRegistry.counter(
                "wallet.pix_key.register.total",
                "result", "success",
                "type", type.name()
        ).increment();
    }

    public void recordRegisterError(PixKeyType type, String errorType) {
        meterRegistry.counter(
                "wallet.pix_key.register.errors.total",
                "error_type", errorType,
                "type", type != null ? type.name() : "UNKNOWN"
        ).increment();

        meterRegistry.counter(
                "wallet.pix_key.register.total",
                "result", "error",
                "type", type != null ? type.name() : "UNKNOWN"
        ).increment();
    }

    public void recordAlreadyInUse(PixKeyType type) {
        meterRegistry.counter(
                "wallet.pix_key.already_in_use.total",
                "type", type.name()
        ).increment();
    }

    public void recordDuration(long nanos, PixKeyType type, String result) {
        Timer.builder("wallet.pix_key.register.duration.seconds")
                .tag("result", result)
                .tag("type", type != null ? type.name() : "UNKNOWN")
                .register(meterRegistry)
                .record(nanos, TimeUnit.NANOSECONDS);
    }
}
