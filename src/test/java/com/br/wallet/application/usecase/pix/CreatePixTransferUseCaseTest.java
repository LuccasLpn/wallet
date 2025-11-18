package com.br.wallet.application.usecase.pix;

import com.br.wallet.domain.enums.LedgerEntryDirection;
import com.br.wallet.domain.enums.LedgerOperationType;
import com.br.wallet.domain.model.LedgerEntry;
import com.br.wallet.domain.model.PixKey;
import com.br.wallet.domain.model.PixTransfer;
import com.br.wallet.domain.model.Wallet;
import com.br.wallet.domain.port.LedgerEntryRepository;
import com.br.wallet.domain.port.PixKeyRepository;
import com.br.wallet.domain.port.PixTransferRepository;
import com.br.wallet.domain.port.WalletRepository;
import com.br.wallet.infrastructure.metrics.PixMetrics;
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
class CreatePixTransferUseCaseTest {

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private PixKeyRepository pixKeyRepository;

    @Mock
    private PixTransferRepository pixTransferRepository;

    @Mock
    private LedgerEntryRepository ledgerEntryRepository;

    @Mock
    private PixMetrics pixMetrics;

    @InjectMocks
    private CreatePixTransferUseCase useCase;

    @Captor
    private ArgumentCaptor<LedgerEntry> ledgerEntryCaptor;

    @Test
    void shouldCreatePixTransferAndDebitLedgerWhenBalanceIsSufficient() {
        UUID fromWalletId = UUID.randomUUID();
        UUID toWalletId = UUID.randomUUID();
        String toPixKey = "user@pix.com";
        String idempotencyKey = "idem-123";
        BigDecimal amount = new BigDecimal("100.00");
        PixKey pixKey = mock(PixKey.class);
        when(pixKey.walletId()).thenReturn(toWalletId);
        when(pixKeyRepository.findByKeyValue(toPixKey)).thenReturn(Optional.of(pixKey));
        when(pixTransferRepository.findByFromWalletIdAndIdempotencyKey(fromWalletId, idempotencyKey))
                .thenReturn(Optional.empty());
        Wallet fromWallet = mock(Wallet.class);
        when(fromWallet.id()).thenReturn(fromWalletId);
        when(walletRepository.findByIdForUpdate(fromWalletId)).thenReturn(Optional.of(fromWallet));
        when(ledgerEntryRepository.calculateCurrentBalance(fromWalletId))
                .thenReturn(new BigDecimal("200.00"));
        PixTransfer savedTransfer = mock(PixTransfer.class);
        UUID transferId = UUID.randomUUID();
        String endToEndId = "E2E-" + UUID.randomUUID();
        when(savedTransfer.id()).thenReturn(transferId);
        when(savedTransfer.endToEndId()).thenReturn(endToEndId);
        when(savedTransfer.fromWalletId()).thenReturn(fromWalletId);
        when(savedTransfer.toWalletId()).thenReturn(toWalletId);
        when(savedTransfer.amount()).thenReturn(amount);
        when(pixTransferRepository.save(any(PixTransfer.class))).thenReturn(savedTransfer);
        PixTransfer result = useCase.execute(fromWalletId, toPixKey, amount, idempotencyKey);
        assertNotNull(result);
        assertEquals(transferId, result.id());
        assertEquals(fromWalletId, result.fromWalletId());
        assertEquals(toWalletId, result.toWalletId());
        assertEquals(amount, result.amount());
        verify(pixKeyRepository).findByKeyValue(toPixKey);
        verify(pixTransferRepository).findByFromWalletIdAndIdempotencyKey(fromWalletId, idempotencyKey);
        verify(walletRepository).findByIdForUpdate(fromWalletId);
        verify(ledgerEntryRepository).calculateCurrentBalance(fromWalletId);
        verify(pixTransferRepository).save(any(PixTransfer.class));
        verify(ledgerEntryRepository).save(ledgerEntryCaptor.capture());
        LedgerEntry ledgerEntry = ledgerEntryCaptor.getValue();
        assertEquals(fromWalletId, ledgerEntry.walletId());
        assertEquals(LedgerOperationType.PIX_DEBIT, ledgerEntry.operationType());
        assertEquals(LedgerEntryDirection.DEBIT, ledgerEntry.direction());
        assertEquals(amount, ledgerEntry.amount());
        assertEquals(transferId.toString(), ledgerEntry.referenceId());
        assertEquals(endToEndId, ledgerEntry.endToEndId());
        assertNotNull(ledgerEntry.occurredAt());
    }

