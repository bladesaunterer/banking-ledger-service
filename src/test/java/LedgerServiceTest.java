import com.carson.ledger.domain.*;
import com.carson.ledger.service.LedgerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

public class LedgerServiceTest {

    private LedgerService ledgerService;

    @BeforeEach
    public void setup() {
        this.ledgerService = new LedgerService();
    }

    @Test
    public void createAccount_creates_account_in_ledgerService() {
        UUID ownerId = UUID.randomUUID();
        Account created = this.ledgerService.createAccount(ownerId, Currency.getInstance("GBP"));

        Account accountFromService = ledgerService.getAccounts(ownerId).getFirst();
        assertEquals(accountFromService.getId(), created.getId());
        assertEquals(accountFromService.getCurrency(), created.getCurrency());
        assertEquals(accountFromService.getOwnerId(), created.getOwnerId());
    }

    @ParameterizedTest(name = "{argumentsWithNames}")
    @MethodSource("provideNullInputsForCreateAccount")
    public void createAccount_checks_for_null_inputs(UUID ownerId, Currency currency) {
        assertThrows(NullPointerException.class, () -> ledgerService.createAccount(ownerId, currency));
    }

    private static Stream<Arguments> provideNullInputsForCreateAccount() {
        return Stream.of(
                Arguments.of(null, Currency.getInstance("GBP")),
                Arguments.of(UUID.randomUUID(), null)
        );
    }

    @Test
    public void getAccounts_returns_all_accounts_for_owner_when_multiple() {
        UUID ownerId = UUID.randomUUID();
        this.ledgerService.createAccount(ownerId, Currency.getInstance("GBP"));
        this.ledgerService.createAccount(ownerId, Currency.getInstance("NZD"));
        assertEquals(2, this.ledgerService.getAccounts(ownerId).size());
    }

    @Test
    public void getAccounts_when_multiple_accounts_all_owned_by_same_owner() {
        UUID ownerId = UUID.randomUUID();
        this.ledgerService.createAccount(ownerId, Currency.getInstance("GBP"));
        this.ledgerService.createAccount(ownerId, Currency.getInstance("NZD"));

        List<Account> accountsFromService = ledgerService.getAccounts(ownerId);
        assertEquals(accountsFromService.getFirst().getOwnerId(), ownerId);
        assertEquals(accountsFromService.get(1).getOwnerId(), ownerId);
    }

    @Test
    public void getAccounts_returns_empty_list_when_owner_has_no_account() {
        assertTrue(this.ledgerService.getAccounts(UUID.randomUUID()).isEmpty());
    }

    @Test
    public void getAccounts_handles_null_input() {
        assertThrows(NullPointerException.class, () -> ledgerService.getAccounts(null));
    }

    @ParameterizedTest(name = "{argumentsWithNames}")
    @MethodSource("provideNullInputsForDepositAndWithdraw")
    public void deposit_checks_for_null_inputs(UUID accountId, BigDecimal amount) {
        assertThrows(NullPointerException.class, () -> ledgerService.deposit(accountId, amount));
    }

    private static Stream<Arguments> provideNullInputsForDepositAndWithdraw() {
        return Stream.of(
                Arguments.of(UUID.randomUUID(), null),
                Arguments.of(null, new BigDecimal(10))
        );
    }

    @Test
    public void deposit_checks_amount_is_positive() {
        assertThrows(IllegalArgumentException.class, () -> this.ledgerService.deposit(UUID.randomUUID(), new BigDecimal(-1)));
    }

    @Test
    public void deposit_checks_account_exists() {
        assertThrows(AccountNotFoundException.class, () -> this.ledgerService.deposit(UUID.randomUUID(), new BigDecimal(1)));
    }

    @Test
    public void deposit_creates_transaction_in_ledger_with_correct_values() throws AccountNotFoundException {
        UUID ownerId = UUID.randomUUID();
        Account account = this.ledgerService.createAccount(ownerId, Currency.getInstance("GBP"));
        BigDecimal depositAmount = new BigDecimal(100);
        Transaction transaction = this.ledgerService.deposit(account.getId(), depositAmount);
        Transaction ledgerTransaction = this.ledgerService.getTransactions(account.getId()).getFirst();
        LedgerEntry depositEntry = transaction.getEntries().getFirst();

        assertSame(transaction, ledgerTransaction);
        assertEquals(TransactionEvent.DEPOSIT, transaction.getEventType());
        assertEquals(depositEntry.getAccountId(), account.getId());
        assertEquals(depositEntry.getAmount(), depositAmount);
    }


