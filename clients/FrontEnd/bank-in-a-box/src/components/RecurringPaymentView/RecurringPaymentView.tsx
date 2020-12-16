import {
    BLANKFIELD,
    EmptyRecurringPayment,
    RecurringPayment,
    RedirectWithRecurringPayment,
    UserType,
} from '../../store/types';
import { Fade, Tab, Tabs } from '@material-ui/core';
import React, { useState } from 'react';
import { TabPanel, a11yProps, useTabStyles } from '../MaterialStyles/TabStyles';
import { useHistory, useLocation } from 'react-router-dom';

import AccountsTab from '../AccountsTab/AccountsTab';
import AccountsTabCustomer from '../AccountsTab/AccountsTabCustomer';
import Header from '../Header/Header';
import RecurringPaymentDetails from './RecurringPaymentDetails';
import { useAuthProvider } from '../../store/AuthenticationContext';
import { useTranslation } from 'react-i18next';

const RecurringPaymentView: React.FC = () => {
    const location = useLocation<RedirectWithRecurringPayment>();
    const history = useHistory();
    const { t } = useTranslation('common');
    const authContext = useAuthProvider();
    const classes = useTabStyles();
    const [defaultTab, setDefaultTab] = useState<number>(0);

    const setIntialRecurringPaymentData = (): RecurringPayment => {
        if (location?.state?.recurringPayment) {
            window.scrollTo(0, 0);
            return location.state.recurringPayment;
            //If the user tried to navigate to this page by url
            //Will be pushed back to homepage
        } else {
            history.push('/');
            return EmptyRecurringPayment;
        }
    };
    const [recurringPayment] = useState<RecurringPayment>(setIntialRecurringPaymentData());
    return (
        <Fade in={true}>
            <div className="transactionWrapper">
                <Header>{t('common:pageTitle.recurringPayment')}</Header>
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
                        label={t('common:recurringPayment.recurringPayment')}
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
                    <RecurringPaymentDetails recurringPayment={recurringPayment} />
                </TabPanel>
                <TabPanel value={defaultTab} index={1}>
                    {authContext?.user?.userType === UserType.ADMIN && (
                        <AccountsTab
                            accountFromId={
                                recurringPayment.accountFrom === BLANKFIELD ? '' : recurringPayment.accountFrom!
                            }
                            accountToId={recurringPayment.accountTo === BLANKFIELD ? '' : recurringPayment.accountTo!}
                        />
                    )}
                    {authContext?.user?.userType === UserType.CUSTOMER && (
                        <AccountsTabCustomer
                            accountFromId={
                                recurringPayment.accountFrom === BLANKFIELD ? '' : recurringPayment.accountFrom!
                            }
                            accountToId={recurringPayment.accountTo === BLANKFIELD ? '' : recurringPayment.accountTo!}
                        />
                    )}
                </TabPanel>
            </div>
        </Fade>
    );
};

export default RecurringPaymentView;
