package com.br.wallet.application.usecase.wallet;

import com.br.wallet.domain.enums.LedgerEntryDirection;
import com.br.wallet.domain.enums.LedgerOperationType;
import com.br.wallet.domain.model.LedgerEntry;
import com.br.wallet.domain.model.Wallet;
import com.br.wallet.domain.port.LedgerEntryRepository;
import com.br.wallet.domain.port.WalletRepository;
import com.br.wallet.infrastructure.metrics.WithdrawMetrics;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WithdrawUseCaseTest {

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private LedgerEntryRepository ledgerEntryRepository;

    @Mock
    private WithdrawMetrics withdrawMetrics;

    @InjectMocks
    private WithdrawUseCase useCase;

    @Captor
    private ArgumentCaptor<LedgerEntry> ledgerEntryCaptor;

    @Test
    void shouldCreateWithdrawLedgerEntryWhenWalletExistsAndHasSufficientBalance() {
        UUID walletId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("150.00");
        Wallet wallet = mock(Wallet.class);
        when(wallet.id()).thenReturn(walletId);
        when(walletRepository.findByIdForUpdate(walletId))
                .thenReturn(Optional.of(wallet));
        when(ledgerEntryRepository.calculateCurrentBalance(walletId))
                .thenReturn(new BigDecimal("300.00"));
        useCase.execute(walletId, amount);
        verify(walletRepository).findByIdForUpdate(walletId);
        verify(ledgerEntryRepository).calculateCurrentBalance(walletId);
        verify(ledgerEntryRepository).save(ledgerEntryCaptor.capture());
        LedgerEntry entry = ledgerEntryCaptor.getValue();
        assertNotNull(entry);
        assertEquals(walletId, entry.walletId());
        assertEquals(LedgerOperationType.WITHDRAW, entry.operationType());
        assertEquals(LedgerEntryDirection.DEBIT, entry.direction());
        assertEquals(amount, entry.amount());
        assertNull(entry.referenceId());
        assertNull(entry.endToEndId());
        assertNotNull(entry.occurredAt());
    }

    @Test
    void shouldThrowWhenWalletNotFound() {
        UUID walletId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("100.00");
        when(walletRepository.findByIdForUpdate(walletId))
                .thenReturn(Optional.empty());
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> useCase.execute(walletId, amount)
        );
        assertEquals("Wallet not found", ex.getMessage());
        verify(walletRepository).findByIdForUpdate(walletId);
        verify(ledgerEntryRepository, never()).calculateCurrentBalance(any());
        verify(ledgerEntryRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenInsufficientFunds() {
        UUID walletId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("200.00");
        Wallet wallet = mock(Wallet.class);
        when(wallet.id()).thenReturn(walletId);
        when(walletRepository.findByIdForUpdate(walletId))
                .thenReturn(Optional.of(wallet));
        when(ledgerEntryRepository.calculateCurrentBalance(walletId))
                .thenReturn(new BigDecimal("50.00"));
        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> useCase.execute(walletId, amount)
        );
        assertEquals("Insufficient funds", ex.getMessage());
        verify(walletRepository).findByIdForUpdate(walletId);
        verify(ledgerEntryRepository).calculateCurrentBalance(walletId);
        verify(ledgerEntryRepository, never()).save(any());
    }
}
