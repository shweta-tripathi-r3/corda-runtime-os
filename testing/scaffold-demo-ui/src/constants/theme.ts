import { alpha, createTheme } from '@material-ui/core';

const theme = createTheme({
    palette: {
        action: {
            disabledBackground: 'grey',
            disabled: 'grey',
            disabledOpacity: 0.6,
        },
        background: {
            default: '#0a0a0a',
        },
        primary: {
            main: '#141414',
            dark: '#0a0a0a',
        },
        secondary: {
            main: '#de3148',
        },
    },
    shape: {
        borderRadius: 12,
    },
    typography: {
        fontFamily: ['Open Sans', 'sans-serif'].join(','),
    },
    spacing: 4,
    overrides: {
        MuiCssBaseline: {
            '@global': {
                '*::-webkit-scrollbar': {
                    width: '0.7em',
                },
                '*::-webkit-scrollbar-track': {
                    '-webkit-box-shadow': 'inset 0 0 6px rgba(0,0,0,0.00)',
                },
                '*::-webkit-scrollbar-thumb': {
                    borderRadius: 12,
                    backgroundColor: alpha('#de3148', 0.3),
                    outline: '0px solid black',
                },
            },
        },
    },
});

export default theme;
