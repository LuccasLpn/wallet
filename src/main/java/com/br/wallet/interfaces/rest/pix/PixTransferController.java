package com.br.wallet.interfaces.rest.pix;

import com.br.wallet.application.usecase.pix.IdempotentCreatePixTransferUseCase;
import com.br.wallet.interfaces.rest.pix.dto.PixTransferRequest;
import com.br.wallet.interfaces.rest.pix.dto.PixTransferResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/pix/transfers")
public class PixTransferController {

    private final IdempotentCreatePixTransferUseCase idempotentCreatePixTransferUseCase;

    public PixTransferController(IdempotentCreatePixTransferUseCase idempotentCreatePixTransferUseCase) {
        this.idempotentCreatePixTransferUseCase = idempotentCreatePixTransferUseCase;
    }

    @PostMapping
    public ResponseEntity<PixTransferResponse> createTransfer(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestBody PixTransferRequest request
    ) {
        PixTransferResponse response = idempotentCreatePixTransferUseCase.execute(request, idempotencyKey);
        return ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .body(response);
    }
}
