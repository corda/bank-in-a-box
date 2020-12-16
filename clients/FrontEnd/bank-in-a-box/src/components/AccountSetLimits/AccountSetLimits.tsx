import { Account, EmptyAccountData, RedirectWithAccount } from '../../store/types';
import { Button, Fade, TextField } from '@material-ui/core';
import React, { useState } from 'react';
import { useHistory, useLocation } from 'react-router-dom';

import Header from '../Header/Header';
import { setAccountLimits } from '../../api/accountApi';
import useEventListener from '@use-it/event-listener';
import { useFormStyles } from '../MaterialStyles/Forms';
import { useSnackbar } from 'notistack';
import { useTranslation } from 'react-i18next';
import { validateStringIsNumber } from '../../utils/Utils';

const AccountSetLimits: React.FC = () => {
    const { t } = useTranslation('common');
    const [submitButtonDisabled, setSubmitButtonDisabled] = useState(false);
    const location = useLocation<RedirectWithAccount>();
    const history = useHistory();
    const formClasses = useFormStyles();
    const { enqueueSnackbar } = useSnackbar();

    const setInitialAccountData = (): Account => {
        //If the user is navigating here from accountDetails page (selecting an account)
        if (location.state !== undefined) {
            window.scroll(0, 0);
            return location.state.account;
            //If the user tried to navigate to this page by url (without a selected account)
            //Will be pushed back to homepage
        } else {
            history.push('/');
            return EmptyAccountData;
        }
    };

    const handleKeyDown = (event: { keyCode: number }) => {
        if (event.keyCode === 13 && !submitButtonDisabled) {
            submitNewLimits();
        }
    };

    useEventListener('keyup', handleKeyDown);

    const [account] = useState<Account>(setInitialAccountData());
    const [withdrawlDailyLimit, setWithdrawalDailyLimit] = useState<string>(
        account.withdrawalDailyLimit ? account.withdrawalDailyLimit.toString() : ''
    );
    const [transferDailyLimit, setTransferDailyLimit] = useState<string>(
        account.transferDailyLimit ? account.transferDailyLimit.toString() : ''
    );

    const submitNewLimits = async () => {
        setSubmitButtonDisabled(true);
        const response = await setAccountLimits(
            account.accountData.accountId,
            //the api endpoint is expecting the values in cents, the user can input the value in full currency units
            withdrawlDailyLimit.length > 0 ? parseFloat(withdrawlDailyLimit) * 100 : 0,
            transferDailyLimit.length > 0 ? parseFloat(transferDailyLimit) * 100 : 0
        );
        if (response.error) {
            setSubmitButtonDisabled(false);
            enqueueSnackbar(t('common:error.setLimits', { error: response.error }), { variant: 'error' });
            setSubmitButtonDisabled(false);
        } else {
            enqueueSnackbar(t('common:success.setLimits'), { variant: 'success' });
            goToPageWithAccount('/accountView');
        }
    };

    const goToPageWithAccount = (path: string) => {
        history.push({
            pathname: path,
            state: { account: account, from: location.pathname },
        });
    };

    const handleNumberInput = (event: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) => {
        const value = event.target.value;
        if (value.length > 0 && !validateStringIsNumber(value)) {
            return;
        }
        if (event.target.name === 'TransferDailyLimit') {
            setTransferDailyLimit(value);
        } else if (event.target.name === 'WithdrawalDailyLimit') {
            setWithdrawalDailyLimit(value);
        }
    };

    return (
        <div className="accountSetStatusWrapper">
            <Header>{t('common:pageTitle.setLimits')}</Header>
            <Fade in={true}>
                <div className={formClasses.inputWrapper}>
                    <div className={formClasses.column}>
                        <TextField
                            className={`${formClasses.columnItem} ${formClasses.formInput} withdrawalDailyLimitInput`}
                            label={t('common:account.withdrawalDailyLimit')}
                            onChange={(event) => handleNumberInput(event)}
                            name="WithdrawalDailyLimit"
                            value={withdrawlDailyLimit}
                            helperText={t('common:account.withdrawalHelpText')}
                            variant="outlined"
                        />

                        <TextField
                            className={`${formClasses.columnItem} ${formClasses.formInput} transferDailyLimitInput`}
                            label={t('common:account.transferDailyLimit')}
                            onChange={(event) => handleNumberInput(event)}
                            name="TransferDailyLimit"
                            value={transferDailyLimit}
                            helperText={t('common:account.transferHelpText')}
                            variant="outlined"
                        />

                        <div className={formClasses.formControl}>
                            <Button
                                className={`${formClasses.formControlButton} submitButton ${formClasses.button}`}
                                color="primary"
                                variant="contained"
                                onClick={() => submitNewLimits()}
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

export default AccountSetLimits;
