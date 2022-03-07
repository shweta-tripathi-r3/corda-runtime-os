import { Theme, createStyles, makeStyles } from '@material-ui/core';

export const useTopNavWrapperStyles = makeStyles(
    (theme: Theme) =>
        createStyles({
            appBar: {
                backgroundColor: theme.palette.primary.dark,
                color: theme.palette.primary.dark,
            },
            appIcon: {
                marginTop: 2,
                marginBottom: 'auto',
                width: 35,
                height: 35,
                marginLeft: 5,
            },
            walletsIcon: {
                marginTop: 'auto',
                marginBottom: 'auto',
                width: 40,
                height: 40,
                marginLeft: 4,
                color: theme.palette.secondary.main,
            },
            grow: {
                marginLeft: theme.spacing(2),
                marginRight: theme.spacing(2),
                flexGrow: 1,
                backgroundColor: theme.palette.primary.dark,
            },
            buttonNav: {
                color: theme.palette.primary.contrastText,
                borderColor: theme.palette.secondary.dark,
                transition: 'all 0.2s ease-in-out',
                '&:hover': {
                    color: theme.palette.secondary.main,
                    borderColor: theme.palette.secondary.main,
                    transform: 'scale(1.1)',
                },
                height: '100%',
                minHeight: 50,
                marginRight: 10,
                padding: 15,
            },
            toggleThemeButton: {
                float: 'right',
            },
            navButtonWrapper: {},
            button: {
                color: theme.palette.primary.contrastText,
                '&:hover': {
                    color: theme.palette.secondary.main,
                },
                '&--toggleTheme': {
                    float: 'right',
                },
            },
            sectionDesktop: {
                display: 'flex',
            },
            title: {
                color: theme.palette.primary.contrastText,
                marginLeft: 8,
                fontSize: 20,
                fontWeight: 'bold',
                display: 'none',
                [theme.breakpoints.up('sm')]: {
                    display: 'block',
                },
            },
            subTitle: {
                fontSize: 12,
                marginTop: 4,
                marginLeft: 6,
                display: 'none',
                color: theme.palette.primary.contrastText,
                [theme.breakpoints.up('sm')]: {
                    display: 'block',
                },
            },
            cordaImg: {
                marginBottom: 3,
                marginLeft: 6,
                display: 'none',
                [theme.breakpoints.up('sm')]: {
                    display: 'block',
                },
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
                fontSize: 18,
                marginLeft: 'auto',
                float: 'right',
                marginTop: theme.spacing(2),
                '& .text': {
                    color: theme.palette.primary.contrastText,
                },
                marginRight: 8,
            },
            selected: {
                color: theme.palette.secondary.main,
            },
            navButtonContainer: {
                [theme.breakpoints.down('sm')]: {
                    display: 'none',
                },
            },
        }),
    { index: 1 }
);
