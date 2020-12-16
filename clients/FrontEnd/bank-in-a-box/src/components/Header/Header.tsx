import { Paper, Theme, createStyles, makeStyles } from '@material-ui/core';

import React from 'react';

const useStyles = makeStyles((theme: Theme) =>
    createStyles({
        paper: {
            height: 150,
            width: 'auto',
            backgroundColor: theme.palette.secondary.main,
            textAlign: 'left',
            borderRadius: 0,
            [theme.breakpoints.down('sm')]: {
                textAlign: 'center',
                height: 160,
            },
        },
        text: {
            fontWeight: 'bold',
            color: theme.palette.secondary.contrastText,
            fontSize: 45,
            paddingTop: 50,
            paddingLeft: '3rem',
            paddingRight: '4rem',
            [theme.breakpoints.down('sm')]: {
                fontSize: 30,
                paddingTop: 60,
            },
        },
    })
);

const Header: React.FC = ({ children }) => {
    const classes = useStyles();
    return (
        <Paper className={classes.paper}>
            <div id="header" className={classes.text}>
                {children}
            </div>
        </Paper>
    );
};

export default Header;
