package com.br.wallet.interfaces.rest.pix.dto;

import com.br.wallet.domain.enums.PixEventType;

import java.time.Instant;

public record PixWebhookRequest(
        String endToEndId,
        String eventId,
        PixEventType eventType,
        Instant occurredAt
) {}
