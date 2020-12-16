import { AxiosResponse } from 'axios';

export type ResolvedPromise = {
    data: any;
    error: string | null;
    message?: string | null;
};

/**
 * @remakrs
 * Resolves a promise returned by a HTTP client
 *
 * @param promise the promise to be resolved
 */
export const resolvePromise = async (promise: Promise<AxiosResponse>) => {
    const resolved: ResolvedPromise = {
        data: null,
        error: null,
        message: null,
    };
    try {
        resolved.data = await promise;
    } catch (error) {
        resolved.error = error.response?.data
            ? error.response.data.message
                ? error.response.data.message
                : error.response.data.error_description
            : error;
    }
    return resolved;
};
