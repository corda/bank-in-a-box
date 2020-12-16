import { Account, CurrencyType, EmptyAccountData, RedirectWithAccount } from '../../store/types';
import { Button, Fade, InputLabel, MenuItem, Select, TextField } from '@material-ui/core';
import React, { useEffect, useState } from 'react';
import { ZeroOrEmptyString, currencyTypes, validateStringIsNumber } from '../../utils/Utils';
import { useHistory, useLocation } from 'react-router-dom';

import Header from '../Header/Header';
import { depositFiat } from '../../api/accountApi';
import useEventListener from '@use-it/event-listener';
import { useFormStyles } from '../MaterialStyles/Forms';
import { useSnackbar } from 'notistack';
import { useTranslation } from 'react-i18next';

const AccountDeposit: React.FC = () => {
    const location = useLocation<RedirectWithAccount>();
    const history = useHistory();
    const { t } = useTranslation();
    const [currencyType, setCurrencyType] = useState<CurrencyType>('EUR');
    const [submitButtonDisabled, setSubmitButtonDisabled] = useState(true);
    const [depositAmount, setDepositAmount] = useState<string>('');
    const formClasses = useFormStyles();

    const { enqueueSnackbar } = useSnackbar();

    const handleKeyDown = (event: { keyCode: number }) => {
        if (event.keyCode === 13 && !submitButtonDisabled) {
            submitDeposit();
        }
    };

    useEventListener('keyup', handleKeyDown);

    const setInitialAccountData = (): Account => {
        //If the user is navigating here from accountDetails page (selecting an account)
        if (location.state !== undefined) {
            window.scrollTo(0, 0);
            return location.state.account;
            //If the user tried to navigate to this page by url (without a selected account)
            //Will be pushed back to homepage
        } else {
            history.push('/');
            return EmptyAccountData;
        }
    };

    const [account] = useState<Account>(setInitialAccountData());

    const goToPageWithAccount = (path: string) => {
        history.push({
            pathname: path,
            state: { account: account, from: location.pathname },
        });
    };

    const handleNumberInput = (event: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) => {
        const value = event.currentTarget.value;
        if (value.length > 0 && !validateStringIsNumber(value)) {
            return;
        }
        setDepositAmount(value);
    };

    const handleCurrencySelect = (event: React.ChangeEvent<{ value: unknown }>) => {
        setCurrencyType(event.target.value as CurrencyType);
    };

    const submitDeposit = async () => {
        setSubmitButtonDisabled(true);
        const depositResponse = await depositFiat(
            account.accountData.accountId,
            currencyType,
            parseFloat(depositAmount) * 100
        );

        if (depositResponse.error) {
            let errorMessage = depositResponse.error;
            if (errorMessage.includes('Token mismatch')) {
                errorMessage = t('common:error:tokenMismatch');
            }
            if (errorMessage.includes('long overflow')) {
                errorMessage = t('common:error.invalidValue');
            }
            enqueueSnackbar(t('common:error:deposit', { error: errorMessage }), { variant: 'error' });
        } else {
            enqueueSnackbar(t('common:success:deposit'), { variant: 'success' });
            goToPageWithAccount('/accountView');
        }

        setSubmitButtonDisabled(false);
    };

    useEffect(() => {
        if (!ZeroOrEmptyString(depositAmount)) {
            setSubmitButtonDisabled(false);
        } else {
            setSubmitButtonDisabled(true);
        }
    }, [depositAmount]);

    return (
        <div className="accountSetStatusWrapper">
            <Header>{t('common:payments.deposit')}</Header>
            <Fade in={true}>
                <div className={formClasses.inputWrapper}>
                    <div className={formClasses.column}>
                        <TextField
                            className={`${formClasses.columnItem} ${formClasses.formInput} depositAmountInput ${
                                ZeroOrEmptyString(depositAmount)
                                    ? formClasses.incompleteInput
                                    : formClasses.completedInput
                            } `}
                            label={t('common:payments.depositAmount')}
                            onChange={(event) => handleNumberInput(event)}
                            name="DepositAmount"
                            value={depositAmount}
                            variant="outlined"
                            helperText={`${t('common:inputHelpText.deposit', {
                                currency: account.accountData.currency,
                            })}. ${ZeroOrEmptyString(depositAmount) ? t('common:inputHelpText.cannotBeZero') : ''}`}
                        />

                        <InputLabel className={formClasses.label} shrink id="currencyType-label">
                            {t('common:account.selectCurrency')}
                        </InputLabel>
                        <Select
                            labelId="currencyType-label"
                            className={`${formClasses.columnItem} currencyInput  ${formClasses.formInput}`}
                            label={t('common:account.selectCurrency')}
                            onChange={(event) => handleCurrencySelect(event)}
                            value={currencyType}
                            variant="outlined"
                        >
                            {currencyTypes.map((currency, index) => {
                                return (
                                    <MenuItem key={index} value={currency}>
                                        {currency}
                                    </MenuItem>
                                );
                            })}
                        </Select>

                        <div className={formClasses.formControl}>
                            <Button
                                className={`${formClasses.formControlButton} submitButton ${formClasses.button}`}
                                variant="contained"
                                color="primary"
                                onClick={() => submitDeposit()}
                                disabled={submitButtonDisabled}
                            >
                                {t('common:button.save')}
                            </Button>

                            <Button
                                className={`${formClasses.formControlButton}  ${formClasses.button} cancelButton`}
                                variant="contained"
                                color="primary"
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

export default AccountDeposit;
