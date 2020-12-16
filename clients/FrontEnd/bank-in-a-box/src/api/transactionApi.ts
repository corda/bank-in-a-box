import { SortOrder } from '../store/types';
import { axiosInstance } from './apiConfig';
import { resolvePromise } from './resolvePromise';
import { trackPromise } from 'react-promise-tracker';

export type TransactionSortFields = 'txId' | 'accountTo' | 'accountFrom' | 'amount' | 'currency' | 'txDate' | 'txType';

/**
 * @remarks
 * Used for sending a request for accounts transactions paginated based on query params. Admin + Customer View
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
export const getAccountTransactions = async (
    accountId: string,
    startPage: number,
    pageSize: number,
    searchTerm: string = '',
    sortField: TransactionSortFields = 'txId',
    sortOrder: SortOrder = 'ASC',
    fromDate?: string,
    todate?: string
) => {
    return await resolvePromise(
        trackPromise(
            axiosInstance.get(`/transactions/account/${accountId}`, {
                params: {
                    startPage: startPage,
                    pageSize: pageSize,
                    searchTerm: searchTerm,
                    sortField: sortField,
                    sortOrder: sortOrder,
                    dateFrom: fromDate,
                    dateTo: todate,
                },
            })
        )
    );
};

/**
 * @remarks
 * Used for sending a request for customers transactions paginated based on query params. Admin + Customer View
 *
 * @param customerId customers id
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
export const getCustomersTransactions = async (
    customerId: string,
    startPage: number,
    pageSize: number,
    searchTerm: string = '',
    sortField: TransactionSortFields = 'txId',
    sortOrder: SortOrder = 'ASC',
    fromDate?: string,
    todate?: string
) => {
    return await resolvePromise(
        trackPromise(
            axiosInstance.get(`/transactions/customer/${customerId}`, {
                params: {
                    startPage: startPage,
                    pageSize: pageSize,
                    searchTerm: searchTerm,
                    sortField: sortField,
                    sortOrder: sortOrder,
                    dateFrom: fromDate,
                    dateTo: todate,
                },
            })
        )
    );
};

/**
 * @remarks
 * Used for sending a request for all transactions paginated based on query params. Admin view
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
export const getTransactions = async (
    startPage: number,
    pageSize: number,
    searchTerm: string = '',
    sortField: TransactionSortFields = 'txId',
    sortOrder: SortOrder = 'ASC',
    fromDate?: string,
    todate?: string
) => {
    return resolvePromise(
        trackPromise(
            axiosInstance.get('/transactions', {
                params: {
                    startPage: startPage,
                    pageSize: pageSize,
                    searchTerm: searchTerm,
                    sortField: sortField,
                    sortOrder: sortOrder,
                    dateFrom: fromDate,
                    dateTo: todate,
                },
            })
        )
    );
};
