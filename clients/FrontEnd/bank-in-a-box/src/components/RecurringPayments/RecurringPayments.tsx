import { Button, Fade, TextField } from '@material-ui/core';
import { CREATERECURRINGPAYMENT, RECURRINGPAYMENT } from '../../constants/Routes';
import { DateTimePicker, MuiPickersUtilsProvider } from '@material-ui/pickers';
import React, { useCallback, useEffect, useState } from 'react';
import { RecurringPayment, RedirectWithRecurringPayment, SortOrder, UserType } from '../../store/types';
import {
    RecurringPaymentsSortFields,
    getCustomerRecurringPayments,
    getRecurringPayments,
} from '../../api/recurringPaymentApi';
import { useHistory, useLocation } from 'react-router-dom';

import { Alert } from '../Notifications/Notifcations';
import DateFnsUtils from '@date-io/date-fns';
import { DynamicTable } from '../DynamicTable/DynamicTable';
import Header from '../Header/Header';
import { ResolvedPromise } from '../../api/resolvePromise';
import { TranslateRecurringPayments } from '../../i18n/TranslateRecurringPayments';
import { mapRecurringPayments } from '../../utils/Utils';
import { useAuthProvider } from '../../store/AuthenticationContext';
import { useTableWrapperStyles } from '../MaterialStyles/TabbleWrapper';
import { useTranslation } from 'react-i18next';

type SortToggle = {
    sortField: RecurringPaymentsSortFields;
    sortOrder: SortOrder;
};

const RecurringPayments: React.FC = () => {
    const [recurringPaymentData, setRecurringPaymentData] = useState<RecurringPayment[]>([]);
    const [dataAvailable, setDataAvailable] = useState<boolean>(false);
    const [totalRecords, setTotalRecords] = useState<number>(0);
    const [error, setError] = useState<string>();
    const [searchFieldInputValue, setSearchFieldInputValue] = useState<string>('');
    const [searchTerm, setSearchTerm] = useState<string>('');
    const [sortToggle, setSortToggle] = useState<SortToggle>({ sortField: 'accountFrom', sortOrder: 'ASC' });
    const [fromDate, setFromDate] = useState<Date>(
        (): Date => {
            const date = new Date();
            date.setDate(date.getDate() - parseInt((window as any).REACT_APP_MONTH_OFFSET!.trim()));
            return date;
        }
    );
    const [toDate, setToDate] = useState<Date>(new Date());
    const pageSize = 10;
    const history = useHistory<RedirectWithRecurringPayment>();
    const location = useLocation();
    const authContext = useAuthProvider();
    const { t } = useTranslation('common');
    const tableWrapperClasses = useTableWrapperStyles();

    const headers = [
        {
            Header: t('common:recurringPayment.period'),
            accessor: 'period',
        },
        {
            Header: t('common:transaction.accountFrom'),
            accessor: 'accountFrom',
        },

        {
            Header: t('common:transaction.accountTo'),
            accessor: 'accountTo',
        },
        {
            Header: t('common:transaction.amount'),
            accessor: 'amount',
        },
        {
            Header: t('common:recurringPayment.iterationsLeft'),
            accessor: 'iterationNum',
        },
        {
            Header: t('common:recurringPayment.paymentDate'),
            accessor: 'txDate',
        },
    ];

    const fetchData = useCallback(
        async (pageIndex: number, pageSize: number) => {
            let transactionsResponse: ResolvedPromise;

            if (authContext?.user?.userType === UserType.CUSTOMER) {
                transactionsResponse = await getCustomerRecurringPayments(
                    authContext?.user?.userId!,
                    pageIndex,
                    pageSize,
                    searchTerm,
                    sortToggle.sortField,
                    sortToggle.sortOrder,
                    fromDate.toISOString(),
                    toDate.toISOString()
                );
            } else {
                transactionsResponse = await getRecurringPayments(
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
                setError(error);
            } else {
                const recurringPaymentData: RecurringPayment[] = TranslateRecurringPayments(
                    mapRecurringPayments(transactionsResponse.data.data.result as RecurringPayment[]),
                    t
                );
                setTotalRecords(transactionsResponse.data.data.totalResults);
                setRecurringPaymentData(recurringPaymentData);
                if (recurringPaymentData.length <= 0) {
                    setDataAvailable(false);
                } else {
                    setDataAvailable(true);
                }
            }
        },
        [sortToggle.sortField, sortToggle.sortOrder, searchTerm, t, fromDate, toDate, authContext, error]
    );

    const onSortHandler = (sortOption: string) => {
        const derivedSortOption = sortOption as RecurringPaymentsSortFields;

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
            setSortToggle({ sortField: 'accountFrom', sortOrder: 'ASC' });
        } else {
            setSearchTerm('');
        }
        setSearchFieldInputValue(value);
    };

    const onRowClickHandler = (dataIndex: number) => {
        history.push({
            pathname: RECURRINGPAYMENT,
            state: { recurringPayment: recurringPaymentData[dataIndex], from: location.pathname },
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
            <Header>{t('common:pageTitle.recurringPayments')}</Header>
            <Fade in={true}>
                <div className={tableWrapperClasses.mainTableWrapper}>
                    <div className={tableWrapperClasses.toolBar}>
                        <div className={`${tableWrapperClasses.toolBarItem} ${tableWrapperClasses.searchFieldItem}`}>
                            <TextField
                                className="searchField"
                                label={t('common:commonText.recurringPaymentsSearch')}
                                onChange={(event) => {
                                    handleSearchInput(event);
                                }}
                                variant="outlined"
                                value={searchFieldInputValue}
                                helperText={t('common:inputHelpText.recurringPaymentsSearch')}
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
                        {authContext?.user?.userType === UserType.CUSTOMER && (
                            <div
                                className={`${tableWrapperClasses.toolBarItem} ${tableWrapperClasses.createNewButtonItem} createNewButtonItem`}
                            >
                                <Button
                                    className={tableWrapperClasses.createNewButton}
                                    variant="contained"
                                    color="primary"
                                    href={CREATERECURRINGPAYMENT}
                                >
                                    {t('common:button.createNew')}
                                </Button>
                            </div>
                        )}
                    </div>

                    <div className="transactionsContent">
                        <div className="toolBar"></div>

                        {!dataAvailable && (
                            <Alert severity="info">
                                {searchFieldInputValue.length >= 3
                                    ? t('common:warning.noSearchResult')
                                    : t('common:warning.noResources', {
                                          resource: t('common:resources.recurringPayments'),
                                      })}
                            </Alert>
                        )}

                        <DynamicTable<RecurringPayment>
                            className={'--transactionsTable'}
                            columns={headers}
                            data={recurringPaymentData}
                            totalRecords={totalRecords}
                            rowsPerPage={pageSize}
                            getRowProps={onRowClickHandler}
                            fetchData={fetchData}
                            sortBy={onSortHandler}
                        ></DynamicTable>
                    </div>
                </div>
            </Fade>
        </div>
    );
};

export default RecurringPayments;
