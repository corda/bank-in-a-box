import { DateTimePicker, MuiPickersUtilsProvider } from '@material-ui/pickers';
import { Fade, TextField } from '@material-ui/core';
import React, { useCallback, useEffect, useState } from 'react';
import { RedirectWithTransaction, SortOrder, Transaction, UserType } from '../../store/types';
import { TransactionSortFields, getCustomersTransactions, getTransactions } from '../../api/transactionApi';
import { useHistory, useLocation } from 'react-router-dom';

import { Alert } from '../Notifications/Notifcations';
import DateFnsUtils from '@date-io/date-fns';
import { DynamicTable } from '../DynamicTable/DynamicTable';
import Header from '../Header/Header';
import { ResolvedPromise } from '../../api/resolvePromise';
import { TRANSACTIONVIEW } from '../../constants/Routes';
import { TranslateTransactions } from '../../i18n/TranslateTransactions';
import { mapTransactions } from '../../utils/Utils';
import { useAuthProvider } from '../../store/AuthenticationContext';
import { useTableWrapperStyles } from '../MaterialStyles/TabbleWrapper';
import { useTranslation } from 'react-i18next';

type SortToggle = {
    sortField: TransactionSortFields;
    sortOrder: SortOrder;
};

const Transactions: React.FC = () => {
    const [transactionsData, setTransactionsData] = useState<Transaction[]>([]);
    const [dataAvailable, setDataAvailable] = useState<boolean>(false);
    const [totalRecords, setTotalRecords] = useState<number>(0);
    const [error, setError] = useState<string>();
    const [searchFieldInputValue, setSearchFieldInputValue] = useState<string>('');
    const [searchTerm, setSearchTerm] = useState<string>('');
    const [sortToggle, setSortToggle] = useState<SortToggle>({ sortField: 'txId', sortOrder: 'ASC' });
    const [fromDate, setFromDate] = useState<Date>(
        (): Date => {
            const date = new Date();
            date.setDate(date.getDate() - parseInt((window as any).REACT_APP_MONTH_OFFSET!.trim()));
            return date;
        }
    );
    const [toDate, setToDate] = useState<Date>(new Date());
    const pageSize = 10;
    const history = useHistory<RedirectWithTransaction>();
    const location = useLocation();
    const authContext = useAuthProvider();
    const { t } = useTranslation('common');
    const tableWrapperClasses = useTableWrapperStyles();

    const headers = [
        {
            Header: t('common:transaction:transactionId'),
            accessor: 'txId',
        },
        {
            Header: t('common:transaction:accountFrom'),
            accessor: 'accountFrom',
        },

        {
            Header: t('common:transaction:accountTo'),
            accessor: 'accountTo',
        },
        {
            Header: t('common:transaction:amount'),
            accessor: 'amount',
        },
        {
            Header: t('common:transaction:type'),
            accessor: 'txType',
        },
        {
            Header: t('common:transaction:transactionDate'),
            accessor: 'txDate',
        },
    ];
    const fetchData = useCallback(
        async (pageIndex: number, pageSize: number) => {
            let transactionsResponse: ResolvedPromise;

            if (authContext?.user?.userType === UserType.CUSTOMER) {
                transactionsResponse = await getCustomersTransactions(
                    authContext.user.userId,
                    pageIndex,
                    pageSize,
                    searchTerm,
                    sortToggle.sortField,
                    sortToggle.sortOrder,
                    fromDate.toISOString(),
                    toDate.toISOString()
                );
            } else {
                transactionsResponse = await getTransactions(
                    pageIndex,
                    pageSize,
                    searchTerm,
                    sortToggle.sortField,
                    sortToggle.sortOrder,
                    fromDate.toISOString(),
                    toDate.toISOString()
                );
            }

            if (transactionsResponse.error) {
                setError(transactionsResponse.error);
            } else {
                const transactionData: Transaction[] = TranslateTransactions(
                    mapTransactions(transactionsResponse.data.data.result as Transaction[]),
                    t
                );
                setTotalRecords(transactionsResponse.data.data.totalResults);
                setTransactionsData(transactionData);
                if (transactionData.length <= 0) {
                    setDataAvailable(false);
                } else {
                    setDataAvailable(true);
                }
            }
        },
        [sortToggle.sortField, sortToggle.sortOrder, searchTerm, t, fromDate, toDate, authContext]
    );

    const onSortHandler = (sortOption: string) => {
        const derivedSortOption = sortOption as TransactionSortFields;

        if (sortToggle.sortField === derivedSortOption!) {
            setSortToggle({
                sortField: sortToggle.sortField,
                sortOrder: sortToggle.sortOrder === 'ASC' ? 'DESC' : 'ASC',
            });
        } else {
            setSortToggle({ sortField: derivedSortOption, sortOrder: 'ASC' });
        }
    };

    const handleSearchInput = (event: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) => {
        event.preventDefault();
        const value: string = event.currentTarget.value;
        if (value.length >= 3) {
            setSearchTerm(value);
            setSortToggle({ sortField: 'txId', sortOrder: 'ASC' });
        } else {
            setSearchTerm('');
        }
        setSearchFieldInputValue(value);
    };

    const onRowClickHandler = (dataIndex: number) => {
        history.push({
            pathname: TRANSACTIONVIEW,
            state: { transaction: transactionsData[dataIndex], from: location.pathname },
        });
    };

    useEffect(() => {
        window.scroll(0, 0);
    }, []);

    if (error) {
        return <Header>{t('common:error.fetchingData')}</Header>;
    }

    return (
        <div className="accountsWrapper">
            <Header>{t('common:pageTitle.transactions')}</Header>
            <Fade in={true}>
                <div className={tableWrapperClasses.mainTableWrapper}>
                    <div className={tableWrapperClasses.toolBar}>
                        <div className={`${tableWrapperClasses.toolBarItem} ${tableWrapperClasses.searchFieldItem}`}>
                            <TextField
                                className="searchField"
                                label={t('common:commonText.transactionsSearch')}
                                onChange={(event) => {
                                    handleSearchInput(event);
                                }}
                                variant="outlined"
                                value={searchFieldInputValue}
                                helperText={t('common:inputHelpText.transactionsSearch')}
                            />
                        </div>

                        <MuiPickersUtilsProvider utils={DateFnsUtils}>
                            <div className={tableWrapperClasses.toolBarItem}>
                                <DateTimePicker
                                    className={`${tableWrapperClasses.datePicker} fromDateInput`}
                                    label={t('common:transaction.fromDate')}
                                    inputVariant="outlined"
                                    value={fromDate}
                                    autoOk
                                    ampm={false}
                                    showTodayButton
                                    onChange={(date) => {
                                        setFromDate(new Date(date!.toISOString()));
                                    }}
                                />

                                <DateTimePicker
                                    className={`${tableWrapperClasses.datePicker} toDateInput`}
                                    label={t('common:transaction.toDate')}
                                    inputVariant="outlined"
                                    value={toDate}
                                    ampm={false}
                                    showTodayButton
                                    onChange={(date) => {
                                        setToDate(new Date(date!.toISOString()));
                                    }}
                                />
                            </div>
                        </MuiPickersUtilsProvider>
                    </div>

                    {!dataAvailable && (
                        <Alert severity="info">
                            {searchFieldInputValue.length >= 3
                                ? t('common:warning.noSearchResult')
                                : t('common:warning.noResources', { resource: t('common:resources.transactions') })}
                        </Alert>
                    )}

                    <DynamicTable<Transaction>
                        className={'--transactionsTable'}
                        columns={headers}
                        data={transactionsData}
                        totalRecords={totalRecords}
                        rowsPerPage={pageSize}
                        getRowProps={onRowClickHandler}
                        fetchData={fetchData}
                        sortBy={onSortHandler}
                    ></DynamicTable>
                </div>
            </Fade>
        </div>
    );
};

export default Transactions;
