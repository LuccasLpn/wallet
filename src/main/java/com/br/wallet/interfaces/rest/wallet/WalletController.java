package com.br.wallet.interfaces.rest.wallet;

import com.br.wallet.application.usecase.wallet.CreateWalletUseCase;
import com.br.wallet.application.usecase.wallet.RegisterPixKeyUseCase;
import com.br.wallet.application.usecase.wallet.GetWalletBalanceUseCase;
import com.br.wallet.application.usecase.wallet.DepositUseCase;
import com.br.wallet.application.usecase.wallet.WithdrawUseCase;
import com.br.wallet.interfaces.rest.wallet.dto.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/wallets")
public class WalletController {

    private final CreateWalletUseCase createWalletUseCase;
    private final RegisterPixKeyUseCase registerPixKeyUseCase;
    private final GetWalletBalanceUseCase getWalletBalanceUseCase;
    private final DepositUseCase depositUseCase;
    private final WithdrawUseCase withdrawUseCase;

    public WalletController(
            CreateWalletUseCase createWalletUseCase,
            RegisterPixKeyUseCase registerPixKeyUseCase,
            GetWalletBalanceUseCase getWalletBalanceUseCase,
            DepositUseCase depositUseCase,
            WithdrawUseCase withdrawUseCase
    ) {
        this.createWalletUseCase = createWalletUseCase;
        this.registerPixKeyUseCase = registerPixKeyUseCase;
        this.getWalletBalanceUseCase = getWalletBalanceUseCase;
        this.depositUseCase = depositUseCase;
        this.withdrawUseCase = withdrawUseCase;
    }

    @PostMapping
    public ResponseEntity<WalletResponse> create(@RequestBody CreateWalletRequest request) {
        var wallet = createWalletUseCase.execute(request.ownerId());
        var response = new WalletResponse(wallet.id(), wallet.ownerId(), wallet.createdAt());
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @PostMapping("/{id}/pix-keys")
    public ResponseEntity<PixKeyResponse> registerPixKey(
            @PathVariable UUID id,
            @RequestBody RegisterPixKeyRequest request
    ) {
        var key = registerPixKeyUseCase.execute(id, request.type(), request.keyValue());
        var response = new PixKeyResponse(
                key.id(),
                key.walletId(),
                key.keyType(),
                key.keyValue(),
                key.createdAt()
        );
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping("/{id}/balance")
    public ResponseEntity<BalanceResponse> getBalance(
            @PathVariable UUID id,
            @RequestParam(value = "at", required = false) Instant at
    ) {
        var balance = (at == null)
                ? getWalletBalanceUseCase.currentBalance(id)
                : getWalletBalanceUseCase.balanceAt(id, at);

        var response = new BalanceResponse(id, balance);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(response);
    }

    @PostMapping("/{id}/deposit")
    public ResponseEntity<DepositWithdrawResponse> deposit(
            @PathVariable UUID id,
            @RequestBody AmountRequest request
    ) {
        var entry = depositUseCase.execute(id, request.amount());
        var response = new DepositWithdrawResponse(
                id,
                entry.id(),
                request.amount(),
                entry.amount(),
                "DEPOSIT",
                "COMPLETED",
                entry.occurredAt()
        );
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @PostMapping("/{id}/withdraw")
    public ResponseEntity<DepositWithdrawResponse> withdraw(
            @PathVariable UUID id,
            @RequestBody AmountRequest request
    ) {
        var entry = withdrawUseCase.execute(id, request.amount());
        var response = new DepositWithdrawResponse(
                id,
                entry.id(),
                request.amount(),
                entry.amount(),
                "WITHDRAW",
                "COMPLETED",
                entry.occurredAt()
        );
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(response);
    }

}
