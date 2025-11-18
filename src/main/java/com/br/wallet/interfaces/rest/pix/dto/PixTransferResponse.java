package com.br.wallet.interfaces.rest.pix.dto;

public record PixTransferResponse(
        String endToEndId,
        String status
) {}
