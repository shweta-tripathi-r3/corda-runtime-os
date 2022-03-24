import { Theme, makeStyles } from '@material-ui/core';

const useTileStyles = makeStyles(({ palette, spacing }: Theme) => ({
    tile: {
        background: palette.primary.main,
        width: 'fit-content',
        padding: spacing(6),
        marginTop: spacing(8),
        display: 'flex',
        height: 'fit-content',
    },
}));

export default useTileStyles;
