import { AccountPaginated, BLANKFIELD, EmptyAccountData, EmptyCustomerData } from '../../store/types';
import { Card, CardContent, Divider, Fade, Paper, Typography } from '@material-ui/core';
import React, { useCallback, useEffect, useState } from 'react';

import { TranslateAccounts } from '../../i18n/TranslateAccounts';
import { getCustomerNameForAccount } from '../../api/userManageApi';
import { getCustomersAccountsPaginatedWithSort } from '../../api/accountApi';
import { mapAccounts } from '../../utils/Utils';
import { useAuthProvider } from '../../store/AuthenticationContext';
import { useHistory } from 'react-router-dom';
import { useInfoDisplayStyles } from '../MaterialStyles/InfoDisplayStyles';
import { useSnackbar } from 'notistack';
import { useTranslation } from 'react-i18next';

type Props = {
    accountFromId: string;
    accountToId: string;
};

const AccountsTabCustomer: React.FC<Props> = (props) => {
    const { t } = useTranslation('common');
    const { accountFromId, accountToId } = props;
    const history = useHistory();
    const authContext = useAuthProvider();
    const [ownAccounts, setOwnAccounts] = useState<AccountPaginated[]>([]);
    const [accountTypeFrom, setAccountTypeFrom] = useState<string>('');
    const [accountTypeTo, setAccountTypeTo] = useState<string>('');
    const [customerFrom, setCustomerFrom] = useState<String>('');
    const [customerTo, setCustomerTo] = useState<String>('');
    const infoDisplayStyles = useInfoDisplayStyles();
    const { enqueueSnackbar } = useSnackbar();

    const getCustomerNameForAccountNo = useCallback(
        async (accountId: string): Promise<string> => {
            let name = '';
            const customerNameResponse = await getCustomerNameForAccount(accountId);

            if (customerNameResponse.error) {
                enqueueSnackbar(t('common:error.serverContactError', { error: customerNameResponse.error }), {
                    variant: 'error',
                });
                history.push('/');
            } else {
                name = customerNameResponse.data.data.customerName;
            }
            return name;
        },
        [history, t, enqueueSnackbar]
    );

    useEffect(() => {
        const getOwnAccounts = async () => {
            const accountsReponse = await getCustomersAccountsPaginatedWithSort(authContext?.user?.userId!);
            if (accountsReponse.error) {
                history.push('/');
                enqueueSnackbar(t('common:error.serverContactError', { error: accountsReponse.error }), {
                    variant: 'error',
                });
            } else {
                const accountsData: AccountPaginated[] = TranslateAccounts(
                    mapAccounts(accountsReponse.data.data.result),
                    t
                );
                setOwnAccounts(accountsData);
            }
        };

        getOwnAccounts();
    }, [authContext, history, t, enqueueSnackbar]);

    useEffect(() => {
        const isOwnAccount = (accountId: string): boolean => {
            for (const accPaginated of ownAccounts) {
                if (accPaginated.account.accountData.accountId === accountId) {
                    return true;
                }
            }
            return false;
        };

        const getOwnAccount = (accountId: string): AccountPaginated => {
            let accountPaginated = { account: EmptyAccountData, customer: EmptyCustomerData };
            for (const accPaginated of ownAccounts) {
                if (accPaginated.account.accountData.accountId === accountId) {
                    return accPaginated;
                }
            }
            return accountPaginated;
        };

        const updateAccountFrom = async () => {
            if (isOwnAccount(accountFromId)) {
                const ownAccount = getOwnAccount(accountFromId);
                setCustomerFrom(ownAccount.customer.customerName);

                setAccountTypeFrom(ownAccount.account.type);
            } else {
                const customerNameResponse = await getCustomerNameForAccountNo(accountFromId);
                setCustomerFrom(customerNameResponse);
            }
        };

        const updateAccountTo = async () => {
            if (isOwnAccount(accountToId)) {
                const ownAccount = getOwnAccount(accountToId);
                setCustomerTo(ownAccount.customer.customerName);
                setAccountTypeTo(ownAccount.account.type);
            } else {
                const customerNameResponse = await getCustomerNameForAccountNo(accountToId);
                setCustomerTo(customerNameResponse);
            }
        };

        if (accountFromId.length > 0 && ownAccounts.length > 0) {
            updateAccountFrom();
        }

        if (accountToId.length > 0 && ownAccounts.length > 0) {
            updateAccountTo();
        }
    }, [ownAccounts, accountFromId, accountToId, getCustomerNameForAccountNo]);

    return (
        <div className="transactionAccountsWrapper">
            <Fade in={true}>
                <Paper elevation={3} className={infoDisplayStyles.infoDisplay}>
                    <div className={infoDisplayStyles.column}>
                        <Card className={infoDisplayStyles.card}>
                            <CardContent>
                                <Typography variant="h5" component="h2">
                                    {t('common:transaction.accountFrom')}
                                </Typography>
                                <br />
                                <Divider />
                                <br />
                                <br />
                                <Typography className={infoDisplayStyles.cardText} variant="body1" component="p">
                                    {accountFromId.length > 0 ? accountFromId : BLANKFIELD}
                                </Typography>
                            </CardContent>
                        </Card>
                        {accountTypeFrom.length > 0 && (
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
                                        {accountTypeFrom}{' '}
                                    </Typography>
                                </CardContent>
                            </Card>
                        )}
                        <Card className={infoDisplayStyles.card}>
                            <CardContent>
                                <Typography variant="h5" component="h2">
                                    {t('common:commonText.customerName')}
                                </Typography>
                                <br />
                                <Divider />
                                <br />
                                <br />
                                <Typography className={infoDisplayStyles.cardText} variant="body1" component="p">
                                    {customerFrom.length > 0 ? customerFrom : BLANKFIELD}
                                </Typography>
                            </CardContent>
                        </Card>
                    </div>
                    <div className={infoDisplayStyles.column}>
                        <Card className={infoDisplayStyles.card}>
                            <CardContent>
                                <Typography variant="h5" component="h2">
                                    {t('common:transaction.accountTo')}
                                </Typography>
                                <br />
                                <Divider />
                                <br />
                                <br />
                                <Typography className={infoDisplayStyles.cardText} variant="body1" component="p">
                                    {accountToId.length > 0 ? accountToId : BLANKFIELD}
                                </Typography>
                            </CardContent>
                        </Card>
                        {accountTypeTo.length > 0 && (
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
                                        {accountTypeTo}{' '}
                                    </Typography>
                                </CardContent>
                            </Card>
                        )}
                        <Card className={infoDisplayStyles.card}>
                            <CardContent>
                                <Typography variant="h5" component="h2">
                                    {t('common:commonText.customerName')}
                                </Typography>
                                <br />
                                <Divider />
                                <br />
                                <br />
                                <Typography className={infoDisplayStyles.cardText} variant="body1" component="p">
                                    {customerTo.length > 0 ? customerTo : BLANKFIELD}
                                </Typography>
                            </CardContent>
                        </Card>
                    </div>
                </Paper>
            </Fade>
        </div>
    );
};

export default AccountsTabCustomer;
