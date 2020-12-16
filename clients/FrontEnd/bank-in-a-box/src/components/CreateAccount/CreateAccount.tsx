import './CreateAccount.scss';

import * as API from '../../api/accountApi';

import { AccountPaginated, AccountType, Customer } from '../../store/types';
import Axios, { CancelTokenSource } from 'axios';
import { Button, Fade, InputLabel, MenuItem, Paper, Select, TextField } from '@material-ui/core';
import { DateTimePicker, MuiPickersUtilsProvider } from '@material-ui/pickers';
import { FAILEDTOCONVERTERRORS, STARTDATEERROR } from '../../constants/APIERRORS';
import React, { useCallback, useEffect, useState } from 'react';
import {
    ZeroOrEmptyString,
    currencyTypes,
    mapAccounts,
    mapCustomersResponse,
    validateStringIsNumber,
} from '../../utils/Utils';

import { Alert } from '../Notifications/Notifcations';
import DateFnsUtils from '@date-io/date-fns';
import Header from '../Header/Header';
import { getAccountsPaginatedWithSort } from '../../api/accountApi';
import { getCustomerPaginatedWithSort } from '../../api/customerApi';
import useEventListener from '@use-it/event-listener';
import { useFormStyles } from '../MaterialStyles/Forms';
import { useHistory } from 'react-router-dom';
import { useSnackbar } from 'notistack';
import { useTranslation } from 'react-i18next';

