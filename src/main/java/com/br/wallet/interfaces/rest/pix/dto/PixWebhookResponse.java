package com.br.wallet.interfaces.rest.pix.dto;

import java.time.Instant;

public record PixWebhookResponse(
        String status,
        String eventId,
        Instant receivedAt
) {}
