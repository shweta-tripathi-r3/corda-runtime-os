import { FlowResult, GameRequestBody, PollRequest } from 'model/common';
import { requestFlowStatus, requestStartFlow } from 'api/connect4';
import { useEffect, useState } from 'react';

import { BoardState } from 'model/BoardState';
import { FlowTypes } from 'api/config';

type HookArgs = {
    updateBoard: (boardState: FlowResult) => void;
};

type StartGameFlowArgs = {
    flowName: FlowTypes;
    col: number;
    row: number;
    gameState: BoardState[][];
    holderShortId: string;
    opponentX500Name: string;
    queueResponse: (response: any) => void;
};

const useCordaFlows = ({ updateBoard }: HookArgs) => {
    const [requestIds, setRequestIds] = useState<PollRequest[]>([]);

    const startGameFlow = async ({
        flowName,
        col,
        row,
        gameState,
        holderShortId,
        opponentX500Name,
        queueResponse,
    }: StartGameFlowArgs) => {
        const boardState: number[][] = [];

        gameState.forEach((col, colIndex) => {
            boardState[colIndex] = [];
            col.forEach((row, rowIndex) => {
                boardState[colIndex][rowIndex] = row === null ? 0 : 1;
            });
        });

        const requestBody: GameRequestBody = {
            opponentX500Name: opponentX500Name,
            column: col,
            row: row,
            boardState: boardState,
        };

        const requestId = Date.now().toString();
        const response = await requestStartFlow(holderShortId, requestId, flowName, {
            requestBody: JSON.stringify(requestBody),
        });
        queueResponse(response.data.flowStatus);
    };

    function queueResponse(initialStatus) {
        const requestId = initialStatus.clientRequestId;
        const existingItem = requestIds.find((req) => requestId === req.requestId);

        if (!existingItem && initialStatus.flowStatus === 'START_REQUESTED') {
            setRequestIds((prev) => prev.concat({ requestId: requestId, flowStatus: initialStatus }));
            return initialStatus;
        }

        // Nothing to do if the status has not changed
        if (existingItem?.flowStatus === initialStatus.flowStatus) {
            return null;
        }

        setRequestIds((prev) => prev.concat({ requestId: requestId, flowStatus: initialStatus }));
        return initialStatus;
    }

    useEffect(() => {
        const getFlowStatus = async ({ flowStatus, requestId }: PollRequest) => {
            const response = await requestFlowStatus(flowStatus.holdingShortId, flowStatus.clientRequestId);
            if (response.data.flowStatus === 'COMPLETED') {
                updateBoard(JSON.parse(response.data.flowResult) as FlowResult);
            }

            const startRequested = requestIds.find(
                (req) => req.requestId === requestId && req.flowStatus.flowStatus === 'START_REQUESTED'
            );
            const completed = requestIds.find(
                (req) => req.requestId === requestId && req.flowStatus.flowStatus === 'COMPLETED'
            );
            if (startRequested && completed) return;
            queueResponse(response.data);
        };
        setTimeout(() => {
            requestIds.forEach((req) => getFlowStatus(req));
        }, 2000);
    }, [requestIds]);

    return { requestIds, startGameFlow, queueResponse };
};

export default useCordaFlows;
