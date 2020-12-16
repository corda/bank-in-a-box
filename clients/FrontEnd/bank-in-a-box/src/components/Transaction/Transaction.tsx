import { BLANKFIELD, EmptyTransaction, RedirectWithTransaction, Transaction, UserType } from '../../store/types';
import { Fade, Tab, Tabs } from '@material-ui/core';
import React, { useState } from 'react';
import { TabPanel, a11yProps, useTabStyles } from '../MaterialStyles/TabStyles';
import { useHistory, useLocation } from 'react-router-dom';

import AccountsTab from '../AccountsTab/AccountsTab';
import AccountsTabCustomer from '../AccountsTab/AccountsTabCustomer';
import Header from '../Header/Header';
import TransactionDetails from './TransactionDetails';
import { useAuthProvider } from '../../store/AuthenticationContext';
import { useTranslation } from 'react-i18next';

const TransactionView: React.FC = () => {
    const location = useLocation<RedirectWithTransaction>();
    const history = useHistory();
    const { t } = useTranslation('common');
    const authContext = useAuthProvider();
    const classes = useTabStyles();
    const [defaultTab, setDefaultTab] = useState<number>(0);

    const setInitialTransactionData = (): Transaction => {
        //If the user is navigating here from customers page (selecting a customer)
        if (location?.state?.transaction) {
            window.scrollTo(0, 0);
            return location.state.transaction;
            //If the user tried to navigate to this page by url (without a selected customer)
            //Will be pushed back to homepage
        } else {
            history.push('/');
            return EmptyTransaction;
        }
    };

    const [transaction] = useState<Transaction>(setInitialTransactionData());

    return (
        <Fade in={true}>
            <div className="transactionWrapper">
                <Header>{t('common:pageTitle.transaction')}</Header>
                <Tabs
                    className={`${classes.root} tabs`}
                    classes={{ flexContainer: classes.tabContainer }}
                    onChange={(event, newValue) => {
                        setDefaultTab(newValue);
                        window.scroll(0, 0);
                    }}
                    value={defaultTab}
                >
                    <Tab
                        className={`transactionTab ${classes.tab}`}
                        classes={{ wrapper: classes.tabText }}
                        label={t('common:pageTitle.transactionDetails')}
                        {...a11yProps(0)}
                    />
                    <Tab
                        className={`accountsTab  ${classes.tab}`}
                        classes={{ wrapper: classes.tabText }}
                        label={t('common:pageTitle.accounts')}
                        {...a11yProps(1)}
                    />
                </Tabs>

                <TabPanel value={defaultTab} index={0}>
                    <TransactionDetails transaction={transaction} />
                </TabPanel>
                <TabPanel value={defaultTab} index={1}>
                    {authContext?.user?.userType === UserType.ADMIN && (
                        <AccountsTab
                            accountFromId={transaction.accountFrom === BLANKFIELD ? '' : transaction.accountFrom!}
                            accountToId={transaction.accountTo === BLANKFIELD ? '' : transaction.accountTo!}
                        />
                    )}
                    {authContext?.user?.userType === UserType.CUSTOMER && (
                        <AccountsTabCustomer
                            accountFromId={transaction.accountFrom === BLANKFIELD ? '' : transaction.accountFrom!}
                            accountToId={transaction.accountTo === BLANKFIELD ? '' : transaction.accountTo!}
                        />
                    )}
                </TabPanel>
            </div>
        </Fade>
    );
};

export default TransactionView;
