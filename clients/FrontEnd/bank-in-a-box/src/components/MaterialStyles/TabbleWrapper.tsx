import { Theme, createStyles, makeStyles } from '@material-ui/core';

export const useTableWrapperStyles = makeStyles((theme: Theme) =>
    createStyles({
        mainTableWrapper: {
            marginLeft: 0,
            marginRight: 0,
            [theme.breakpoints.up('sm')]: {
                margin: 20,
            },
        },
        toolBar: {
            display: 'flex',
            flexFlow: 'wrap',
        },
        toolBarItem: {
            display: 'flex',
            flexFlow: 'wrap',
            marginLeft: 10,
            marginRight: 10,
            marginTop: 10,
            [theme.breakpoints.down('sm')]: {
                marginLeft: 'auto',
                marginRight: 'auto',
                marginTop: 10,
                marginBottom: 10,
            },
        },
        searchButon: {
            color: theme.palette.primary.contrastText,
            margingTop: 'auto',
            marginBottom: 49,
            marginLeft: 10,
        },
        selectDropdown: {
            width: '100%',
            height: '70%',
            marginBottom: 20,
        },
        label: {
            marginTop: -18,
            marginBottom: -20,
            marginLeft: 10,
        },
        dateInput: {
            marginBottom: 49,
        },
        searchFieldItem: {
            textAlign: 'left',
            order: 0,
            [theme.breakpoints.up('sm')]: {
                marginLeft: 0,
            },
        },
        createNewButton: {
            marginBottom: 35,
            height: 45,
        },
        createNewButtonItem: {
            marginRight: 'auto',
            marginLeft: 'auto',
            [theme.breakpoints.up('md')]: {
                marginBottom: 'auto',
                marginLeft: 'auto',
                marginRight: 0,
                order: 2,
            },
        },
        noSearchMatch: {
            margin: 'auto',
            fontSize: 'large',
        },
        datePicker: {
            margin: 'auto',
            marginTop: 10,
            marginBottom: 10,
            [theme.breakpoints.up('sm')]: {
                marginLeft: 10,
                marginRight: 10,
                marginTop: 0,
                marginBottom: 0,
            },
        },
    })
);
