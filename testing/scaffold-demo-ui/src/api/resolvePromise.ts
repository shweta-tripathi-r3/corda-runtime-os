import { AxiosResponse } from 'axios';

export type ResolvedPromise = {
    data: any;
    error?: string;
    message?: string;
};

/**
 * @remakrs
 * Resolves a promise returned by a HTTP client
 *
 * @param promise the promise to be resolved
 */
export const resolvePromise = async (promise: Promise<AxiosResponse>) => {
    const resolved: ResolvedPromise = { data: null };
    try {
        const resolvedPromise = await promise;
        resolved.data = resolvedPromise.data;
    } catch (error: any) {
        resolved.error = error.response?.data
            ? error.response.data.message
                ? error.response.data.message
                : error.response.data.status + ' ' + error.response.data.error + ' ' + error.response.data.title
            : error;
    }
    return resolved;
};
