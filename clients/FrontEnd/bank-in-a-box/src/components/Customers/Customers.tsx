import * as API from '../../api/customerApi';

import { Button, Fade, TextField } from '@material-ui/core';
import { CREATECUSTOMER, UPDATECUSTOMER } from '../../constants/Routes';
import { Customer, RedirectWithCustomer, SortOrder } from '../../store/types';
import React, { useCallback, useEffect, useState } from 'react';
import { useHistory, useLocation } from 'react-router-dom';

import { Alert } from '../Notifications/Notifcations';
import { DynamicTable } from '../DynamicTable/DynamicTable';
import Header from '../Header/Header';
import { ResolvedPromise } from '../../api/resolvePromise';
import { mapCustomersResponse } from '../../utils/Utils';
import { useTableWrapperStyles } from '../MaterialStyles/TabbleWrapper';
import { useTranslation } from 'react-i18next';

type SortToggle = {
    sortField: API.CustomerSortFields;
    sortOrder: SortOrder;
};

const Customers: React.FC = () => {
    const [customersData, setCustomersData] = useState<Customer[]>([]);
    const [searchFieldInputValue, setSearchFieldInputValue] = useState<string>('');
    const [searchTerm, setSearchTerm] = useState<string>('');
    const [totalRecords, setTotalRecords] = useState<number>(0);
    const [sortToggle, setSortToggle] = useState<SortToggle>({ sortField: 'customerName', sortOrder: 'ASC' });
    const [error, setError] = useState<string>();
    const [dataAvailable, setDataAvailable] = useState<boolean>(false);
    const pageSize = 10;
    const location = useLocation();
    const history = useHistory<RedirectWithCustomer>();
    const { t } = useTranslation('common');
    const tableWrapperClasses = useTableWrapperStyles();

    const headers = [
        {
            Header: t('common:customer.customerId'),
            accessor: 'customerId',
        },
        {
            Header: t('common:commonText.customerName'),
            accessor: 'customerName',
        },
        {
            Header: t('common:commonText.emailAddress'),
            accessor: 'emailAddress',
        },
        {
            Header: t('common:commonText.contactNumber'),
            accessor: 'contactNumber',
        },
    ];

    const fetchData = useCallback(
        async (pageIndex: number, pageSize: number) => {
            let customers: ResolvedPromise = await API.getCustomerPaginatedWithSort(
                pageIndex,
                pageSize,
                sortToggle.sortField,
                sortToggle.sortOrder,
                searchTerm
            );

            if (customers.error) {
                setError(customers.error);
            } else {
                let customersData: Customer[] = mapCustomersResponse(customers.data.data.result);

                setTotalRecords(customers.data.data.totalResults);

                if (customersData.length <= 0) {
                    setDataAvailable(false);
                } else {
                    setDataAvailable(true);
                }
                setCustomersData(customersData);
            }
        },
        [searchTerm, sortToggle.sortField, sortToggle.sortOrder]
    );

    const handleSearchInput = (event: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) => {
        event.preventDefault();
        const value: string = event.currentTarget.value;
        if (value.length >= 3) {
            setSearchTerm(value);
            setSortToggle({ sortField: 'customerName', sortOrder: 'ASC' });
        } else {
            setSearchTerm('');
        }
        setSearchFieldInputValue(value);
    };

    const onRowClickHandler = (dataIndex: number) => {
        history.push({
            pathname: UPDATECUSTOMER,
            state: { customer: customersData[dataIndex], from: location.pathname },
        });
    };

    const onSortHandler = (sortOption: string) => {
        let derivedSortOption: API.CustomerSortFields = 'customerName';

        if (sortOption === 'customerName' || sortOption === 'emailAddress' || sortOption === 'contactNumber') {
            derivedSortOption = sortOption;
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

    useEffect(() => {
        window.scroll(0, 0);
    }, []);

    if (error) {
        return <Header>{t('common:error.fetchingData')}</Header>;
    }

    return (
        <div className="customersWrapper">
            <Header>{t('common:pageTitle.customers')}</Header>
            <Fade in={true}>
                <div className={tableWrapperClasses.mainTableWrapper}>
                    <div className={tableWrapperClasses.toolBar}>
                        <div className={`${tableWrapperClasses.toolBarItem} ${tableWrapperClasses.searchFieldItem}`}>
                            <TextField
                                label={t('common:commonText.customersSearch')}
                                className="searchField"
                                onChange={(event) => {
                                    handleSearchInput(event);
                                }}
                                value={searchFieldInputValue}
                                helperText={t('common:inputHelpText.tableSearch')}
                                variant="outlined"
                            />
                        </div>

                        <div
                            className={`${tableWrapperClasses.toolBarItem} ${tableWrapperClasses.createNewButtonItem} createNewButtonItem`}
                        >
                            <Button
                                className={tableWrapperClasses.createNewButton}
                                variant="contained"
                                href={CREATECUSTOMER}
                                color="primary"
                            >
                                {t('common:button.createNew')}
                            </Button>
                        </div>
                    </div>

                    {!dataAvailable && (
                        <Alert severity="info">
                            {searchFieldInputValue.length >= 3
                                ? t('common:warning.noSearchResult')
                                : t('common:warning.noDataAvailable')}
                        </Alert>
                    )}

                    <DynamicTable<Customer>
                        className={'--customersTable'}
                        columns={headers}
                        data={customersData}
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

export default Customers;
