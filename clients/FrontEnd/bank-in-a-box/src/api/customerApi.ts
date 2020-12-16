import { CancelToken } from 'axios';
import { axiosInstance } from './apiConfig';
import qs from 'qs';
import { resolvePromise } from './resolvePromise';
import { trackPromise } from 'react-promise-tracker';

export type CustomerSortFields = 'customerName' | 'contactNumber' | 'emailAddress';

/**
 * @remarks
 * Used for sending a request for getting customers paginated based on query params. Admin view request
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
export const getCustomerPaginatedWithSort = async (
    startPage: number = 1,
    pageSize: number = 10,
    sortField: CustomerSortFields = 'customerName',
    sortOrder: 'DESC' | 'ASC' = 'ASC',
    searchTerm: string = '',
    cancelToken?: CancelToken
) => {
    return await resolvePromise(
        trackPromise(
            axiosInstance.get(`customers`, {
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
 * Used for getting a specific customer data by the customers Id
 *
 * @param custId the customers Id
 *
 * @returns
 * AxiosResponse which resolves in an error or response data
 */
export const getCustomerById = async (custId: string) => {
    return await resolvePromise(trackPromise(axiosInstance.get(`customers/${custId}`)));
};

type LooseParams = {
    [key: string]: string | string[];
};

/**
 * @remarks
 * Used by the customer to update their own customer data
 *
 * @param custId customer Id
 * @param contactNumber customers contact number
 * @param emailAddress customers email address
 * @param attachments any new attachments to be uploaded
 *
 * @returns
 * AxiosResponse which resolves into an error or response data
 */
export const updateCustomerCustomer = async (
    custId: string,
    contactNumber?: string,
    emailAddress?: string,
    attachments?: string[]
) => {
    let body: LooseParams = {};

    if (contactNumber && contactNumber.length > 0) {
        body.contactNumber = contactNumber;
    }

    if (emailAddress && emailAddress.length > 0) {
        body.emailAddress = emailAddress;
    }

    if (attachments && attachments.length > 0) {
        body.attachments = attachments.toString();
    }
    return await resolvePromise(trackPromise(axiosInstance.put(`customers/update/${custId}`, qs.stringify(body))));
};

/**
 * @remarks
 * Used for making a request to update a customers data by an Admin
 *
 * @param custId customers id to be updated
 * @param customerName customers name to be updated
 * @param postCode postcode name to be updated
 * @param contactNumber contact number name to be updated
 * @param emailAddress email address name to be updated
 * @param attachments any new attachments to be uploaded
 */
export const updateCustomerAdmin = async (
    custId: string,
    customerName: string,
    postCode: string,
    contactNumber?: string,
    emailAddress?: string,
    attachments?: string[]
) => {
    let body: LooseParams = {};

    if (contactNumber && contactNumber.length > 0) {
        body.contactNumber = contactNumber;
    }

    if (emailAddress && emailAddress.length > 0) {
        body.emailAddress = emailAddress;
    }

    if (attachments && attachments.length > 0) {
        body.attachments = attachments.toString();
    }

    body.customerName = customerName;
    body.postCode = postCode;

    return await resolvePromise(trackPromise(axiosInstance.put(`customers/update/${custId}`, qs.stringify(body))));
};

/**
 * @remarks
 * Used to upload attachments for a customer to corda
 *
 * @param uploader the uploader name
 * @param file the file to be uploaded
 */
export const uploadAttachment = async (uploader: string, file: File) => {
    const form = new FormData();
    form.append('uploader', uploader);
    form.append('file', file);
    return await resolvePromise(
        trackPromise(
            axiosInstance.post(`customers/upload-attachment`, form, {
                headers: { 'Content-Type': 'multipart/form-data' },
            })
        )
    );
};

/**
 * @remarks
 * Used to make a request to create a new customer
 *
 * @param customerName the customers name
 * @param contactNumber the contact number
 * @param emailAddress the email address
 * @param postCode the post code
 * @param attachments attachments names to be uploaded
 */
export const createCustomer = async (
    customerName: string,
    contactNumber: string,
    emailAddress: string,
    postCode: string,
    attachments: string[]
) => {
    return await resolvePromise(
        trackPromise(
            axiosInstance.post(
                `customers/create`,
                qs.stringify({
                    customerName: customerName,
                    contactNumber: contactNumber,
                    emailAddress: emailAddress,
                    postCode: postCode,
                    attachments: attachments.toString(),
                })
            )
        )
    );
};
