package com.r3.refapp.client.controllers

import com.r3.refapp.client.NodeRPCConnection
import com.r3.refapp.client.response.IssueLoanResponse
import com.r3.refapp.client.utils.ControllerUtils
import com.r3.refapp.domain.PaginatedResponse
import com.r3.refapp.exceptions.RefappException
import com.r3.refapp.flows.*
import com.r3.refapp.flows.reports.GetAccountFlow
import com.r3.refapp.flows.reports.GetAccountsForCustomerPaginatedFlow
import com.r3.refapp.flows.reports.GetAccountsPaginatedFlow
import com.r3.refapp.repositories.AccountRepository
import com.r3.refapp.repositories.RepositoryQueryParams
import com.r3.refapp.schemas.CustomerSchemaV1
import com.r3.refapp.states.*
import com.r3.refapp.util.of
import net.corda.core.messaging.startFlow
import net.corda.core.utilities.getOrThrow
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.*
import java.time.Instant
import java.util.*


/**
 * Provides Account API endpoints.
 */
@RestController
@RequestMapping("/accounts") // The paths for HTTP requests are relative to this base path.
class AccountController(rpc: NodeRPCConnection){

    companion object {
        private val logger = LoggerFactory.getLogger(RestController::class.java)
    }
    private val proxy = rpc.proxy

    /**
     * Invokes the create current account flow which creates a new current account and returns the newly created account object
     * @param customerId the id of the customer
     * @param tokenType token type of the account balance
     * @param withdrawalDailyLimit withdrawal daily limit
     * @param transferDailyLimit transfer daily limit
     * @return [CurrentAccountState] the created current account
     */
    @PostMapping(value = ["/create-current-account"], produces = ["application/json"])
    private fun createCurrentAccount(@RequestParam(value = "customerId") customerId: UUID,
                                     @RequestParam(value = "tokenType") tokenType: String,
                                     @RequestParam(value = "withdrawalDailyLimit") withdrawalDailyLimit: Long?,
                                     @RequestParam(value = "transferDailyLimit") transferDailyLimit: Long?
    ): CurrentAccountState {
        val currencyType = ControllerUtils.getCurrencyInstanceFromString(tokenType)
        return proxy.startFlow(::CreateCurrentAccountFlow, customerId, currencyType,
                withdrawalDailyLimit, transferDailyLimit
        ).returnValue.getOrThrow().tx.outputsOfType<CurrentAccountState>().single()
    }

    /**
     * Invokes the create savings account flow which creates a new savings account and returns the newly created account object
     * @param customerId the id of the customer
     * @param tokenType amount token type of the account balance
     * @param currentAccountId id of the associated current account
     * @param savingsAmount monthly amount to be transferred from current to savings account
     * @param savingsStartDate date of the first savings payment
     * @param savingsPeriod savings period in months
     * @return [SavingsAccountState] the created savings account
     */
    @PostMapping(value = ["/create-savings-account"], produces = ["application/json"])
    private fun createSavingsAccount(@RequestParam(value = "customerId") customerId: UUID,
                                     @RequestParam(value = "tokenType") tokenType: String,
                                     @RequestParam(value = "currentAccountId") currentAccountId: UUID,
                                     @RequestParam(value = "savingsAmount") savingsAmount: Long,
                                     @RequestParam(value = "savingsStartDate") savingsStartDate: Instant,
                                     @RequestParam(value = "savingsPeriod") savingsPeriod: Int
    ): SavingsAccountState {
        val currencyType = ControllerUtils.getCurrencyInstanceFromString(tokenType)
        return proxy.startFlow(::CreateSavingsAccountFlow, customerId, currencyType,
                currentAccountId, savingsAmount of currencyType, savingsStartDate, savingsPeriod
        ).returnValue.getOrThrow().tx.outputsOfType<SavingsAccountState>().single()
    }

    /**
     * Public REST endpoint for overdraft approval. Invokes approve overdraft flow for given current account [currentAccountId] with given [amount] as
     * overdraft limit.
     * @param currentAccountId the id of the current account which will be approved as overdraft account
     * @param amount overdraft limit
     * @param tokenType token type of the balance
     * @return [CurrentAccountState] the current account with approved overdraft
     */
    @PutMapping(value = ["/approve-overdraft-account"], produces = ["application/json"])
    private fun approveOverdraftAccount(@RequestParam(value = "currentAccountId") currentAccountId: UUID,
                                        @RequestParam(value = "amount") amount: Long
    ): CurrentAccountState {
        return proxy.startFlow(::ApproveOverdraftFlow, currentAccountId, amount)
                .returnValue.getOrThrow().tx.outputsOfType<CurrentAccountState>().single()
    }

    /**
     * Public REST endpoint for loan issuance. Invokes issue loan flow for given [accountId] ID.
     * @param accountId ID of the account
     * @param loan principal of the loan
     * @param tokenType loan token type
     * @param periodInMonths repayment period
     * @return [String] the created two accounts in json format: {"currentAccount": $currentAccount, "loanAccount": $loanAccount}
     */
    @PostMapping(value = ["/issue-loan"], produces = ["application/json"])
    private fun issueLoan(@RequestParam(value = "accountId") accountId: UUID,
                          @RequestParam(value = "loanAmount") loan: Long,
                          @RequestParam(value = "tokenType") tokenType: String,
                          @RequestParam(value = "period") periodInMonths: Int
    ): IssueLoanResponse {

        val amount = ControllerUtils.getAmountFromQuantityAndCurrency(loan, tokenType)
        val txt = proxy.startFlow(::IssueLoanFlow, accountId, amount, periodInMonths)
                .returnValue.getOrThrow().tx
        val currAccount = txt.outputsOfType<CurrentAccountState>().single()
        val loanAccount = txt.outputsOfType<LoanAccountState>().single()
        return IssueLoanResponse(currAccount, loanAccount)
    }