    @ParameterizedTest(name = "{argumentsWithNames}")
    @MethodSource("provideNullInputsForDepositAndWithdraw")
    public void withdraw_checks_for_null_inputs(UUID accountId, BigDecimal amount) {
        assertThrows(NullPointerException.class, () -> ledgerService.withdraw(accountId, amount));
    }

    @Test
    public void withdraw_checks_amount_is_positive() {
        assertThrows(IllegalArgumentException.class, () -> this.ledgerService.withdraw(UUID.randomUUID(), new BigDecimal(-1)));
    }

    @Test
    public void withdraw_checks_account_exists() {
        assertThrows(AccountNotFoundException.class, () -> this.ledgerService.withdraw(UUID.randomUUID(), new BigDecimal(1)));
    }

    @Test
    public void withdraw_checks_funds_sufficient_for_withdrawal() throws AccountNotFoundException, InsufficientFundsException {
        Account account = this.ledgerService.createAccount(UUID.randomUUID(), Currency.getInstance("GBP"));
        assertThrows(InsufficientFundsException.class, () -> this.ledgerService.withdraw(account.getId(), new BigDecimal(100)));
    }

    @Test
    public void withdraw_creates_transaction_in_ledger_with_correct_values() throws AccountNotFoundException, InsufficientFundsException {
        UUID ownerId = UUID.randomUUID();
        Account account = this.ledgerService.createAccount(ownerId, Currency.getInstance("GBP"));
        this.ledgerService.deposit(account.getId(), new BigDecimal(100));

        BigDecimal withdrawAmount = new BigDecimal(10);

        Transaction transaction = this.ledgerService.withdraw(account.getId(), withdrawAmount);
        Transaction ledgerTransaction = this.ledgerService.getTransactions(account.getId()).stream()
                .filter(t -> t.getEventType().equals(TransactionEvent.WITHDRAWAL))
                .findFirst()
                .orElseThrow();
        LedgerEntry withdrawalEntry = transaction.getEntries().getFirst();

        assertSame(transaction, ledgerTransaction);
        assertEquals(withdrawalEntry.getAccountId(), account.getId());
        assertEquals(withdrawalEntry.getAmount(), withdrawAmount.negate());
    }

    @ParameterizedTest(name = "{argumentsWithNames}")
    @MethodSource("provideNullInputsForTransfer")
    public void transfer_checks_for_null_inputs(UUID fromAccountId, UUID toAccountId, BigDecimal amount) {
        assertThrows(NullPointerException.class, () -> ledgerService.transfer(fromAccountId, toAccountId, amount));
    }

    private static Stream<Arguments> provideNullInputsForTransfer() {
        return Stream.of(
                Arguments.of(null, UUID.randomUUID(), new BigDecimal(10)),
                Arguments.of(UUID.randomUUID(), null, new BigDecimal(10)),
                Arguments.of(UUID.randomUUID(), UUID.randomUUID(), null)
        );
    }

    @Test
    public void transfer_checks_fromAccount_and_toAccount_are_different() {
        UUID accountId = UUID.randomUUID();
        assertThrows(IllegalArgumentException.class, () -> this.ledgerService.transfer(accountId, accountId, new BigDecimal(10)));
    }

    @Test
    public void transfer_checks_amount_is_positive() {
        assertThrows(IllegalArgumentException.class, () -> this.ledgerService.transfer(UUID.randomUUID(), UUID.randomUUID(), new BigDecimal(-1)));
    }

    @Test
    public void transfer_checks_fromAccount_exist() {
        Account fromAccount = this.ledgerService.createAccount(UUID.randomUUID(), Currency.getInstance("GBP"));
        assertThrows(AccountNotFoundException.class, () -> this.ledgerService.transfer(fromAccount.getId(), UUID.randomUUID(), new BigDecimal(10)));
    }

    @Test
    public void transfer_checks_toAccount_exist() {
        Account toAccount = this.ledgerService.createAccount(UUID.randomUUID(), Currency.getInstance("GBP"));
        assertThrows(AccountNotFoundException.class, () -> this.ledgerService.transfer(UUID.randomUUID(), toAccount.getId(), new BigDecimal(10)));
    }

