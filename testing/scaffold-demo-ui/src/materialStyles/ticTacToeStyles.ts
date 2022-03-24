import { Theme, alpha, makeStyles } from '@material-ui/core';

import { TTT_BOARD_WIDTH } from 'constants/tictactoe';

const useTicTacToeStyles = makeStyles(({ palette, spacing }: Theme) => ({
    boxIcon: {
        color: palette.secondary.light,
        animation: '$grow 1000ms',
        transition: 'linear',
        animationFillMode: 'forwards',
    },
    nought: {
        color: 'lightgreen',
    },
    clickableBox: {
        display: 'flex',
        alignItems: 'center',
        alignContent: 'center',
        justifyContent: 'center',
        width: TTT_BOARD_WIDTH / 3,
        height: TTT_BOARD_WIDTH / 3,
        border: `1px solid ${alpha(palette.primary.contrastText, 0.2)}`,
    },
    board: {
        margin: 'auto',
        marginTop: spacing(8),
        marginBottom: spacing(8),
        width: TTT_BOARD_WIDTH,
        height: TTT_BOARD_WIDTH,
    },
    '@keyframes grow': {
        '0%': {
            transform: 'scale(0)',
        },
        '100%': {
            transform: 'scale(3)',
        },
    },
}));

export default useTicTacToeStyles;
