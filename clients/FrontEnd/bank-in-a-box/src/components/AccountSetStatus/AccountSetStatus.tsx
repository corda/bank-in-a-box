import { Account, AccountStatus, EmptyAccountData, RedirectWithAccount } from '../../store/types';
import { Button, Fade, InputLabel, MenuItem, Select } from '@material-ui/core';
import React, { useState } from 'react';
import { useHistory, useLocation } from 'react-router-dom';

import Header from '../Header/Header';
import { setAccountStatus } from '../../api/accountApi';
import useEventListener from '@use-it/event-listener';
import { useFormStyles } from '../MaterialStyles/Forms';
import { useSnackbar } from 'notistack';
import { useTranslation } from 'react-i18next';

const AccountSetStatus: React.FC = () => {
    const { t } = useTranslation('common');
    const location = useLocation<RedirectWithAccount>();
    const history = useHistory();
    const [submitButtonDisabled, setSubmitButtonDisabled] = useState<boolean>(false);
    const formClasses = useFormStyles();

    const setInitialAccountData = (): Account => {
        //If the user is navigating here from accountDetails page (selecting an account)
        if (location?.state?.account) {
            window.scroll(0, 0);
            return location.state.account;
            //If the user tried to navigate to this page by url (without a selected account)
            //Will be pushed back to homepage
        } else {
            history.push('/');
            return EmptyAccountData;
        }
    };

    const setDefaultStatus = (): AccountStatus => {
        if (account.accountData.status === 'ACTIVE') {
            return 'SUSPENDED';
        } else {
            return 'ACTIVE';
        }
    };

    const [account] = useState<Account>(setInitialAccountData());
    const [status, setStatus] = useState<AccountStatus>(setDefaultStatus());
    const { enqueueSnackbar } = useSnackbar();

    const handleKeyDown = (event: { keyCode: number }) => {
        if (event.keyCode === 13 && !submitButtonDisabled) {
            submitAccountStatus();
        }
    };

    useEventListener('keyup', handleKeyDown);

    const goToPageWithAccount = (path: string) => {
        history.push({
            pathname: path,
            state: { account: account, from: location.pathname },
        });
    };

    const submitAccountStatus = async () => {
        setSubmitButtonDisabled(true);
        const response = await setAccountStatus(account.accountData.accountId, status);

        if (response.error) {
            enqueueSnackbar(t('common:error.setStatus', { error: response.error }), { variant: 'error' });
            setSubmitButtonDisabled(false);
        } else {
            enqueueSnackbar(t('common:success.setStatus', { status: status }), { variant: 'success' });
            goToPageWithAccount('/accountView');
        }
    };

    const handleAccountTypeSelect = (event: React.ChangeEvent<{ value: unknown }>) => {
        event.preventDefault();
        setStatus(event.target.value as AccountStatus);
    };

    return (
        <div className="accountSetStatusWrapper">
            <Header>{t('common:pageTitle.setStatus')}</Header>
            <Fade in={true}>
                <div className={formClasses.inputWrapper}>
                    <div className={formClasses.column}>
                        <InputLabel className={formClasses.label} shrink id="status-label">
                            {t('common:account.selectStatus')}
                        </InputLabel>
                        <Select
                            className={`${formClasses.columnItem} ${formClasses.formInput} accountStatusSelect`}
                            labelId="status-label"
                            onChange={(event) => handleAccountTypeSelect(event)}
                            value={status}
                            variant="outlined"
                        >
                            {account.accountData.status !== 'ACTIVE' && (
                                <MenuItem value="ACTIVE">{t('common:accountStatus.active')}</MenuItem>
                            )}
                            {account.accountData.status !== 'SUSPENDED' && (
                                <MenuItem value="SUSPENDED">{t('common:accountStatus.suspended')}</MenuItem>
                            )}
                        </Select>

                        <div className={formClasses.formControl}>
                            <Button
                                className={`${formClasses.formControlButton} submitButton ${formClasses.button}`}
                                color="primary"
                                variant="contained"
                                onClick={() => submitAccountStatus()}
                                disabled={submitButtonDisabled}
                            >
                                {t('common:button.save')}
                            </Button>

                            <Button
                                className={`${formClasses.formControlButton}  ${formClasses.button} cancelButton`}
                                color="primary"
                                variant="contained"
                                onClick={() => goToPageWithAccount('/accountView')}
                            >
                                {t('common:button.cancel')}
                            </Button>
                        </div>
                    </div>
                </div>
            </Fade>
        </div>
    );
};

export default AccountSetStatus;
