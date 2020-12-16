import { Theme, createStyles, makeStyles } from '@material-ui/core/styles';

import { Divider } from '@material-ui/core';
import Drawer from '@material-ui/core/Drawer';
import React from 'react';
import { useTranslation } from 'react-i18next';

type Props = {
    open: boolean;
    closeDrawer: Function;
};

const useStyles = makeStyles((theme: Theme) =>
    createStyles({
        appTitle: {
            display: 'block',
            color: theme.palette.secondary.main,
            [theme.breakpoints.up('sm')]: {
                display: 'none',
            },
        },
        title: {
            textAlign: 'center',
            paddingTop: 20,
            paddingBottom: 20,
            fontWeight: 'bold',
            color: theme.palette.secondary.main,
            fontSize: 20,
        },
        paperRoot: {
            backgroundColor: theme.palette.secondary.contrastText,
        },
        img: {
            marginLeft: 'auto',
            marginRight: 'auto',
            [theme.breakpoints.up('sm')]: {
                display: 'none',
            },
        },
    })
);

const DrawerNav: React.FC<Props> = ({ open, closeDrawer, children }) => {
    const drawerStyles = useStyles();
    const { t } = useTranslation('common');
    const imageSource = (window as any).PUBLIC_URL ? (window as any).PUBLIC_URLtrim() : '';
    return (
        <div className="navbar">
            <Drawer classes={{ paper: drawerStyles.paperRoot }} onClose={() => closeDrawer()} open={open}>
                <img
                    className={drawerStyles.img}
                    src={`${imageSource}cordablk.png`}
                    height={45}
                    width={110}
                    alt="Corda R3"
                />
                <Divider />
                <div className={`${drawerStyles.title} ${drawerStyles.appTitle}`}>{t('common:appTitle')}</div>
                <Divider />
                <div className={drawerStyles.title}>{t('common:navigation')}</div>
                <span className="navContentWrapper">{children}</span>
            </Drawer>
        </div>
    );
};

export default DrawerNav;
