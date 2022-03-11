import { C4_BOARD_SIZE_X, C4_BOARD_SIZE_Y } from 'constants/connect4';
import { useEffect, useState } from 'react';

import ConnectFour from './ConnectFour';
import { NodeDetails } from 'model/common';
import { SharedConnect4BoardContextProvider } from 'contexts/SharedConnect4BoardContext';
import { Typography } from '@material-ui/core';
import useAppDataContext from 'contexts/AppDataContext';
import { useAppStyles } from 'materialStyles/appStyles';

const ConnectFourWrapper = () => {
    const appClasses = useAppStyles();

    const { vNodes, refreshVNodes } = useAppDataContext();
    useEffect(() => {
        refreshVNodes();
    }, []);

    useEffect(() => {
        const players = vNodes.filter((vNode) => vNode.cpiIdentifier.name === 'connect4');
        if (players.length === 2) {
            setPlayerOne({ playerName: players[0].holdingIdentity.x500Name, shortId: players[0].holdingIdentity.id });
            setPlayerTwo({ playerName: players[1].holdingIdentity.x500Name, shortId: players[1].holdingIdentity.id });
        }
    }, [vNodes]);

    const [playerOne, setPlayerOne] = useState<NodeDetails | undefined>(undefined);
    const [playerTwo, setPlayerTwo] = useState<NodeDetails | undefined>(undefined);

    if (playerOne === undefined || playerTwo === undefined) {
        return (
            <div style={{ padding: 16 }}>
                <Typography variant="h3" className={appClasses.contrastText}>
                    Please create two V nodes with connect4 CPIs
                </Typography>
            </div>
        );
    }

    return (
        <div style={{ width: 'fit-content', display: 'flex', gap: 80, marginLeft: 'auto', marginRight: 'auto' }}>
            <SharedConnect4BoardContextProvider
                boardSizeY={C4_BOARD_SIZE_Y}
                boardSizeX={C4_BOARD_SIZE_X}
                playerOne={playerOne.playerName}
                playerTwo={playerTwo.playerName}
            >
                <ConnectFour
                    holderShortId={playerOne.shortId}
                    playerName={playerOne.playerName}
                    opponentX500Name={playerTwo.playerName}
                />
                <ConnectFour
                    holderShortId={playerTwo.shortId}
                    playerName={playerTwo.playerName}
                    opponentX500Name={playerOne.playerName}
                />
            </SharedConnect4BoardContextProvider>
        </div>
    );
};

export default ConnectFourWrapper;
