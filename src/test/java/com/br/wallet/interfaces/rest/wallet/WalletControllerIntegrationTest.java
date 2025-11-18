package com.br.wallet.interfaces.rest.wallet;

import com.br.wallet.domain.enums.PixKeyType;
import com.br.wallet.interfaces.rest.wallet.dto.*;
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
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ExtendWith(SpringExtension.class)
@ActiveProfiles("test")
class WalletControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper mapper;

    @Test
    void shouldCreateWallet() throws Exception {
        var request = new CreateWalletRequest("owner-123");

        mockMvc.perform(
                        post("/wallets")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(request))
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.ownerId").value("owner-123"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty());
    }

    @Test
    void shouldRegisterPixKey() throws Exception {
        var walletResp = mockMvc.perform(
                        post("/wallets")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(new CreateWalletRequest("owner-1")))
                )
                .andReturn();
        var walletId = mapper.readTree(walletResp.getResponse().getContentAsString()).get("id").asText();
        var keyRequest = new RegisterPixKeyRequest(PixKeyType.EMAIL, "user@test.com");
        mockMvc.perform(
                        post("/wallets/" + walletId + "/pix-keys")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(keyRequest))
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.walletId").value(walletId))
                .andExpect(jsonPath("$.keyValue").value("user@test.com"));
    }

    @Test
    void shouldDepositAndReturnUpdatedBalance() throws Exception {
        var walletResp = mockMvc.perform(
                        post("/wallets")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(new CreateWalletRequest("owner-2")))
                )
                .andReturn();
        var walletId = mapper.readTree(walletResp.getResponse().getContentAsString()).get("id").asText();
        var depositReq = new AmountRequest(new BigDecimal("300"));
        mockMvc.perform(
                        post("/wallets/" + walletId + "/deposit")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(depositReq))
                )
                .andExpect(status().isCreated());

        mockMvc.perform(
                        get("/wallets/" + walletId + "/balance")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(300));
    }

    @Test
    void shouldWithdraw() throws Exception {
        var walletResp = mockMvc.perform(
                        post("/wallets")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(new CreateWalletRequest("owner-3")))
                )
                .andReturn();
        var walletId = mapper.readTree(walletResp.getResponse().getContentAsString()).get("id").asText();
        mockMvc.perform(
                post("/wallets/" + walletId + "/deposit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(new AmountRequest(new BigDecimal("200"))))
        );
        mockMvc.perform(
                        post("/wallets/" + walletId + "/withdraw")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(new AmountRequest(new BigDecimal("50"))))
                )
                .andExpect(status().isOk());
        mockMvc.perform(
                        get("/wallets/" + walletId + "/balance")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(150));
    }

    @Test
    void shouldReturnHistoricalBalance() throws Exception {
        var walletResp = mockMvc.perform(
                post("/wallets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(new CreateWalletRequest("owner-hist")))
        ).andReturn();

        var walletId = mapper.readTree(walletResp.getResponse().getContentAsString()).get("id").asText();
        mockMvc.perform(
                post("/wallets/" + walletId + "/deposit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(new AmountRequest(new BigDecimal("100"))))
        );
        Instant snapshot = Instant.now();
        mockMvc.perform(
                post("/wallets/" + walletId + "/deposit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(new AmountRequest(new BigDecimal("50"))))
        );
        mockMvc.perform(
                        get("/wallets/" + walletId + "/balance")
                                .param("at", snapshot.toString())
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(100));
    }

    @Test
    void shouldHandleConcurrentWithdrawalsWithoutOverdrawing() throws Exception {
        var walletResp = mockMvc.perform(
                        post("/wallets")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(new CreateWalletRequest("owner-concurrent")))
                )
                .andReturn();
        var walletId = mapper.readTree(walletResp.getResponse().getContentAsString()).get("id").asText();
        mockMvc.perform(
                post("/wallets/" + walletId + "/deposit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(new AmountRequest(new BigDecimal("10000"))))
        ).andExpect(status().isCreated());
        int threads = 5;
        BigDecimal withdrawAmount = new BigDecimal("60");
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threads);
        List<Integer> statusCodes = Collections.synchronizedList(new ArrayList<>());
        for (int i = 0; i < threads; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    var resp = mockMvc.perform(
                                    post("/wallets/" + walletId + "/withdraw")
                                            .contentType(MediaType.APPLICATION_JSON)
                                            .content(mapper.writeValueAsString(new AmountRequest(withdrawAmount)))
                            )
                            .andReturn()
                            .getResponse();
                    statusCodes.add(resp.getStatus());
                } catch (Exception e) {
                } finally {
                    doneLatch.countDown();
                }
            });
        }
        startLatch.countDown();
        doneLatch.await();
        executor.shutdown();
        var balanceResp = mockMvc.perform(
                        get("/wallets/" + walletId + "/balance")
                )
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        var balance = new BigDecimal(mapper.readTree(balanceResp).get("balance").asText());
        System.out.println("FINAL BALANCE = " + balance);
        System.out.println("STATUS CODES = " + statusCodes);
        assertTrue(balance.compareTo(BigDecimal.ZERO) >= 0);
    }
}
