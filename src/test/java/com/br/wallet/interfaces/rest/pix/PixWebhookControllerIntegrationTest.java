package com.br.wallet.interfaces.rest.pix;

import com.br.wallet.domain.enums.PixEventType;
import com.br.wallet.domain.enums.PixKeyType;
import com.br.wallet.domain.model.PixTransfer;
import com.br.wallet.domain.port.PixTransferRepository;
import com.br.wallet.interfaces.rest.pix.dto.PixTransferRequest;
import com.br.wallet.interfaces.rest.pix.dto.PixWebhookRequest;
import com.br.wallet.interfaces.rest.wallet.dto.AmountRequest;
import com.br.wallet.interfaces.rest.wallet.dto.CreateWalletRequest;
import com.br.wallet.interfaces.rest.wallet.dto.RegisterPixKeyRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ExtendWith(SpringExtension.class)
@ActiveProfiles("test")
class PixWebhookControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private PixTransferRepository pixTransferRepository;

    private String createWallet(String ownerId) throws Exception {
        var resp = mockMvc.perform(
                        post("/wallets")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(new CreateWalletRequest(ownerId)))
                ).andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode json = mapper.readTree(resp);
        return json.get("id").asText();
    }

    private void deposit(String walletId, BigDecimal amount) throws Exception {
        mockMvc.perform(
                post("/wallets/" + walletId + "/deposit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(new AmountRequest(amount)))
        ).andExpect(status().isCreated());
    }

    private void registerPixKey(String walletId, String keyValue) throws Exception {
        var req = new RegisterPixKeyRequest(PixKeyType.EMAIL, keyValue);
        mockMvc.perform(
                post("/wallets/" + walletId + "/pix-keys")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req))
        ).andExpect(status().isCreated());
    }

    private BigDecimal getBalance(String walletId) throws Exception {
        var resp = mockMvc.perform(
                        get("/wallets/" + walletId + "/balance")
                ).andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode json = mapper.readTree(resp);
        return new BigDecimal(json.get("balance").asText());
    }

    @Test
    void shouldExecutePixTransferEndToEndWithWebhookConfirmed() throws Exception {
        String fromWalletId = createWallet("origin-user");
        String toWalletId = createWallet("target-user");
        String pixKey = "target-e2e@test.com";
        registerPixKey(toWalletId, pixKey);
        deposit(fromWalletId, new BigDecimal("500.00"));
        assertThat(getBalance(fromWalletId)).isEqualByComparingTo("500.00");
        assertThat(getBalance(toWalletId)).isEqualByComparingTo("0.00");
        BigDecimal amount = new BigDecimal("200.00");
        String idempotencyKey = "e2e-" + UUID.randomUUID();
        PixTransferRequest transferRequest = new PixTransferRequest(
                UUID.fromString(fromWalletId),
                pixKey,
                amount
        );
        mockMvc.perform(
                        post("/pix/transfers")
                                .header("Idempotency-Key", idempotencyKey)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(transferRequest))
                )
                .andExpect(status().isAccepted());
        UUID fromWalletUuid = UUID.fromString(fromWalletId);
        PixTransfer transfer = pixTransferRepository
                .findByFromWalletIdAndIdempotencyKey(fromWalletUuid, idempotencyKey)
                .orElseThrow(() -> new IllegalStateException("PixTransfer not found in DB for E2E test"));
        String endToEndId = transfer.endToEndId();
        assertThat(getBalance(fromWalletId)).isEqualByComparingTo("300.00");
        assertThat(getBalance(toWalletId)).isEqualByComparingTo("0.00");
        String eventId = "evt-" + UUID.randomUUID();
        Instant occurredAt = Instant.now();
        PixWebhookRequest webhookRequest = new PixWebhookRequest(
                endToEndId,
                eventId,
                PixEventType.CONFIRMED,
                occurredAt
        );
        mockMvc.perform(
                        post("/pix/webhook")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(webhookRequest))
                )
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("ACCEPTED"));
        BigDecimal fromFinal = getBalance(fromWalletId);
        BigDecimal toFinal = getBalance(toWalletId);
        assertThat(fromFinal).isEqualByComparingTo("300.00");
        assertThat(toFinal).isEqualByComparingTo("200.00");
    }
}
