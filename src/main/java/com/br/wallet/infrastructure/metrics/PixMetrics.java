package com.br.wallet.infrastructure.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class PixMetrics {

    private final Counter pixTransferRequestsTotal;
    private final Counter pixTransferIdempotencyHitTotal;
    private final Counter pixTransferIdempotencyMissTotal;
    private final Counter pixTransferInsufficientFundsTotal;
    private final Counter pixTransferCreatedTotal;

    private final Timer pixTransferProcessingSeconds;

    public PixMetrics(MeterRegistry registry) {
        this.pixTransferRequestsTotal = Counter.builder("pix_transfer_requests_total")
                .description("Total de requisicoes de transferencia Pix")
                .register(registry);

        this.pixTransferIdempotencyHitTotal = Counter.builder("pix_transfer_idempotency_hit_total")
                .description("Total de requisicoes Pix reaproveitadas pela mesma Idempotency-Key (HIT)")
                .register(registry);

        this.pixTransferIdempotencyMissTotal = Counter.builder("pix_transfer_idempotency_miss_total")
                .description("Total de requisicoes Pix novas (Idempotency-Key nao encontrada - MISS)")
                .register(registry);

        this.pixTransferInsufficientFundsTotal = Counter.builder("pix_transfer_insufficient_funds_total")
                .description("Total de transferencias Pix recusadas por saldo insuficiente")
                .register(registry);

        this.pixTransferCreatedTotal = Counter.builder("pix_transfer_created_total")
                .description("Total de transferencias Pix criadas em estado PENDING")
                .register(registry);

        this.pixTransferProcessingSeconds = Timer.builder("pix_transfer_processing_seconds")
                .description("Tempo de processamento do use case de criacao de transferencia Pix")
                .publishPercentileHistogram(true)
                .register(registry);
    }

    public void onPixTransferRequested() {
        pixTransferRequestsTotal.increment();
    }

    public void onIdempotencyHit() {
        pixTransferIdempotencyHitTotal.increment();
    }

    public void onIdempotencyMiss() {
        pixTransferIdempotencyMissTotal.increment();
    }

    public void onInsufficientFunds() {
        pixTransferInsufficientFundsTotal.increment();
    }

    public void onPixTransferCreated() {
        pixTransferCreatedTotal.increment();
    }

    public void recordProcessingTime(long startNanos) {
        long duration = System.nanoTime() - startNanos;
        pixTransferProcessingSeconds.record(duration, TimeUnit.NANOSECONDS);
    }
}
