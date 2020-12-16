import { Account, CurrencyType, EmptyAccountData, RedirectWithAccount } from '../../store/types';
import { Button, Fade, TextField } from '@material-ui/core';
import { FAILEDTOCONVERTERRORS, OVERDRAFTERROR } from '../../constants/APIERRORS';
import React, { useEffect, useState } from 'react';
import { ZeroOrEmptyString, validateStringIsNumber } from '../../utils/Utils';
import { useHistory, useLocation } from 'react-router-dom';

import Header from '../Header/Header';
import { issueLoan } from '../../api/accountApi';
import useEventListener from '@use-it/event-listener';
import { useFormStyles } from '../MaterialStyles/Forms';
import { useSnackbar } from 'notistack';
import { useTranslation } from 'react-i18next';

const AccountIssueLoan: React.FC = () => {
    const { t } = useTranslation('common');
    const [submitButtonDisabled, setSubmitButtonDisabled] = useState(true);
    const location = useLocation<RedirectWithAccount>();
    const history = useHistory();
    const formClasses = useFormStyles();

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

    const [account] = useState<Account>(setInitialAccountData());
    const [loanAmount, setLoanAmount] = useState<string>('');
    const [period, setPeriod] = useState<string>('');
    const { enqueueSnackbar } = useSnackbar();

    const handleKeyDown = (event: { keyCode: number }) => {
        if (event.keyCode === 13 && !submitButtonDisabled) {
            submitIssueLoan();
        }
    };

    useEventListener('keyup', handleKeyDown);

    const submitIssueLoan = async () => {
        setSubmitButtonDisabled(true);
        const response = await issueLoan(
            account.accountData.accountId,
            parseInt(loanAmount) * 100,
            parseInt(period),
            account.accountData.currency as CurrencyType
        );
        if (response.error) {
            setSubmitButtonDisabled(false);
            let errorMessage = '';
            errorMessage = response.error;
            if (errorMessage?.includes(OVERDRAFTERROR)) {
                errorMessage = t('common:error:issueLoanOverdraft');
            }
            if (errorMessage?.includes(FAILEDTOCONVERTERRORS)) {
                errorMessage = t('common:error.invalidValue');
            }
            enqueueSnackbar(t('common:error.issueLoan', { error: errorMessage }), { variant: 'error' });
            setSubmitButtonDisabled(false);
        } else {
            enqueueSnackbar(t('common:success.issueLoan'), { variant: 'success' });
            goToPageWithAccount('/accountView');
        }
    };

    const handleNumberInput = (event: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) => {
        const value = event.target.value;
        if (value.length > 0 && !validateStringIsNumber(value)) {
            return;
        }
        if (event.target.name === 'LoanAmount') {
            setLoanAmount(value);
        } else if (event.target.name === 'Period') {
            setPeriod(value);
        }
    };

    const goToPageWithAccount = (path: string) => {
        history.push({
            pathname: path,
            state: { account: account, from: location.pathname },
        });
    };

    useEffect(() => {
        if (!ZeroOrEmptyString(period) && !ZeroOrEmptyString(loanAmount)) {
            setSubmitButtonDisabled(false);
        } else {
            setSubmitButtonDisabled(true);
        }
    }, [period, loanAmount]);

    return (
        <div className="accountSetStatusWrapper">
            <Header>{t('common:pageTitle.issueLoan')}</Header>
            <Fade in={true}>
                <div className={formClasses.inputWrapper}>
                    <div className={formClasses.column}>
                        <TextField
                            className={`${formClasses.columnItem} ${formClasses.formInput} loanAmountInput ${
                                ZeroOrEmptyString(loanAmount) ? formClasses.incompleteInput : formClasses.completedInput
                            }`}
                            label={t('common:account.loanAmount')}
                            onChange={(event) => handleNumberInput(event)}
                            name="LoanAmount"
                            required
                            variant="outlined"
                            value={loanAmount}
                            helperText={`${t('common:account.loanAmountHelpText', {
                                currency: account.accountData.currency,
                            })}. ${ZeroOrEmptyString(loanAmount) ? t('common:inputHelpText.cannotBeZero') : ''}`}
                        />

                        <TextField
                            className={`${formClasses.columnItem} ${formClasses.formInput} loanPeriodInput ${
                                ZeroOrEmptyString(period) ? formClasses.incompleteInput : formClasses.completedInput
                            }`}
                            label={t('common:account.loanPeriod')}
                            onChange={(event) => handleNumberInput(event)}
                            name="Period"
                            required
                            variant="outlined"
                            value={period}
                            helperText={`${t('common:account.loanPeriodHelpText')}. ${
                                ZeroOrEmptyString(period) ? t('common:inputHelpText.cannotBeZero') : ''
                            }`}
                        />

                        <div className={formClasses.formControl}>
                            <Button
                                className={`${formClasses.formControlButton} submitButton ${formClasses.button}`}
                                color="primary"
                                variant="contained"
                                onClick={() => submitIssueLoan()}
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

export default AccountIssueLoan;
