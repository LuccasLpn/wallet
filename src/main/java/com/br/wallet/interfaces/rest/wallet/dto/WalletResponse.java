package com.br.wallet.interfaces.rest.wallet.dto;

import java.time.Instant;
import java.util.UUID;

public record WalletResponse(UUID id, String ownerId, Instant createdAt) {}
