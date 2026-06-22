package com.carson.ledger;

import com.carson.ledger.domain.*;
import com.carson.ledger.service.LedgerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.shaded.org.checkerframework.checker.units.qual.C;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Testcontainers
@SpringBootTest
public class LedgerServiceIntegrationTests {

    @Container
    @SuppressWarnings("resource")
    static PostgreSQLContainer<?> sqlContainer = new PostgreSQLContainer<>("postgres:latest")
            .waitingFor(Wait.forListeningPort());

    @Autowired
    LedgerService ledgerService;
    
    @DynamicPropertySource
    static void postgresSqlProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", sqlContainer::getJdbcUrl);
        registry.add("spring.datasource.username", sqlContainer::getUsername);
        registry.add("spring.datasource.password", sqlContainer::getPassword);
    }

    @Test
    void when_deposit_completes_account_balance_increases() throws AccountNotFoundException, NegativeAmountException {
        Account account = this.ledgerService.createAccount(UUID.randomUUID(), Currency.getInstance("GBP"));
        BigDecimal depositAmount = new BigDecimal(10);
        this.ledgerService.deposit(account.getId(), depositAmount);
        assertThat(this.ledgerService.getBalance(account.getId())).isEqualByComparingTo(depositAmount);
    }

    @Test
    void CurrencyMismatchException_thrown_when_wrong_currencies_on_accounts() throws NegativeAmountException, CurrencyMismatchException, InsufficientFundsException, SameAccountException, AccountNotFoundException {
        Account account1 = this.ledgerService.createAccount(UUID.randomUUID(), Currency.getInstance("GBP"));
        Account account2 = this.ledgerService.createAccount(UUID.randomUUID(), Currency.getInstance("NZD"));
        assertThrows(CurrencyMismatchException.class, () -> this.ledgerService.transfer(account1.getId(), account2.getId(), new BigDecimal(10)));

    }

    @Test
    void when_withdraw_completes_account_balance_decreases() throws AccountNotFoundException, InsufficientFundsException, NegativeAmountException {
        Account account = this.ledgerService.createAccount(UUID.randomUUID(), Currency.getInstance("GBP"));
        this.ledgerService.deposit(account.getId(), new BigDecimal(10));
        this.ledgerService.withdraw(account.getId(), new BigDecimal("5.5"));
        BigDecimal expectedAmount = new BigDecimal("4.5");
        assertThat(this.ledgerService.getBalance(account.getId())).isEqualByComparingTo(expectedAmount);
    }

    @Test
    void when_transfer_transaction_completes_both_account_balances_change() throws NegativeAmountException, AccountNotFoundException, CurrencyMismatchException, InsufficientFundsException, SameAccountException {
        Account acc1 = this.ledgerService.createAccount(UUID.randomUUID(), Currency.getInstance("GBP"));
        Account acc2 = this.ledgerService.createAccount(UUID.randomUUID(), Currency.getInstance("GBP"));
        this.ledgerService.deposit(acc1.getId(), new BigDecimal(100));
        BigDecimal transferAmount = new BigDecimal("55.50");
        this.ledgerService.transfer(acc1.getId(), acc2.getId(),transferAmount );
        assertThat(this.ledgerService.getBalance(acc1.getId())).isEqualByComparingTo(new BigDecimal("44.50"));
        assertThat(this.ledgerService.getBalance(acc2.getId())).isEqualByComparingTo(transferAmount);
    }

    @Test
    public void transfer_checks_currencies_match() {
        Account acc1 = this.ledgerService.createAccount(UUID.randomUUID(), Currency.getInstance("GBP"));
        Account acc2 = this.ledgerService.createAccount(UUID.randomUUID(), Currency.getInstance("NZD"));
        assertThrows(CurrencyMismatchException.class, () -> this.ledgerService.transfer(acc1.getId(), acc2.getId(), new BigDecimal(100)));
    }

    @Test
    void transfer_checks_sufficient_funds() {
        Account acc1 = this.ledgerService.createAccount(UUID.randomUUID(), Currency.getInstance("GBP"));
        Account acc2 = this.ledgerService.createAccount(UUID.randomUUID(), Currency.getInstance("GBP"));
        assertThrows(InsufficientFundsException.class, () -> this.ledgerService.transfer(acc1.getId(), acc2.getId(), new BigDecimal(100)));
    }

    @Test
    void deposit_checks_account_exists() {
        assertThrows(AccountNotFoundException.class, () -> this.ledgerService.deposit(UUID.randomUUID(), new BigDecimal(10)));
    }

    @Test
    void withdraw_checks_account_exists() {
        assertThrows(AccountNotFoundException.class, () -> this.ledgerService.withdraw(UUID.randomUUID(), new BigDecimal(10)));
    }

    @Test
    void transfer_checks_fromAccount_exists() {
        assertThrows(AccountNotFoundException.class, () -> this.ledgerService.transfer(UUID.randomUUID(), UUID.randomUUID(), new BigDecimal(10)));
    }

    @Test
    void transfer_checks_toAccount_exists() {
        Account account = this.ledgerService.createAccount(UUID.randomUUID(), Currency.getInstance("GBP"));
        assertThrows(AccountNotFoundException.class, () -> this.ledgerService.transfer(account.getId(), UUID.randomUUID(), new BigDecimal(10)));
    }


}