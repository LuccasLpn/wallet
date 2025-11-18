package com.br.wallet.domain.model;

import com.br.wallet.domain.enums.PixEventType;

import java.time.Instant;
import java.util.UUID;

public record PixEvent(
        UUID id,
        String eventId,
        String endToEndId,
        PixEventType eventType,
        Instant occurredAt,
        Instant processedAt
) {

    public static PixEvent newEvent(
            String eventId,
            String endToEndId,
            PixEventType eventType,
            Instant occurredAt
    ) {
        return new PixEvent(
                UUID.randomUUID(),
                eventId,
                endToEndId,
                eventType,
                occurredAt,
                Instant.now()
        );
    }
}
