package com.br.wallet.application.usecase.wallet;

import com.br.wallet.domain.port.LedgerEntryRepository;
import com.br.wallet.infrastructure.metrics.WalletBalanceMetrics;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GetWalletBalanceUseCaseTest {

    @Mock
    private LedgerEntryRepository ledgerEntryRepository;

    @Mock
    private WalletBalanceMetrics walletBalanceMetrics;

    @InjectMocks
    private GetWalletBalanceUseCase useCase;

    @Test
    void shouldReturnCurrentBalance() {
        UUID walletId = UUID.randomUUID();
        BigDecimal expectedBalance = new BigDecimal("250.50");
        when(ledgerEntryRepository.calculateCurrentBalance(walletId))
                .thenReturn(expectedBalance);
        BigDecimal result = useCase.currentBalance(walletId);
        assertEquals(expectedBalance, result);
        verify(ledgerEntryRepository).calculateCurrentBalance(walletId);
    }

    @Test
    void shouldReturnHistoricalBalance() {
        UUID walletId = UUID.randomUUID();
        Instant at = Instant.parse("2025-01-01T10:00:00Z");
        BigDecimal expectedBalance = new BigDecimal("100.00");
        when(ledgerEntryRepository.calculateBalanceAt(walletId, at))
                .thenReturn(expectedBalance);
        BigDecimal result = useCase.balanceAt(walletId, at);
        assertEquals(expectedBalance, result);
        verify(ledgerEntryRepository).calculateBalanceAt(walletId, at);
    }
}
