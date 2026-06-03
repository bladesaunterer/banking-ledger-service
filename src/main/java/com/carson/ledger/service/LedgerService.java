package com.carson.ledger.service;

import com.carson.ledger.domain.*;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class LedgerService {
    private final Map<UUID, Account> accounts;
    private final Map<UUID, Transaction> transactions;

    public LedgerService() {
        this.accounts = new ConcurrentHashMap<>();
        this.transactions = new ConcurrentHashMap<>();
    }

    public Account createAccount(UUID ownerId, Currency currency) {
        this.validateNotNull(ownerId);
        this.validateNotNull(currency);
        Account account = new Account(ownerId, currency);
        this.accounts.put(account.getId(), account);
        return account;
    }

    public List<Account> getAccounts(UUID ownerId) {
        this.validateNotNull(ownerId);
        return this.accounts.values().stream()
                .filter(account -> account.getOwnerId().equals(ownerId))
                .toList();
    }

    public Transaction deposit(UUID accountId, BigDecimal amount) throws AccountNotFoundException {
        this.validateNotNull(amount);
        this.validateNotNull(accountId);
        this.checkAmountPositive(amount);
        this.checkAccountExists(accountId);

        Transaction newDeposit = new Transaction.Builder()
                .addEventType(TransactionEvent.DEPOSIT)
                .addEntry(new LedgerEntry(accountId, amount))
                .build();
        this.transactions.put(newDeposit.getId(), newDeposit);
        return newDeposit;
    }

    public Transaction withdraw(UUID accountId, BigDecimal amount) throws AccountNotFoundException, InsufficientFundsException {
        this.validateNotNull(amount);
        this.validateNotNull(accountId);
        this.checkAmountPositive(amount);
        this.checkAccountExists(accountId);
        this.checkFundsSufficient(accountId, amount);

        Transaction newWithdrawal = new Transaction.Builder()
                .addEventType(TransactionEvent.WITHDRAWAL)
                .addEntry(new LedgerEntry(accountId, amount.negate()))
                .build();

        this.transactions.put(newWithdrawal.getId(), newWithdrawal);
        return newWithdrawal;
    }

    public Transaction transfer(UUID fromAccountId, UUID toAccountId, BigDecimal amount) throws AccountNotFoundException, InsufficientFundsException {
        this.validateNotNull(amount);
        this.validateNotNull(fromAccountId);
        this.validateNotNull(toAccountId);
        if (fromAccountId.equals(toAccountId)) {
            throw new IllegalArgumentException("Account identifiers cannot be the same");
        }
        this.checkAmountPositive(amount);
        this.checkAccountExists(fromAccountId);
        this.checkAccountExists(toAccountId);
        this.checkFundsSufficient(fromAccountId, amount);

        Transaction newTransfer = new Transaction.Builder()
                .addEventType(TransactionEvent.TRANSFER)
                .addEntry(new LedgerEntry(toAccountId, amount))
                .addEntry(new LedgerEntry(fromAccountId, amount.negate()))
                .build();

        this.transactions.put(newTransfer.getId(), newTransfer);
        return newTransfer;
    }

    public List<Transaction> getTransactions(UUID accountId) throws AccountNotFoundException {
        this.validateNotNull(accountId);
        this.checkAccountExists(accountId);

        return this.transactions.values().stream()
                .filter(transaction -> transaction.getEntries().stream()
                        .anyMatch(ledgerEntry -> ledgerEntry.getAccountId().equals(accountId)))
                .sorted(Comparator.comparing(Transaction::getTimestamp)).toList();
    }

    public BigDecimal getBalance(UUID accountId) throws AccountNotFoundException {
        this.validateNotNull(accountId);
        this.checkAccountExists(accountId);
        return calculateBalance(accountId);
    }

    private BigDecimal calculateBalance(UUID accountId) {
        return this.transactions.values().stream()
                .flatMap(transaction -> transaction.getEntries().stream())
                .filter(ledgerEntry -> ledgerEntry.getAccountId().equals(accountId))
                .map(LedgerEntry::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void checkAccountExists(UUID accountId) throws AccountNotFoundException {
        if(!this.accounts.containsKey(accountId)) {
            throw new AccountNotFoundException("Account does not exist");
        }
    }

    private void checkAmountPositive(BigDecimal amount) {
        if(amount.signum() <= 0) {
            throw new IllegalArgumentException("Amount cannot be negative or zero");
        }
    }

    private void checkFundsSufficient(UUID accountId, BigDecimal amount) throws InsufficientFundsException {
        if (calculateBalance(accountId).compareTo(amount) < 0) {
            throw new InsufficientFundsException("Insufficient funds");
        }
    }

    private void validateNotNull(Object value) {
        Objects.requireNonNull(value, "Param cannot be null");
    }
}
