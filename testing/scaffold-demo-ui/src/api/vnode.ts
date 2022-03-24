import axiosInstance from './config';
import { resolvePromise } from './resolvePromise';

export const getVNodeList = async () => {
    return axiosInstance.get(`/api/v1/virtualnode/list`);
};

export const requestCreateNode = async (cpiIdHash: string, x500Name: string) => {
    return resolvePromise(
        axiosInstance.post(`/api/v1/virtualnode/create`, {
            request: {
                cpiFileChecksum: cpiIdHash,
                x500Name: x500Name,
            },
        })
    );
};
