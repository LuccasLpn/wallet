package com.br.wallet.application.usecase.pix;

import com.br.wallet.domain.enums.LedgerEntryDirection;
import com.br.wallet.domain.enums.LedgerOperationType;
import com.br.wallet.domain.enums.PixEventType;
import com.br.wallet.domain.enums.PixTransferStatus;
import com.br.wallet.domain.model.LedgerEntry;
import com.br.wallet.domain.model.PixEvent;
import com.br.wallet.domain.model.PixTransfer;
import com.br.wallet.domain.port.LedgerEntryRepository;
import com.br.wallet.domain.port.PixEventRepository;
import com.br.wallet.domain.port.PixTransferRepository;
import com.br.wallet.infrastructure.metrics.PixWebhookMetrics;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HandlePixWebhookUseCaseTest {

    @Mock
    private PixTransferRepository pixTransferRepository;

    @Mock
    private PixEventRepository pixEventRepository;

    @Mock
    private LedgerEntryRepository ledgerEntryRepository;

    @Mock
    private PixWebhookMetrics pixWebhookMetrics;

    @InjectMocks
    private HandlePixWebhookUseCase useCase;

    @Captor
    private ArgumentCaptor<LedgerEntry> ledgerEntryCaptor;

    @Test
    void shouldIgnoreWhenEventAlreadyProcessed() {
        String endToEndId = "E2E-123";
        String eventId = "evt-1";
        PixEventType type = PixEventType.CONFIRMED;
        Instant occurredAt = Instant.now();
        when(pixEventRepository.findByEventId(eventId))
                .thenReturn(Optional.of(mock(PixEvent.class)));
        useCase.execute(endToEndId, eventId, type, occurredAt);
        verify(pixTransferRepository, never()).findByEndToEndId(anyString());
        verify(ledgerEntryRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenTransferNotFound() {
        String endToEndId = "E2E-404";
        String eventId = "evt-2";
        PixEventType type = PixEventType.CONFIRMED;
        Instant occurredAt = Instant.now();
        when(pixEventRepository.findByEventId(eventId)).thenReturn(Optional.empty());
        when(pixTransferRepository.findByEndToEndId(endToEndId)).thenReturn(Optional.empty());
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> useCase.execute(endToEndId, eventId, type, occurredAt)
        );
        assertEquals("Transfer not found", ex.getMessage());
        verify(ledgerEntryRepository, never()).save(any());
    }

    @Test
    void shouldApplyCreditAndMarkTransferConfirmedWhenEventConfirmed() {
        String endToEndId = "E2E-OK";
        String eventId = "evt-3";
        PixEventType type = PixEventType.CONFIRMED;
        Instant occurredAt = Instant.now();
        UUID transferId = UUID.randomUUID();
        UUID toWalletId = UUID.randomUUID();
        when(pixEventRepository.findByEventId(eventId)).thenReturn(Optional.empty());
        PixTransfer transfer = mock(PixTransfer.class);
        when(transfer.status()).thenReturn(PixTransferStatus.PENDING);
        when(transfer.id()).thenReturn(transferId);
        when(transfer.endToEndId()).thenReturn(endToEndId);
        when(transfer.toWalletId()).thenReturn(toWalletId);
        when(transfer.amount()).thenReturn(java.math.BigDecimal.valueOf(150));
        when(pixTransferRepository.findByEndToEndId(endToEndId)).thenReturn(Optional.of(transfer));
        PixTransfer confirmedTransfer = mock(PixTransfer.class);
        when(confirmedTransfer.id()).thenReturn(transferId);
        when(transfer.markConfirmed()).thenReturn(confirmedTransfer);
        useCase.execute(endToEndId, eventId, type, occurredAt);
        verify(pixEventRepository).save(any(PixEvent.class));
        verify(ledgerEntryRepository).save(ledgerEntryCaptor.capture());
        LedgerEntry ledgerEntry = ledgerEntryCaptor.getValue();
        assertEquals(toWalletId, ledgerEntry.walletId());
        assertEquals(LedgerOperationType.PIX_CREDIT, ledgerEntry.operationType());
        assertEquals(LedgerEntryDirection.CREDIT, ledgerEntry.direction());
        assertEquals(transfer.amount(), ledgerEntry.amount());
        assertEquals(transferId.toString(), ledgerEntry.referenceId());
        assertEquals(occurredAt, ledgerEntry.occurredAt());
        verify(pixTransferRepository).save(confirmedTransfer);
    }


    @Test
    void shouldApplyReversalAndMarkTransferRejectedWhenEventRejected() {
        String endToEndId = "E2E-REJ";
        String eventId = "evt-4";
        PixEventType type = PixEventType.REJECTED;
        Instant occurredAt = Instant.now();
        UUID transferId = UUID.randomUUID();
        UUID fromWalletId = UUID.randomUUID();
        when(pixEventRepository.findByEventId(eventId)).thenReturn(Optional.empty());
        PixTransfer transfer = mock(PixTransfer.class);
        when(transfer.status()).thenReturn(PixTransferStatus.PENDING);
        when(transfer.id()).thenReturn(transferId);
        when(transfer.endToEndId()).thenReturn(endToEndId);
        when(transfer.fromWalletId()).thenReturn(fromWalletId);
        when(transfer.amount()).thenReturn(java.math.BigDecimal.valueOf(200));
        when(pixTransferRepository.findByEndToEndId(endToEndId)).thenReturn(Optional.of(transfer));
        PixTransfer rejectedTransfer = mock(PixTransfer.class);
        when(rejectedTransfer.id()).thenReturn(transferId);
        when(transfer.markRejected()).thenReturn(rejectedTransfer);
        useCase.execute(endToEndId, eventId, type, occurredAt);
        verify(pixEventRepository).save(any(PixEvent.class));
        verify(ledgerEntryRepository).save(ledgerEntryCaptor.capture());
        LedgerEntry ledgerEntry = ledgerEntryCaptor.getValue();
        assertEquals(fromWalletId, ledgerEntry.walletId());
        assertEquals(LedgerOperationType.PIX_REVERSAL, ledgerEntry.operationType());
        assertEquals(LedgerEntryDirection.CREDIT, ledgerEntry.direction());
        assertEquals(transfer.amount(), ledgerEntry.amount());
        assertEquals(transferId.toString(), ledgerEntry.referenceId());
        assertEquals(occurredAt, ledgerEntry.occurredAt());
        verify(pixTransferRepository).save(rejectedTransfer);
    }


    @Test
    void shouldNotChangeTransferWhenAlreadyFinalized() {
        String endToEndId = "E2E-FINAL";
        String eventId = "evt-5";
        PixEventType type = PixEventType.CONFIRMED;
        Instant occurredAt = Instant.now();
        when(pixEventRepository.findByEventId(eventId)).thenReturn(Optional.empty());
        PixTransfer transfer = mock(PixTransfer.class);
        when(transfer.status()).thenReturn(PixTransferStatus.CONFIRMED);
        when(pixTransferRepository.findByEndToEndId(endToEndId)).thenReturn(Optional.of(transfer));
        useCase.execute(endToEndId, eventId, type, occurredAt);
        verify(pixEventRepository).save(any(PixEvent.class));
        verify(ledgerEntryRepository, never()).save(any());
        verify(pixTransferRepository, never()).save(any());
    }
}
