import './AccountView.scss';

import { Account, EmptyAccountData, RedirectWithAccount } from '../../store/types';
import { Fade, Tab, Tabs } from '@material-ui/core';
import { LSACCOUNTTABKEY, mapAccount } from '../../utils/Utils';
import React, { useEffect, useState } from 'react';
import { TabPanel, a11yProps, useTabStyles } from '../MaterialStyles/TabStyles';
import { useHistory, useLocation } from 'react-router-dom';

import AccountDetails from './AccountDetails';
import AccountRecurringPayments from '../AccountRecurringPayments/AccountRecurringPayments';
import AccountTransactions from '../AccountTransactions/AccountTransactions';
import CustomerDetails from '../CustomerDetails/CustomerDetails';
import Header from '../Header/Header';
import { TranslateAccount } from '../../i18n/TranslateAccounts';
import { getAccountById } from '../../api/accountApi';
import { useSnackbar } from 'notistack';
import { useTranslation } from 'react-i18next';

const AccountView: React.FC = () => {
    const location = useLocation<RedirectWithAccount>();
    const history = useHistory();
    const { t } = useTranslation('common');
    const classes = useTabStyles();
    const { enqueueSnackbar } = useSnackbar();

    const setInitialAccountData = (): Account => {
        //If the user is navigating here from accounts page (selecting an account)
        if (location.state?.account) {
            window.scroll(0, 0);
            return location.state.account;
            //If the user tried to navigate to this page by url (without a selected account)
            //Will be pushed back to homepage
        } else {
            history.push('/');
            return EmptyAccountData;
        }
    };

    const [account, setAccount] = useState<Account>(setInitialAccountData());

    const assignDefaultTab = (): number => {
        const defaultTab = localStorage.getItem(LSACCOUNTTABKEY);
        if (defaultTab) {
            return parseInt(localStorage.getItem(LSACCOUNTTABKEY)!);
        } else {
            return 0;
        }
    };

    const [defaultTab, setDefaultTab] = useState<number>(assignDefaultTab());

    useEffect(() => {
        const fetchAccount = async () => {
            if (account.accountData.accountId.length <= 0) {
                return;
            }
            const accResponse = await getAccountById(account.accountData.accountId);
            if (accResponse.error) {
                enqueueSnackbar(t('common:error.serverContactError', { error: accResponse.error }), {
                    variant: 'error',
                });
                history.goBack();
            } else {
                const account = TranslateAccount(mapAccount(accResponse.data.data), t);
                setAccount(account);
            }
        };

        fetchAccount();
    }, [account.accountData.accountId, t, history, enqueueSnackbar]);

    return (
        <Fade in={true}>
            <div className={'accountViewWrapper'}>
                <Header>{t('common:commonText.account')}</Header>
                <Tabs
                    className={`${classes.root} tabs`}
                    classes={{ flexContainer: classes.tabContainer }}
                    onChange={(event, newValue) => {
                        window.localStorage.setItem(LSACCOUNTTABKEY, newValue);
                        setDefaultTab(newValue);
                        window.scroll(0, 0);
                    }}
                    value={defaultTab}
                >
                    <Tab
                        className={`accountTab ${classes.tab}`}
                        classes={{ wrapper: classes.tabText }}
                        label={t('common:commonText.account')}
                        {...a11yProps(0)}
                    />
                    <Tab
                        className={`customerTab  ${classes.tab}`}
                        classes={{ wrapper: classes.tabText }}
                        label={t('common:commonText.customer')}
                        {...a11yProps(1)}
                    />
                    <Tab
                        className={`transactionsTab ${classes.tab}`}
                        classes={{ wrapper: classes.tabText }}
                        label={t('common:pageTitle.transactions')}
                        {...a11yProps(2)}
                    />
                    <Tab
                        className={`recurringPaymentsTab ${classes.tab}`}
                        classes={{ wrapper: classes.tabText }}
                        label={t('common:pageTitle.recurringPayments')}
                        {...a11yProps(3)}
                    />
                </Tabs>
                <TabPanel value={defaultTab} index={0}>
                    <AccountDetails account={account} />
                </TabPanel>
                <TabPanel value={defaultTab} index={1}>
                    <CustomerDetails customerId={account.accountData.customerId!} />
                </TabPanel>
                <TabPanel value={defaultTab} index={2}>
                    <AccountTransactions accountId={account.accountData.accountId} />
                </TabPanel>
                <TabPanel value={defaultTab} index={3}>
                    <AccountRecurringPayments accountId={account.accountData.accountId} />
                </TabPanel>
            </div>
        </Fade>
    );
};

export default AccountView;
