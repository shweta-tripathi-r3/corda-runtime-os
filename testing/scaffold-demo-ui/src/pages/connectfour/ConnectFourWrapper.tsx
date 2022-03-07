import { C4_BOARD_SIZE_X, C4_BOARD_SIZE_Y } from 'constants/connect4';
import { PlayerOneC4, PlayerTwoC4 } from 'constants/players';

import ConnectFour from './ConnectFour';
import { SharedConnect4BoardContextProvider } from 'contexts/SharedConnect4BoardContext';

const ConnectFourWrapper = () => {
    return (
        <div style={{ width: 'fit-content', display: 'flex', gap: 80, marginLeft: 'auto', marginRight: 'auto' }}>
            <SharedConnect4BoardContextProvider
                boardSizeY={C4_BOARD_SIZE_Y}
                boardSizeX={C4_BOARD_SIZE_X}
                playerOne={PlayerOneC4.playerName}
                playerTwo={PlayerTwoC4.playerName}
            >
                <ConnectFour
                    holderShortId={PlayerOneC4.shortId}
                    playerName={PlayerOneC4.playerName}
                    opponentX500Name={PlayerTwoC4.playerName}
                />
                <ConnectFour
                    holderShortId={PlayerTwoC4.shortId}
                    playerName={PlayerTwoC4.playerName}
                    opponentX500Name={PlayerOneC4.playerName}
                />
            </SharedConnect4BoardContextProvider>
        </div>
    );
};

export default ConnectFourWrapper;
