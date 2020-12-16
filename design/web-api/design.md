
# Web API

## Introduction

### Rationale

Corda allows interaction with nodes via a custom RPC client. The `CordaRPCClient` class enables connection to the node with a message queue protocol and provides a simple RPC interface for interacting with the node (see https://docs.corda.net/docs/corda-os/4.6/clientrpc.html). Calls are made using JVM objects and the marshaling back-and-forth is handled automatically. This form of connection requires clients to be built on JVM-based platforms.

Most popular web development frameworks are based on platforms other than the JVM. Web apps built on these frameworks will require an intermediary service, built on the JVM, that implements a Corda RPC client and provides web service endpoints to allow communication to the CorDapp. Flows that are to be exposed will have an endpoint created for them and calling the endpoint will result in an underlying RPC client connection to the Corda node.

Access to the web service and its endpoints will need to be restricted through authentication and authorization. Valid users of the system will need to be authenticated and once within the system, users should be authorized according to their assigned roles e.g. guest, customer or admin. In the context of this app, Bank in a box, both security processes will need to be implemented correctly and to industry standards, as non-authenticated users should not be allowed to make any transactions, admins should not have access to funds on a customer's account and customers should not have access to admin level functionality, such as to self approve loans.

### Background

REST has become a widely used standard for web services as they are easy to build and easy to consume. There are many benefits to REST APIs including scalability, flexibility and portability, and independence due to client-server isolation.

The Spring Framework provides a comprehensive programming and configuration model for modern Java-based enterprise applications (https://spring.io). Spring provides extensive infrastructural support for enterprise applications allowing developers to focus on application-level business logic. Spring Boot is a core subset of features that supports easy-to-develop production-grade apps that can be "just run". The framework contains all the necessities to quickly build and deploy a RESTful web service, along with extensive authentication and authorization features.

OAuth2 is the industry standard protocol for authorization, enabling applications to obtain limited access to user accounts on an HTTP service. It works by delegating user authentication to the service that hosts the user account and authorizes third-party applications to access that account. OAuth2 provides authorization for web and desktop applications, and mobile devices.

Spring Security is a framework that provides authentication, authorization, and protection against common attacks (https://spring.io/projects/spring-security). It supports multiple password encoding algorithms and some of the more widely used authentication protocols, including LDAP and OAuth2. The security framework can be added to Spring Boot apps relatively easily and it offers endless customization possibilities.

### Requirements

 - Expose CorDapp functionality to external systems via REST APIs.
 - Provide endpoints for all accessible flows keeping the APIs and data models consistent where possible.
 - Add secure authorization and authentication mechanisms using current industry standards to restrict access to the exposed flows.
 - Add flows and APIs to query and retrieve customers, accounts, payments and transactions, that are required by the UI.

## Design

### Data Model

#### Repository Query Params

The `RepositoryQueryParams` data class provides a flexible means to define custom queries, and specify how and which query results should be returned.

The class defines a nested enum class `RepositoryQueryParams.SortOrder`, which specifies the sort order of the query results. The class defines two sort values `ASC` and `DESC`.

The `RepositoryQueryParams` data class constructor accepts the following parameters:

```kotlin
RepositoryQueryParams(
    startPage: Int = 1,
    pageSize: Int = 100,
    sortField: String? = null,
    sortOrder: SortOrder = SortOrder.ASC,
    searchTerm: String? = ""
)
```

 - `startPage` position of the result page to retrieve (one-based index)
 - `pageSize` the maximum number of elements in a page
 - `sortField` sort results by this field
 - `sortOrder` sort order, ascending or descending
 - `searchTerm` fuzzy search terms for all fields

#### Paginated Response

The `PaginatedResponse` data class stores the result of a paginated query along with pagination information. The constructor accepts the following parameters:

```kotlin
PaginatedResponse<T>(
    result: List<T>,
    totalResults: Long,
    pageSize: Int,
    pageNumber: Int,
    totalPages: Int
)
```

 - `result` generic list of query results
 - `pageSize` number of results per page
 - `pageNumber` page number of this query result
 - `totalPages` total number of pages in the query result

#### Issue Loan Response

The `IssueLoanResponse` data class stores the result of the issue loan endpoint and stores a reference to the repayment current account and the issued loan account. The constructor is as follows:

```kotlin
IssueLoanResponse(
    currentAccount: CurrentAccountState,
    loanAccount: LoanAccountState
)
```

 - `currentAccount` the current account the loan amount was transferred to and the repayments will be transferred from
 - `loanAccount` the issued loan account

#### Intrabank Payment Response

The `IntrabankPaymentResponse` data class stores the result of the intrabank payment endpoint. It contains a reference to the current account the funds were transferred from and the account the funds were transferred to. The constructor is as follows:

```kotlin
IntrabankPaymentResponse(
    fromAccount: CurrentAccountState,
    toAccountId: UUID,
    toAccountCustomerId: UUID,
    toAccountCustomerName: String
)
```

 - `fromAccount` the current account the funds were transferred from
 - `toAccountId` the ID of the account the funds were transferred to
 - `toAccountCustomerId` the ID of the customer of the account the funds were transferred to
 - `toAccountCustomerName` the name of the customer of the account the funds were transferred to

#### Message Response

The `MessageResponse` data class wraps a message response from an endpoint. The constructor is as follows:

```kotlin
MessageResponse(message: String)
```

 - `message` response message

#### Recurring Payment Response

The `RecurringPaymentResponse` data class is a data transfer object (DTO) that stores details of a recurring payment derived from the `RecurringPaymentSchemaV1.RecurringPayment` and `RecurringPaymentLogSchemaV1.RecurringPaymentLog` classes. This class combines details of how and when the payment should be executed (`RecurringPaymentSchemaV1.RecurringPayment`) together with a logged execution of the payment (`RecurringPaymentLogSchemaV1.RecurringPaymentLog`). The constructor is as follows:

```kotlin
RecurringPaymentResponse(
    accountFrom: UUID,
    accountTo: UUID,
    amount: Long,
    currencyCode: String,
    dateStart: Instant,
    period: String,
    iterationNum: Int?,
    recurringPaymentId: UUID,
    error: String?,
    logId: String?,
    txDate: Instant?
)
```

 - `accountFrom` ID of the account to transfer funds from
 - `accountTo` ID of the account to transfer funds to
 - `amount` quantity of currency units e.g. 1000 cents
 - `currencyCode` the currency code of amount
 - `dateStart` the start date of the recurring payment
 - `period` String representation of the payment duration e.g. '10 days'
 - `iterationNum` the number if recurring payment iterations remaining
 - `recurringPaymentId` unique ID of this recurring payment
 - `error` error message if this recurring payment failed to execute
 - `logId` ID of the recurring payment log entry
 - `txDate` timestamp of the recurring payment execution

#### User response

The `UserResponse` data class wraps a user response from an endpoint. The constructor is as follows:

```kotlin
UserReponse(username: String, email: String, roles: String)
```

 - `username` User's username
 - `email` User's email address
 - `roles` User's granted roles

#### Customer name response

The `CustomerNameResponse` data class wraps a customer name response from an endpoint. The constructor is as follows:

```kotlin
CustomerNameResponse(customerName: String)
```

 - `customerName` name of the customer

### Interface/API definitions

#### Flows

The following flows were added as part of the UI requirements:

```kotlin
GetCustomerByIdFlow(customerId: UUID): CustomerSchemaV1.Customer
```

Returns the `CustomerSchemaV1.Customer` with the given `customerId`. An `IllegalArgumentException` is thrown if the customer cannot be found.

```kotlin
GetCustomerNameByAccountFlow(accountId: UUID): String
```

Return the `CustomerSchemaV1.Customer.customerName` of the account owner for the given account ID.

```kotlin
GetAccountFlow(accountId: UUID): Account
```

Returns the `Account` with `accountId`. A `RefAppException` is thrown if the account cannot be found.

```kotlin
GetCustomersPaginatedFlow(queryParams: RepositoryQueryParams): PaginatedResponse<CustomerSchemaV1.Customer>
```

Return a paginated response containing customers matching the given search criteria. The `repositoryQueryParams` object specifies the search term to apply to all customer fields along with sort and pagination information (see [Repository Query Params](#repository-query-params)). Please refer to the `CustomerSchemaV1.Customer` data model described in the Accounts design document for more information on search and sort fields.

```kotlin
GetAccountsPaginatedFlow(
    repositoryQueryParams: RepositoryQueryParams, 
    dateFrom: Instant?, 
    dateTo: Instant?
): PaginatedResponse<Pair<Account, CustomerSchemaV1.Customer>>
```

Return a paginated response containing account and associated customer pairs matching the given search criteria. The `repositoryQueryParams` object specifies the search term to apply to all account fields along with sort and pagination information (see [Repository Query Params](#repository-query-params)). The `dateFrom` and `dateTo` params filter accounts by the last transaction timestamp (`txDate`). Please refer to the account data models for more information on search and sort fields.

```kotlin
GetAccountsForCustomerPaginatedFlow(
    repositoryQueryParams: RepositoryQueryParams,
    customerId: UUID,
    dateFrom: Instant?,
    dateTo: Instant?
): PaginatedResponse<Pair<Account, CustomerSchemaV1.Customer>>
```

Return a paginated response containing account and associated customer pairs matching the given search criteria for the customer given by `customerId`. The `repositoryQueryParams` object specifies the search term to apply to all account fields along with sort and pagination information (see [Repository Query Params](#repository-query-params)). The `dateFrom` and `dateTo` params filter accounts by the last transaction timestamp (`txDate`). Please refer to the account data models for more information on search and sort fields.

```kotlin
GetRecurringPaymentByIdFlow(linearId: UUID): RecurringPaymentState
```

Return the `RecurringPaymentState` with linear ID `linearId`. A `RefAppException` is thrown if the recurring payment state cannot be found.

```kotlin
GetRecurringPaymentsPaginatedFlow(
    repositoryQueryParams: RepositoryQueryParams,
    dateFrom: Instant?,
    dateTo: Instant?
): PaginatedResponse<RecurringPaymentLogSchemaV1.RecurringPaymentLog>

GetRecurringPaymentsForCustomerPaginatedFlow(
    repositoryQueryParams: RepositoryQueryParams,
    customerId: UUID,
    dateFrom: Instant?,
    dateTo: Instant?
): PaginatedResponse<RecurringPaymentLogSchemaV1.RecurringPaymentLog>

GetRecurringPaymentsForAccountPaginatedFlow(
    repositoryQueryParams: RepositoryQueryParams,
    accountId: UUID,
    dateFrom: Instant?,
    dateTo: Instant?
): PaginatedResponse<RecurringPaymentLogSchemaV1.RecurringPaymentLog>
```

Return a paginated response containing completed recurring payments matching the given search criteria with variants for additionally filtering for customer given by `customerId` or account given by `accountId`. The `repositoryQueryParams` object specifies the search term to apply to all recurring payment fields along with sort and pagination information (see [Repository Query Params](#repository-query-params)). The `dateFrom` and `dateTo` params filter recurring payments by payment timestamp (`txDate`). Please refer to the `RecurringPaymentLogSchemaV1.RecurringPaymentLog` data model described in the Transfers and Payments design document for more information on search and sort fields.

```kotlin
GetTransactionByIdFlow(txId: String): TransactionLogSchemaV1.TransactionLog
```

Return the `TransactionLogSchemaV1.TransactionLog` with transaction ID `txId`. An `IllegalArgumentException` is thrown if the transaction cannot be found.

```kotlin
GetTransactionsPaginatedFlow(
    repositoryQueryParams: RepositoryQueryParams,
    dateFrom: Instant?,
    dateTo: Instant?
): PaginatedResponse<TransactionLogSchemaV1.TransactionLog>

GetTransactionsForCustomerPaginatedFlow(
    repositoryQueryParams: RepositoryQueryParams,
    customerId: UUID,
    dateFrom: Instant?,
    dateTo: Instant?
): PaginatedResponse<TransactionLogSchemaV1.TransactionLog>

GetTransactionsForAccountPaginatedFlow(
    repositoryQueryParams: RepositoryQueryParams,
    accountId: UUID,
    dateFrom: Instant?,
    dateTo: Instant?
): PaginatedResponse<TransactionLogSchemaV1.TransactionLog>
```

Return a paginated response containing transactions matching the given search criteria with variants for additionally filtering for customer given by `customerId` or for account given by `accountId`. The `repositoryQueryParams` object specifies the search term to apply to all transaction fields along with sort and pagination information (see [Repository Query Params](#repository-query-params)). The `dateFrom` and `dateTo` params filter transactions by transaction timestamp (`txDate`). Please refer to the `TransactionLogSchemaV1.TransactionLog` data model for more information on search and sort fields.

#### REST API

##### Registration Controller

`registerUserAccount`
 - Request type: `POST`
 - Path: `/register/guest`
 - Param: `username` unique identifier of the user to register
 - Param: `password` user's password
 - Param: `email` user's email address
 - Param: `customerId` associated customer ID for this account [optional]
 - Register a new user account with a guest role and optionally link a customer account with customer ID

`addRoleToUser`
 - Request type: `GET`
 - Path: `/register/admin/addRole`
 - Param: `username` unique identifier of the username
 - Param: `roleName` name of the new role to be added
 - Grants a new role to a user

`getUsersInRole`
 - Request type: `GET`
 - Path: `/register/admin/users`
 - Param: `roleName` name of the role
 - Param: `startPage` position of the start page to return
 - Param: `pageSize` the maximum number of results in a page
 - Param: `sortField` sort results on username or email
 - Param: `sortOrder` order of the sort (ASC or DESC)
 - Param: `searchTerm` term to partially match against username or email
 - Return: `PaginatedResponse<UserResponse>`
 - Return all users with the given `roleName` matching the given search criteria

`revokeRole`
 - Request type: `POST`
 - Path: `/register/admin/revokeRole`
 - Param: `username` user's unique identifier
 - Param: `roleName` name of the role to revoke
 - Revoke role from the user

##### Account Controller

`createCurrentAccount`
 - Request type: `POST`
 - Path: `/accounts/create-current-account`
 - Param: `customerId` ID of the owning customer
 - Param: `tokenType` token type of the account balance
 - Param: `withdrawalDailyLimit` set a limit for the max daily withdrawal amount on the account [optional]
 - Param: `transferDailyLimit` set a limit for the max daily transfer amount on the account [optional]
 - Return: `CurrentAccountState`
 - Execute the create current account flow and return the newly created current account object

`createSavingsAccount`
 - Request type: `POST`
 - Path: `/accounts/create-savings-account`
 - Param: `customerId` ID of the owning customer
 - Param: `tokenType` token type of the account balance
 - Param: `currentAccountId` ID of the current account to transfer savings from
 - Param: `savingsAmount` monthly amount to be transferred
 - Param: `savingsStartDate` start date of the first savings payment
 - Param: `savingsPeriod` duration in months of the savings plan
 - Return: `SavingsAccountState`
 - Execute the create savings account flow and return the newly created savings account object

`approveOverdraftAccount`
 - Request type: `PUT`
 - Path: `/accounts/approve-overdraft-account`
 - Param: `currentAccountId` approve overdraft for this current account
 - Param: `amount` overdraft limit
 - Param: `tokenType` token type of the overdraft balance
 - Return: `CurrentAccountState`
 - Execute the approve overdraft flow on the current account with `currentAccountId` and return a `CurrentAccountState` with an overdraft limit of `amount`

`issueLoan`
 - Request type: `POST`
 - Path: `/accounts/issue-loan`
 - Param: `accountId` ID of the current account to deposit loan amount to and to transfer repayments from
 - Param: `loanAmount` principal of the loan
 - Param: `tokenType` token type of the loan balance
 - Param: `period` repayment period in months
 - Return: [IssueLoanResponse](#issue-loan-response)
 - Issue a loan to the owner of account with `accountId` and deposit the loan amount to the current account with ID `accountId`

`setAccountStatus`
 - Request type: `PUT`
 - Path: `/accounts/set-status`
 - Param: `accountId` ID of the account to update
 - Param: `status` the new status of the account (`PENDING`, `ACTIVE`, `SUSPENDED`)
 - Return: `Account`
 - Set the status of the account with `accountId` to new `status`

`setAccountLimit`
 - Request type: `PUT`
 - Path: `/accounts/set-limits`
 - Param: `accountId` ID of the current account to update
 - Param: `withdrawalDailyLimit` the max daily withdrawal limit [optional]
 - Param: `transferDailyLimit` the max daily transfer limit [optional]
 - Return: `CurrentAccountState`
 - Set a new withdrawal and/or transfer daily limit on a current account

`getAccountById`
 - Request type: `GET`
 - Path: `/accounts/{accountId}`
 - Return: `Account`
 - Return the account for the request account ID

`getAccounts`
 - Request type: `GET`
 - Path: `/accounts`
 - Param: `startPage` position of the start page to return
 - Param: `pageSize` the maximum number of results in a page
 - Param: `sortField` sort results on this field
 - Param: `sortOrder` order of the sort (ASC or DESC)
 - Param: `searchTerm` term to partially match against multiple fields
 - Param: `dateFrom` filter accounts with txDate after this date
 - Param: `dateTo` filter accounts with txDate before this date
 - Return: `PaginatedResponse<Pair<Account, CustomerSchemaV1.Customer>>`
 - Return a paginated response containing account and associated customer pairs matching the given search criteria

`getAccountsForCustomer`
 - Request type: `GET`
 - Path: `/accounts/customer/{customerId}`
 - Param: `startPage` position of the start page to return
 - Param: `pageSize` the maximum number of results in a page
 - Param: `sortField` sort results on this field
 - Param: `sortOrder` order of the sort (ASC or DESC)
 - Param: `searchTerm` term to partially match against multiple fields
 - Param: `dateFrom` filter accounts with txDate after this date
 - Param: `dateTo` filter accounts with txDate before this date
 - Return: `PaginatedResponse<Pair<Account, CustomerSchemaV1.Customer>>`
 - Return a paginated response containing account and associated customer pairs for the request customer ID matching the given search criteria

##### Customer Controller

`createCustomer`
 - Request type: `POST`
 - Path: `/customers/create`
 - Param: `customerName` the new customer's name
 - Param: `contactNumber` the new customer's contact phone number
 - Param: `emailAddress` the new customer's email address
 - Param: `postCode` the post code of the new customer's address
 - Param: `attachments` a list of attachments containing the supporting documentation for the customer (attachment hash, attachment name pairs)
 - Return: `String` ID of the created customer
 - Execute the create customer flow and return the ID

`uploadAttachment`
 - Request type: `POST`
 - Path: `/customers/upload-attachment`
 - Param: `file` ZIP or jar file attachment
 - Param: `uploader` name of the uploader
 - Return: `String` secure hash of the attachment (JSON: {"secureHash": "$hash")
 - Upload file attachment to the node and return the secure hash

`getCustomersPaginated`
 - Request type: `GET`
 - Path: `/customers`
 - Param: `startPage` position of the start page to return
 - Param: `pageSize` the maximum number of results in a page
 - Param: `sortField` sort results on this field
 - Param: `sortOrder` order of the sort (ASC or DESC)
 - Param: `searchTerm` term to partially match against multiple fields
 - Return `PaginatedResponse<CustomerSchemaV1.Customer>`
 - Return a paginated response containing customers matching the given search criteria

`getCustomer`
 - Request type: `GET`
 - Path: `/customers/{customerId}`
 - Return: `CustomerSchemaV1.Customer`
 - Return the customer for the request customer ID

`getCustomerNameForAccountId`
 - Request type: `GET`
 - Path: `/customers/name/{accountId}`
 - Return: `CustomerNameResponse`
 - Return the account owner's customer name for the request account ID

`updateCustomer`
 - Request type: `PUT`
 - Path: `/customers/update/{customerId}`
 - Param: `contactNumber` the customer's new contact phone number
 - Param: `emailAddress` the customer's new email address
 - Param: `attachments` a list of attachments to append to the customer's profile (attachment hash, attachment name pairs)
 - Return: `CustomerSchemaV1.Customer` the updated customer
 - Update the customer with the request customer ID and return the updated customer instance

##### Payments Controller

`withdrawFiat`
 - Request type: `POST`
 - Path: `/payments/withdraw-fiat`
 - Param: `accountId` ID of the account to withdraw from
 - Param: `tokenType` token type of the withdrawal amount
 - Param: `amount` withdrawal amount
 - Return: `Account`
 - Withdraw `amount` of fiat currency of type `tokenType` from the account with ID `accountId` and return the account with updated balance

`depositFiat`
 - Request type: `POST`
 - Path: `/payments/deposit-fiat`
 - Param: `accountId` ID of the account to deposit to
 - Param: `tokenType` token type of the deposit amount
 - Param: `amount` deposit amount
 - Return: `Account`
 - Deposit `amount` of fiat currency of type `tokenType` to the account with ID `accountId` and return the account with updated balance

`intrabankPayment`
 - Request type: `POST`
 - Path: `/payments/intrabank-payment`
 - Param: `fromAccountId` ID of the account to transfer funds from
 - Param: `toAccountId` ID of the account to transfer funds to
 - Param: `tokenType` token type of the transfer amount
 - Param: `amount` the amount of fiat currency to transfer
 - Return: [IntrabankPaymentResponse](#intrabank-payment-response)
 - Execute a payment of `amount` funds of fiat currency of type `tokenType` from the current account given by ID `fromAccountId` to the account with ID `toAccountId`

`createRecurringPayment`
 - Request type: `POST`
 - Path: `/payments/create-recurring-payment`
 - Param: `fromAccountId` ID of the account to transfer funds from
 - Param: `toAccountId` ID of the account to transfer funds to
 - Param: `tokenType` token type of the transfer amount
 - Param: `amount` the amount of fiat currency to transfer
 - Param: `dateStart` start date of the recurring payment
 - Param: `period` duration of the payment
 - Param: `iterationNum` number of payment iterations [optional]
 - Return: `RecurringPaymentState`
 - Create a recurring payment (see the `intrabank-payment` endpoint) starting from `dateStart` and executing every `period` duration for an optional maximum number of `iterationNum` iterations

`cancelRecurringPayment`
 - Request type: `POST`
 - Path: `/payments/cancel-recurring-payment`
 - Param: `recurringPaymentId` ID of the recurring payment to cancel
 - Return: [MessageResponse](#message-response)
 - Cancel recurring payment with ID `recurringPaymentId`

##### Recurring Payments Controller

`getRecurringPaymentById`
 - Request type: `GET`
 - Path: `/recurring-payments/{recurringPaymentId}`
 - Return: [RecurringPaymentResponse](#recurring-payment-response)
 - Return the recurring payment for the request recurring payment ID

`getRecurringPaymentsPaginated`
 - Request type: `GET`
 - Path: `/recurring-payments`
 - Param: `startPage` position of the start page to return
 - Param: `pageSize` the maximum number of results in a page
 - Param: `sortField` sort results on this field
 - Param: `sortOrder` order of the sort (ASC or DESC)
 - Param: `searchTerm` term to partially match against multiple fields
 - Param: `dateFrom` filter recurring payments with txDate after this date
 - Param: `dateTo` filter recurring payments with txDate before this date
 - Return `PaginatedResponse<RecurringPaymentResponse>`
 - Return a paginated response containing recurring payments matching the given search criteria

`getRecurringPaymentsForAccountPaginated`
 - Request type: `GET`
 - Path: `/recurring-payments/account/{accountId}`
 - Param: `startPage` position of the start page to return
 - Param: `pageSize` the maximum number of results in a page
 - Param: `sortField` sort results on this field
 - Param: `sortOrder` order of the sort (ASC or DESC)
 - Param: `searchTerm` term to partially match against multiple fields
 - Param: `dateFrom` filter recurring payments with txDate after this date
 - Param: `dateTo` filter recurring payments with txDate before this date
 - Return `PaginatedResponse<RecurringPaymentResponse>`
 - Return a paginated response containing recurring payments for the request account ID matching the given search criteria

`getRecurringPaymentsForCustomerPaginated`
 - Request type: `GET`
 - Path: `/recurring-payments/customer/{customerId}`
 - Param: `startPage` position of the start page to return
 - Param: `pageSize` the maximum number of results in a page
 - Param: `sortField` sort results on this field
 - Param: `sortOrder` order of the sort (ASC or DESC)
 - Param: `searchTerm` term to partially match against multiple fields
 - Param: `dateFrom` filter recurring payments with txDate after this date
 - Param: `dateTo` filter recurring payments with txDate before this date
 - Return `PaginatedResponse<RecurringPaymentResponse>`
 - Return a paginated response containing recurring payments for the request customer ID matching the given search criteria

##### Transaction Controller

`getTransactions`
 - Request type: `GET`
 - Path: `/transactions`
 - Param: `startPage` position of the start page to return
 - Param: `pageSize` the maximum number of results in a page
 - Param: `sortField` sort results on this field
 - Param: `sortOrder` order of the sort (ASC or DESC)
 - Param: `searchTerm` term to partially match against multiple fields
 - Param: `dateFrom` filter recurring payments with txDate after this date
 - Param: `dateTo` filter recurring payments with txDate before this date
 - Return `PaginatedResponse<TransactionLogSchemaV1.TransactionLog>`
 - Return a paginated response containing transactions matching the given search criteria

`getAccountTransactions`
 - Request type: `GET`
 - Path: `/transactions/account/{accountId}`
 - Param: `startPage` position of the start page to return
 - Param: `pageSize` the maximum number of results in a page
 - Param: `sortField` sort results on this field
 - Param: `sortOrder` order of the sort (ASC or DESC)
 - Param: `searchTerm` term to partially match against multiple fields
 - Param: `dateFrom` filter recurring payments with txDate after this date
 - Param: `dateTo` filter recurring payments with txDate before this date
 - Return `PaginatedResponse<TransactionLogSchemaV1.TransactionLog>`
 - Return a paginated response containing transactions for the request account ID matching the given search criteria

`getCustomerTransactions`
 - Request type: `GET`
 - Path: `/transactions/customer/{customerId}`
 - Param: `startPage` position of the start page to return
 - Param: `pageSize` the maximum number of results in a page
 - Param: `sortField` sort results on this field
 - Param: `sortOrder` order of the sort (ASC or DESC)
 - Param: `searchTerm` term to partially match against multiple fields
 - Param: `dateFrom` filter recurring payments with txDate after this date
 - Param: `dateTo` filter recurring payments with txDate before this date
 - Return `PaginatedResponse<TransactionLogSchemaV1.TransactionLog>`
 - Return a paginated response containing transactions for the request customer ID matching the given search criteria

`getTransactionById`
 - Request type: `GET`
 - Path: `/transactions/{transactionId}`
 - Return: `TransactionLogSchemaV1.TransactionLog`
 - Return the transaction for the request transaction ID

### Business Logic

#### Adding OAuth2 authorization to a Spring Boot App

The following describes how to add OAuth2 authorization to a Spring Boot app. It is worth noting that this covers the basic setup, there are many more configuration options and features to consider. Please refer to the [Spring Security Reference](https://docs.spring.io/spring-security/site/docs/current/reference/html5/).

The `@EnableAuthorizationServer` annotation is a convenience annotation that enables OAuth2 authorization features in the current application context. These features include access token generation, verification, access token rotation, etc. The `@EnableResourceServer` annotation enables the Spring Security filter that authenticates requests with an incoming OAuth2 token. These annotations are added to the Spring Boot application `Server` class:

```kotlin
@SpringBootApplication
@EnableAuthorizationServer
@EnableResourceServer
open class Server {

}
```

##### Authentication

The authentication server component can be configured with a custom class derived from the `AuthorizationServerConfigurerAdapter` class.

```kotlin
@Configuration
open class AuthorizationServerConfig : AuthorizationServerConfigurerAdapter() {

    @Autowired
    lateinit var passwordEncoder: PasswordEncoder

    @Autowired
    lateinit var dataSource: DataSource

    @Throws(Exception::class)
    override fun configure(oauthServer: AuthorizationServerSecurityConfigurer) {
        oauthServer.tokenKeyAccess("permitAll()").checkTokenAccess("isAuthenticated()")
    }

    @Throws(Exception::class)
    override fun configure(clients: ClientDetailsServiceConfigurer) {
        clients.jdbc(dataSource).passwordEncoder(passwordEncoder)
    }
}
```

The Spring Security OAuth server exposes two endpoints: `/oauth/checktoken`, which checks for valid tokens, and `/oauth/token_key`, which generates valid tokens. By default, these endpoints have "deny all" permissions. The first `configure` method sets the permissions to allow anyone to validate their token and only allows authenticated users to generate tokens.

The second `configure` method, configures how client details are stored. Here we specify a JDBC data source, which is configured in `application.yml` and the password encoder, which is configured in a `WebSecurityConfigurerAdapter` derived class, which is implemented as follows:

```kotlin
@Configuration
open class EndpointWebSecurity : WebSecurityConfigurerAdapter() {
    @Autowired
    lateinit var userDetailsService: UserDetailsService

    @Bean
    open fun passwordEncoder(): PasswordEncoder {
        return BCryptPasswordEncoder()
    }

    @Throws(Exception::class)
    @Autowired
    override fun configure(auth: AuthenticationManagerBuilder) {
        auth.userDetailsService(userDetailsService).passwordEncoder(passwordEncoder())
    }

    @Throws(Exception::class)
    override fun configure(http: HttpSecurity) {
        http.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
    }
}
```

The first `configure` method adds authentication using a custom implementation of the `UserDetailsService` interface (see below) and also sets the password encoder, which is configured with the `passwordEncoder` bean, and in this case is the `BCryptPasswordEncoder` class.

The second `configure` method allows web-based security configuration for specific HTTP requests. The above configuration disables session creation, which is not required as our REST web service endpoints are stateless.

The `UserDetailsService` interface loads user-specific data through implementation of a single method `loadUserByUsername`.

```kotlin
@Service(value = "userDetailsService")
class DaoUserDetailsService: UserDetailsService {

    @Autowired
    private lateinit var userRepository: UserRepository

    override fun loadUserByUsername(username: String): UserDetails {
        val user = userRepository.findByUsername(username)
        AccountStatusUserDetailsChecker().check(user)
        return user
    }
}
```

The above implementation queries the user repository by the given username. If the username is found, the status of the user account is checked to ensure that it is enabled, not locked, and has not expired.

The above will allow authentication through the `login` endpoint, which is provided by Spring Security. However, there are no restrictions in place to prevent anyone accessing any endpoint. These restrictions will be added through authorization.

##### Authorization

The Spring Security filter uses a `ResourceServerConfigurerAdapter` derived class to configure a set of filters to use for authorization.

```kotlin
@Configuration
@EnableResourceServer
open class ResourceServerConfiguration : ResourceServerConfigurerAdapter() {

    private val accessDeniedEntryPoint = AccessDeniedEntryPoint()

    @Value("#{'\${auth.server.guest-paths}'.split(',')}")
    lateinit var guestPaths: List<String>

    @Value("#{'\${auth.server.customer-matched-paths}'.split(',')}")
    lateinit var customerMatchedPaths: List<String>

    @Throws(Exception::class)
    override fun configure(http: HttpSecurity) {
        http.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                .authorizeRequests()

                // security config for paths accessible to everyone (user registration)
                .mvcMatchers(*guestPaths.toTypedArray()).anonymous()

                // security config for paths accessible to ADMIN and CUSTOMER with matching customerId
                .mvcMatchers(*customerMatchedPaths.toTypedArray()).access("@idMatcherVerifier" +
                        ".verifyWithCustomerIdMatching(authentication,#customerId)")
    }
}
```

As mentioned previously, we disable session creation as we are using stateless endpoints. The `authorizeRequests` configuration enables access restriction. The proceeding configuration specifies how roles are verified and what endpoints each role has access to.

The `guestPaths` property contains a list of endpoints accessible by guests, and it is initialized from the `application.yml` config file (an example config might be: `guest-paths: /register/guest*`). The `anonymous` configuration indicates that the guest paths will be accessible by everyone.

Similarly, the `customerMatchedPaths` property contains a list of endpoints the customer role has access to, and is also initialized from the `application.yml` config file. In this case, the customer role restricts access to certain users, and the `access` method accepts a Spring Expression Language string that is used to verify that the user has access to these endpoints. The verification method contained within this expression is implemented in the `IdMatcherVerifier` class and is annotated with the `Component` annotation, indicating that it is a Spring-managed bean. The method is implemented as follows:

```kotlin
@Component
class IdMatcherVerifier {
    fun verifyWithCustomerIdMatching(authentication: Authentication, customerId: String): Boolean {
        if (authentication.authorities.find { it.authority == "ADMIN" } != null) {
            return true
        }

        return extractCustomerId(authentication) == customerId
    }
}
```

The method returns `true` if the authenticated user (token) has been granted the authority (role) of admin, or if the authenticated user's customer ID is the same as the customer ID of the request, in other words, the customer is trying to view their own profile and not the profile of another customer.

### Adding a REST controller

This section describes a simple implementation of a REST controller that interacts with a Corda node. Spring provides the `RestController` annotation that marks the annotated class as a controller and indicates that every method returns a domain object instead of a view.

```kotlin
@RestController
@RequestMapping("/customers")
class CustomerController(rpc: NodeRPCConnection) {
    private val proxy = rpc.proxy
}
```

The above is a skeleton implementation of the customer controller. The `RequestMapping` annotation specifies the base path for this controller and the paths for all controller methods will be relative to this path.

We can implement an endpoint as follows:

```kotlin
@GetMapping(value = ["/{customerId}"], produces = ["application/json"])
private fun getCustomer(@PathVariable customerId: UUID): CustomerSchemaV1.Customer {
    return proxy.startFlow(::GetCustomerByIdFlow, customerId).returnValue.getOrThrow()
}
```

The `GetMapping` annotation ensures that HTTP GET requests to `/customers/{customerId}` are mapped to the `getCustomer()` method. The `PathVariable` annotation indicates that a method parameter should be bound to a URI template variable, which in this case, is the ID of the customer to retrieve.

The `NodeRPCConnection` object contains a reference to the `CordaRPCOps` class, which provides methods to interact with a node via RPC. Here we use the `startFlow` method to call `GetCustomerByIdFlow` with the `customerId` request variable. The result of the flow is returned or an exception is thrown by calling the `getOrThrow` method on the `CordaFuture`.

## Examples

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

Create a new current account:
```kotlin
val signedTx = subFlow(
    CreateCurrentAccount(
        customerId = customerId,
        tokenType = Currency.getInstance("EUR"),
        withdrawalDailyLimit = 500,
        transferDailyLimit = 1000))

val accountId = signedTx.tx.outputsOfType<CurrentAccountState>().single().accountId
```

Set account to active:
```kotlin
subFlow(SetAccountStatusFlow(accountId, AccountStatus.ACTIVE))
```

Customer queries:
```kotlin
val customer = GetCustomerByIdFlow(customerId)

val customerName = GetCustomerNameByAccountFlow(accountId)

val accountState = GetAccountFlow(accountId)

val customerQueryParams = RepositoryQueryParams(1, 10, null, RepositoryQueryParams.SortOrder.ASC, "AN Other")
val customersPaginated = GetCustomersPaginatedFlow(customerQueryParams)
```

Account queries:
```kotlin
val accountQueryParams = RepositoryQueryParams(1, 10, null, RepositoryQueryParams.SortOrder.ASC, "")

# query accounts that were last updated today
val accountsUpdatedTodayPaginated = GetAccountsPaginatedFlow(
    accountQueryParams,
    dateFrom = accountState.txDate.truncatedTo(ChronoUnit.DAYS))

val accountsForCustomerPaginated = GetAccountsForCustomerPaginatedFlow(
    accountQueryParams,
    customerId)
```

Create a recurring payment of 100 euro every 30 days for 5 repayments:
```kotlin
val signedTx = subFlow(
    CreateCurrentAccount(
        customerId = customerId,
        tokenType = Currency.getInstance("EUR"),
        withdrawalDailyLimit = 500,
        transferDailyLimit = 1000))

val otherAccountId = signedTx.tx.outputsOfType<CurrentAccountState>().single().accountId

val amountOneThousandOfEuro = Amount(100000, Currency.getInstance("EUR"))
val amountOneHundredOfEuro = Amount(10000, Currency.getInstance("EUR"))

subFlow(DepositFiatFlow(accountId, amountOneThousandOfEuro))
subFlow(CreateRecurringPaymentFlow(accountId, otherAccountId, amountOneHundredOfEuro, Instant.now(), Duration.ofDays(30), 5))
```

Recurring payment queries:
```kotlin
val recurringPaymentQueryParams = RepositoryQueryParams(1, 10, null, RepositoryQueryParams.SortOrder.ASC, "")

val allPayments = GetRecurringPaymentsPaginatedFlow(recurringPaymentQueryParams)

val paymentsForCustomer = GetRecurringPaymentsForCustomerPaginatedFlow(
    recurringPaymentQueryParams,
    customerId)

val paymentsForAccount = GetRecurringPaymentsForAccountPaginatedFlow(
    recurringPaymentQueryParams,
    accountId)

val loggedPayment = paymentsForAccount.result.first()
val recurringPaymentResult = GetRecurringPaymentByIdFlow(loggedPayment.recurringPayment.linearId)
```

Transaction queries:
```kotlin
val txQueryParams = RepositoryQueryParams(1, 10, null, RepositoryQueryParams.SortOrder.ASC, "")

val allTransactions = GetTransactionsForPaginatedFlow(txQueryParams)

val txsForCustomer = GetTransactionsForCustomerPaginatedFlow(txQueryParams, customerId)

val txsForAccount = GetTransactionsForAccountPaginatedFlow(txQueryParams, accountId)

val tx = txsForAccount.result.first()
val txResult = GetTransactionByIdFlow(tx.txId)
```