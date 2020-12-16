import { AccountStatus, CurrencyType } from '../store/types';

import { CancelToken } from 'axios';
import { axiosInstance } from './apiConfig';
import qs from 'qs';
import { resolvePromise } from './resolvePromise';
import { trackPromise } from 'react-promise-tracker';

export type AccountSortFields =
    | 'account'
    | 'status'
    | 'txDate'
    | 'customerId'
    | 'linearId'
    | 'balance'
    | 'customerName';

/**
 * @remarks
 * Used for sending a request for getting accounts paginated based on query params. Admin view request
 *
 * @param startPage the start page of paginated response
 * @param pageSize the page of paginated response
 * @param sortField the sort field result will be sorted by
 * @param sortOrder the sort order results will be sorted by
 * @param searchTerm the search term that the results will be filtered by
 * @param cancelToken the cancel token which can cancel the request (used for interupting in type ahead search) (optional)
 *
 * @returns
 * AxiosResponse which resolves either in an error or response data
 */
export const getAccountsPaginatedWithSort = async (
    startPage: number = 1,
    pageSize: number = 10,
    sortField: AccountSortFields = 'account',
    sortOrder: 'DESC' | 'ASC' = 'ASC',
    searchTerm: string = '',
    cancelToken?: CancelToken
) => {
    return await resolvePromise(
        trackPromise(
            axiosInstance.get(`accounts`, {
                params: {
                    startPage: startPage,
                    pageSize: pageSize,
                    sortField: sortField,
                    sortOrder: sortOrder,
                    searchTerm: searchTerm,
                },
                cancelToken: cancelToken,
            })
        )
    );
};

/**
 * @remarks
 * Used for sending a request for customers accounts paginated based on query params. Admin view request
 *
 * @param customerId customers id
 * @param startPage the start page of paginated response
 * @param pageSize the page of paginated response
 * @param sortField the sort field result will be sorted by
 * @param sortOrder the sort order results will be sorted by
 * @param searchTerm the search term that the results will be filtered by
 * @param cancelToken the cancel token which can cancel the request (used for interupting in type ahead search) (optional)
 *
 * @returns
 * AxiosResponse which resolves either in an error or response data
 */
export const getCustomersAccountsPaginatedWithSort = async (
    customerId: string,
    startPage: number = 1,
    pageSize: number = 10,
    sortField: AccountSortFields = 'customerName',
    sortOrder: 'DESC' | 'ASC' = 'ASC',
    searchTerm: string = '',
    cancelToken?: CancelToken
) => {
    return resolvePromise(
        trackPromise(
            axiosInstance.get(`accounts/customer/${customerId}`, {
                params: {
                    startPage: startPage,
                    pageSize: pageSize,
                    sortField: sortField,
                    sortOrder: sortOrder,
                    searchTerm: searchTerm,
                },
                cancelToken: cancelToken,
            })
        )
    );
};

/**
 * @remarks
 * Method used for getting an account by its account ID. Admin + Customer resquest.
 *
 * @param accountId the id of the account
 *
 * @returns
 * AxiosResponse which resolves either in an error or response data
 */
export const getAccountById = async (accountId: string) => {
    return await resolvePromise(trackPromise(axiosInstance.get(`accounts/${accountId}`)));
};

/**
 * @remakrs
 * Used for making a request to create a new current account for an existing customer
 *
 * @param customerId the customers Id accounts is being created for
 * @param tokenType the token type (currency) e.g EUR
 * @param withdrawalDailyLimit the limit for daily withdrawals (optional)
 * @param transferDailyLimit the limit for daily transfers (optional)
 *
 * @returns
 * AxiosResponse which resolves either in an error or response data
 */
export const createCurrentAccount = async (
    customerId: string,
    tokenType: string,
    withdrawalDailyLimit: string,
    transferDailyLimit: string
) => {
    return await resolvePromise(
        trackPromise(
            axiosInstance.post(
                'accounts/create-current-account',
                qs.stringify({
                    customerId: customerId,
                    tokenType: tokenType,
                    withdrawalDailyLimit: withdrawalDailyLimit,
                    transferDailyLimit: transferDailyLimit,
                })
            )
        )
    );
};

/**
 * @remarks
 * Used for making a request to create a new savings account for an existing customer. Admin request.
 *
 * @param customerId the customers Id accounts is being created for
 * @param tokenType the token type (currency) e.g EUR
 * @param currentAccountId the account of the associated current account
 * @param savingsAmount the savings amount value
 * @param savingsStartDate the start date for savings account
 * @param savingsPeriod the period for savings account
 *
 * @returns
 * AxiosResponse which resolves either in an error or response data
 */
