import { Theme, alpha, createStyles, makeStyles } from '@material-ui/core';

export const useAppStyles = makeStyles(
    (theme: Theme) =>
        createStyles({
            menuButton: {
                color: theme.palette.primary.contrastText,
                fontWeight: 'bold',
                fontSize: 18,

                '&:hover': {
                    color: theme.palette.secondary.main,
                },

                [theme.breakpoints.up('md')]: {
                    display: 'none',
                },
            },
            notifBase: {
                '& #notistack-snackbar': {
                    maxWidth: 600,
                    wordBreak: 'break-word',
                },
                '& .MuiCollapse-wrapperInner': {
                    width: '99%!important',
                },
                right: 10,
                pointerEvents: 'all',
                marginBottom: 4,
            },
            mainContent: {
                minHeight: '100hv',
                width: '100%',
                paddingBottom: 85,
                display: 'flex',
                flexDirection: 'column',
                alignItems: 'center',
            },
            blurred: {
                filter: 'blur(5px)',
            },
            appIcon: {
                marginTop: 'auto',
                marginBottom: 'auto',
                width: 40,
                height: 40,
                marginLeft: 4,
                color: theme.palette.secondary.main,
            },
            shadow: {
                boxShadow: `0px 4px 30px 0px ${alpha(theme.palette.secondary.main, 0.4)}`,
            },
            shadowSmall: {
                boxShadow: '0px 0px 30px -3px #dc354523',
            },
        }),
    { index: 1 }
);
