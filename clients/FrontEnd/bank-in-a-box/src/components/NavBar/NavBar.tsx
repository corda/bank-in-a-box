import * as ROUTES from '../../constants/Routes';

import { Divider, Theme, createStyles, makeStyles } from '@material-ui/core';

import AccountBalanceIcon from '@material-ui/icons/AccountBalance';
import Button from '@material-ui/core/Button';
import LockOpenIcon from '@material-ui/icons/LockOpen';
import PaymentIcon from '@material-ui/icons/Payment';
import PeopleAltIcon from '@material-ui/icons/PeopleAlt';
import PersonIcon from '@material-ui/icons/Person';
import React from 'react';
import ReceiptIcon from '@material-ui/icons/Receipt';
import SupervisedUserCircleIcon from '@material-ui/icons/SupervisedUserCircle';
import UpdateIcon from '@material-ui/icons/Update';
import { UserType } from '../../store/types';
import VpnKeyIcon from '@material-ui/icons/VpnKey';
import { useAuthProvider } from '../../store/AuthenticationContext';
import { useTranslation } from 'react-i18next';

const useStyles = makeStyles((theme: Theme) =>
    createStyles({
        button: {
            minWidth: 250,
            width: '100%',
            minHeight: 80,
            fontSize: 15,
            fontWeight: 'bold',
            color: theme.palette.primary.contrastText,

            '&:hover': {
                background: theme.palette.primary.main,
                color: theme.palette.primary.contrastText,
            },
        },
        dividerRoot: {
            backgroundColor: theme.palette.secondary.main,
        },
    })
);

const NavBar: React.FC = () => {
    const { t } = useTranslation(['common']);
    const authContext = useAuthProvider();
    const user = authContext?.user;
    const classes = useStyles();
    if (user?.userType === UserType.NOTLOGGEDIN) {
        return (
            <>
                <Divider classes={{ root: classes.dividerRoot }} />
                <Button
                    className={`navButton loginButton ${classes.button}`}
                    color="primary"
                    href={ROUTES.LOGIN}
                    startIcon={<LockOpenIcon />}
                >
                    {t('common:commonText.login')}
                </Button>
                <Divider classes={{ root: classes.dividerRoot }} />
                <Button
                    className={`navButton registerButton ${classes.button}`}
                    startIcon={<VpnKeyIcon />}
                    color="primary"
                    href={ROUTES.REGISTER}
                >
                    {t('common:pageTitle.register')}
                </Button>
                <Divider classes={{ root: classes.dividerRoot }} />
            </>
        );
    }

    if (user?.userType === UserType.GUEST) {
        return <span className="noAccess"> You have no access to any resources</span>;
    }

    return (
        <>
            {user?.userType === UserType.ADMIN && (
                <>
                    <Divider classes={{ root: classes.dividerRoot }} />
                    <Button
                        className={`navButton ${classes.button}`}
                        startIcon={<SupervisedUserCircleIcon />}
                        color="primary"
                        href={ROUTES.ASSIGNROLE}
                    >
                        {t('common:pageTitle.userManagement')}
                    </Button>
                    <Divider classes={{ root: classes.dividerRoot }} />
                    <Button
                        className={`navButton  ${classes.button}`}
                        startIcon={<PeopleAltIcon />}
                        color="primary"
                        href={ROUTES.CUSTOMERS}
                    >
                        {t('common:pageTitle.customers')}
                    </Button>
                </>
            )}
            {user?.userType === UserType.CUSTOMER && (
                <>
                    <Divider classes={{ root: classes.dividerRoot }} />
                    <Button
                        className={`navButton  ${classes.button}`}
                        startIcon={<PersonIcon />}
                        color="primary"
                        href={ROUTES.UPDATECUSTOMER}
                    >
                        {t('common:pageTitle.myProfile')}
                    </Button>
                </>
            )}
            <Divider classes={{ root: classes.dividerRoot }} />
            <Button
                className={`navButton  ${classes.button}`}
                startIcon={<AccountBalanceIcon />}
                color="primary"
                href={ROUTES.ACCOUNTS}
            >
                {t('common:pageTitle.accounts')}
            </Button>
            <Divider classes={{ root: classes.dividerRoot }} />
            <Button
                className={`navButton  ${classes.button}`}
                startIcon={<ReceiptIcon />}
                color="primary"
                href={ROUTES.TRANSACTIONS}
            >
                {t('common:pageTitle.transactions')}
            </Button>

            {user?.userType === UserType.CUSTOMER && (
                <>
                    <Divider classes={{ root: classes.dividerRoot }} />
                    <Button
                        className={`navButton  ${classes.button}`}
                        startIcon={<PaymentIcon />}
                        color="primary"
                        href={ROUTES.PAYMENTS}
                    >
                        {t('common:pageTitle.intrabankPayment')}
                    </Button>
                </>
            )}
            <Divider classes={{ root: classes.dividerRoot }} />
            <Button
                className={`navButton  ${classes.button}`}
                startIcon={<UpdateIcon />}
                color="primary"
                href={ROUTES.RECURRINGPAYMENTS}
            >
                {t('common:pageTitle.recurringPayments')}
            </Button>
            <Divider classes={{ root: classes.dividerRoot }} />
        </>
    );
};

export default NavBar;
