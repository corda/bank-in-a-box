import { Theme, createStyles, makeStyles } from '@material-ui/core';

export const useFormStyles = makeStyles((theme: Theme) =>
    createStyles({
        formInput: {
            width: '100%',
            marginTop: 20,
            marginBottom: 20,
        },
        button: {
            minWidth: 150,
            minHeight: 50,
            marginBottom: 20,
            marginTop: 20,
            marginLeft: 'auto',
            marginRight: 'auto',
        },
        attachmentChip: {
            marginLeft: 20,
            minHeight: 40,
            marginTop: 2,
        },
        completedInput: {
            '& input': { backgroundColor: '#caf7ca40' },
            '& .MuiOutlinedInput-notchedOutline': {
                borderColor: 'green',
            },
        },
        incompleteInput: {
            '& input': { backgroundColor: '#f0c0d20f' },
            '& .MuiOutlinedInput-notchedOutline': {
                borderColor: 'red',
            },
        },
        inputWrapper: {
            margin: 10,
            marginBottom: 30,
            display: 'flex',
            flexWrap: 'wrap',
            width: 'auto',
            paddingTop: 30,
            marginTop: 30,
            paddingBottom: 30,
            height: '100%',
        },
        column: {
            minWidth: 250,
            marginLeft: 20,
            marginRight: 20,
            [theme.breakpoints.up('sm')]: {
                marginLeft: 'auto',
                marginRight: 'auto',
                width: '40%',
            },
        },
        columnItem: {
            maringTop: 30,
            marginBottom: 30,
        },
        attachmentItem: {
            padding: 10,
        },
        formControl: {
            display: 'flex',
            flexFlow: 'wrap',
            marginTop: 30,
            marginBottom: 30,
        },
        formControlButton: {
            margin: 'auto',
            marginBottom: 20,
            minWidth: 200,
            minHeight: 40,
            [theme.breakpoints.up('md')]: {
                margin: 'auto',
                marginBottom: 20,
                minWidth: 150,
                minHeight: 40,
            },
        },
        label: {
            marginTop: 15,
            marginBottom: -20,
            marginLeft: 10,
        },
    })
);
