import { Account, CurrencyType, EmptyAccountData, RedirectWithAccount } from '../../store/types';
import { Button, Fade, InputLabel, MenuItem, Select, TextField } from '@material-ui/core';
import React, { useEffect, useState } from 'react';
import { ZeroOrEmptyString, currencyTypes, validateStringIsNumber } from '../../utils/Utils';
import { useHistory, useLocation } from 'react-router-dom';

import Header from '../Header/Header';
import { INSUFFICIENTFUNDSERRORKEYWORD } from '../../constants/APIERRORS';
import useEventListener from '@use-it/event-listener';
import { useFormStyles } from '../MaterialStyles/Forms';
import { useSnackbar } from 'notistack';
import { useTranslation } from 'react-i18next';
import { withdrawFiat } from '../../api/accountApi';

const AccountWithdraw: React.FC = () => {
    const location = useLocation<RedirectWithAccount>();
    const history = useHistory();
    const { t } = useTranslation();
    const [currencyType, setCurrencyType] = useState<CurrencyType>('EUR');
    const [submitButtonDisabled, setSubmitButtonDisabled] = useState(true);
    const [withdrawAmount, setWithdrawAmount] = useState<string>('');
    const formClasses = useFormStyles();
    const { enqueueSnackbar } = useSnackbar();

    const handleKeyDown = (event: { keyCode: number }) => {
        if (event.keyCode === 13 && !submitButtonDisabled) {
            submitWithdraw();
        }
    };

    useEventListener('keyup', handleKeyDown);

    const setInitialAccountData = (): Account => {
        //If the user is navigating here from accountDetails page (selecting an account)
        if (location.state !== undefined) {
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
        const value = event.target.value;
        if (value.length > 0 && !validateStringIsNumber(value)) {
            return;
        }
        setWithdrawAmount(value);
    };

    const handleCurrencySelect = (event: React.ChangeEvent<{ value: unknown }>) => {
        setCurrencyType(event.target.value as CurrencyType);
    };

    const submitWithdraw = async () => {
        setSubmitButtonDisabled(true);
        const depositResponse = await withdrawFiat(
            account.accountData.accountId,
            currencyType,
            parseFloat(withdrawAmount) * 100
        );

        if (depositResponse.error) {
            let errorMessage = t('common:error.withdraw', { error: depositResponse.error });
            if (depositResponse.error.includes(INSUFFICIENTFUNDSERRORKEYWORD)) {
                errorMessage = t('common:error.insufficientBalance');
            }
            if (depositResponse.error.includes('Token mismatch')) {
                errorMessage = t('common:error:tokenMismatch');
            }
            if (depositResponse.error.includes('withdrawal limit exceeded')) {
                errorMessage = t('common:error.withdrawalLimitExceeded');
            }
            enqueueSnackbar(errorMessage, { variant: 'error' });
        } else {
            enqueueSnackbar(t('common:success.withdraw'), { variant: 'success' });
            goToPageWithAccount('/accountView');
        }

        setSubmitButtonDisabled(false);
    };

    useEffect(() => {
        if (!ZeroOrEmptyString(withdrawAmount)) {
            setSubmitButtonDisabled(false);
        } else {
            setSubmitButtonDisabled(true);
        }
    }, [withdrawAmount]);

    return (
        <div className="accountSetStatusWrapper">
            <Header>{t('common:payments.withdraw')}</Header>
            <Fade in={true}>
                <div className={formClasses.inputWrapper}>
                    <div className={formClasses.column}>
                        <TextField
                            className={`${formClasses.columnItem} ${formClasses.formInput} withdrawAmountInput ${
                                ZeroOrEmptyString(withdrawAmount)
                                    ? formClasses.incompleteInput
                                    : formClasses.completedInput
                            }`}
                            label={t('common:payments.withdrawAmount')}
                            onChange={(event) => handleNumberInput(event)}
                            name="DepositAmount"
                            value={withdrawAmount}
                            variant="outlined"
                            helperText={`${t('common:inputHelpText.withdrawAmount', {
                                currency: account.accountData.currency,
                            })}. ${ZeroOrEmptyString(withdrawAmount) ? t('common:inputHelpText.cannotBeZero') : ''}`}
                        />

                        <InputLabel className={formClasses.label} shrink id="currencyType-label">
                            {t('common:account.selectCurrency')}
                        </InputLabel>
                        <Select
                            className={`${formClasses.columnItem} ${formClasses.formInput} currencyInput`}
                            labelId="currencyType-label"
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
                                color="primary"
                                variant="contained"
                                onClick={() => submitWithdraw()}
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

export default AccountWithdraw;
