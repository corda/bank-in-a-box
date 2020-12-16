import { DateTimePicker, MuiPickersUtilsProvider } from '@material-ui/pickers';
import { Fade, Paper } from '@material-ui/core';
import React, { useCallback, useState } from 'react';
import { RecurringPayment, RedirectWithRecurringPayment, SortOrder } from '../../store/types';
import { RecurringPaymentsSortFields, getRecurringPaymentsByAccount } from '../../api/recurringPaymentApi';
import { useHistory, useLocation } from 'react-router-dom';

import { Alert } from '../Notifications/Notifcations';
import DateFnsUtils from '@date-io/date-fns';
import { DynamicTable } from '../DynamicTable/DynamicTable';
import { RECURRINGPAYMENT } from '../../constants/Routes';
import { ResolvedPromise } from '../../api/resolvePromise';
import { TranslateRecurringPayments } from '../../i18n/TranslateRecurringPayments';
import { mapRecurringPayments } from '../../utils/Utils';
import { useInfoDisplayStyles } from '../MaterialStyles/InfoDisplayStyles';
import { useTranslation } from 'react-i18next';

type Props = {
    accountId: string;
};

type SortToggle = {
    sortField: RecurringPaymentsSortFields;
    sortOrder: SortOrder;
};

const AccountRecurringPayments: React.FC<Props> = (props) => {
    const { accountId } = props;
    const history = useHistory<RedirectWithRecurringPayment>();
    const location = useLocation();
    const { t } = useTranslation('common');
    const [recurringPaymentData, setRecurringPaymentData] = useState<RecurringPayment[]>([]);
    const [totalRecords, setTotalRecords] = useState<number>(0);
    const [pageSize] = useState<number>(10);
    const [dataAvailable, setDataAvailable] = useState<boolean>(true);
    const [fromDate, setFromDate] = useState<Date>(
        (): Date => {
            const date = new Date();
            date.setDate(date.getDate() - parseInt((window as any).REACT_APP_MONTH_OFFSET!.trim()));
            return date;
        }
    );
    const [toDate, setToDate] = useState<Date>(new Date());
    const [sortToggle, setSortToggle] = useState<SortToggle>({ sortField: 'accountFrom', sortOrder: 'ASC' });
    const infoDisplayClasses = useInfoDisplayStyles();

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
            Header: t('common:recurringPayment.nextPaymentDate'),
            accessor: 'txDate',
        },
    ];

    const fetchData = useCallback(
        async (pageIndex: number, pageSize: number) => {
            let transactionsResponse: ResolvedPromise;

            if (fromDate && toDate) {
                transactionsResponse = await getRecurringPaymentsByAccount(
                    accountId,
                    pageIndex,
                    pageSize,
                    sortToggle.sortField,
                    sortToggle.sortOrder,
                    fromDate.toISOString(),
                    toDate.toISOString(),
                    ''
                );
            } else {
                transactionsResponse = await getRecurringPaymentsByAccount(
                    accountId,
                    pageIndex,
                    pageSize,
                    sortToggle.sortField,
                    sortToggle.sortOrder,
                    '',
                    '',
                    ''
                );
            }

            if (transactionsResponse.error) {
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
        [accountId, fromDate, toDate, sortToggle.sortOrder, sortToggle.sortField, t]
    );

    const onSortHandler = (sortOption: string) => {
        let derivedSortOption = sortOption as RecurringPaymentsSortFields;

        if (sortToggle.sortField === derivedSortOption!) {
            setSortToggle({
                sortField: sortToggle.sortField,
                sortOrder: sortToggle.sortOrder === 'ASC' ? 'DESC' : 'ASC',
            });
        } else {
            setSortToggle({ sortField: derivedSortOption, sortOrder: 'ASC' });
        }
    };

    const onRowClickHandler = (dataIndex: number) => {
        history.push({
            pathname: RECURRINGPAYMENT,
            state: { recurringPayment: recurringPaymentData[dataIndex], from: location.pathname },
        });
    };

    return (
        <Fade in={true}>
            <Paper elevation={3} className={infoDisplayClasses.infoDisplay}>
                <div className={infoDisplayClasses.tableContainer}>
                    {!dataAvailable && <Alert severity="warning">{t('common:warning.noRecurringPayments')}</Alert>}
                    <div className={infoDisplayClasses.tableContainerInputs}>
                        <MuiPickersUtilsProvider utils={DateFnsUtils}>
                            <DateTimePicker
                                className={`${infoDisplayClasses.tableContainerInput} fromeDateInput`}
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
                                className={`${infoDisplayClasses.tableContainerInput} fromeDateInput`}
                                label={t('common:transaction.toDate')}
                                inputVariant="outlined"
                                value={toDate}
                                ampm={false}
                                showTodayButton
                                onChange={(date) => {
                                    setToDate(new Date(date!.toISOString()));
                                }}
                            />
                        </MuiPickersUtilsProvider>
                    </div>

                    <DynamicTable<RecurringPayment>
                        className={'--recurringPaymentsTable'}
                        columns={headers}
                        data={recurringPaymentData}
                        totalRecords={totalRecords}
                        rowsPerPage={pageSize}
                        getRowProps={onRowClickHandler}
                        fetchData={fetchData}
                        sortBy={onSortHandler}
                    ></DynamicTable>
                </div>
            </Paper>
        </Fade>
    );
};

export default AccountRecurringPayments;
