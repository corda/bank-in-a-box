import { SortOrder } from '../store/types';
import { axiosInstance } from './apiConfig';
import qs from 'qs';
import { resolvePromise } from './resolvePromise';
import { trackPromise } from 'react-promise-tracker';

export type RecurringPaymentsSortFields =
    | 'accountFrom'
    | 'accountTo'
    | 'amount'
    | 'period'
    | 'linearId'
    | 'iterationNum';

/**
 * @remarks
 * Used for sending a request for an accounts recuring payments paginated response, based reon query params. Admin + Customer View
 *
 * @param accountId account Id
 * @param startPage the start page of paginated response
 * @param pageSize the page of paginated response
 * @param sortField the sort field result will be sorted by
 * @param sortOrder the sort order results will be sorted by
 * @param searchTerm the search term that the results will be filtered by
 * @param fromDate start date in range for filtering results
 * @param todate end date in range for filtering results
 *
 * @returns
 * AxiosResponse which resolves either in an error or response data
 */
export const getRecurringPaymentsByAccount = async (
    accountId: string,
    startPage: number,
    pageSize: number,
    sortField: string,
    sortOrder: SortOrder = 'ASC',
    fromDate?: string,
    todate?: string,
    searchTerm?: string
) => {
    return await resolvePromise(
        trackPromise(
            axiosInstance.get(`recurring-payments/account/${accountId}`, {
                params: {
                    startPage: startPage,
                    pageSize: pageSize,
                    dateFrom: fromDate,
                    dateTo: todate,
                    sortField: sortField,
                    sortOrder: sortOrder,
                    searchTerm: searchTerm,
                },
            })
        )
    );
};

/**
 * @remarks
 * Used for sending a request for all accounts recuring payments paginated response, based on query params. Admin + Customer View
 *
 * @param startPage the start page of paginated response
 * @param pageSize the page of paginated response
 * @param sortField the sort field result will be sorted by
 * @param sortOrder the sort order results will be sorted by
 * @param searchTerm the search term that the results will be filtered by
 * @param fromDate start date in range for filtering results
 * @param todate end date in range for filtering results
 *
 * @returns
 * AxiosResponse which resolves either in an error or response data
 */
export const getRecurringPayments = async (
    startPage: number,
    pageSize: number,
    searchTerm?: string,
    sortField: RecurringPaymentsSortFields = 'accountTo',
    sortOrder: SortOrder = 'ASC',
    fromDate?: string,
    todate?: string
) => {
    return await resolvePromise(
        trackPromise(
            axiosInstance.get('recurring-payments', {
                params: {
                    startPage: startPage,
                    pageSize: pageSize,
                    sortField: sortField,
                    sortOrder: sortOrder,
                    searchTerm: searchTerm,
                    dateFrom: fromDate,
                    dateTo: todate,
                },
            })
        )
    );
};

/**
 * @remarks
 * Used for sending a request for a customers recuring payments paginated, based on query params. Admin + Customer View
 *
 * @param customerId customers Id
 * @param startPage the start page of paginated response
 * @param pageSize the page of paginated response
 * @param sortField the sort field result will be sorted by
 * @param sortOrder the sort order results will be sorted by
 * @param searchTerm the search term that the results will be filtered by
 * @param fromDate start date in range for filtering results
 * @param todate end date in range for filtering results
 *
 * @returns
 * AxiosResponse which resolves either in an error or response data
 */
export const getCustomerRecurringPayments = async (
    customerId: string,
    startPage: number,
    pageSize: number,
    searchTerm: string = '',
    sortField: RecurringPaymentsSortFields = 'accountTo',
    sortOrder: SortOrder = 'ASC',
    fromDate?: string,
    todate?: string
) => {
    return await resolvePromise(
        trackPromise(
            axiosInstance.get(`recurring-payments/customer/${customerId}`, {
                params: {
                    startPage: startPage,
                    pageSize: pageSize,
                    sortField: sortField,
                    sortOrder: sortOrder,
                    searchTerm: searchTerm,
                    dateFrom: fromDate,
                    dateTo: todate,
                },
            })
        )
    );
};

/**
 * @remarks
 * Used for making a request to create a new recurring payment from an account to another
 *
 * @param fromAccountId origin account id
 * @param toAccountId destination account id
 * @param amount amount value
 * @param dateStart the start date of recurring payments
 * @param period the period between each recurring payment
 * @param iterationNum the amount of iterations of the reccurring payment
 */
export const createRecurringPayment = async (
    fromAccountId: string,
    toAccountId: string,
    amount: number,
    dateStart: string,
    period: string,
    iterationNum: number
) => {
    return resolvePromise(
        trackPromise(
            axiosInstance.post(
                'payments/create-recurring-payment',
                qs.stringify({
                    fromAccountId: fromAccountId,
                    toAccountId: toAccountId,
                    amount: amount,
                    tokenType: 'EUR',
                    dateStart: dateStart,
                    period: period,
                    iterationNum: iterationNum,
                })
            )
        )
    );
};
