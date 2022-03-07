import { Theme, createStyles, makeStyles } from '@material-ui/core';

export const useNavButtonStyles = makeStyles(
    (theme: Theme) =>
        createStyles({
            buttonNav: {
                fontSize: 12,
                color: theme.palette.primary.contrastText,
                borderColor: theme.palette.secondary.dark,
                transition: 'all 0.1s linear',
                '&:hover': {
                    color: theme.palette.secondary.main,
                    borderColor: theme.palette.secondary.main,
                    transform: 'scale(1.1)',
                },
                minHeight: 50,
                marginRight: 4,
                padding: 12,
                [theme.breakpoints.down('sm')]: {
                    width: 300,
                    height: 100,
                },
            },
            toggleThemeButton: {
                float: 'right',
            },
            navButtonWrapper: {
                [theme.breakpoints.down('sm')]: {
                    display: 'flex',
                    flexDirection: 'column',
                },
                marginLeft: 8,
            },
            divider: {
                backgroundColor: theme.palette.secondary.dark,
                width: '99.3%',
                height: 1,
                marginLeft: 'auto',
                marginRight: 'auto',
                filter: 'blur(0.5px)',
            },
            notifSwitch: {
                fontSize: 14,
                marginLeft: 'auto',
                float: 'right',
                marginTop: theme.spacing(3),
                '& .text': {
                    color: theme.palette.primary.contrastText,
                },

                [theme.breakpoints.down('sm')]: {
                    float: 'none',
                    marginRight: 'auto',
                    marginTop: 42,
                },
                marginRight: 8,
            },
            selected: {
                color: theme.palette.secondary.main,
            },
        }),
    { index: 1 }
);