    /**
     *  Sets the status of provided account to give [accountStatus]
     *  @param accountId id of the account
     *  @param accountStatus new status of the account
     *  @return [Account] the modified account object
     */
    @PutMapping(value = ["/set-status"], produces = ["application/json"])
    private fun setAccountStatus(@RequestParam(value = "accountId")  accountId: UUID,
                                 @RequestParam(value = "status") accountStatus: AccountStatus): Account {
        return proxy.startFlow(::SetAccountStatusFlow, accountId, accountStatus)
                .returnValue.getOrThrow().tx.outputsOfType<Account>().single()
    }

    /**
     *  Sets the withdrawal and transfer daily limits of a current account
     *  @param accountId id of the account
     *  @param withdrawalDailyLimit withdrawal daily limit
     *  @param transferDailyLimit transfer daily limit
     *  @return [CurrentAccountState] the modified account object
     */
    @PutMapping(value = ["/set-limits"], produces = ["application/json"])
    private fun setAccountLimit(@RequestParam(value = "accountId") accountId: UUID,
                                @RequestParam(value = "withdrawalDailyLimit") withdrawalDailyLimit: Long?,
                                @RequestParam(value = "transferDailyLimit") transferDailyLimit: Long?): CurrentAccountState {
        return proxy.startFlow(::SetAccountLimitsFlow, accountId, withdrawalDailyLimit, transferDailyLimit)
                .returnValue.getOrThrow().tx.outputsOfType<CurrentAccountState>().single()
    }

    /**
     * Endpoint retrieves [Account] for given [accountId].
     * @param accountId Id of the account
     * @return [Account] object for given [accountId]
     * @throws [RefappException] if account with given [accountId] cannot be found
     */
    @GetMapping(value = ["/{accountId}"], produces = ["application/json"])
    private fun getAccountById(@PathVariable(value = "accountId") accountId: UUID): Account {
        return proxy.startFlow(::GetAccountFlow, accountId)
                .returnValue.getOrThrow()
    }

    /**
     * Retrieves accounts and associated customers paginated,
     * @param startPage position of the result page to retrieve
     * @param pageSize specifies the maximum number of customers in one page
     * @param searchTerm can be matched in LIKE fashion against multiple fields, for full list @see [AccountRepository].
     * @param sortField sort result based on this field
     * @param sortOrder order of the sort (ASC or DESC) [RepositoryQueryParams.SortOrder]
     * @param dateFrom (Optional) parameter to filter accounts with txDate after given date
     * @param dateTo (Optional) parameter to filter accounts with txDate before given date
     * @return [PaginatedResponse<Pair<Account, CustomerSchemaV1.Customer>>]
     */
    @GetMapping(value = [""], produces = ["application/json"])
    private fun getAccounts(@RequestParam startPage: Int?, @RequestParam pageSize: Int?,
                            @RequestParam sortField: String?, @RequestParam sortOrder: RepositoryQueryParams.SortOrder?,
                            @RequestParam searchTerm: String?,
                            @RequestParam dateFrom: Instant?, @RequestParam dateTo: Instant?
    ): PaginatedResponse<Pair<Account, CustomerSchemaV1.Customer>> {
        val tmpStartPage = startPage ?: 1
        val tmpPageSize = pageSize ?: 100
        val tmpSearchTerm = searchTerm ?: ""
        val queryParam = RepositoryQueryParams(tmpStartPage, tmpPageSize, sortField, sortOrder ?: RepositoryQueryParams.SortOrder.ASC,
                tmpSearchTerm)
        return proxy.startFlow(::GetAccountsPaginatedFlow, queryParam, dateFrom, dateTo)
                .returnValue.getOrThrow()
    }

    /**
     * Retrieves accounts and associated customers paginated for given [customerId],
     * @param startPage position of the result page to retrieve
     * @param pageSize specifies the maximum number of customers in one page
     * @param searchTerm can be matched in LIKE fashion against multiple fields, for full list @see [AccountRepository].
     * @param sortField sort result based on this field
     * @param sortOrder order of the sort (ASC or DESC) [RepositoryQueryParams.SortOrder]
     * @param dateFrom (Optional) parameter to filter accounts with txDate after given date
     * @param dateTo (Optional) parameter to filter accounts with txDate before given date
     * @param customerId Id of the customer
     * @return [PaginatedResponse<Pair<Account, CustomerSchemaV1.Customer>>]
     */
    @GetMapping(value = ["customer/{customerId}"], produces = ["application/json"])
    private fun getAccountsForCustomer(@RequestParam startPage: Int?, @RequestParam pageSize: Int?,
                                       @RequestParam sortField: String?, @RequestParam sortOrder: RepositoryQueryParams.SortOrder?,
                                       @RequestParam searchTerm: String?, @RequestParam dateFrom: Instant?,
                                       @RequestParam dateTo: Instant?, @PathVariable customerId: UUID
    ): PaginatedResponse<Pair<Account, CustomerSchemaV1.Customer>> {
        val tmpStartPage = startPage ?: 1
        val tmpPageSize = pageSize ?: 100
        val tmpSearchTerm = searchTerm ?: ""
        val queryParam = RepositoryQueryParams(tmpStartPage, tmpPageSize, sortField, sortOrder ?: RepositoryQueryParams.SortOrder.ASC,
                tmpSearchTerm)
        return proxy.startFlow(::GetAccountsForCustomerPaginatedFlow, queryParam, customerId, dateFrom, dateTo)
                .returnValue.getOrThrow()
    }
}