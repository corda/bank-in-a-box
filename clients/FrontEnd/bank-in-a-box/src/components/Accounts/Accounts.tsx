import { ACCOUNTVIEW, CREATEACCOUNT } from '../../constants/Routes';
import { AccountPaginated, RedirectWithAccount, SortOrder, UserType } from '../../store/types';
import {
    AccountSortFields,
    getAccountsPaginatedWithSort,
    getCustomersAccountsPaginatedWithSort,
} from '../../api/accountApi';
import { Button, Fade, TextField } from '@material-ui/core';
import { LSACCOUNTTABKEY, mapAccounts } from '../../utils/Utils';
import React, { useCallback, useEffect, useState } from 'react';
import { useHistory, useLocation } from 'react-router-dom';

import { Alert } from '../Notifications/Notifcations';
import { DynamicTable } from '../DynamicTable/DynamicTable';
import Header from '../Header/Header';
import { TranslateAccounts } from '../../i18n/TranslateAccounts';
import { useAuthProvider } from '../../store/AuthenticationContext';
import { useTableWrapperStyles } from '../MaterialStyles/TabbleWrapper';
import { useTranslation } from 'react-i18next';

type SortToggle = {
    sortField: AccountSortFields;
    sortOrder: SortOrder;
};

const Accounts: React.FC = () => {
    const [accountsData, setAccountsData] = useState<AccountPaginated[]>([]);
    const [dataAvailable, setDataAvailable] = useState<boolean>(false);
    const [totalRecords, setTotalRecords] = useState<number>(0);
    const [searchFieldInputValue, setSearchFieldInputValue] = useState<string>('');
    const [searchTerm, setSearchTerm] = useState<string>('');
    const [error, setError] = useState<string>('');
    const [sortToggle, setSortToggle] = useState<SortToggle>({ sortField: 'customerName', sortOrder: 'ASC' });
    const pageSize = 10;
    const history = useHistory<RedirectWithAccount>();
    const location = useLocation();
    const authContext = useAuthProvider();
    const { t } = useTranslation('common');
    const tableWrapperClasses = useTableWrapperStyles();

    const headers = [
        {
            Header: t('common:commonText:accountKey'),
            accessor: 'account.accountData.accountId',
        },
        {
            Header: t('common:commonText:customerName'),
            accessor: 'customer.customerName',
        },

        {
            Header: t('common:commonText:accountType'),
            accessor: 'account.type',
        },
        {
            Header: t('common:commonText:currency'),
            accessor: 'account.accountData.currency',
        },
        {
            Header: t('common:commonText:balance'),
            accessor: 'account.accountData.balance',
        },
        {
            Header: t('common:commonText:status'),
            accessor: 'account.accountData.status',
        },
        {
            Header: t('common:commonText:lastTxDate'),
            accessor: 'account.accountData.txDate',
        },
    ];

    const fetchData = useCallback(
        async (pageIndex: number, pageSize: number) => {
            let accounts;

            if (authContext?.user?.userType === UserType.ADMIN) {
                accounts = await getAccountsPaginatedWithSort(
                    pageIndex,
                    pageSize,
                    sortToggle.sortField,
                    sortToggle.sortOrder,
                    searchTerm
                );
            } else if (authContext?.user?.userType === UserType.CUSTOMER) {
                accounts = await getCustomersAccountsPaginatedWithSort(
                    authContext.user.userId,
                    pageIndex,
                    pageSize,
                    sortToggle.sortField,
                    sortToggle.sortOrder,
                    searchTerm
                );
            }

            if (accounts.error) {
                setError(accounts.error);
            } else {
                const accountsData: AccountPaginated[] = TranslateAccounts(mapAccounts(accounts.data.data.result), t);
                setTotalRecords(accounts.data.data.totalResults);
                setAccountsData(accountsData);
                if (accountsData.length <= 0) {
                    setDataAvailable(false);
                } else {
                    setDataAvailable(true);
                }
            }
        },
        [searchTerm, sortToggle.sortField, sortToggle.sortOrder, t, authContext]
    );

    const onSortHandler = (sortOption: string) => {
        //There is no sort options for these headers
        if (sortOption === 'account.currency') {
            return;
        }

        let derivedSortOption: AccountSortFields = 'account';
        //Not using string split to derive as not all api calls consistent with header names
        if (sortOption === 'account.accountData.accountId') {
            derivedSortOption = 'account';
        } else if (sortOption === 'customer.customerName') {
            derivedSortOption = 'customerName';
        } else if (sortOption === 'account.accountData.balance') {
            derivedSortOption = 'balance';
        } else if (sortOption === 'account.accountData.status') {
            derivedSortOption = 'status';
        } else if (sortOption === 'account.accountData.txDate') {
            derivedSortOption = 'txDate';
        } else {
            //Other cols not supported (type) (currency)
            return;
        }

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
            setSortToggle({ sortField: 'account', sortOrder: 'ASC' });
        } else {
            setSearchTerm('');
        }
        setSearchFieldInputValue(value);
    };

    const onRowClickHandler = (dataIndex: number) => {
        window.localStorage.setItem(LSACCOUNTTABKEY, '0');
        history.push({
            pathname: ACCOUNTVIEW,
            state: { account: accountsData[dataIndex].account, from: location.pathname },
        });
    };

    useEffect(() => {
        window.scroll(0, 0);
    }, []);

    if (error) {
        return <Header>{t('common:error.fetchingDataAccounts')}</Header>;
    }

    return (
        <div className="accountsWrapper">
            <Header>{t('common:pageTitle.accounts')}</Header>
            <Fade in={true}>
                <div className={tableWrapperClasses.mainTableWrapper}>
                    <div className={tableWrapperClasses.toolBar}>
                        <div className={`${tableWrapperClasses.toolBarItem} ${tableWrapperClasses.searchFieldItem}`}>
                            <TextField
                                className="searchField"
                                label={t('common:commonText.accountsSearch')}
                                onChange={(event) => {
                                    handleSearchInput(event);
                                }}
                                variant="outlined"
                                value={searchFieldInputValue}
                                helperText={t('common:inputHelpText.accountTableSearch')}
                            />
                        </div>
                        {authContext?.user?.userType === UserType.ADMIN && (
                            <div
                                className={`${tableWrapperClasses.toolBarItem} ${tableWrapperClasses.createNewButtonItem} createNewButtonItem`}
                            >
                                <Button
                                    className={tableWrapperClasses.createNewButton}
                                    variant="contained"
                                    color="primary"
                                    href={CREATEACCOUNT}
                                >
                                    {t('common:button.createNew')}
                                </Button>
                            </div>
                        )}
                    </div>

                    {!dataAvailable && (
                        <Alert severity="info">
                            {searchFieldInputValue.length >= 3
                                ? t('common:warning.noSearchResult')
                                : t('common:warning.noResources', { resource: t('common:resources.accounts') })}
                        </Alert>
                    )}

                    <DynamicTable<AccountPaginated>
                        className={'--accountsTable'}
                        columns={headers}
                        data={accountsData}
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

export default Accounts;
