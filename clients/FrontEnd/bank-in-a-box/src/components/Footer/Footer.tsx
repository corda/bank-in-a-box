import { Theme, createStyles, makeStyles } from '@material-ui/core/styles';

import React from 'react';

type Props = {
    copyright: string;
};

const useStyles = makeStyles((theme: Theme) =>
    createStyles({
        footer: {
            position: 'absolute',
            bottom: 0,
            width: '100%',
            textAlign: 'center',
            backgroundColor: theme.palette.secondary.dark,
            color: theme.palette.secondary.contrastText,
            display: 'flex',
            margin: 'auto',
            height: 70,
            paddingTop: 20,
        },
        children: {
            marginLeft: 'auto',
            marginRight: 50,
        },
        copyright: {
            marginTop: 15,
            marginLeft: 30,
        },
    })
);

const Footer: React.FC<Props> = ({ copyright, children }) => {
    const styles = useStyles();
    return (
        <div className={styles.footer}>
            <span className={styles.copyright}>{copyright}</span> <div className={styles.children}></div>
        </div>
    );
};

export default Footer;
