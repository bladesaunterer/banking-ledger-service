# Decision Log

### Used BigDecimal to represent amounts of money
The double type cant accurately represent decimal values due to the binary floating point and introduces very small
rounding errors. In most systems this doesn't represent an issue however in a financial system correctness matters.
The standard type to use is BigDecimal as it is able to store values accurately By storing the value as an exact integer 
with a separate value representing the decimal place position. The tradeoff is performance for accuracy.

### Account Balance derived using LedgerEntries
Having the account balances derived from the ledger entries. Mean that data is auditable and prevents inconsistency from 
having to keep track of balance in multiple places e.g. having a balance associated with an account object as well
as ensuring the value is in line with what's in the ledger. Tradeoff is performance for accuracy

### Used ConcurrentHashmap over HashMap
Transactions are kept in a ConcurrentHashmap. This is in anticipation of concurrent access to the Transactions by multiple
processes. For example when multiple transactions are happening at the same time from different client instances. Tradeoff
is performance for thread safety

### Used Builder pattern for Transaction object
Transaction object represents different transaction types and can be made up with different combinations of fields all 
which need to be immutable once instantiated (as we don't want transaction fields changing after the fact). Builder pattern
allows us to build transaction object incrementally at call location keeping things readable and keeps the source class
clean as we don't have to define multiple constructors for each transaction type (deposit, withdraw, transfer). Method
for building object only gets called once all components of transaction are ready so allows us to return an immutable transaction
object

### Created AccountNotFoundException
Checked exception forcing caller to handle the situation where operations are attempted with accounts that don't exist. 
This is a valid scenario in everyday type operations (e.g. caller isn't aware that account has been deleted) so should be 
a checked exception

### Created InsufficientFundsException
Checked exceptions forcing caller to handle situation where operations are attempted using accounts with insufficient funds.
This is a valid scenario in everyday operations (e.g. user tries to send money without checking their balance first), so scenario
should be handled with a checked exception 

### Used Parameterized Unit tests to handle null case checks
Making sure methods handle null cases requires calling the same method multiple times with nulls at each input field position.
This can be done by having multiple asserts in a test with the nulls at different positions, but it wouldn't be clear which assert failed
specifically. Using a parameterized test means we can run the same test multiple times with different combinations of inputs and the
output makes it clear which of the specific input combos failed

### Debt vs credit in LedgerEntry is represented with a signed amount value rather than separate field
Using an unsigned amount with a separate field would make the validation more complex and opens the possibility for inconsistent states.

### Transaction amount values are always positive
While ledger entries can contain negative values, the transaction level amounts are defined only using positive values in the 
interface as this is more natural at the call site. e.g. a negative deposit doesn't make semantic sense
