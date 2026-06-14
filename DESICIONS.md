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

### Use enum type to define currency constraint in DB as last line defense
Define currency type using ENUM in db to provide a level of validation at the DB level as a last level defense against bugs
from the application layer. Given that correctness is very important in a banking system, we should be defensive where we
can. 

A currencies table was an option, but the overhead wasn't needed for this demo system. In real world banking systems, a table
may be more appropriate as we would then need to store additional details like decimal places, symbols, active/inactive etc.

### Combine LedgerEntry retrieval logic with Transaction retrieval logic at database level
Transactions at Application level contain the related LedgerEntries, however the persistence level
conceptually separates the storage of these objects. If we wanted to follow the same pattern for retrieval we would
retrieve both LedgerEntries and Transactions separately and combine at the application level. The issue is that we would
then be doing multiple database trips and the processing would be slow. 

I have therefore implemented a JOIN on the Transactions with the LedgerEntries so that combining the datasets happens at the 
database level. The trade-off is a loss in separation of concerns for a gain in performance.

### Use Spring Profiles for local development
Created an application-local.yml containing db credentials. File is ignored by git so credentials don't get pushed to the root
repo.

### Use Flyway for schema creation/migration
Automatest the creation of the database using versions SQL as opposed to manually managing the DB 

### Implemented factory methods for reconstruction of domain level objects from persistence layer
On construction at domain level, ids and timestamps for example are autogenerated on construction. When retrieving from 
the database however, these values are already set so the normal constructors cant be used. Instead of creating extra
constructors, I implemented static factory methods to create these objects. Using a static "reconstruct" method makes
the intent explicit that we are reconstructing an existing object not creating a new one.

### Used interfaces to define persistence interaction in application level
Defined interfaces for interactions with the data store. This allows us to mock the persistence layer in the unit tests
(so we can test interfaces without needing the underlying tech), but also means we aren't locked to using one underlying
technology for persistence

### Balance calculated at database level
Leveraged database operations to calculate account balance. These operations are much more efficiently done at the 
database level rather than at the application level

### Used @Transaction on write operations
Used the @Transactional Spring tag on methods writing to the database. This ensures that all operations in the methods
behave atomically. Not having the methods act atomically could lead to the database ending up in an invalid state if say
for example only half the method operations complete successfully.
