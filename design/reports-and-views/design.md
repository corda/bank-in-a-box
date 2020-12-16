
# Reports and views

## Introduction

### Rationale

The most important feature for a customer is the ability to view and interrogate the balances and transactions on their accounts. Customers place a significant responsibility on a bank to manage their wealth and it is important to allow customers the facility to see that this responsibility is being met. Reports and views allow customers to account for all withdrawals, transfers, and payments, and they should be able to reconcile these outgoings with the available balances on their accounts.

A Recurring payment is a periodic transfer of funds to or from an account. They facilitate monthly payments, including bill payments such as utilities, standing orders such as rent, and savings plan payments or loan repayments. A customer should be able to easily query these standing charges as they need to be able to quickly calculate their monthly disposable income. Additionally, they will need to be able to view recurring payments they have put in place, so they can add additional payments, or amend or cancel existing payments.

### Background

Corda allows all or some parts of a contract state to be exposed to an *Object Relational Mapping* (ORM) tool to be persisted in a *Relational Database Management System* (RDBMS). This allows state data to be persisted to custom database tables and used in queries (see https://docs.corda.net/docs/corda-os/4.5/api-persistence.html). The ORM is specified using Java Persistence API annotations (JPA, see https://www.oracle.com/java/technologies/persistence-jsp.html), and this mapping is persisted to the database as a table row by the node automatically every time a state is added to the node's vault as part of a transaction.

The Corda Node exposes the Java Persistence API to flows. This allows entities to be persisted and queried, and is useful if off-ledger data must be maintained in conjunction with on-ledger state data. The entity to be persisted is created as a mapped schema type and a corresponding table is automatically generated. The entity can be persisted and queried using node services with standard ORM practices (see https://docs.corda.net/docs/corda-os/4.5/api-persistence.html#jpa-support).

### Requirements

 - A flow to return the balance for each account for a customer with a given id
 - A flow to return the transactions for each account for a customer with id and transaction date between a given start and end date
 - A flow to return all recurring payments for each account for a customer with a given id

## Design

### Data Model

The recurring payment state and schema (`RecurringPaymentState` and `RecurringPayment` schema) is described in the transfers and payments design document, and the transaction log schema (`TransactionLog` schema) is described in the deposits and withdrawals design document.

### Interface/API definitions

`GetAccountFlow(accountId: UUID): Account`

Return the account for the given `accountId`.

`GetCustomerTransactionsFlow(customerId: UUID, dateStart: Instant, dateEnd: Instant): List<TransactionLogSchemaV1.TransactionLog>`

Return a list of transactions for customer with `customerId` between `dateStart` and `dateEnd`.

`GetRecurringPaymentsFlow(customerId: UUID): List<RecurringPaymentState>`

Return a list of recurring payments for customer with `customerId`.

### Business Logic

#### Creating a Mapped Schema

A contract state implementing the `QueryableState` interface indicates that a custom table should be created for it in the node's database and made accessible using SQL. The following shows an example of the `RecurringPaymentState` class with several unrelated properties and methods removed for clarity:

```kotlin
@BelongsToContract(RecurringPaymentContract::class)
data class RecurringPaymentState(
    val accountFrom: UUID,
    val accountTo: UUID,
    val amount: Amount<Currency>,
    val dateStart: Instant,
    val period: Duration,
    val iterationNum: Int?,
    val owningParty: AbstractParty,
    override val linearId: UniqueIdentifier = UniqueIdentifier()
) : QueryableState {

    override fun generateMappedObject(schema: MappedSchema): PersistentState {
        return when (schema) {
            is RecurringPaymentSchemaV1 -> RecurringPaymentSchemaV1.RecurringPayment(
                    this.accountFrom,
                    this.accountTo,
                    this.amount.quantity,
                    this.dateStart,
                    this.period,
                    this.iterationNum,
                    this.linearId.id)
            else -> throw IllegalArgumentException("Unrecognised schema $schema")
        }
    }

    override fun supportedSchemas(): Iterable<MappedSchema> = listOf(RecurringPaymentSchemaV1)
}
```

The `QueryableState` interface requires the `generateMappedObject` and `supportedSchemas` methods to be implemented. The `supportedSchemas` method returns a list of relational schemas supported by the state and the `generateMappedObject` method returns a generated instance of a schema from the current state's context.

The following is a concise representation of the associated `RecurringPaymentSchemaV1` class:

```kotlin
object RecurringPaymentSchemaV1 : MappedSchema(
    schemaFamily = RecurringPaymentSchema.javaClass,
    version = 1,
    mappedTypes = listOf(RecurringPayment::class.java)) {

    @Entity
    @Table(name = "recurring_payment")
    class RecurringPayment(

        @Column(name="account_from")
        @Type(type = "uuid-char")
        var accountFrom: UUID,

        @Column(name="account_to")
        @Type(type = "uuid-char")
        var accountTo: UUID,

        @Column(name="amount")
        var amount: Long,

        @Column(name="date_start")
        var dateStart: Instant,

        @Column(name="period")
        var period: Duration,

        @Column(name="iteration_num", nullable = true)
        var iterationNum: Int?,

        @Column(name="linear_id")
        @Type(type = "uuid-char")
        var linearId: UUID

) : PersistentState()
```

The `RecurringPaymentSchemaV1` class defines a single entity to represent a recurring payment database table. It uses JPA annotations to define the table name, column names, and types. The schema contains properties to store the relevant properties of the state, including `accountFrom`, `accountTo`, `amount`, `dateStart`, `period`, `iterationNum`, and `linearId`.

An off-ledger schema can be defined in a similar manner to the above `MappedSchema` class but will obviously not have an associating contract state.

#### Querying Transactions

The Corda Node provides services to query and persist mapped schemas. The following is an example query for the transaction log schema:

```kotlin
fun getTransactionLogByTransactionType(accountId: UUID, txType: TransactionType)
    : List<TransactionLogSchemaV1.TransactionLog> {
        
    return serviceHub.withEntityManager {
        val query = createQuery(
            "SELECT tl FROM TransactionLogSchemaV1\$TransactionLog tl " + 
            "WHERE (tl.accountFrom = :accountId OR tl.accountTo = :accountId) " +
            "AND tl.txType = :txType ",
            TransactionLogSchemaV1.TransactionLog::class.java
        )
        query.setParameter("accountId", accountId)
        query.setParameter("txType", txType)
        query.resultList
    }
}
```

The above queries all transactions for account with id `accountId` and of type `transactionType` (deposit, withdrawal, transfer). The `withEntityManager` method of the Corda Node services (`serviceHub`) provides access to the JPA API. An HQL query is constructed, selecting all fields from the transaction log schema, where the from or to account is equal to the given account parameter and the transaction type is equal to the given transaction type parameter.

## Examples

Create a new customer account:

```kotlin
val supportingDocumentationPath = File("/path/to/supportDocumentation.zip")

val attachment = serviceHub.attachments.importAttachment(
        supportingDocumentationPath.inputStream(),
        ourIdentity.toString(),
        supportingDocumentationPath.name)

val attachments = listOf(Pair(attachment, "Supporting documentation"))

val customerId = subFlow(
    CreateCustomerFlow(
        customerName = "AN Other", 
        contactNumber = "5551234", 
        emailAddress = "another@r3.com", 
        postCode = "ZIP 1234", 
        attachments = attachments))
```

Create two new current accounts and set their status to active:

```kotlin
val signedTxFromAccount = subFlow(
    CreateCurrentAccount(
        customerId = customerId, 
        tokenType = Currency.getInstance("EUR"),
        withdrawalDailyLimit = 500,
        transferDailyLimit = 1000))

val signedTxToAccount = subFlow(
    CreateCurrentAccount(
        customerId = customerId, 
        tokenType = Currency.getInstance("EUR"),
        withdrawalDailyLimit = 500,
        transferDailyLimit = 1000))

val accountFrom = signedTxFromAccount.tx.outputsOfType<CurrentAccountState>().single()
val accountTo = signedTxToAccount.tx.outputsOfType<CurrentAccountState>().single()

subFlow(SetAccountStatusFlow(accountFrom.accountId, AccountStatus.ACTIVE))
subFlow(SetAccountStatusFlow(accountTo.accountId, AccountStatus.ACTIVE))
```

Create some transactions:

```kotlin
val amountOneThousandOfEuro = Amount(100000, Currency.getInstance("EUR"))
val amountOneHundredOfEuro = Amount(10000, Currency.getInstance("EUR"))


subFlow(DepositFiatFlow(accountFrom, amountOneThousandOfEuro))
subFlow(IntrabankPaymentFlow(accountFrom, accountTo, amountOneHundredOfEuro))
```

Query the balance on the accounts:

```kotlin
val accountFromBalance = subFlow(GetAccountFlow(accountFrom.accountData.accountId))
val accountToBalance = subFlow(GetAccountFlow(accountTo.accountData.accountId))
```

Query the transactions on the accounts:

```kotlin
val transactionLogAccountFrom = subFlow(GetCustomerTransactionsFlow(accountFrom.accountData.customerId)).single()
val transactionLogAccountTo = subFlow(GetCustomerTransactionsFlow(accountTo.accountData.customerId)).single()
```

Create a recurring payment:

```kotlin
val amount = Amount(10000, Currency.getInstance("EUR")) // 100 euro
subFlow(CreateRecurringPaymentFlow(accountFrom, accountTo, amount, Instant.now(), Duration.ofDays(30), 5))
```

Query the recurring payment:

```kotlin
val recurringPaymentsFrom = subFlow(GetRecurringPaymentsFlow(accountFrom.accountData.customerId))
```
