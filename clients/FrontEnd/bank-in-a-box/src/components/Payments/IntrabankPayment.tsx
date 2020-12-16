import { Account, AccountPaginated } from '../../store/types';
import { Button, Fade, InputLabel, MenuItem, Select, TextField } from '@material-ui/core';
import React, { useEffect, useState } from 'react';
import {
    ZeroOrEmptyString,
    filterForActiveAccounts,
    filterForActiveCurrentAccounts,
    mapAccounts,
    validateStringIsNumber,
} from '../../utils/Utils';

import { FAILEDTOCONVERTERRORS } from '../../constants/APIERRORS';
import Header from '../Header/Header';
import { TranslateAccounts } from '../../i18n/TranslateAccounts';
import { getCustomersAccountsPaginatedWithSort } from '../../api/accountApi';
import { intrabankPayment } from '../../api/paymentApi';
import { useAuthProvider } from '../../store/AuthenticationContext';
import { useDropDownStyles } from '../MaterialStyles/ElementStyles';
import { useFormStyles } from '../MaterialStyles/Forms';
import { useHistory } from 'react-router-dom';
import { useSnackbar } from 'notistack';
import { useTranslation } from 'react-i18next';

const IntrabankPayment: React.FC = () => {
    const history = useHistory();
    const { t } = useTranslation();
    const [submitButtonDisabled, setSubmitButtonDisabled] = useState<boolean>(true);
    const [accountFromSelectVal, setAccountFromSelectVal] = useState<string>('');
    const [accountToSelectVal, setAccountToSelectVal] = useState<string>('');
    const [accountFromInput, setAccountFromInput] = useState<string>('');
    const [accountToInput, setAccountToInput] = useState<string>('');
    const [amount, setAmount] = useState<string>('0');
    const authContext = useAuthProvider();
    const [currentAccounts, setCurrentAccounts] = useState<Account[]>([]);
    const [toAccounts, setToAccounts] = useState<Account[]>([]);
    const { enqueueSnackbar } = useSnackbar();
    const formClasses = useFormStyles();
    const dropDownStyles = useDropDownStyles();

    const handleNumberInput = (event: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) => {
        const value = event.target.value;
        if (value.length > 0 && !validateStringIsNumber(value)) {
            return;
        }
        setAmount(value);
    };

    const handleAccountSelectFrom = (event: React.ChangeEvent<{ value: unknown }>) => {
        const val = event.target.value;
        setAccountFromSelectVal(val as string);
        setAccountFromInput(val as string);
    };

    const handleAccountSelectTo = (event: React.ChangeEvent<{ value: unknown }>) => {
        const val = event.target.value;
        setAccountToSelectVal(val as string);
        setAccountToInput(val as string);
    };

    const setSelectIfOneOptionFrom = () => {
        if (currentAccounts.length === 1) {
            setAccountFromInput(currentAccounts[0].accountData.accountId);
        }
    };

    const setSelectIfOneOptionTo = () => {
        if (toAccounts.length === 1) {
            setAccountFromInput(toAccounts[0].accountData.accountId);
        }
    };

    const submitIntrabankPayment = async () => {
        setSubmitButtonDisabled(true);
        const intrabankPaymentResponse = await intrabankPayment(
            accountFromInput,
            accountToInput,
            parseFloat(amount) * 100
        );
        if (intrabankPaymentResponse.error) {
            let errorMessage = intrabankPaymentResponse.error;
            if (errorMessage.includes('Daily transfer limit exceeded')) {
                errorMessage = t('common:error.transferLimitExceeded');
            }
            if (errorMessage?.includes(FAILEDTOCONVERTERRORS)) {
                errorMessage = t('common:error.invalidValue');
            }
            enqueueSnackbar(t('common:error.intrabankPayment', { error: errorMessage }), {
                variant: 'error',
            });
        } else {
            enqueueSnackbar(t('common:success.intrabankPayment'), { variant: 'success' });
            clearAllInputs();
        }
        setSubmitButtonDisabled(false);
    };

    const clearAllInputs = () => {
        setAccountFromInput('');
        setAccountToInput('');
        setAmount('0');
    };

    useEffect(() => {
        const getCustAccounts = async () => {
            const accountsResponse = await getCustomersAccountsPaginatedWithSort(
                authContext!.user!.userId,
                1,
                1000,
                'customerName',
                'ASC'
            );

            if (accountsResponse.error) {
                let errorMessage = accountsResponse.error;
                enqueueSnackbar(errorMessage, { variant: 'error' });
                return;
            } else {
                const accountsData: AccountPaginated[] = TranslateAccounts(
                    mapAccounts(accountsResponse.data.data.result),
                    t
                );

                const accountsOnly: Account[] = filterForActiveAccounts(
                    accountsData.map((account) => {
                        return account.account;
                    })
                );

                const currentAccs = filterForActiveCurrentAccounts(accountsOnly);
                if (accountsOnly.length <= 0 || currentAccs.length <= 0) {
                    enqueueSnackbar(t('common:error.noCurrentAccounts'), { variant: 'error' });
                    history.push('/updateCustomer');
                    return;
                }
                const firstAccId = accountsOnly[accountsOnly.length === 1 ? 0 : 1].accountData.accountId;
                setAccountToSelectVal(firstAccId);
                setAccountToInput(firstAccId);
                setToAccounts(accountsOnly);

                const currAccId = currentAccs[0].accountData.accountId;
                setAccountFromSelectVal(currAccId);
                setAccountFromInput(currAccId);
                setCurrentAccounts(currentAccs);
            }
        };
        getCustAccounts();
    }, [history, authContext, t, enqueueSnackbar]);

    useEffect(() => {
        if (
            accountFromInput.length > 0 &&
            accountToInput.length > 0 &&
            amount.length > 0 &&
            accountToInput !== accountFromInput &&
            amount !== '0'
        ) {
            setSubmitButtonDisabled(false);
        } else {
            setSubmitButtonDisabled(true);
        }
    }, [accountFromInput, accountToInput, amount]);

    const sameAccounts = accountToInput === accountFromInput;

    return (
        <div className="createRecurringPaymentWrapper">
            <Header>{t('common:pageTitle.intrabankPayment')}</Header>
            <Fade in={true}>
                <div className={formClasses.inputWrapper}>
                    <div className={formClasses.column}>
                        <InputLabel className={formClasses.label} shrink id="fromAccount-label">
                            {t('common:payments.selectFromAccount')}
                        </InputLabel>

                        <Select
                            className={`${formClasses.columnItem} ${formClasses.formInput} fromAccountSelect`}
                            labelId="fromAccount-label"
                            onChange={(event) => handleAccountSelectFrom(event)}
                            name="FromAccountSelect"
                            // helperText={t('common:payments.selectFromAccount')}
                            value={accountFromSelectVal}
                            onClick={(event) => {
                                setSelectIfOneOptionFrom();
                            }}
                            variant="outlined"
                        >
                            {currentAccounts.map((account, index) => {
                                return (
                                    <MenuItem
                                        className={dropDownStyles.menuItem}
                                        key={index}
                                        value={account.accountData.accountId}
                                    >
                                        <div>ID: {account.accountData.accountId}</div>
                                        <div>Balance: {account.accountData.balance}</div>
                                    </MenuItem>
                                );
                            })}
                        </Select>

                        <TextField
                            className={`${formClasses.columnItem} ${formClasses.formInput} fromAccountInput ${
                                sameAccounts || accountFromInput.length === 0
                                    ? formClasses.incompleteInput
                                    : formClasses.completedInput
                            }`}
                            label={t('common:payments.fromAccount')}
                            onChange={(event) => setAccountFromInput(event.target.value)}
                            value={accountFromInput}
                            helperText={
                                sameAccounts
                                    ? t('common:error.sameAccountsError')
                                    : t('common:inputHelpText.recurringPaymentFromAccount')
                            }
                            required
                            disabled
                            variant="outlined"
                        />

                        <InputLabel className={formClasses.label} shrink id="toAccount-label">
                            {t('common:payments.selectToAccount')}
                        </InputLabel>

                        <Select
                            className={`${formClasses.columnItem} ${formClasses.formInput} toAccountSelect`}
                            labelId="toAccount-label"
                            onChange={(event) => handleAccountSelectTo(event)}
                            name="ToAccountSelect"
                            variant="outlined"
                            value={accountToSelectVal}
                            onClick={(event) => {
                                setSelectIfOneOptionTo();
                            }}
                        >
                            {toAccounts.map((account, index) => {
                                return (
                                    <MenuItem
                                        className={dropDownStyles.menuItem}
                                        key={index}
                                        value={account.accountData.accountId}
                                    >
                                        <div>ID: {account.accountData.accountId}</div>
                                        <div> Balance: {account.accountData.balance}</div>
                                        <div>Type:{account.type}</div>
                                    </MenuItem>
                                );
                            })}
                        </Select>

                        <TextField
                            className={`${formClasses.columnItem} ${formClasses.formInput} toAccountInput  ${
                                sameAccounts || accountToInput.length === 0
                                    ? formClasses.incompleteInput
                                    : formClasses.completedInput
                            }`}
                            label={t('common:payments.toAccount')}
                            onChange={(event) => setAccountToInput(event.target.value)}
                            name="DepositAmount"
                            value={accountToInput}
                            required
                            variant="outlined"
                            helperText={
                                sameAccounts
                                    ? t('common:error.sameAccountsError')
                                    : t('common:inputHelpText.recurringPaymentToAccount')
                            }
                        />

                        <TextField
                            className={`${formClasses.columnItem} ${formClasses.formInput} amountInput  ${
                                ZeroOrEmptyString(amount) ? formClasses.incompleteInput : formClasses.completedInput
                            }`}
                            label={t('common:transaction.amount')}
                            onChange={(event) => handleNumberInput(event)}
                            name="AmountInput"
                            value={amount}
                            required
                            variant="outlined"
                            helperText={`${t('common:transaction.amount')}. ${
                                ZeroOrEmptyString(amount) ? t('common:inputHelpText.cannotBeZero') : ''
                            }`}
                        />
                    </div>
                    <div className={formClasses.column}>
                        <div className={formClasses.formControl}>
                            <Button
                                className={`${formClasses.formControlButton} submitButton `}
                                color="primary"
                                variant="contained"
                                onClick={() => submitIntrabankPayment()}
                                disabled={submitButtonDisabled}
                            >
                                {t('common:button.save')}
                            </Button>

                            <Button
                                className={`${formClasses.formControlButton}`}
                                color="primary"
                                variant="contained"
                                onClick={() => history.push('/recurringPayments')}
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

export default IntrabankPayment;
