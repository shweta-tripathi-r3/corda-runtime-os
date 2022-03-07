import { C4_BOARD_HEIGHT, C4_BOARD_SIZE_X, C4_BOARD_SIZE_Y } from 'constants/connect4';
import { Paper, Typography } from '@material-ui/core';

import FiberManualRecordIcon from '@material-ui/icons/FiberManualRecord';
import { GameProps } from 'model/common';
import RequestTable from 'components/requestTable';
import SportsHandballIcon from '@material-ui/icons/SportsHandball';
import useConnect4Styles from 'materialStyles/connectFourStyles';
import useCordaFlows from 'hooks/useCordaFlows';
import useGameWrapperStyles from 'materialStyles/gameWrapperStyles';
import useSharedConnect4BoardContext from 'contexts/SharedConnect4BoardContext';

const ConnectFour: React.FunctionComponent<GameProps> = ({ holderShortId, playerName, opponentX500Name }) => {
    const classes = useConnect4Styles();
    const gameWrapperClasses = useGameWrapperStyles();
    const { gameState, updateBoard, currentPlayerTurn, playerOne } = useSharedConnect4BoardContext();
    const { requestIds, queueResponse, startGameFlow } = useCordaFlows({ updateBoard: updateBoard });

    const handleClick = async (rowIndex: number) => {
        let colIndex: number | null = null;
        for (let i = C4_BOARD_SIZE_Y - 1; i > -1; i--) {
            if (gameState[i][rowIndex] === null) {
                colIndex = i;
                break;
            }
        }
        if (colIndex === null) return;
        await startGameFlow({
            flowName: 'net.corda.demo.connectfour.StartConnectFourGameFlow',
            col: colIndex,
            row: rowIndex,
            gameState: gameState,
            holderShortId: holderShortId,
            opponentX500Name: opponentX500Name,
            queueResponse: queueResponse,
        });
    };

    const getBoxRender = (colIndex: number, rowIndex: number) => {
        const dropEffect = `
            @keyframes movePos${colIndex} {
            0%   { transform: translateY(-${
                C4_BOARD_HEIGHT - (C4_BOARD_HEIGHT / C4_BOARD_SIZE_Y) * (C4_BOARD_SIZE_Y - colIndex)
            }px); }
            100% { opacity: translate(0); }
            }
        `;
        if (gameState[colIndex][rowIndex] !== null) {
            return (
                <>
                    <style children={dropEffect} />
                    <FiberManualRecordIcon
                        className={`${
                            gameState[colIndex][rowIndex] === 'O' ? classes.filledIconRed : classes.filledIconGreen
                        } ${classes.icon}`}
                        style={{
                            animationDuration: `1s`,
                            animationIterationCount: 1,
                            animationName: `movePos${colIndex}`,
                        }}
                    />
                </>
            );
        }
        return null;
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
                            marginLeft: 84,
                            marginRight: 84,
                        }}
                    >
                        <div style={{ display: 'flex' }}>
                            {Array.from(Array(C4_BOARD_SIZE_X)).map((a, rowIndex) => {
                                return (
                                    <div
                                        className={`${classes.slot} ${
                                            playerName === playerOne
                                                ? classes.interactiveSlotGreen
                                                : classes.interactiveSlotRed
                                        }`}
                                        onClick={() => {
                                            handleClick(rowIndex);
                                        }}
                                    >
                                        <SportsHandballIcon style={{ height: 60, width: 70 }} />
                                    </div>
                                );
                            })}
                        </div>
                        {Array.from(Array(C4_BOARD_SIZE_Y)).map((a, rowIndex) => {
                            return (
                                <div key={rowIndex} style={{ display: 'flex' }}>
                                    {Array.from(Array(C4_BOARD_SIZE_X)).map((b, colIndex) => (
                                        <div
                                            id={`${rowIndex}, ${colIndex}`}
                                            key={`${rowIndex}, ${colIndex} ${gameState[rowIndex][colIndex]}`}
                                            className={classes.slot}
                                        >
                                            <FiberManualRecordIcon className={`${classes.emptyIcon} ${classes.icon}`} />
                                            {getBoxRender(rowIndex, colIndex)}
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

export default ConnectFour;
