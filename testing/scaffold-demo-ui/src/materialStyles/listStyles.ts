import { Theme, makeStyles } from '@material-ui/core';

const useListStyles = makeStyles(({ palette, spacing, shape }: Theme) => ({
    item: {
        border: '1px grey solid',
        padding: spacing(2),
        borderRadius: shape.borderRadius,
        display: 'flex',
        marginBottom: spacing(3),
    },
    divider: { backgroundColor: 'white', width: '90%', marginTop: 8, marginBottom: 8, opacity: 0.5 },
    refreshListButton: { marginRight: 6, marginLeft: 'auto', width: 32, height: 32 },
    emptyListText: { marginBottom: 16, marginLeft: 6, opacity: 0.8 },
}));

export default useListStyles;
