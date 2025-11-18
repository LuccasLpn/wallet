package com.br.wallet.application.usecase.wallet;

import com.br.wallet.domain.enums.LedgerEntryDirection;
import com.br.wallet.domain.enums.LedgerOperationType;
import com.br.wallet.domain.model.LedgerEntry;
import com.br.wallet.domain.model.Wallet;
import com.br.wallet.domain.port.LedgerEntryRepository;
import com.br.wallet.domain.port.WalletRepository;
import com.br.wallet.infrastructure.metrics.DepositMetrics;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DepositUseCaseTest {

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private LedgerEntryRepository ledgerEntryRepository;

    @Mock
    private DepositMetrics depositMetrics;

    @InjectMocks
    private DepositUseCase useCase;

    @Captor
    private ArgumentCaptor<LedgerEntry> ledgerEntryCaptor;

    @Test
    void shouldCreateDepositLedgerEntryWhenWalletExists() {
        UUID walletId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("100.00");
        Wallet wallet = mock(Wallet.class);
        when(wallet.id()).thenReturn(walletId);
        when(walletRepository.findById(walletId)).thenReturn(Optional.of(wallet));
        Instant before = Instant.now();
        useCase.execute(walletId, amount);
        verify(walletRepository).findById(walletId);
        verify(ledgerEntryRepository).save(ledgerEntryCaptor.capture());
        LedgerEntry entry = ledgerEntryCaptor.getValue();
        assertNotNull(entry);
        assertEquals(walletId, entry.walletId());
        assertEquals(LedgerOperationType.DEPOSIT, entry.operationType());
        assertEquals(LedgerEntryDirection.CREDIT, entry.direction());
        assertEquals(amount, entry.amount());
        assertNull(entry.referenceId());
        assertNull(entry.endToEndId());
        assertNotNull(entry.occurredAt());
        assertFalse(entry.occurredAt().isBefore(before));
    }

    @Test
    void shouldThrowWhenWalletNotFound() {
        UUID walletId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("50.00");
        when(walletRepository.findById(walletId)).thenReturn(Optional.empty());
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> useCase.execute(walletId, amount)
        );
        assertEquals("Wallet not found", ex.getMessage());
        verify(walletRepository).findById(walletId);
        verify(ledgerEntryRepository, never()).save(any());
    }
}
