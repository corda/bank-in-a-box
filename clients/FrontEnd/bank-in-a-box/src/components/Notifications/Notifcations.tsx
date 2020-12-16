import MuiAlert, { AlertProps } from '@material-ui/lab/Alert';
import { Theme, createStyles, makeStyles } from '@material-ui/core';

import React from 'react';

const useStyles = makeStyles((theme: Theme) =>
    createStyles({
        alert: {
            marginBottom: 10,
        },
    })
);

export const Alert = (props: AlertProps) => {
    const classes = useStyles();

    return <MuiAlert className={classes.alert} elevation={6} variant="filled" {...props} />;
};
