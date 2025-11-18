package com.br.wallet.interfaces.rest.pix;

import com.br.wallet.domain.enums.PixKeyType;
import com.br.wallet.interfaces.rest.wallet.dto.CreateWalletRequest;
import com.br.wallet.interfaces.rest.wallet.dto.AmountRequest;
import com.br.wallet.interfaces.rest.wallet.dto.RegisterPixKeyRequest;
import com.br.wallet.interfaces.rest.pix.dto.PixTransferRequest;
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
import java.util.UUID;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ExtendWith(SpringExtension.class)
@ActiveProfiles("test")
class PixTransferControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper mapper;


    private String createWallet(String owner) throws Exception {
        var resp = mockMvc.perform(
                post("/wallets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(new CreateWalletRequest(owner)))
        ).andReturn();

        return mapper.readTree(resp.getResponse().getContentAsString()).get("id").asText();
    }

    private void deposit(String walletId, BigDecimal amount) throws Exception {
        mockMvc.perform(
                post("/wallets/" + walletId + "/deposit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(new AmountRequest(amount)))
        ).andExpect(status().isCreated());
    }

    private void registerPixKey(String walletId, String email) throws Exception {
        mockMvc.perform(
                post("/wallets/" + walletId + "/pix-keys")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(new RegisterPixKeyRequest(PixKeyType.EMAIL, email)))
        ).andExpect(status().isCreated());
    }

    @Test
    void shouldCreatePixTransferAndRespectIdempotency() throws Exception {
        String fromWalletId = createWallet(UUID.randomUUID().toString());
        deposit(fromWalletId, new BigDecimal("500"));
        String toWalletId = createWallet(UUID.randomUUID().toString());
        String pixKey = "target@test.com";
        registerPixKey(toWalletId, pixKey);
        PixTransferRequest transferRequest = new PixTransferRequest(
                UUID.fromString(fromWalletId),
                pixKey,
                new BigDecimal("200")
        );
        String idemKey = "test-idem-key-123";
        var firstResponse = mockMvc.perform(
                        post("/pix/transfers")
                                .header("Idempotency-Key", idemKey)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(transferRequest))
                )
                .andExpect(status().isAccepted())
                .andReturn()
                .getResponse()
                .getContentAsString();
        mockMvc.perform(
                        post("/pix/transfers")
                                .header("Idempotency-Key", idemKey)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(transferRequest))
                )
                .andExpect(status().isAccepted())
                .andExpect(content().json(firstResponse));
    }
}
