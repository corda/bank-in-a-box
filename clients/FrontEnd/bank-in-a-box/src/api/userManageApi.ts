import { SortOrder } from '../store/types';
import { axiosInstance } from './apiConfig';
import qs from 'qs';
import { resolvePromise } from './resolvePromise';
import { trackPromise } from 'react-promise-tracker';

export const getCustomerNameForAccount = async (accountId: string) => {
    return resolvePromise(trackPromise(axiosInstance.get(`customers/name/${accountId}`)));
};

export type ApiRoles = 'ADMIN' | 'GUEST' | 'CUSTOMER';

export type ApiUsersSortField = 'username' | 'email' | 'roles';

/**
 * @remarks
 * Used to making a request for a list of users paginated
 *
 * @param roleName the name of the role
 * @param startPage the start page of paginated results
 * @param pageSize the page size of paginated results
 * @param searchTerm the search term results will be filtered by
 * @param sortField the sort field results will be sorted by
 * @param sortOrder the order in which results will be sorted
 */
export const getUsers = async (
    roleName: ApiRoles,
    startPage: number,
    pageSize: number,
    searchTerm?: string,
    sortField: ApiUsersSortField = 'username',
    sortOrder: SortOrder = 'ASC'
) => {
    return resolvePromise(
        trackPromise(
            axiosInstance.get('register/admin/users', {
                params: {
                    roleName: roleName,
                    startPage: startPage,
                    pageSize: pageSize,
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
 * Used for making a request to revoke a users Admin or Customer role
 *
 * @param username the username
 * @param roleName the rolename to revoke
 */
export const revokeUser = async (username: string, roleName: ApiRoles) => {
    return resolvePromise(
        trackPromise(
            axiosInstance.post('register/admin/revokeRole', qs.stringify({ username: username, roleName: roleName }))
        )
    );
};
