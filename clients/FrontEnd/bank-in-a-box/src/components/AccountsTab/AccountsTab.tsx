import { Account, BLANKFIELD, Customer, EmptyAccountData, EmptyCustomerData } from '../../store/types';
import { Card, CardContent, Divider, Fade, Paper, Typography } from '@material-ui/core';
import React, { useEffect, useState } from 'react';
import { mapAccount, mapCustomerData } from '../../utils/Utils';

import { ResolvedPromise } from '../../api/resolvePromise';
import { TranslateAccount } from '../../i18n/TranslateAccounts';
import { getAccountById } from '../../api/accountApi';
import { getCustomerById } from '../../api/customerApi';
import { useHistory } from 'react-router-dom';
import { useInfoDisplayStyles } from '../MaterialStyles/InfoDisplayStyles';
import { useSnackbar } from 'notistack';
import { useTranslation } from 'react-i18next';

type Props = {
    accountFromId: string;
    accountToId: string;
};

const AccountsTab: React.FC<Props> = (props) => {
    const { t } = useTranslation('common');
    const { accountFromId, accountToId } = props;
    const history = useHistory();
    const infoDisplayStyles = useInfoDisplayStyles();

    const [accountFrom, setAccountFrom] = useState<Account>(EmptyAccountData);
    const [accountTo, setAccountTo] = useState<Account>(EmptyAccountData);
    const [customerFrom, setCustomerFrom] = useState<Customer>(EmptyCustomerData);
    const [customerTo, setCustomerTo] = useState<Customer>(EmptyCustomerData);
    const { enqueueSnackbar } = useSnackbar();

    useEffect(() => {
        const updateAccountFrom = async () => {
            const responseAccountFrom = await getAccountById(accountFromId);
            if (responseAccountFrom.error) {
                enqueueSnackbar(t('common:error.serverContactError', { error: responseAccountFrom.error }), {
                    variant: 'error',
                });
                history.goBack();
            } else {
                setAccountFrom(TranslateAccount(mapAccount(responseAccountFrom.data.data), t));
            }
        };

        const updateaccountTo = async () => {
            const responseAccountTo = await getAccountById(accountToId);
            if (responseAccountTo.error) {
                enqueueSnackbar(t('common:error.serverContactError', { error: responseAccountTo.error }), {
                    variant: 'success',
                });
                history.goBack();
            } else {
                setAccountTo(TranslateAccount(mapAccount(responseAccountTo.data.data), t));
            }
        };
        if (accountFromId.length > 0) {
            updateAccountFrom();
        }

        if (accountToId.length > 0) {
            updateaccountTo();
        }
    }, [accountToId, accountFromId, t, history, enqueueSnackbar]);

    useEffect(() => {
        const updateCustomerFrom = async () => {
            const customerFromResponse: ResolvedPromise = await getCustomerById(accountFrom.accountData.customerId!);
            if (customerFromResponse.error) {
                enqueueSnackbar(t('common:error.serverContactError', { error: customerFromResponse.error }), {
                    variant: 'error',
                });
            } else {
                const customerMapped = mapCustomerData(customerFromResponse.data.data);
                setCustomerFrom(customerMapped);
            }
        };

        const updateCustomerTo = async () => {
            const customerToResponse: ResolvedPromise = await getCustomerById(accountTo.accountData.customerId!);
            if (customerToResponse.error) {
                enqueueSnackbar(t('common:error.serverContactError', { error: customerToResponse.error }), {
                    variant: 'error',
                });
            } else {
                const customerMapped = mapCustomerData(customerToResponse.data.data);
                setCustomerTo(customerMapped);
            }
        };
        if (accountTo.accountData.accountId.length > 0) {
            updateCustomerTo();
        }
        if (accountFrom.accountData.accountId.length > 0) {
            updateCustomerFrom();
        }
    }, [accountFrom, accountTo, t, enqueueSnackbar]);

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
                                    {accountFrom.accountData.accountId.length > 0
                                        ? accountFrom.accountData.accountId
                                        : BLANKFIELD}
                                </Typography>
                            </CardContent>
                        </Card>
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
                                    {accountFrom.type.length > 0 ? accountFrom.type : BLANKFIELD}
                                </Typography>
                            </CardContent>
                        </Card>
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
                                    {customerFrom.customerName.length > 0 ? customerFrom.customerName : BLANKFIELD}
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
                                    {accountTo.accountData.accountId.length > 0
                                        ? accountTo.accountData.accountId
                                        : BLANKFIELD}
                                </Typography>
                            </CardContent>
                        </Card>
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
                                    {accountTo.type.length > 0 ? accountTo.type : BLANKFIELD}
                                </Typography>
                            </CardContent>
                        </Card>
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
                                    {customerTo.customerName.length > 0 ? customerTo.customerName : BLANKFIELD}
                                </Typography>
                            </CardContent>
                        </Card>
                    </div>
                </Paper>
            </Fade>
        </div>
    );
};

export default AccountsTab;
