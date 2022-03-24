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
};

const useCordaFlows = ({ updateBoard }: HookArgs) => {
    const [pollCount, setPollCount] = useState<number>(0);
    const [requestIds, setRequestIds] = useState<PollRequest[]>([]);
    const [pollingRequests, setPollingRequests] = useState<PollRequest[]>([]);

    const startGameFlow = async ({
        flowName,
        col,
        row,
        gameState,
        holderShortId,
        opponentX500Name,
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

        setRequestIds((prev) => prev.concat({ requestId: requestId, flowStatus: response.data.flowStatus }));
        setPollingRequests((prev) => prev.concat({ requestId: requestId, flowStatus: response.data.flowStatus }));
    };

    const getFlowStatus = async ({ flowStatus, requestId }: PollRequest) => {
        const completed = requestIds.find(
            (req) => req.requestId === requestId && req.flowStatus.flowStatus === 'COMPLETED'
        );
        if (completed) return;
        const response = await requestFlowStatus(flowStatus.holdingShortId, flowStatus.clientRequestId);
        if (response.data.flowStatus === 'COMPLETED') {
            updateBoard(JSON.parse(response.data.flowResult) as FlowResult);
        }
        const existingItem = requestIds.find(
            (req) => req.flowStatus.flowStatus === response.data.flowStatus && req.requestId === requestId
        );
        if (!existingItem) {
            setRequestIds((prev) => prev.concat({ requestId: requestId, flowStatus: response.data }));
        }
    };

    useEffect(() => {
        if (pollingRequests.length === 0) return;
        pollingRequests.forEach((req) => getFlowStatus(req));
    }, [pollCount]);

    useEffect(() => {
        const interval = setInterval(() => {
            setPollCount((prev) => prev + 1);
        }, 2000);

        return () => {
            clearInterval(interval);
        };
        // eslint-disable-next-line
    }, []);

    return { requestIds, startGameFlow };
};

export default useCordaFlows;