    @Test
    public void transfer_checks_funds_sufficient_for_transfer_in_fromAccount() throws AccountNotFoundException, InsufficientFundsException {
        Account fromAccount = this.ledgerService.createAccount(UUID.randomUUID(), Currency.getInstance("GBP"));
        Account toAccount = this.ledgerService.createAccount(UUID.randomUUID(), Currency.getInstance("GBP"));
        assertThrows(InsufficientFundsException.class, () -> this.ledgerService.transfer(fromAccount.getId(), toAccount.getId(), new BigDecimal(100)));
    }

    @Test
    public void transfer_creates_transaction_in_ledger_with_correct_values() throws AccountNotFoundException, InsufficientFundsException {
        UUID fromOwnerId = UUID.randomUUID();
        UUID toOwnerId = UUID.randomUUID();
        Account fromAccount = this.ledgerService.createAccount(fromOwnerId, Currency.getInstance("GBP"));
        Account toAccount = this.ledgerService.createAccount(toOwnerId, Currency.getInstance("GBP"));

        this.ledgerService.deposit(fromAccount.getId(), new BigDecimal(100));
        BigDecimal transferAmount = new BigDecimal(10);

        Transaction expectedTransaction = this.ledgerService.transfer(fromAccount.getId(), toAccount.getId(), transferAmount);
        Transaction actualTransaction = this.ledgerService.getTransactions(fromAccount.getId()).stream()
                .filter(transaction -> transaction.getEventType().equals(TransactionEvent.TRANSFER))
                .findFirst()
                .orElseThrow();

        assertSame(expectedTransaction, actualTransaction);

        LedgerEntry creditEntry = actualTransaction.getEntries().stream()
                .filter(t -> t.getAccountId().equals(toAccount.getId()))
                .findFirst()
                .orElseThrow();
        LedgerEntry debtEntry = actualTransaction.getEntries().stream()
                .filter(t -> t.getAccountId().equals(fromAccount.getId()))
                .findFirst()
                .orElseThrow();

        assertEquals(creditEntry.getAmount(), debtEntry.getAmount().negate());
        assertEquals(creditEntry.getAmount(), transferAmount);
    }

    @Test
    public void getBalance_handles_null_inputs() {
        assertThrows(NullPointerException.class, () -> ledgerService.getBalance(null));
    }

    @Test
    public void getBalance_checks_account_exists() {
        assertThrows(AccountNotFoundException.class, () -> ledgerService.getBalance(UUID.randomUUID()));
    }

    @Test
    void getBalance_returns_correct_balance_for_account_after_deposit() throws AccountNotFoundException {
        Account account = this.ledgerService.createAccount(UUID.randomUUID(), Currency.getInstance("GBP"));
        BigDecimal expectedBalance = new BigDecimal("10.50");
        this.ledgerService.deposit(account.getId(), expectedBalance);
        assertEquals(expectedBalance, this.ledgerService.getBalance(account.getId()));
    }

    @Test
    void getBalance_returns_correct_balance_for_account_after_withdrawal() throws AccountNotFoundException, InsufficientFundsException {
        Account account = this.ledgerService.createAccount(UUID.randomUUID(), Currency.getInstance("GBP"));
        BigDecimal expectedBalance = new BigDecimal("10.50");
        this.ledgerService.deposit(account.getId(), new BigDecimal("20.50"));
        this.ledgerService.withdraw(account.getId(), new BigDecimal("10"));
        assertEquals(expectedBalance, this.ledgerService.getBalance(account.getId()));
    }

    @Test
    void getBalance_returns_correct_balance_for_account_after_transfer() throws AccountNotFoundException, InsufficientFundsException {
        Account fromAccount = this.ledgerService.createAccount(UUID.randomUUID(), Currency.getInstance("GBP"));
        Account toAccount = this.ledgerService.createAccount(UUID.randomUUID(), Currency.getInstance("GBP"));
        BigDecimal expectedBalance = new BigDecimal("10.50");
        this.ledgerService.deposit(fromAccount.getId(), new BigDecimal("20.50"));
        this.ledgerService.transfer(fromAccount.getId(), toAccount.getId(), new BigDecimal("10"));
        assertEquals(expectedBalance, this.ledgerService.getBalance(fromAccount.getId()));
    }

}