export const createSavingsAccount = async (
    customerId: string,
    tokenType: string,
    currentAccountId: string,
    savingsAmount: number,
    savingsStartDate: string,
    savingsPeriod: string
) => {
    return await resolvePromise(
        trackPromise(
            axiosInstance.post(
                'accounts/create-savings-account',
                qs.stringify({
                    customerId: customerId,
                    tokenType: tokenType,
                    currentAccountId: currentAccountId,
                    savingsAmount: savingsAmount,
                    savingsStartDate: savingsStartDate,
                    savingsPeriod: savingsPeriod,
                })
            )
        )
    );
};

/**
 * @remarks
 * Used for sending a request to approve an overdraft on an active current account. Admin Request.
 *
 * @param currentAccountId the current account id
 * @param amount
 * @param tokenType
 *
 * @returns
 * AxiosResponse which resolves either in an error or response data
 */
export const approveOverdraft = async (currentAccountId: string, amount: number) => {
    return await resolvePromise(
        trackPromise(
            axiosInstance.put(
                'accounts/approve-overdraft-account',
                qs.stringify({
                    currentAccountId: currentAccountId,
                    amount: amount,
                })
            )
        )
    );
};

/**
 * @remakrs
 *
 * @param accountId
 * @param status
 *
 * @returns
 * AxiosResponse which resolves either in an error or response data
 */
export const setAccountStatus = async (accountId: string, status: AccountStatus) => {
    return await resolvePromise(
        trackPromise(axiosInstance.put('accounts/set-status', qs.stringify({ accountId: accountId, status: status })))
    );
};

/**
 * @remarks
 * Used for making a request to set new transfer and withdrawal limits for a current account
 *
 * @param accountId the current account ID
 * @param withdrawalDailyLimit the value for the daily withdrawal limit (optional)
 * @param transferDailyLimit the value for the daily transfer limit (optional)
 *
 * @returns
 * AxiosResponse which resolves either in an error or response data
 */
export const setAccountLimits = async (
    accountId: string,
    withdrawalDailyLimit?: number,
    transferDailyLimit?: number
) => {
    return await resolvePromise(
        trackPromise(
            axiosInstance.put(
                'accounts/set-limits',
                qs.stringify({
                    accountId: accountId,
                    withdrawalDailyLimit: withdrawalDailyLimit,
                    transferDailyLimit: transferDailyLimit,
                })
            )
        )
    );
};

/**
 * @remarks
 * Used for making a request for issuing a loan to an active current account
 *
 * @param accountId the current account ID
 * @param loanAmount the value of loan amount to issue
 * @param period the period of the loan
 * @param tokenType the token type (currency)
 *
 * @returns
 * AxiosResponse which resolves either in an error or response data
 */
export const issueLoan = async (accountId: string, loanAmount: number, period: number, tokenType: CurrencyType) => {
    return await resolvePromise(
        trackPromise(
            axiosInstance.post(
                'accounts/issue-loan',
                qs.stringify({ accountId: accountId, loanAmount: loanAmount, period: period, tokenType: tokenType })
            )
        )
    );
};

/**
 * @remarks
 * Used for making a request to deposit into a current account
 *
 * @param accountId current account ID
 * @param tokenType the type of token deposit value is in (currency type)
 * @param amount the amount value for deposit
 *
 * @returns
 * AxiosResponse which resolves either in an error or response data
 */
export const depositFiat = async (accountId: string, tokenType: CurrencyType, amount: number) => {
    return resolvePromise(
        trackPromise(
            axiosInstance.post(
                '/payments/deposit-fiat',
                qs.stringify({ accountId: accountId, tokenType: tokenType, amount: amount })
            )
        )
    );
};

/**
 * @remarks
 * This is used for making a request to withdraw a value from a current account
 *
 * @param accountId the current account ID
 * @param tokenType the type of token witdrawal value is in (currency type)
 * @param amount the amount value of withdrawal
 *
 * @returns
 * AxiosResponse which resolves either in an error or response data
 */
export const withdrawFiat = async (accountId: string, tokenType: CurrencyType, amount: number) => {
    return resolvePromise(
        trackPromise(
            axiosInstance.post(
                '/payments/withdraw-fiat',
                qs.stringify({ accountId: accountId, tokenType: tokenType, amount: amount })
            )
        )
    );
};
