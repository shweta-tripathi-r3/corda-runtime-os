import { ResolvedPromise, resolvePromise } from './resolvePromise';
import axios, { AxiosInstance, AxiosRequestConfig, AxiosStatic, CancelToken } from 'axios';

import { trackPromise } from 'react-promise-tracker';

//Globally tracking all api calls with react-promise-tracker
//TODO Make dynamic
export const CLUSTERS: { [key: string]: string } = {
    cluster0: 'https://localhost:8888',
    cluster1: 'https://localhost:8888',
    cluster2: 'https://localhost:8888',
};

export const P2P_GATEWAYS: { [key: string]: string } = {
    cluster0: 'corda-p2p-gateway-worker.corda-con-demo-cluster-a',
    cluster1: 'corda-p2p-gateway-worker.corda-con-demo-cluster-b',
    cluster2: 'corda-p2p-gateway-worker.corda-con-demo-cluster-c',
};

export const P2P_GATEWAY_PORT = "8080"

export type ApiCallParams = {
    method: 'get' | 'post' | 'put';
    path: string;
    dontTrackRequest?: boolean;
    params?: any;
    config?: any;
    cancelToken?: CancelToken;
    axiosInstance?: AxiosInstance;
    auth?: { username: string; password: string };
    cluster?: string
};

export default async function apiCall({
    method,
    path,
    dontTrackRequest,
    params,
    cancelToken,
    config,
    axiosInstance,
    auth,
    cluster = "cluster0"
}: ApiCallParams): Promise<ResolvedPromise> {
    const parameters = method === 'get' ? { params } : { data: params };
    const requestConfig: AxiosRequestConfig = {
        baseURL: CLUSTERS[cluster],
        url: `${path}`,
        method,
        cancelToken: cancelToken,
        auth: auth,
        ...config,
        ...parameters,
    };
    const axiosHandler: AxiosInstance | AxiosStatic = axiosInstance ?? axios;

    return dontTrackRequest
        ? await resolvePromise(axiosHandler(requestConfig))
        : await resolvePromise(trackPromise(axiosHandler(requestConfig)));
}