    @Test
    void shouldReturnExistingTransferWhenIdempotencyHit() {
        UUID fromWalletId = UUID.randomUUID();
        UUID toWalletId = UUID.randomUUID();
        String toPixKey = "user@pix.com";
        String idempotencyKey = "idem-123";
        BigDecimal amount = new BigDecimal("100.00");
        PixKey pixKey = mock(PixKey.class);
        when(pixKey.walletId()).thenReturn(toWalletId);
        when(pixKeyRepository.findByKeyValue(toPixKey)).thenReturn(Optional.of(pixKey));
        PixTransfer existingTransfer = mock(PixTransfer.class);
        when(pixTransferRepository.findByFromWalletIdAndIdempotencyKey(fromWalletId, idempotencyKey))
                .thenReturn(Optional.of(existingTransfer));
        PixTransfer result = useCase.execute(fromWalletId, toPixKey, amount, idempotencyKey);
        assertSame(existingTransfer, result);
        verify(walletRepository, never()).findByIdForUpdate(any());
        verify(ledgerEntryRepository, never()).calculateCurrentBalance(any());
        verify(pixTransferRepository, never()).save(any());
        verify(ledgerEntryRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenPixKeyNotFound() {
        UUID fromWalletId = UUID.randomUUID();
        String toPixKey = "not_found@pix.com";
        String idempotencyKey = "idem-123";
        BigDecimal amount = new BigDecimal("50.00");
        when(pixKeyRepository.findByKeyValue(toPixKey)).thenReturn(Optional.empty());
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> useCase.execute(fromWalletId, toPixKey, amount, idempotencyKey)
        );
        assertEquals("Pix key not found", ex.getMessage());
        verify(pixTransferRepository, never()).findByFromWalletIdAndIdempotencyKey(any(), any());
        verify(walletRepository, never()).findByIdForUpdate(any());
        verify(ledgerEntryRepository, never()).calculateCurrentBalance(any());
    }

    @Test
    void shouldThrowWhenWalletNotFound() {
        UUID fromWalletId = UUID.randomUUID();
        UUID toWalletId = UUID.randomUUID();
        String toPixKey = "user@pix.com";
        String idempotencyKey = "idem-123";
        BigDecimal amount = new BigDecimal("50.00");
        PixKey pixKey = mock(PixKey.class);
        when(pixKey.walletId()).thenReturn(toWalletId);
        when(pixKeyRepository.findByKeyValue(toPixKey)).thenReturn(Optional.of(pixKey));
        when(pixTransferRepository.findByFromWalletIdAndIdempotencyKey(fromWalletId, idempotencyKey))
                .thenReturn(Optional.empty());
        when(walletRepository.findByIdForUpdate(fromWalletId)).thenReturn(Optional.empty());
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> useCase.execute(fromWalletId, toPixKey, amount, idempotencyKey)
        );
        assertEquals("Wallet not found", ex.getMessage());
        verify(ledgerEntryRepository, never()).calculateCurrentBalance(any());
        verify(pixTransferRepository, never()).save(any());
        verify(ledgerEntryRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenInsufficientFunds() {
        UUID fromWalletId = UUID.randomUUID();
        UUID toWalletId = UUID.randomUUID();
        String toPixKey = "user@pix.com";
        String idempotencyKey = "idem-123";
        BigDecimal amount = new BigDecimal("100.00");
        PixKey pixKey = mock(PixKey.class);
        when(pixKey.walletId()).thenReturn(toWalletId);
        when(pixKeyRepository.findByKeyValue(toPixKey)).thenReturn(Optional.of(pixKey));
        when(pixTransferRepository.findByFromWalletIdAndIdempotencyKey(fromWalletId, idempotencyKey))
                .thenReturn(Optional.empty());
        Wallet fromWallet = mock(Wallet.class);
        when(fromWallet.id()).thenReturn(fromWalletId);
        when(walletRepository.findByIdForUpdate(fromWalletId)).thenReturn(Optional.of(fromWallet));
        when(ledgerEntryRepository.calculateCurrentBalance(fromWalletId))
                .thenReturn(new BigDecimal("50.00"));
        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> useCase.execute(fromWalletId, toPixKey, amount, idempotencyKey)
        );
        assertEquals("Insufficient funds", ex.getMessage());
        verify(pixTransferRepository, never()).save(any());
        verify(ledgerEntryRepository, never()).save(any());
    }
}
