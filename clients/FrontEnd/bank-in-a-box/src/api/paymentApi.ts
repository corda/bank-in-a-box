import { axiosInstance } from './apiConfig';
import qs from 'qs';
import { resolvePromise } from './resolvePromise';
import { trackPromise } from 'react-promise-tracker';

/**
 * @remark
 * Used for making a request to make an account payment from one account to another (users own accounts or user -> user)
 *
 * @param fromAccountId the origin account id
 * @param toAccountId  the destination account id
 * @param amount the value amount to be transffered
 *
 * @returns
 * AxiosResponse which resolved in an error or repsonse data
 */
export const intrabankPayment = async (fromAccountId: string, toAccountId: string, amount: number) => {
    return resolvePromise(
        trackPromise(
            axiosInstance.post(
                'payments/intrabank-payment',
                qs.stringify({
                    fromAccountId: fromAccountId,
                    toAccountId: toAccountId,
                    amount: amount,
                    tokenType: 'EUR',
                })
            )
        )
    );
};
