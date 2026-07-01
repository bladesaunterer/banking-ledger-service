package com.carson.ledger.rest;

import com.carson.ledger.domain.*;
import com.carson.ledger.service.LedgerService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.net.URI;
import java.util.Currency;
import java.util.UUID;

@RestController
@RequestMapping("/ledger/accounts")
public class LedgerController {

    @Autowired
    LedgerService ledgerService;

    @PostMapping
    public ResponseEntity<CreateAccountResponse> createAccount(@Valid @RequestBody CreateAccountRequest request) {

        Account account = this.ledgerService.createAccount(
                request.getOwnerId(),
                Currency.getInstance(request.getCurrency())
        );

        return ResponseEntity
                .created(URI.create("/ledger/accounts/" + account.getId()))
                .body(new CreateAccountResponse(account));
    }


    @PostMapping("/{accountId}/deposit")
    public ResponseEntity<AccountBalanceResponse> deposit(@Valid @RequestBody DepositRequest depositRequest, @PathVariable UUID accountId) throws NegativeAmountException, AccountNotFoundException {
        this.ledgerService.deposit(accountId, new BigDecimal(depositRequest.getAmount()));
        BigDecimal balance = this.ledgerService.getBalance(accountId);
        return ResponseEntity.ok(new AccountBalanceResponse(balance));
    }

    @PostMapping("/{accountId}/withdraw")
    public ResponseEntity<AccountBalanceResponse> withdraw(@Valid @RequestBody WithdrawRequest withdrawRequest, @PathVariable UUID accountId) throws AccountNotFoundException, NegativeAmountException, InsufficientFundsException {
        this.ledgerService.withdraw(accountId, new BigDecimal(withdrawRequest.getAmount()));
        BigDecimal balance = this.ledgerService.getBalance(accountId);
        return ResponseEntity.ok(new AccountBalanceResponse(balance));
    }

    @PostMapping("/{fromAccountId}/transfer")
    public ResponseEntity<AccountBalanceResponse> transfer(@Valid @RequestBody TransferRequest transferRequest, @PathVariable UUID fromAccountId) throws NegativeAmountException, CurrencyMismatchException, InsufficientFundsException, AccountNotFoundException, SameAccountException {
        this.ledgerService.transfer(fromAccountId, transferRequest.getToAccountId(), new BigDecimal(transferRequest.getAmount()));
        BigDecimal balance = this.ledgerService.getBalance(fromAccountId);
        return ResponseEntity.ok(new AccountBalanceResponse(balance));
    }

    @GetMapping("/{accountId}/balance")
    public ResponseEntity<AccountBalanceResponse> getBalance(@PathVariable UUID accountId) throws AccountNotFoundException {
        return ResponseEntity.ok(new AccountBalanceResponse(this.ledgerService.getBalance(accountId)));
    }
}
