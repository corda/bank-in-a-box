import { AxiosError, AxiosResponse } from 'axios';

import { EXPIREDTOKENERROR } from '../constants/APIERRORS';
import { axiosInstance } from './apiConfig';
import qs from 'qs';
import { resolvePromise } from './resolvePromise';
import { trackPromise } from 'react-promise-tracker';

const clientId = (window as any).REACT_APP_CLIENTID!.trim();
const clientSecret = (window as any).REACT_APP_CLIENT_SECRET!.trim();

/**
 * Interceptor for axios http client instance to refresh access token
 * This will only be triggered when a request which uses a stored access token failed
 * The request will be retried with a new access token (obtained using refresh token) (if the refresh token is not expired)
 */
axiosInstance.interceptors.response.use(
    (response: AxiosResponse) => {
        return response;
    },
    async function (error: AxiosError) {
        const originalRequest = error.config;
        if (error.response?.data?.error_description?.includes(EXPIREDTOKENERROR)) {
            const refreshToken = sessionStorage.getItem('refresh_token')!;
            if (!refreshToken) {
                return Promise.reject(error);
            }
            const tokenResponse = await refreshTokens(refreshToken);
            const data = tokenResponse.data.data;
            sessionStorage.setItem('refresh_token', data.refresh_token);
            axiosInstance.defaults.headers.common['Authorization'] = `bearer ${data.access_token}`;
            originalRequest.headers['Authorization'] = `bearer ${data.access_token}`;
            return new Promise((resolve, reject) => {
                axiosInstance
                    .request(originalRequest)
                    .then((response) => {
                        resolve(response);
                    })
                    .catch((error) => {
                        reject(error);
                    });
            });
        }
        return Promise.reject(error);
    }
);

/**
 * @remarks
 * Used to refresh the access and refresh tokens for authenticated user, if the refresh token is still valid
 *
 * @param refreshToken the refresh token auth user holds
 *
 * @returns
 * AxiosPromise which resolves in an error or response data
 */
export const refreshTokens = async (refreshToken: string) => {
    return resolvePromise(
        trackPromise(
            axiosInstance.post(
                'oauth/token',
                qs.stringify({ grant_type: 'refresh_token', refresh_token: refreshToken }),
                {
                    auth: { username: clientId, password: clientSecret },
                }
            )
        )
    );
};

/**
 * @remarks
 * Used for making a login request by admin or customer
 *
 * @param username the username
 * @param password the password
 *
 * @returns
 * AxiosResponse which resolves in an error or response data
 */
export const logInUser = async (username: string, password: string) => {
    return await resolvePromise(
        trackPromise(
            axiosInstance.post(
                `oauth/token`,
                qs.stringify({ grant_type: 'password', username: username, password: password }),
                {
                    auth: { username: clientId, password: clientSecret },
                }
            )
        )
    );
};

/**
 * @remakrs
 * Used for making a request to register a new user after their customer data has been created by Admin
 *
 * @param username the new username of the user
 * @param password the users new password
 * @param email the users email address
 * @param customerId the customer id if the user is creating a customer account (optional)
 * @param file the file to send for verification (comparison with already uploaded document by admin)
 *
 * @returns
 * AxiosResponse which resolves in an error or response data
 */
export const registerUser = async (
    username: string,
    password: string,
    email: string,
    customerId?: string,
    file?: File
) => {
    const form = new FormData();

    form.append('username', username);
    form.append('password', password);
    form.append('email', email);

    if (customerId) {
        form.append('customerId', customerId);
    }
    if (file) {
        form.append('file', file);
    }
    return resolvePromise(
        trackPromise(
            axiosInstance.post('/register/guest', form, { headers: { 'Content-Type': 'multipart/form-data' } })
        )
    );
};

/**
 * @remakrs
 * Used to make a request to assign a new role to a user
 *
 * @param username the username which role to be changed
 * @param role the new role to be assigned
 *
 * @returns
 * AxiosResponse which resolves in an error or response data
 */
export const assignRoleToUser = async (username: string, role: string) => {
    return resolvePromise(
        trackPromise(
            axiosInstance.post('/register/admin/addRole', qs.stringify({ username: username, roleName: role }))
        )
    );
};
