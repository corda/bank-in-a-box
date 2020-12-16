import {
    ACCOUNTAPPROVEOVERDRAFT,
    ACCOUNTISSUELOAN,
    ACCOUNTSETLIMITS,
    ACCOUNTSETSTATUS,
    DEPOSIT,
    WITHDRAW,
} from '../../constants/Routes';
import { Account, AccountType, UserType } from '../../store/types';
import { Button, Card, CardContent, Divider, Fade, Paper, Typography } from '@material-ui/core';
import { useHistory, useLocation } from 'react-router-dom';

import React from 'react';
import { useAuthProvider } from '../../store/AuthenticationContext';
import { useInfoDisplayStyles } from '../MaterialStyles/InfoDisplayStyles';
import { useTranslation } from 'react-i18next';

type Props = {
    account: Account;
};

const AccountDetails: React.FC<Props> = (props) => {
    const { account } = props;
    const accountType = account.type as AccountType;
    const history = useHistory();
    const location = useLocation();
    const { t } = useTranslation('common');
    const authContext = useAuthProvider();
    const infoDisplayStyles = useInfoDisplayStyles();

    const goToPageWithAccount = (path: string) => {
        history.push({
            pathname: path,
            state: { account: account, from: location.pathname },
        });
    };

    return (
        <Fade in={true}>
            <Paper elevation={3} className={infoDisplayStyles.infoDisplay}>
                <div className={infoDisplayStyles.column}>
                    <Card className={infoDisplayStyles.card}>
                        <CardContent>
                            <Typography variant="h5" component="h2">
                                {t('common:commonText.accountKey')}
                            </Typography>
                            <br />
                            <Divider />
                            <br />
                            <br />
                            <Typography className={infoDisplayStyles.cardText} variant="body1" component="p">
                                {account.accountData.accountId}
                            </Typography>
                        </CardContent>
                    </Card>
                    <Card className={infoDisplayStyles.card}>
                        <CardContent>
                            <Typography variant="h5" component="h2">
                                {t('common:commonText.currency')}
                            </Typography>
                            <br />
                            <Divider />
                            <br />
                            <br />
                            <Typography className={infoDisplayStyles.cardText} variant="body1" component="p">
                                {account.accountData.currency}
                            </Typography>
                        </CardContent>
                    </Card>
                    <Card className={infoDisplayStyles.card}>
                        <CardContent>
                            <Typography variant="h5" component="h2">
                                {t('common:commonText.balance')}
                            </Typography>
                            <br />
                            <Divider />
                            <br />
                            <br />
                            <Typography className={infoDisplayStyles.cardText} variant="body1" component="p">
                                {account.accountData.balance}
                            </Typography>
                        </CardContent>
                    </Card>
                    {(accountType === 'CURRENT' || accountType === 'OVERDRAFT') && (
                        <>
                            <Card className={infoDisplayStyles.card}>
                                <CardContent>
                                    <Typography variant="h5" component="h2">
                                        {t('common:commonText.accountType')}
                                    </Typography>
                                    <br />
                                    <Divider />
                                    <br />
                                    <br />
                                    <Typography className={infoDisplayStyles.cardText} variant="body1" component="p">
                                        {account.type}
                                    </Typography>
                                </CardContent>
                            </Card>
                            <Card className={infoDisplayStyles.card}>
                                <CardContent>
                                    <Typography variant="h5" component="h2">
                                        {t('common:commonText.status')}
                                    </Typography>
                                    <br />
                                    <Divider />
                                    <br />
                                    <br />
                                    <Typography className={infoDisplayStyles.cardText} variant="body1" component="p">
                                        {account.accountData.status}
                                    </Typography>
                                </CardContent>
                            </Card>
                        </>
                    )}
                </div>
                <div className={infoDisplayStyles.column}>
                    {(accountType === 'SAVINGS' || accountType === 'LOAN') && (
                        <>
                            <Card className={infoDisplayStyles.card}>
                                <CardContent>
                                    <Typography variant="h5" component="h2">
                                        {t('common:commonText.accountType')}
                                    </Typography>
                                    <br />
                                    <Divider />
                                    <br />
                                    <br />
                                    <Typography variant="body1" component="p">
                                        {account.type}
                                    </Typography>
                                </CardContent>
                            </Card>
                            <Card className={infoDisplayStyles.card}>
                                <CardContent>
                                    <Typography variant="h5" component="h2">
                                        {t('common:commonText.status')}
                                    </Typography>
                                    <br />
                                    <Divider />
                                    <br />
                                    <br />
                                    <Typography variant="body1" component="p">
                                        {account.accountData.status}
                                    </Typography>
                                </CardContent>
                            </Card>
                        </>
                    )}
                    <Card className={infoDisplayStyles.card}>
                        <CardContent>
                            <Typography variant="h5" component="h2">
                                {t('common:commonText.lastTxDate')}
                            </Typography>
                            <br />
                            <Divider />
                            <br />
                            <br />
                            <Typography variant="body1" component="p">
                                {account.accountData.txDate}
                            </Typography>
                        </CardContent>
                    </Card>
                    {(accountType === 'CURRENT' || accountType === 'OVERDRAFT') && (
                        <>
                            <Card className={infoDisplayStyles.card}>
                                <CardContent>
                                    <Typography variant="h5" component="h2">
                                        {t('common:account.withdrawalDailyLimit')}
                                    </Typography>
                                    <br />
                                    <Divider />
                                    <br />
                                    <br />
                                    <Typography variant="body1" component="p">
                                        {account.withdrawalDailyLimit
                                            ? account.withdrawalDailyLimit
                                            : t('common:commonText.unlimitedAmount')}
                                    </Typography>
                                </CardContent>
                            </Card>
                            <Card className={infoDisplayStyles.card}>
                                <CardContent>
                                    <Typography variant="h5" component="h2">
                                        {t('common:account.transferDailyLimit')}
                                    </Typography>
                                    <br />
                                    <Divider />
                                    <br />
                                    <br />
                                    <Typography variant="body1" component="p">
                                        {account.transferDailyLimit
                                            ? account.transferDailyLimit
                                            : t('common:commonText.unlimitedAmount')}
                                    </Typography>
                                </CardContent>
                            </Card>
                            <Card className={infoDisplayStyles.card}>
                                <CardContent>
                                    <Typography variant="h5" component="h2">
                                        {t('common:account.overdraftLimit')}
                                    </Typography>
                                    <br />
                                    <Divider />
                                    <br />
                                    <br />
                                    <Typography variant="body1" component="p">
                                        {account.approvedOverdraftLimit
                                            ? account.approvedOverdraftLimit
                                            : t('common:account.overdraftNotApproved')}
                                    </Typography>
                                </CardContent>
                            </Card>
                            <Card className={infoDisplayStyles.card}>
                                <CardContent>
                                    <Typography variant="h5" component="h2">
                                        {t('common:account.overdraftBalance')}
                                    </Typography>
                                    <br />
                                    <Divider />
                                    <br />
                                    <br />
                                    <Typography variant="body1" component="p">
                                        {account.overdraftBalance
                                            ? account.overdraftBalance
                                            : t('common:account.overdraftNotApproved')}
                                    </Typography>
                                </CardContent>
                            </Card>
                        </>
                    )}
                </div>

                <div className={infoDisplayStyles.column}>
                    {authContext?.user?.userType === UserType.ADMIN && (
                        <>
                            <Button
                                className={`${infoDisplayStyles.button} depositButton`}
                                variant="contained"
                                color="primary"
                                onClick={() => {
                                    goToPageWithAccount(DEPOSIT);
                                }}
                                disabled={account.accountData.status === 'ACTIVE' ? false : true}
                            >
                                {t('common:payments.deposit')}
                            </Button>
                            {account.accountData.status !== 'ACTIVE' && (
                                <div>{t('common:account.depositDisabled')}</div>
                            )}
                            {(accountType === 'CURRENT' || accountType === 'OVERDRAFT') && (
                                <>
                                    <Button
                                        className={`${infoDisplayStyles.button} approveOverdraftButton`}
                                        variant="contained"
                                        color="primary"
                                        disabled={account.approvedOverdraftLimit! > 0 ? true : false}
                                        onClick={() => {
                                            goToPageWithAccount(ACCOUNTAPPROVEOVERDRAFT);
                                        }}
                                    >
                                        {t('common:button.approveOverdraft')}
                                    </Button>
                                    <Button
                                        className={`${infoDisplayStyles.button} issueLoanButton`}
                                        variant="contained"
                                        color="primary"
                                        onClick={() => {
                                            goToPageWithAccount(ACCOUNTISSUELOAN);
                                        }}
                                        disabled={account.accountData.status === 'ACTIVE' ? false : true}
                                    >
                                        {t('common:button.issueLoan')}
                                    </Button>
                                    {account.accountData.status !== 'ACTIVE' && (
                                        <div>{t('common:account.loanDisabled')}</div>
                                    )}
                                </>
                            )}
                            <Button
                                className={`${infoDisplayStyles.button} setStatusButton`}
                                variant="contained"
                                color="primary"
                                onClick={() => {
                                    goToPageWithAccount(ACCOUNTSETSTATUS);
                                }}
                            >
                                {t('common:button.setStatus')}
                            </Button>
                        </>
                    )}

                    {(accountType === 'CURRENT' || accountType === 'OVERDRAFT') && (
                        <>
                            <Button
                                className={`${infoDisplayStyles.button} setLimitsButton`}
                                variant="contained"
                                color="primary"
                                onClick={() => {
                                    goToPageWithAccount(ACCOUNTSETLIMITS);
                                }}
                            >
                                {t('common:button.setLimits')}
                            </Button>
                            {authContext?.user?.userType === UserType.ADMIN && (
                                <Button
                                    className={`${infoDisplayStyles.button} withdrawButton`}
                                    variant="contained"
                                    color="primary"
                                    onClick={() => {
                                        goToPageWithAccount(WITHDRAW);
                                    }}
                                    disabled={account.accountData.status === 'ACTIVE' ? false : true}
                                >
                                    {t('common:payments.withdraw')}
                                </Button>
                            )}
                        </>
                    )}
                </div>
            </Paper>
        </Fade>
    );
};

export default AccountDetails;
