package com.br.wallet.infrastructure.config;

import com.br.wallet.application.usecase.pix.CreatePixTransferUseCase;
import com.br.wallet.application.usecase.pix.HandlePixWebhookUseCase;
import com.br.wallet.application.usecase.pix.IdempotentCreatePixTransferUseCase;
import com.br.wallet.domain.enums.PixEventType;
import com.br.wallet.domain.enums.PixTransferStatus;
import com.br.wallet.domain.port.*;
import com.br.wallet.infrastructure.metrics.PixMetrics;
import com.br.wallet.infrastructure.metrics.PixTransferMetrics;
import com.br.wallet.infrastructure.metrics.PixWebhookMetrics;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.config.StateMachineFactory;

@Configuration
public class PixUseCaseConfig {

    @Bean
    public CreatePixTransferUseCase createPixTransferUseCase(
            WalletRepository walletRepository,
            PixKeyRepository pixKeyRepository,
            PixTransferRepository pixTransferRepository,
            LedgerEntryRepository ledgerEntryRepository,
            PixMetrics pixMetrics
    ) {
        return new CreatePixTransferUseCase(
                walletRepository,
                pixKeyRepository,
                pixTransferRepository,
                ledgerEntryRepository,
                pixMetrics
        );
    }

    @Bean
    public IdempotentCreatePixTransferUseCase idempotentCreatePixTransferUseCase(
            IdempotencyRecordRepository idempotencyRecordRepository,
            CreatePixTransferUseCase createPixTransferUseCase,
            ObjectMapper objectMapper,
            PixTransferMetrics pixTransferMetrics
    ) {
        return new IdempotentCreatePixTransferUseCase(
                idempotencyRecordRepository,
                createPixTransferUseCase,
                objectMapper,
                pixTransferMetrics
        );
    }

    @Bean
    public HandlePixWebhookUseCase handlePixWebhookUseCase(
            PixTransferRepository pixTransferRepository,
            PixEventRepository pixEventRepository,
            PixWebhookMetrics pixWebhookMetrics,
            StateMachineFactory<PixTransferStatus, PixEventType> stateMachineFactory
    ) {
        return new HandlePixWebhookUseCase(
                pixTransferRepository,
                pixEventRepository,
                pixWebhookMetrics,
                stateMachineFactory
        );
    }
}
