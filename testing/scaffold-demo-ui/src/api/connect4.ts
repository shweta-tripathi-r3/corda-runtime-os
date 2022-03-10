import axiosInstance, { FlowTypes } from './config';

export const requestStartFlow = async (
    holderShortId: string,
    clientRequestId: string,
    flowType: FlowTypes,
    payload: any
) => {
    return axiosInstance.post(`/api/v1/flow/start/${holderShortId}/${clientRequestId}/${flowType}`, payload);
};

export const requestFlowStatus = async (shortId: string, clientRequestId: string) => {
    return axiosInstance.get(`/api/v1/flow/status/${shortId}/${clientRequestId}`);
};
