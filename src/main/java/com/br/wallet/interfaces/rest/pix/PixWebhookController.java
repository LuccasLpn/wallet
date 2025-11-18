package com.br.wallet.interfaces.rest.pix;

import com.br.wallet.application.usecase.pix.HandlePixWebhookUseCase;
import com.br.wallet.interfaces.rest.pix.dto.PixWebhookRequest;
import com.br.wallet.interfaces.rest.pix.dto.PixWebhookResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/pix/webhook")
public class PixWebhookController {

    private final HandlePixWebhookUseCase handlePixWebhookUseCase;

    public PixWebhookController(HandlePixWebhookUseCase handlePixWebhookUseCase) {
        this.handlePixWebhookUseCase = handlePixWebhookUseCase;
    }

    @PostMapping
    public ResponseEntity<PixWebhookResponse> receive(@RequestBody PixWebhookRequest request) {
        handlePixWebhookUseCase.execute(
                request.endToEndId(),
                request.eventId(),
                request.eventType(),
                request.occurredAt()
        );
        return ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .body(new PixWebhookResponse("ACCEPTED"));
    }
}

