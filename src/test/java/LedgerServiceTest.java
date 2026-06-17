import com.carson.ledger.domain.*;
import com.carson.ledger.persistence.AccountRepository;
import com.carson.ledger.persistence.LedgerEntryRepository;
import com.carson.ledger.persistence.TransactionRepository;
import com.carson.ledger.service.LedgerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Currency;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class LedgerServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private LedgerEntryRepository ledgerEntryRepository;

    private LedgerService ledgerService;

    @BeforeEach
    public void setup() {
        this.ledgerService = new LedgerService(accountRepository, transactionRepository, ledgerEntryRepository);
    }

    @Test
    public void createAccount_creates_account_in_ledgerService() {
        UUID ownerId = UUID.randomUUID();
        Account expected = Account.reconstruct(UUID.randomUUID(), ownerId, Currency.getInstance("GBP"));

        when(accountRepository.save(any(Account.class))).thenReturn(expected);

        Account created = this.ledgerService.createAccount(ownerId, Currency.getInstance("GBP"));
        assertEquals(expected.getId(), created.getId());
        assertEquals(expected.getCurrency(), created.getCurrency());
        assertEquals(expected.getOwnerId(), created.getOwnerId());
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
    public void getAccounts_when_multiple_accounts_all_owned_by_same_owner() {
        UUID ownerId = UUID.randomUUID();
        Account expected = Account.reconstruct(UUID.randomUUID(), ownerId, Currency.getInstance("GBP"));
        Account expected2 = Account.reconstruct(UUID.randomUUID(), ownerId, Currency.getInstance("NZD"));
        when(accountRepository.findByOwnerId(ownerId)).thenReturn(Arrays.asList(expected, expected2));

        List<Account> accountsFromService = ledgerService.getAccounts(ownerId);
        assertEquals(2, accountsFromService.size());
        assertEquals(accountsFromService.getFirst().getOwnerId(), ownerId);
        assertEquals(accountsFromService.get(1).getOwnerId(), ownerId);
    }

    @Test
    public void getAccounts_returns_empty_list_when_owner_has_no_account() {
        UUID ownerId = UUID.randomUUID();
        when(accountRepository.findByOwnerId(ownerId)).thenReturn(List.of());
        assertTrue(this.ledgerService.getAccounts(ownerId).isEmpty());
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
        UUID ownerId = UUID.randomUUID();
        when(accountRepository.existsById(ownerId)).thenReturn(false);
        assertThrows(AccountNotFoundException.class, () -> this.ledgerService.deposit(ownerId, new BigDecimal(1)));
    }

    @Test
    public void deposit_creates_transaction_in_ledger_with_correct_values() throws AccountNotFoundException {
        UUID accountId = UUID.randomUUID();
        when(accountRepository.existsById(accountId)).thenReturn(true);

        BigDecimal depositAmount = new BigDecimal(100);
        LedgerEntry expectedLedgerEntry = new LedgerEntry(accountId, depositAmount);
        Transaction depositTransaction = new Transaction.Builder()
                .addEventType(TransactionEvent.DEPOSIT)
                .addEntry(expectedLedgerEntry)
                .build();
        when(transactionRepository.save(any(Transaction.class))).thenReturn(depositTransaction);
        when(transactionRepository.findByAccountId(any(UUID.class))).thenReturn(List.of(depositTransaction));
        when(ledgerEntryRepository.save(any(LedgerEntry.class), any(UUID.class))).thenReturn(expectedLedgerEntry);

        Transaction actualTransaction = this.ledgerService.deposit(accountId, depositAmount);
        Transaction ledgerTransaction = this.ledgerService.getTransactions(accountId).getFirst();

        assertSame(actualTransaction, ledgerTransaction);
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
        when(accountRepository.existsById(any(UUID.class))).thenReturn(true);
        when(ledgerEntryRepository.calculateAccountBalance(any(UUID.class))).thenReturn(BigDecimal.ZERO);
        assertThrows(InsufficientFundsException.class, () -> this.ledgerService.withdraw(UUID.randomUUID(), new BigDecimal(100)));
    }

    @Test
    public void withdraw_creates_transaction_in_ledger_with_correct_values() throws AccountNotFoundException, InsufficientFundsException {
        UUID accountId = UUID.randomUUID();
        when(accountRepository.existsById(accountId)).thenReturn(true);
        BigDecimal withdrawAmount = new BigDecimal(10);
        LedgerEntry expectedLedgerEntry = new LedgerEntry(accountId, withdrawAmount.negate());
        Transaction withdrawalTransaction = new Transaction.Builder()
                .addEventType(TransactionEvent.WITHDRAWAL)
                .addEntry(expectedLedgerEntry)
                .build();

        when(transactionRepository.save(any(Transaction.class))).thenReturn(withdrawalTransaction);
        when(transactionRepository.findByAccountId(any(UUID.class))).thenReturn(List.of(withdrawalTransaction));
        when(ledgerEntryRepository.save(any(LedgerEntry.class), any(UUID.class))).thenReturn(expectedLedgerEntry);
        when(ledgerEntryRepository.calculateAccountBalance(any(UUID.class))).thenReturn(new BigDecimal(100));

        Transaction actualTransaction = this.ledgerService.withdraw(accountId, withdrawAmount);
        Transaction ledgerTransaction = this.ledgerService.getTransactions(accountId).getFirst();

        assertSame(actualTransaction, ledgerTransaction);
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
    public void transfer_checks_toAccount_exist() {
        UUID doesntExist = UUID.randomUUID();
        UUID exists = UUID.randomUUID();
        when(accountRepository.existsById(exists)).thenReturn(true);
        when(accountRepository.existsById(doesntExist)).thenReturn(false);
        assertThrows(AccountNotFoundException.class, () -> this.ledgerService.transfer(exists, doesntExist, new BigDecimal(10)));

    }

    @Test
    public void transfer_checks_fromAccount_exist() {
        UUID doesntExist = UUID.randomUUID();
        when(accountRepository.existsById(doesntExist)).thenReturn(false);
        assertThrows(AccountNotFoundException.class, () -> this.ledgerService.transfer(doesntExist, UUID.randomUUID(), new BigDecimal(10)));
    }

    @Test
    public void transfer_checks_funds_sufficient_for_transfer_in_fromAccount() throws AccountNotFoundException, InsufficientFundsException {
        when(accountRepository.existsById(any(UUID.class))).thenReturn(true);
        when(ledgerEntryRepository.calculateAccountBalance(any(UUID.class))).thenReturn(BigDecimal.ZERO);
        assertThrows(InsufficientFundsException.class, () -> this.ledgerService.transfer(UUID.randomUUID(), UUID.randomUUID(), new BigDecimal(100)));
    }

    @Test
    public void transfer_creates_transaction_in_ledger() throws AccountNotFoundException, InsufficientFundsException {
        UUID fromAccountId = UUID.randomUUID();
        UUID toAccountId = UUID.randomUUID();
        BigDecimal transferAmount = new BigDecimal(10);

        when(accountRepository.existsById(any(UUID.class))).thenReturn(true);
        when(ledgerEntryRepository.calculateAccountBalance(any(UUID.class))).thenReturn(new BigDecimal(1000));

        LedgerEntry creditEntry = new LedgerEntry(toAccountId, transferAmount);
        LedgerEntry debtEntry = new LedgerEntry(fromAccountId, transferAmount.negate());
        Transaction transferTransaction = new Transaction.Builder()
                .addEventType(TransactionEvent.TRANSFER)
                .addEntry(creditEntry)
                .addEntry(debtEntry)
                .build();

        when(transactionRepository.save(any(Transaction.class))).thenReturn(transferTransaction);
        when(transactionRepository.findByAccountId(any(UUID.class))).thenReturn(List.of(transferTransaction));

        Transaction expectedTransaction = this.ledgerService.transfer(fromAccountId, toAccountId, transferAmount);
        Transaction actualTransaction = this.ledgerService.getTransactions(fromAccountId).stream()
                .filter(transaction -> transaction.getEventType().equals(TransactionEvent.TRANSFER))
                .findFirst()
                .orElseThrow();

        assertSame(expectedTransaction, actualTransaction);
    }

    @Test
    public void getBalance_handles_null_inputs() {
        assertThrows(NullPointerException.class, () -> ledgerService.getBalance(null));
    }

    @Test
    public void getBalance_checks_account_exists() {
        assertThrows(AccountNotFoundException.class, () -> ledgerService.getBalance(UUID.randomUUID()));
    }

}
