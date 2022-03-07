import { CSSProperties, useState } from 'react';
import { IconButton, Paper, Typography } from '@material-ui/core';

import CloseRoundedIcon from '@material-ui/icons/CloseRounded';
import { GameProps } from 'model/common';
import RadioButtonUncheckedRoundedIcon from '@material-ui/icons/RadioButtonUncheckedRounded';
import RefreshRoundedIcon from '@material-ui/icons/RefreshRounded';
import RequestTable from 'components/requestTable';
import { TTT_BOARD_SIZE } from 'constants/tictactoe';
import useCordaFlows from 'hooks/useCordaFlows';
import useGameWrapperStyles from 'materialStyles/gameWrapperStyles';
import useSharedConnect4BoardContext from 'contexts/SharedConnect4BoardContext';
import useTicTacToeStyles from 'materialStyles/ticTacToeStyles';

type State = 'X' | 'O' | null;

const initialState = (): State[][] => {
    return Array.from({ length: TTT_BOARD_SIZE }, () => Array.from({ length: TTT_BOARD_SIZE }, () => null));
};

const turnStates: State[] = ['X', 'O'];

const getClickableBoxStyle = (colIndex: number, rowIndex: number): CSSProperties => {
    const style: CSSProperties = {};
    if (rowIndex === 0) style.borderLeft = 'none';
    if (rowIndex === TTT_BOARD_SIZE - 1) style.borderRight = 'none';
    if (colIndex === 0) style.borderTop = 'none';
    if (colIndex === TTT_BOARD_SIZE - 1) style.borderBottom = 'none';
    return style;
};

const TicTacToe: React.FunctionComponent<GameProps> = ({ holderShortId, playerName, opponentX500Name }) => {
    const classes = useTicTacToeStyles();
    const gameWrapperClasses = useGameWrapperStyles();
    // const [turnCount, setTurnCount] = useState<number>(0);
    //const [gameState, setGameState] = useState<State[][]>(initialState());
    const { gameState, updateBoard, currentPlayerTurn, playerOne } = useSharedConnect4BoardContext();
    const { requestIds, queueResponse, startGameFlow } = useCordaFlows({ updateBoard: updateBoard });

    const getBoxRender = (colIndex: number, rowIndex: number) => {
        switch (gameState[colIndex][rowIndex]) {
            case 'O':
                return <RadioButtonUncheckedRoundedIcon className={classes.boxIcon} />;
            case 'X':
                return <CloseRoundedIcon className={`${classes.boxIcon} ${classes.nought}`} />;
            default:
                return null;
        }
    };

    const handleClick = async (colIndex: number, rowIndex: number) => {
        // //Will need to send some data to the corda node here and wait for response
        // //Update the state based on the data returned from the corda node.
        // if (!gameState[colIndex][rowIndex]) {
        //     const tempGameState = [...gameState];
        //     tempGameState[colIndex][rowIndex] = turnStates[turnCount % 2];
        //     setGameState(tempGameState);
        //     setTurnCount((prev) => prev + 1);
        // }
        await startGameFlow({
            flowName: 'net.corda.demo.tictactoe.StartTicTacToeGameFlow',
            col: colIndex,
            row: rowIndex,
            gameState: gameState,
            holderShortId: holderShortId,
            opponentX500Name: opponentX500Name,
            queueResponse: queueResponse,
        });
    };

    const myTurn = playerName === currentPlayerTurn;

    return (
        <div>
            <Paper className={gameWrapperClasses.wrapper} elevation={10}>
                <div style={{ width: '100%' }}>
                    <div style={{ width: '100%', display: 'flex', flexDirection: 'column' }}>
                        <Typography
                            variant="h5"
                            style={{
                                color: myTurn ? (playerName === playerOne ? 'lightgreen' : 'indianred') : 'white',
                                margin: 'auto',
                            }}
                        >
                            {playerName}
                        </Typography>
                        <Typography style={{ color: 'white', margin: 'auto', marginTop: 12 }}>
                            V-Node Short ID: {holderShortId}
                        </Typography>
                    </div>
                    <div
                        className={classes.board}
                        style={{
                            opacity: myTurn ? 1 : 0.4,
                            pointerEvents: myTurn ? 'all' : 'none',
                            marginLeft: 134,
                            marginRight: 134,
                        }}
                    >
                        {Array.from(Array(TTT_BOARD_SIZE)).map((a, colIndex) => {
                            return (
                                <div key={colIndex} style={{ display: 'flex' }}>
                                    {Array.from(Array(TTT_BOARD_SIZE)).map((b, rowIndex) => (
                                        <div
                                            id={`${colIndex}, ${rowIndex}`}
                                            key={`${colIndex}, ${rowIndex}`}
                                            style={getClickableBoxStyle(colIndex, rowIndex)}
                                            className={classes.clickableBox}
                                            onClick={() => {
                                                handleClick(colIndex, rowIndex);
                                            }}
                                        >
                                            {getBoxRender(colIndex, rowIndex)}
                                        </div>
                                    ))}
                                </div>
                            );
                        })}
                    </div>
                </div>
            </Paper>
            <Paper
                className={gameWrapperClasses.wrapper}
                style={{ maxHeight: 200, overflowY: 'auto', overflowX: 'hidden' }}
                elevation={10}
            >
                <RequestTable requestIds={requestIds} />
            </Paper>
        </div>
    );
};

export default TicTacToe;
