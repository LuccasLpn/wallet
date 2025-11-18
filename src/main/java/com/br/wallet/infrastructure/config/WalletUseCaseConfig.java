package com.br.wallet.infrastructure.config;

import com.br.wallet.application.usecase.wallet.CreateWalletUseCase;
import com.br.wallet.application.usecase.wallet.DepositUseCase;
import com.br.wallet.application.usecase.wallet.GetWalletBalanceUseCase;
import com.br.wallet.application.usecase.wallet.RegisterPixKeyUseCase;
import com.br.wallet.application.usecase.wallet.WithdrawUseCase;
import com.br.wallet.domain.port.LedgerEntryRepository;
import com.br.wallet.domain.port.PixKeyRepository;
import com.br.wallet.domain.port.WalletRepository;
import com.br.wallet.infrastructure.metrics.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WalletUseCaseConfig {

    @Bean
    public CreateWalletUseCase createWalletUseCase(WalletRepository walletRepository, WalletMetrics walletMetrics) {
        return new CreateWalletUseCase(walletRepository, walletMetrics);
    }

    @Bean
    public RegisterPixKeyUseCase registerPixKeyUseCase(
            WalletRepository walletRepository,
            PixKeyRepository pixKeyRepository,
            PixKeyMetrics pixKeyMetrics
    ) {
        return new RegisterPixKeyUseCase(walletRepository, pixKeyRepository, pixKeyMetrics);
    }

    @Bean
    public GetWalletBalanceUseCase getWalletBalanceUseCase(
            LedgerEntryRepository ledgerEntryRepository,
            WalletBalanceMetrics walletBalanceMetrics
    ) {
        return new GetWalletBalanceUseCase(ledgerEntryRepository, walletBalanceMetrics);
    }

    @Bean
    public DepositUseCase depositUseCase(
            WalletRepository walletRepository,
            LedgerEntryRepository ledgerEntryRepository,
            DepositMetrics depositMetrics
    ) {
        return new DepositUseCase(walletRepository, ledgerEntryRepository, depositMetrics);
    }

    @Bean
    public WithdrawUseCase withdrawUseCase(
            WalletRepository walletRepository,
            LedgerEntryRepository ledgerEntryRepository,
            WithdrawMetrics withdrawMetrics
    ) {
        return new WithdrawUseCase(walletRepository, ledgerEntryRepository, withdrawMetrics);
    }
}
