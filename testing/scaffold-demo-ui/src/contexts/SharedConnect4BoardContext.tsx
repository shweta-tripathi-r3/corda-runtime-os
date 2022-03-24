import { BoardState } from 'model/BoardState';
import { FlowResult } from 'model/common';
import { createCtx } from 'contexts/createCtx';
import { useState } from 'react';

type ContextProps = {
    gameState: BoardState[][];
    updateBoard: (boardState: FlowResult) => void;
    currentPlayerTurn: string;
    playerOne: string;
    playerTwo: string;
};

const [useSharedConnect4BoardContext, Provider] = createCtx<ContextProps>();
export default useSharedConnect4BoardContext;

type ProviderProps = {
    children?: React.ReactNode;
    playerOne: string;
    playerTwo: string;
    boardSizeX: number;
    boardSizeY: number;
};

export const SharedConnect4BoardContextProvider = ({
    children,
    playerOne,
    playerTwo,
    boardSizeX,
    boardSizeY,
}: ProviderProps) => {
    const initialState = (): BoardState[][] => {
        return Array.from({ length: boardSizeY }, () => Array.from({ length: boardSizeX }, () => null));
    };

    const [gameState, setGameState] = useState<BoardState[][]>(initialState());
    const [currentPlayerTurn, setCurrentPlayerTurn] = useState<string>(playerOne);

    const updateBoard = ({ boardState, player1X500Name }: FlowResult) => {
        const tempBoardState = [...gameState];
        let colIndexToUpdate = 100;
        let rowIndexToUpdate = 100;
        outerloop: for (let i = 0; i < boardSizeY; i++) {
            for (let j = 0; j < boardSizeX; j++) {
                if (boardState[i][j] === 1 && tempBoardState[i][j] === null) {
                    colIndexToUpdate = i;
                    rowIndexToUpdate = j;
                    break outerloop;
                }
            }
        }
        tempBoardState[colIndexToUpdate][rowIndexToUpdate] = player1X500Name === playerOne ? 'X' : 'O';
        setGameState(tempBoardState);
        setCurrentPlayerTurn(player1X500Name === playerOne ? playerTwo : playerOne);
    };

    return <Provider value={{ gameState, updateBoard, currentPlayerTurn, playerOne, playerTwo }}>{children}</Provider>;
};
