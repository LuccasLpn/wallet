package com.br.wallet.application.usecase.wallet;

import com.br.wallet.domain.enums.PixKeyType;
import com.br.wallet.domain.model.PixKey;
import com.br.wallet.domain.model.Wallet;
import com.br.wallet.domain.port.PixKeyRepository;
import com.br.wallet.domain.port.WalletRepository;
import com.br.wallet.infrastructure.metrics.PixKeyMetrics;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegisterPixKeyUseCaseTest {

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private PixKeyRepository pixKeyRepository;

    @Mock
    private PixKeyMetrics pixKeyMetrics;

    @InjectMocks
    private RegisterPixKeyUseCase useCase;

    @Captor
    private ArgumentCaptor<PixKey> pixKeyCaptor;

    @Test
    void shouldRegisterPixKeyWhenWalletExistsAndKeyIsNotInUse() {
        UUID walletId = UUID.randomUUID();
        String keyValue = "test@pix.com";
        PixKeyType type = PixKeyType.EMAIL;
        Wallet wallet = mock(Wallet.class);
        when(wallet.id()).thenReturn(walletId);
        when(walletRepository.findById(walletId))
                .thenReturn(Optional.of(wallet));
        when(pixKeyRepository.findByKeyValue(keyValue))
                .thenReturn(Optional.empty());
        PixKey savedKey = mock(PixKey.class);
        when(savedKey.id()).thenReturn(UUID.randomUUID());
        when(savedKey.walletId()).thenReturn(walletId);
        when(savedKey.keyType()).thenReturn(type);
        when(savedKey.keyValue()).thenReturn(keyValue);
        when(pixKeyRepository.save(any(PixKey.class)))
                .thenReturn(savedKey);
        PixKey result = useCase.execute(walletId, type, keyValue);
        assertNotNull(result);
        assertEquals(walletId, result.walletId());
        assertEquals(type, result.keyType());
        assertEquals(keyValue, result.keyValue());
        verify(pixKeyRepository).save(pixKeyCaptor.capture());
        PixKey newKey = pixKeyCaptor.getValue();
        assertEquals(walletId, newKey.walletId());
        assertEquals(type, newKey.keyType());
        assertEquals(keyValue, newKey.keyValue());
        verify(walletRepository).findById(walletId);
        verify(pixKeyRepository).findByKeyValue(keyValue);
    }

    @Test
    void shouldThrowWhenWalletNotFound() {
        UUID walletId = UUID.randomUUID();
        String keyValue = "test@pix.com";
        when(walletRepository.findById(walletId))
                .thenReturn(Optional.empty());
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> useCase.execute(walletId, PixKeyType.EMAIL, keyValue)
        );
        assertEquals("Wallet not found", ex.getMessage());
        verify(pixKeyRepository, never()).findByKeyValue(anyString());
        verify(pixKeyRepository, never()).save(any());
    }

}
