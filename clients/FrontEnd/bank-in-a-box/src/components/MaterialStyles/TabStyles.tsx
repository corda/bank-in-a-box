import { Theme, makeStyles } from '@material-ui/core';

import Box from '@material-ui/core/Box/Box';
import React from 'react';

export const useTabStyles = makeStyles((theme: Theme) => ({
    root: {
        backgroundColor: theme.palette.secondary.main,
        color: theme.palette.secondary.contrastText,
        borderRadius: 0,
        indicator: {
            backgroundColor: 'red',
            color: 'red',
            display: 'flex',
            justifyContent: 'center',
        },
        '& .MuiTabs-indicator': {
            color: theme.palette.primary.main,
            backgroundColor: theme.palette.primary.main,
        },
    },
    tabContainer: {
        display: 'flex',
        flexWrap: 'wrap',
    },
    tab: {
        fontSize: 16,
        minWidth: 300,
        borderRadius: 0,
        [theme.breakpoints.down('md')]: {
            marginLeft: 'auto',
            marginRight: 'auto',
        },
    },
    tabText: {
        textAlign: 'center',
        alignItems: 'center',
    },
}));

interface TabPanelProps {
    children?: React.ReactNode;
    index: any;
    value: any;
}

export const TabPanel = (props: TabPanelProps) => {
    const { children, value, index, ...other } = props;
    return (
        <div
            role="tabpanel"
            hidden={value !== index}
            id={`simple-tabpanel-${index}`}
            aria-labelledby={`simple-tab-${index}`}
            {...other}
        >
            {value === index && <Box p={1}>{children}</Box>}
        </div>
    );
};

export const a11yProps = (index: any) => {
    return {
        id: `simple-tab-${index}`,
        'aria-controls': `simple-tabpanel-${index}`,
    };
};
