package com.br.wallet.application.usecase.wallet;

import com.br.wallet.domain.model.Wallet;
import com.br.wallet.domain.port.WalletRepository;
import com.br.wallet.infrastructure.metrics.WalletMetrics;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreateWalletUseCaseTest {

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private WalletMetrics walletMetrics;

    @InjectMocks
    private CreateWalletUseCase useCase;

    @Captor
    private ArgumentCaptor<Wallet> walletCaptor;

    @Test
    void shouldCreateWalletForOwnerAndReturnSavedWallet() {
        String ownerId = "owner-123";
        Wallet savedWallet = mock(Wallet.class);
        when(savedWallet.id()).thenReturn(java.util.UUID.randomUUID());
        when(savedWallet.ownerId()).thenReturn(ownerId);
        when(walletRepository.save(any(Wallet.class))).thenReturn(savedWallet);
        Wallet result = useCase.execute(ownerId);
        assertNotNull(result);
        assertEquals(ownerId, result.ownerId());
        verify(walletRepository).save(walletCaptor.capture());
        Wallet walletPassed = walletCaptor.getValue();
        assertNotNull(walletPassed);
        assertEquals(ownerId, walletPassed.ownerId());
    }

    @Test
    void shouldPropagateExceptionWhenRepositoryFails() {
        String ownerId = "owner-error";
        when(walletRepository.save(any(Wallet.class)))
                .thenThrow(new RuntimeException("DB error"));
        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> useCase.execute(ownerId)
        );
        assertEquals("DB error", ex.getMessage());
        verify(walletRepository).save(any(Wallet.class));
    }
}
