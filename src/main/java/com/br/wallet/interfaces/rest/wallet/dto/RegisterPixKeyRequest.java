package com.br.wallet.interfaces.rest.wallet.dto;

import com.br.wallet.domain.enums.PixKeyType;

public record RegisterPixKeyRequest(PixKeyType type, String keyValue) {}
