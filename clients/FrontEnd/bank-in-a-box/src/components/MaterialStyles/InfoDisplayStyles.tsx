import { Theme, createStyles, makeStyles } from '@material-ui/core';

export const useInfoDisplayStyles = makeStyles((theme: Theme) =>
    createStyles({
        card: {
            marginTop: 10,
            marginBottom: 15,
            width: '100%',
            height: 225,

            [theme.breakpoints.down('md')]: {
                maxWidth: 350,
                minWidth: 0,
                display: 'block',
            },
        },
        cardText: {
            overflowWrap: 'break-word',
        },
        infoDisplay: {
            margin: 10,
            marginBottom: 30,
            marginTop: 30,
            display: 'flex',
            flexWrap: 'wrap',
            width: 'auto',
        },
        column: {
            textAlign: 'center',
            marginTop: 10,
            marginBottom: 10,
            marginLeft: '4vw',
            marginRight: '4vw',
            width: 380,

            [theme.breakpoints.down('md')]: {
                marginLeft: 'auto',
                marginRight: 'auto',
                width: 350,
            },
        },
        button: {
            marginTop: 15,
            marginBottom: 15,
            marginLeft: 'auto',
            marginRight: 'auto',
            width: 'auto',
            minWidth: 230,
            minHeight: 50,
        },
        tableContainer: {
            textAlign: 'center',
            marginTop: 10,
            marginBottom: 10,
            marginLeft: '4vw',
            marginRight: '4vw',
            maxWidth: '100%',

            [theme.breakpoints.down('md')]: {
                marginLeft: 'auto',
                marginRight: 'auto',
            },
        },
        tableContainerInputs: {
            marginTop: 20,
            marginBottom: 20,
            display: 'flex',
            flexWrap: 'wrap',
        },
        tableContainerInput: {
            margin: 20,
            marginTop: 10,
            marginBottom: 10,

            '&:first-of-type': {
                marginLeft: 0,
            },

            [theme.breakpoints.down('md')]: {
                marginLeft: 'auto',
                marginRight: 'auto',

                '&:first-of-type': {
                    marginLeft: 'auto',
                },
            },
        },
    })
);
