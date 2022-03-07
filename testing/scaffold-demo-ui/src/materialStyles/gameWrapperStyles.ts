import { Theme, makeStyles } from '@material-ui/core';

const useGameWrapperStyles = makeStyles(({ palette, spacing }: Theme) => ({
    wrapper: {
        background: palette.primary.main,
        minWidth: 400,
        maxWidth: 800,
        minHeight: 400,
        padding: spacing(4),
        marginTop: spacing(8),
        marginLeft: 'auto',
        marginRight: 'auto',
        display: 'flex',
    },
}));

export default useGameWrapperStyles;
