import { FlowTypes, requestFlowStatus, requestStartFlow } from '@/api/flows';

import { FlowStatus } from '@/models/common';
import { FLOW_STATUS_INTERVAL } from '@/config';

type HandleFlowParams = {
    flowType: FlowTypes;
    holderShortId: string;
    payload?: any;
    clientRequestId?: string;
    pollIntervalMs?: number;
    onStartFailure?: (errorText: string) => void;
    onStartSuccess?: (data: any) => void;
    onStatusFailure?: (errorText: string) => void;
    onStatusSuccess?: (flowResult: string) => void;
    auth?: { username: string; password: string };
    cluster: string
};

export const handleFlow = async ({
    flowType,
    holderShortId,
    payload,
    clientRequestId = Date.now().toString(),
    pollIntervalMs = FLOW_STATUS_INTERVAL,
    onStartFailure,
    onStartSuccess,
    onStatusSuccess,
    onStatusFailure,
    auth,
    cluster
}: HandleFlowParams) => {
    const response = await requestStartFlow(holderShortId, clientRequestId, flowType, cluster, payload, auth);
    if (response.error) {
        if (onStartFailure) {
            onStartFailure(response.error);
        }
    } else {
        if (onStartSuccess) {
            onStartSuccess(response.data);
        }
    }
    return setupFlowStatusPolling({
        holderShortId,
        clientRequestId,
        pollIntervalMs,
        onStatusSuccess,
        onStatusFailure,
        auth,
        cluster
    });
};

type FlowStatusPollingParams = {
    holderShortId: string;
    clientRequestId?: string;
    pollIntervalMs?: number;
    onStatusFailure?: (errorText: string) => void;
    onStatusSuccess?: (flowResult: string) => void;
    onError?: (errorText: string) => void;
    auth?: { username: string; password: string };
    cluster: string
};

export const setupFlowStatusPolling = ({
    holderShortId,
    clientRequestId = Date.now().toString(),
    pollIntervalMs = FLOW_STATUS_INTERVAL,
    onStatusSuccess,
    onStatusFailure,
    onError,
    auth,
    cluster
}: FlowStatusPollingParams) => {
    const flowPollingInterval = setInterval(async () => {
        const response = await requestFlowStatus(holderShortId, clientRequestId, cluster, auth);
        if (response.error) {
            if (onError) {
                onError(response.error);
            }
            clearInterval(flowPollingInterval);
        }

        const flowStatusData: FlowStatus = response.data;

        if (flowStatusData.flowStatus === 'COMPLETED') {
            if (onStatusSuccess) {
                onStatusSuccess(flowStatusData.flowResult);
            }
            clearInterval(flowPollingInterval);
        }

        if (flowStatusData.flowStatus === 'FAILED') {
            if (onStatusFailure) {
                onStatusFailure(flowStatusData.flowResult);
            }
            clearInterval(flowPollingInterval);
        }
    }, pollIntervalMs);

    return flowPollingInterval;
};
