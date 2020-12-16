import { DateTimePicker, MuiPickersUtilsProvider } from '@material-ui/pickers';
import { Fade, Paper } from '@material-ui/core';
import React, { useCallback, useState } from 'react';
import { RedirectWithTransaction, SortOrder, Transaction } from '../../store/types';
import { TransactionSortFields, getAccountTransactions } from '../../api/transactionApi';
import { useHistory, useLocation } from 'react-router-dom';

import { Alert } from '../Notifications/Notifcations';
import DateFnsUtils from '@date-io/date-fns';
import { DynamicTable } from '../DynamicTable/DynamicTable';
import { ResolvedPromise } from '../../api/resolvePromise';
import { TRANSACTIONVIEW } from '../../constants/Routes';
import { TranslateTransactions } from '../../i18n/TranslateTransactions';
import { mapTransactions } from '../../utils/Utils';
import { useInfoDisplayStyles } from '../MaterialStyles/InfoDisplayStyles';
import { useTranslation } from 'react-i18next';

type Props = {
    accountId: string;
};

type SortToggle = {
    sortField: TransactionSortFields;
    sortOrder: SortOrder;
};

const AccountTransactions: React.FC<Props> = (props) => {
    const { accountId } = props;
    const { t } = useTranslation('common');
    const [transactionsData, setTransactionsData] = useState<Transaction[]>([]);
    const [totalRecords, setTotalRecords] = useState<number>(0);
    const [pageSize] = useState<number>(10);
    const [dataAvailable, setDataAvailable] = useState<boolean>(false);
    const [fromDate, setFromDate] = useState(
        (): Date => {
            const date = new Date();
            date.setDate(date.getDate() - parseInt((window as any).REACT_APP_MONTH_OFFSET!.trim()));
            return date;
        }
    );
    const [toDate, setToDate] = useState<Date>(new Date());
    const [sortToggle, setSortToggle] = useState<SortToggle>({ sortField: 'txId', sortOrder: 'ASC' });
    const history = useHistory<RedirectWithTransaction>();
    const location = useLocation();
    const infoDisplayClasses = useInfoDisplayStyles();

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

            if (fromDate && toDate) {
                transactionsResponse = await getAccountTransactions(
                    accountId,
                    pageIndex,
                    pageSize,
                    '',
                    sortToggle.sortField,
                    sortToggle.sortOrder,
                    fromDate.toISOString(),
                    toDate.toISOString()
                );
            } else {
                transactionsResponse = await getAccountTransactions(
                    accountId,
                    pageIndex,
                    pageSize,
                    '',
                    sortToggle.sortField,
                    sortToggle.sortOrder
                );
            }

            if (transactionsResponse.error) {
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
        [accountId, t, fromDate, toDate, sortToggle.sortField, sortToggle.sortOrder]
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

    const onRowClickHandler = (dataIndex: number) => {
        history.push({
            pathname: TRANSACTIONVIEW,
            state: { transaction: transactionsData[dataIndex], from: location.pathname },
        });
    };

    return (
        <Fade in={true}>
            <Paper elevation={3} className={infoDisplayClasses.infoDisplay}>
                <div className={infoDisplayClasses.tableContainer}>
                    {!dataAvailable && <Alert severity="warning">{t('common:warning.noTransactions')}</Alert>}
                    {true && (
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
            </Paper>
        </Fade>
    );
};

export default AccountTransactions;
