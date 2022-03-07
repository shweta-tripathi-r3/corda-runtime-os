import { C4_BOARD_HEIGHT, C4_BOARD_SIZE_X, C4_BOARD_SIZE_Y, C4_BOARD_WIDTH } from 'constants/connect4';
import { Theme, alpha, makeStyles } from '@material-ui/core';

const useConnect4Styles = makeStyles(({ palette, spacing }: Theme) => ({
    icon: { height: 80, width: 80, position: 'relative', zIndex: 20 },
    emptyIcon: {
        color: palette.primary.light,
    },
    filledIconRed: {
        color: palette.secondary.light,
        transition: 'all .5s ease-in-out',
        position: 'absolute',
    },
    filledIconGreen: {
        color: 'lightgreen',
        transition: 'all .5s ease-in-out',
        position: 'absolute',
    },
    slot: {
        display: 'flex',
        alignItems: 'center',
        alignContent: 'center',
        justifyContent: 'center',
        width: C4_BOARD_WIDTH / C4_BOARD_SIZE_Y,
        height: C4_BOARD_HEIGHT / C4_BOARD_SIZE_X,
        border: `1px solid ${alpha(palette.primary.contrastText, 0.2)}`,
        position: 'relative',
    },
    interactiveSlotRed: {
        color: palette.primary.main,
        '&:hover': {
            color: palette.secondary.light,
        },
    },
    interactiveSlotGreen: {
        color: palette.primary.main,
        '&:hover': {
            color: 'lightgreen',
        },
    },
    board: {
        margin: 'auto',
        marginTop: spacing(8),
        marginBottom: spacing(8),
        width: C4_BOARD_WIDTH,
        height: C4_BOARD_WIDTH,
    },
    '@keyframes moveToPos': {
        '0%': {
            transform: `translateY(-300px)`,
        },
        '100%': {
            transform: 'translateY(0)',
        },
    },
}));

export default useConnect4Styles;
