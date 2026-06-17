package com.carson.ledger.service;

import com.carson.ledger.domain.*;
import com.carson.ledger.persistence.AccountRepository;
import com.carson.ledger.persistence.LedgerEntryRepository;
import com.carson.ledger.persistence.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LedgerService {
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final LedgerEntryRepository ledgerEntryRepository;

    public LedgerService(AccountRepository accountRepository, TransactionRepository transactionRepository, LedgerEntryRepository ledgerEntryRepository) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;

    }

    public Account createAccount(UUID ownerId, Currency currency) {
        this.validateNotNull(ownerId);
        this.validateNotNull(currency);
        Account account = new Account(ownerId, currency);
        return this.accountRepository.save(account);
    }

    public List<Account> getAccounts(UUID ownerId) {
        this.validateNotNull(ownerId);
        return this.accountRepository.findByOwnerId(ownerId);
    }

    @Transactional
    public Transaction deposit(UUID accountId, BigDecimal amount) throws AccountNotFoundException {
        this.validateNotNull(amount);
        this.validateNotNull(accountId);
        this.checkAmountPositive(amount);
        this.checkAccountExists(accountId);

        LedgerEntry entry = new LedgerEntry(accountId, amount);
        Transaction depositTransaction = this.transactionRepository.save(new Transaction.Builder()
                .addEventType(TransactionEvent.DEPOSIT)
                .addEntry(entry)
                .build());
        this.ledgerEntryRepository.save(entry, depositTransaction.getId());

        return depositTransaction;
    }

    @Transactional
    public Transaction withdraw(UUID accountId, BigDecimal amount) throws AccountNotFoundException, InsufficientFundsException {
        this.validateNotNull(amount);
        this.validateNotNull(accountId);
        this.checkAmountPositive(amount);
        this.checkAccountExists(accountId);
        this.checkFundsSufficient(accountId, amount);

        LedgerEntry entry = new LedgerEntry(accountId, amount.negate());
        Transaction withdrawalTransaction = this.transactionRepository.save(new Transaction.Builder()
                .addEventType(TransactionEvent.WITHDRAWAL)
                .addEntry(entry)
                .build());
        this.ledgerEntryRepository.save(entry, withdrawalTransaction.getId());

        return withdrawalTransaction;
    }

    @Transactional
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

        LedgerEntry credit = new LedgerEntry(toAccountId, amount);
        LedgerEntry debt = new LedgerEntry(fromAccountId, amount.negate());
        Transaction transferTransaction = this.transactionRepository.save(new Transaction.Builder()
                .addEventType(TransactionEvent.TRANSFER)
                .addEntry(credit)
                .addEntry(debt)
                .build());
        this.ledgerEntryRepository.save(credit, transferTransaction.getId());
        this.ledgerEntryRepository.save(debt, transferTransaction.getId());

        return transferTransaction;
    }

    public List<Transaction> getTransactions(UUID accountId) throws AccountNotFoundException {
        this.validateNotNull(accountId);
        this.checkAccountExists(accountId);
        return transactionRepository.findByAccountId(accountId);
    }

    public BigDecimal getBalance(UUID accountId) throws AccountNotFoundException {
        this.validateNotNull(accountId);
        this.checkAccountExists(accountId);
        return this.ledgerEntryRepository.calculateAccountBalance(accountId);
    }

    private void checkAccountExists(UUID accountId) throws AccountNotFoundException {
        if(!this.accountRepository.existsById(accountId)) {
            throw new AccountNotFoundException("Account does not exist");
        }
    }

    private void checkAmountPositive(BigDecimal amount) {
        if(amount.signum() <= 0) {
            throw new IllegalArgumentException("Amount cannot be negative or zero");
        }
    }

    private void checkFundsSufficient(UUID accountId, BigDecimal amount) throws InsufficientFundsException, AccountNotFoundException {
        if (getBalance(accountId).compareTo(amount) < 0) {
            throw new InsufficientFundsException("Insufficient funds");
        }
    }

    private void validateNotNull(Object value) {
        Objects.requireNonNull(value, "Param cannot be null");
    }
}
