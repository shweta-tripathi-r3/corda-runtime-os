import { PlayerOneTTT, PlayerTwoTTT } from 'constants/players';

import { SharedConnect4BoardContextProvider } from 'contexts/SharedConnect4BoardContext';
import { TTT_BOARD_SIZE } from 'constants/tictactoe';
import TicTacToe from './TicTacToe';

const TicTacToeWrapper = () => {
    return (
        <div style={{ width: 'fit-content', display: 'flex', gap: 80, marginLeft: 'auto', marginRight: 'auto' }}>
            <SharedConnect4BoardContextProvider
                boardSizeY={TTT_BOARD_SIZE}
                boardSizeX={TTT_BOARD_SIZE}
                playerOne={PlayerOneTTT.playerName}
                playerTwo={PlayerTwoTTT.playerName}
            >
                <TicTacToe
                    holderShortId={PlayerOneTTT.shortId}
                    playerName={PlayerOneTTT.playerName}
                    opponentX500Name={PlayerTwoTTT.playerName}
                />
                <TicTacToe
                    holderShortId={PlayerTwoTTT.shortId}
                    playerName={PlayerTwoTTT.playerName}
                    opponentX500Name={PlayerOneTTT.playerName}
                />
            </SharedConnect4BoardContextProvider>
        </div>
    );
};

export default TicTacToeWrapper;
