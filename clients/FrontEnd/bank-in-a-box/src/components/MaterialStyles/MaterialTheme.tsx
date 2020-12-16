import { createMuiTheme } from '@material-ui/core/styles';

const theme = createMuiTheme({
    palette: {
        primary: {
            main: '#F79679',
        },
        secondary: {
            main: '#2e2e2e',
        },
    },
    shape: {
        borderRadius: 3,
    },
    typography: {
        fontFamily: ['Open Sans', 'sans-serif'].join(','),
    },
});

export default theme;
