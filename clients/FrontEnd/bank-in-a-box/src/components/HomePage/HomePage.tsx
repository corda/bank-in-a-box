import { Theme, createStyles, makeStyles } from '@material-ui/core';

import { Alert } from '../Notifications/Notifcations';
import Header from '../Header/Header';
import React from 'react';
import { UserType } from '../../store/types';
import { useAuthProvider } from '../../store/AuthenticationContext';
import { useTranslation } from 'react-i18next';

export const useStyles = makeStyles((theme: Theme) =>
    createStyles({
        guestMessage: {
            margin: 60,
        },
        snackBar: {
            margin: 10,
        },
    })
);

const HomePage: React.FC = () => {
    const { t } = useTranslation(['common']);
    const authContext = useAuthProvider();
    const classes = useStyles();
    return (
        <div className="homePageWrapper">
            <Header>{t('common:welcome.title', { title: t('common:appTitle') })}</Header>

            {authContext?.user?.userType === UserType.GUEST && (
                <div className={classes.guestMessage}>
                    <Alert severity="info" className={classes.snackBar}>
                        {t('common:guest.guestMessage', {
                            accountId: authContext?.user?.userId,
                            custMsg: authContext?.user?.userId ? t('common:guest.custMsg') : '',
                        })}
                    </Alert>
                </div>
            )}
        </div>
    );
};

export default HomePage;
