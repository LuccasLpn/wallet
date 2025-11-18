package com.br.wallet.domain.model;

import com.br.wallet.domain.enums.PixTransferStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PixTransfer(
        UUID id,
        String endToEndId,
        UUID fromWalletId,
        UUID toWalletId,
        BigDecimal amount,
        String idempotencyKey,
        Instant createdAt,
        Instant updatedAt,
        Long version,
        PixTransferStatus status
) {

    public static PixTransfer newPending(
            UUID fromWalletId,
            UUID toWalletId,
            BigDecimal amount,
            String endToEndId,
            String idempotencyKey
    ) {
        Instant now = Instant.now();
        return new PixTransfer(
                UUID.randomUUID(),
                endToEndId,
                fromWalletId,
                toWalletId,
                amount,
                idempotencyKey,
                now,
                now,
                null,
                PixTransferStatus.PENDING
        );
    }

    public PixTransfer markConfirmed() {
        if (this.status == PixTransferStatus.CONFIRMED || this.status == PixTransferStatus.REJECTED) {
            return this;
        }
        return new PixTransfer(
                this.id,
                this.endToEndId,
                this.fromWalletId,
                this.toWalletId,
                this.amount,
                this.idempotencyKey,
                this.createdAt,
                Instant.now(),
                this.version,
                PixTransferStatus.CONFIRMED
        );
    }

    public PixTransfer markRejected() {
        if (this.status == PixTransferStatus.CONFIRMED || this.status == PixTransferStatus.REJECTED) {
            return this;
        }
        return new PixTransfer(
                this.id,
                this.endToEndId,
                this.fromWalletId,
                this.toWalletId,
                this.amount,
                this.idempotencyKey,
                this.createdAt,
                Instant.now(),
                this.version,
                PixTransferStatus.REJECTED
        );
    }
}