const CreateAccount: React.FC = () => {
    //Values related to current account + savings account
    const [customerSearchFieldInput, setCustomerSearchFieldInputValue] = useState<string>('');
    const [currencyType, setCurrencyType] = useState<string>('EUR');
    const [accountType, setAccountType] = useState<AccountType>('CURRENT');
    const [customer, setCustomer] = useState<Customer | null>(null);
    const [searchCustomerResult, setSearchCustomerResult] = useState<Customer[]>([]);
    const [totalCustomerResults, setTotalCustomerResults] = useState<number>(0);
    const [searchCancelToken, setSearchCancelToken] = useState<CancelTokenSource>();
    const [customerSearchPage, setCustomerSearchPage] = useState<number>(1);
    const [submitButtonDisabled, setSubmitButtonDisable] = useState<boolean>(true);

    //Values related to current account
    const [withdrawlDailyLimit, setWithdrawalDailyLimit] = useState<string>('');
    const [transferDailyLimit, setTransferDailyLimit] = useState<string>('');

    //Values related to savings account
    const [currentAccount, setCurrentAccount] = useState<AccountPaginated | null>(null);
    const [searchAccountResult, setSearchAccountResults] = useState<AccountPaginated[]>([]);
    const [currentAccountdataAvailable, setCurrentAccountDataAvailable] = useState<boolean>(false);
    const [savingsAmountInput, setSavingsAmountInput] = useState<string>('');
    const [savingsStartDateInput, setSavingsStartDateInput] = useState<Date>(new Date());
    const [savingsPeriodInput, setSavingsPeriodInput] = useState<string>('0');
    const [searchedSinceDismissed, setSearchedSinceDismissed] = useState<boolean>(true);

    const history = useHistory();
    const { t } = useTranslation('common');
    const cancelErrorMessage = 'CANCELLED BECAUSE NEW REQUEST';
    const resultsPerCustomerSearch = 20;
    const formClasses = useFormStyles();
    const { enqueueSnackbar } = useSnackbar();

    const handleKeyDown = (event: { keyCode: number }) => {
        if (event.keyCode === 13 && !submitButtonDisabled) {
            accountType === 'CURRENT' ? createCurrentAccount() : createSavingsAccount();
        }
    };

    const showSearchResult = searchCustomerResult.length > 0 && customer === null && searchedSinceDismissed;

    useEventListener('keyup', handleKeyDown);

    useEffect(() => {
        const fetchCurrentAccounts = async () => {
            let searchTerm = customer?.customerId;
            let accounts = await getAccountsPaginatedWithSort(1, 3000, 'customerName', 'ASC', searchTerm);
            if (accounts.error) {
                enqueueSnackbar(t('common:error.serverContactError', { error: accounts.error }), { variant: 'error' });
            } else {
                let accountsData: AccountPaginated[] = mapAccounts(filterForCurrentAccounts(accounts.data.data.result));

                if (accountsData.length <= 0) {
                    enqueueSnackbar(
                        t('common:error.noCurrentAccountsForCustomer', { customerName: customer?.customerName }),
                        { variant: 'error' }
                    );
                    setCurrentAccountDataAvailable(false);
                    setCustomerSearchFieldInputValue('');
                    setSearchCustomerResult([]);
                    setCustomer(null);
                } else {
                    enqueueSnackbar(
                        accountsData.length > 1
                            ? t('common:success.foundCurrentAccounts', {
                                  amount: accountsData.length,
                                  customerName: customer?.customerName,
                              })
                            : t('common:success.foundCurrentAccount', { customerName: customer?.customerName }),
                        {
                            variant: 'success',
                        }
                    );
                    setCurrentAccountDataAvailable(true);
                }
                setSearchAccountResults(accountsData);
                setCurrentAccount(accountsData[0]);
            }
        };

        if (customer !== null && accountType === 'SAVINGS') {
            fetchCurrentAccounts();
        }
    }, [customer, accountType, t, enqueueSnackbar]);

    useEffect(() => {
        if (
            accountType === 'SAVINGS' &&
            customer !== null &&
            currentAccount !== null &&
            savingsPeriodInput !== '0' &&
            savingsPeriodInput.length > 0 &&
            savingsAmountInput !== '0' &&
            savingsAmountInput.length > 0
        ) {
            setSubmitButtonDisable(false);
        } else if (accountType === 'CURRENT' && customer !== null) {
            setSubmitButtonDisable(false);
        } else {
            setSubmitButtonDisable(true);
        }
    }, [
        customerSearchFieldInput,
        currentAccount,
        savingsAmountInput,
        savingsStartDateInput,
        savingsPeriodInput,
        customer,
        accountType,
    ]);

    const fetchCustomers = useCallback(
        async (startPage: number) => {
            const cancelToken = Axios.CancelToken.source();
            setSearchCancelToken(cancelToken);

            let customers = await getCustomerPaginatedWithSort(
                startPage,
                resultsPerCustomerSearch,
                'customerName',
                'ASC',
                customerSearchFieldInput,
                cancelToken.token
            );
            if (customers.error) {
                if (customers.error.toString().includes(cancelErrorMessage)) {
                    return;
                }
                enqueueSnackbar(t('common:error.serverContactError', { error: customers.error }), { variant: 'error' });
            } else {
                let customersData: Customer[] = mapCustomersResponse(customers.data.data.result);
                setTotalCustomerResults(customers.data.data.totalResults);
                if (customersData.length <= 0 && startPage === 1) {
                    setSearchCustomerResult([]);
                } else if (customersData.length > 0 && startPage > 1) {
                    setSearchCustomerResult((s) => s.concat(customersData));
                } else {
                    setSearchCustomerResult(customersData);
                }
            }
        },
        [customerSearchFieldInput, t, enqueueSnackbar]
    );

    useEffect(() => {
        if (customerSearchFieldInput.length >= 3 && customer === null) {
            fetchCustomers(customerSearchPage);
        }

        if (customerSearchFieldInput.length < 3) {
            setSearchCustomerResult([]);
        }
    }, [customerSearchFieldInput, customer, customerSearchPage, fetchCustomers]);

    const selectCustomer = (customer: Customer) => {
        setCustomerSearchFieldInputValue(customer.customerName);
        setCustomer(customer);
    };

    const handleCustomerSearch = (event: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) => {
        const value = event.target.value;
        setCustomerSearchFieldInputValue(value);
        if (typeof searchCancelToken !== typeof undefined) {
            searchCancelToken?.cancel(cancelErrorMessage);
        }
        setCustomer(null);
        setCustomerSearchPage(1);
        setSearchAccountResults([]);
        setCurrentAccountDataAvailable(false);
        setCurrentAccount(null);
        setSearchedSinceDismissed(true);
    };

    const handleCurrencySelect = (event: React.ChangeEvent<{ value: unknown }>) => {
        setCurrencyType(event.target.value as string);
    };

    const filterForCurrentAccounts = (data: any[]) => {
        return data.filter((item) => item.first.type === 'current' || item.first.type === 'overdraft');
    };

    const handleAccountTypeSelect = (event: React.ChangeEvent<{ value: unknown }>) => {
        event.preventDefault();
        setAccountType(event.target.value as AccountType);
    };

    const selectCurrentAccount = (accountId: string) => {
        searchAccountResult.forEach((acc) => {
            if (accountId === acc.account.accountData.accountId) {
                setCurrentAccount(acc);
                return;
            }
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
        } else if (event.target.name === 'SavingsAmount') {
            setSavingsAmountInput(value);
        } else if (event.target.name === 'SavingsPeriod') {
            setSavingsPeriodInput(value);
        }
    };

    const createSavingsAccount = async () => {
        if (customer === null || currentAccount === null) {
            return;
        }
        setSubmitButtonDisable(true);
        const response = await API.createSavingsAccount(
            customer.customerId,
            currencyType,
            currentAccount.account.accountData.accountId,
            parseInt(savingsAmountInput) * 100,
            savingsStartDateInput.toISOString(),
            savingsPeriodInput
        );
        if (response.error) {
            let errorMessage = response.error;
            if (errorMessage?.includes(FAILEDTOCONVERTERRORS)) {
                errorMessage = t('common:error.invalidValue');
            }
            if (response.error.includes(STARTDATEERROR)) {
                errorMessage = t('common:error.startDateCannotBePast');
            }
            enqueueSnackbar(t('common:error.savingsAccount', { error: errorMessage }), { variant: 'error' });
        } else {
            enqueueSnackbar(t('common:success.accountCreated'), { variant: 'success' });
            history.push('/accounts');
        }

        setSubmitButtonDisable(false);
    };

    const createCurrentAccount = async () => {
        if (customer === null) {
            return;
        }
        const response = await API.createCurrentAccount(
            customer.customerId,
            currencyType,
            withdrawlDailyLimit !== '' ? (parseInt(withdrawlDailyLimit) * 100).toString() : '',
            transferDailyLimit !== '' ? (parseInt(transferDailyLimit) * 100).toString() : ''
        );
        if (response.error) {
            let errorMessage = response.error;
            if (errorMessage?.includes(FAILEDTOCONVERTERRORS)) {
                errorMessage = t('common:error.invalidValue');
            }
            enqueueSnackbar(t('common:error.currentAccount', { error: errorMessage }), { variant: 'error' });
        } else {
            enqueueSnackbar(t('common:success.accountCreated'), { variant: 'success' });
            history.push('/accounts');
        }
    };

    const createAccount = () => {
        if (accountType === 'SAVINGS') {
            createSavingsAccount();
        } else {
            createCurrentAccount();
        }
    };

    return (
        <div className="createAccountWrapper">
            <Header>{t('common:pageTitle.createAccount')}</Header>

            <Fade in={true}>
                <Paper elevation={3} className={formClasses.inputWrapper}>
                    <div className={formClasses.column}>
                        <InputLabel className={formClasses.label} shrink id="accountType-label">
                            {t('common:account.selectAccountType')}
                        </InputLabel>

                        <Select
                            className={`${formClasses.columnItem} accountTypeSelect  ${formClasses.formInput}`}
                            labelId="accountType-label"
                            onChange={(event) => handleAccountTypeSelect(event)}
                            value={accountType}
                            variant="outlined"
                        >
                            <MenuItem value="CURRENT">{t('common:account.currentAccountOption')}</MenuItem>
                            <MenuItem value="SAVINGS">{t('common:account.savingsAccountOption')}</MenuItem>
                        </Select>

                        <TextField
                            className={`${formClasses.columnItem} customerNameSearch  ${formClasses.formInput} ${
                                customer !== null ? formClasses.completedInput : formClasses.incompleteInput
                            }`}
                            label={t('common:commonText.customersSearch')}
                            onChange={(event) => {
                                handleCustomerSearch(event);
                            }}
                            onBlur={() => setSearchedSinceDismissed(false)}
                            name="Customer Search"
                            value={customerSearchFieldInput}
                            required
                            variant="outlined"
                            helperText={t('common:inputHelpText.customerSearchAccountCreation')}
                            autoComplete="new-password"
                        />

                        {showSearchResult && (
                            <div className="searchResultDropdown">
                                {searchCustomerResult.map((customer, index) => {
                                    return (
                                        <div
                                            key={index}
                                            className="searchResultDropdown__result"
                                            onMouseDown={() => {
                                                selectCustomer(customer);
                                            }}
                                        >
                                            <div className="searchResultDropdown__result__row">
                                                <div className="searchResultDropdown__result__heading">
                                                    {t('common:account.customerResults.customerName')}
                                                </div>
                                                <div className="searchResultDropdown__result__value">
                                                    {customer.customerName}
                                                </div>
                                            </div>
                                            <div className="searchResultDropdown__result__row">
                                                <div className="searchResultDropdown__result__heading">
                                                    {t('common:account.customerResults.customerId')}
                                                </div>
                                                <div className="searchResultDropdown__result__value">
                                                    {customer.customerId}
                                                </div>
                                            </div>
                                            <div className="searchResultDropdown__result__row">
                                                <div className="searchResultDropdown__result__heading">
                                                    {t('common:account.customerResults.customerEmail')}
                                                </div>
                                                <div className="searchResultDropdown__result__value">
                                                    {customer.emailAddress}
                                                </div>
                                            </div>
                                            <div className="searchResultDropdown__result__row">
                                                <div className="searchResultDropdown__result__heading">
                                                    {t('common:account.customerResults.contactNumber')}
                                                </div>
                                                <div className="searchResultDropdown__result__value">
                                                    {customer.contactNumber}
                                                </div>
                                            </div>
                                        </div>
                                    );
                                })}
                                {totalCustomerResults > customerSearchPage * resultsPerCustomerSearch && (
                                    <div
                                        className="searchResultDropdown__result"
                                        onClick={() => {
                                            setCustomerSearchPage((customerSearchPage) => customerSearchPage + 1);
                                        }}
                                    >
                                        {t('common:account.customerResults.loadMore', {
                                            amount:
                                                totalCustomerResults - customerSearchPage * resultsPerCustomerSearch,
                                        })}
                                    </div>
                                )}
                            </div>
                        )}
                        <InputLabel className={formClasses.label} shrink id="currencyType-label">
                            {t('common:account.selectCurrency')}
                        </InputLabel>
                        <Select
                            labelId="currencyType-label"
                            className={`${formClasses.columnItem} currencyInput  ${formClasses.formInput}`}
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
                        {accountType === 'CURRENT' && (
                            <>
                                <TextField
                                    className={`${formClasses.columnItem} withdrawalDailyLimitInput  ${formClasses.formInput}`}
                                    label={t('common:account.withdrawalDailyLimit')}
                                    onChange={(event) => handleNumberInput(event)}
                                    name="WithdrawalDailyLimit"
                                    value={withdrawlDailyLimit}
                                    helperText={t('common:account.withdrawalHelpText')}
                                    variant="outlined"
                                />
                                <TextField
                                    className={`${formClasses.columnItem} transferDailyLimitInput  ${formClasses.formInput}`}
                                    label={t('common:account.transferDailyLimit')}
                                    onChange={(event) => handleNumberInput(event)}
                                    name="TransferDailyLimit"
                                    value={transferDailyLimit}
                                    helperText={t('common:account.transferHelpText')}
                                    variant="outlined"
                                />
                            </>
                        )}

                        {accountType === 'SAVINGS' && currentAccountdataAvailable && (
                            <>
                                <Select
                                    className={`${formClasses.columnItem} selectCurrentAccountInput  ${formClasses.formInput}`}
                                    label={t('common:account.selectCurrentAccount')}
                                    onChange={(event) => selectCurrentAccount(event.target.value as string)}
                                    value={currentAccount ? currentAccount.account.accountData.accountId : ''}
                                    variant="outlined"
                                >
                                    {searchAccountResult.map((account, index) => {
                                        return (
                                            <MenuItem key={index} value={account.account.accountData.accountId}>
                                                {t('common:account.accountID')} {account.account.accountData.accountId}{' '}
                                                {t('common:commonText.accountStatus')}{' '}
                                                {account.account.accountData.status}
                                            </MenuItem>
                                        );
                                    })}
                                </Select>
                                <TextField
                                    className={`${formClasses.columnItem} savingsAmountInput  ${
                                        formClasses.formInput
                                    }  ${
                                        ZeroOrEmptyString(savingsAmountInput)
                                            ? formClasses.incompleteInput
                                            : formClasses.completedInput
                                    }`}
                                    label={t('common:account.savingsAmount')}
                                    onChange={(event) => handleNumberInput(event)}
                                    name="SavingsAmount"
                                    required
                                    helperText={`${
                                        ZeroOrEmptyString(savingsAmountInput)
                                            ? t('common:inputHelpText.cannotBeZero')
                                            : ''
                                    }`}
                                    value={savingsAmountInput}
                                    variant="outlined"
                                />
                            </>
                        )}
                        {accountType === 'SAVINGS' && !currentAccountdataAvailable && (
                            <Alert severity="warning">{t('common:warning.selectCustomerWithCurrentAccount')}</Alert>
                        )}
                    </div>
                    <div className={formClasses.column}>
                        {accountType === 'SAVINGS' && (
                            <>
                                <MuiPickersUtilsProvider utils={DateFnsUtils}>
                                    <DateTimePicker
                                        className={`${formClasses.columnItem} savingsStartDateInput  ${formClasses.formInput}`}
                                        label={t('common:transaction.fromDate')}
                                        inputVariant="outlined"
                                        value={savingsStartDateInput}
                                        autoOk
                                        ampm={false}
                                        showTodayButton
                                        onChange={(date) => {
                                            setSavingsStartDateInput(new Date(date!.toISOString()));
                                        }}
                                    />
                                </MuiPickersUtilsProvider>
                                <TextField
                                    className={`${formClasses.columnItem} savingsPeriodInput  ${
                                        formClasses.formInput
                                    } ${
                                        ZeroOrEmptyString(savingsPeriodInput)
                                            ? formClasses.incompleteInput
                                            : formClasses.completedInput
                                    }`}
                                    label={t('common:account.savingsPeriod')}
                                    onChange={(event) => handleNumberInput(event)}
                                    name="SavingsPeriod"
                                    required
                                    value={savingsPeriodInput}
                                    variant="outlined"
                                    helperText={`${
                                        ZeroOrEmptyString(savingsPeriodInput)
                                            ? t('common:inputHelpText.cannotBeZero')
                                            : ''
                                    }`}
                                />
                            </>
                        )}
                        <div className={formClasses.formControl}>
                            <Button
                                className={`${formClasses.formControlButton} submitButton `}
                                disabled={submitButtonDisabled}
                                variant="contained"
                                color="primary"
                                onClick={createAccount}
                            >
                                {t('common:button.save')}
                            </Button>
                            <Button
                                className={`${formClasses.formControlButton}`}
                                variant="contained"
                                color="primary"
                                onClick={() => history.push('/accounts')}
                            >
                                {t('common:button.cancel')}
                            </Button>
                        </div>
                    </div>
                </Paper>
            </Fade>
        </div>
    );
};

export default CreateAccount;
