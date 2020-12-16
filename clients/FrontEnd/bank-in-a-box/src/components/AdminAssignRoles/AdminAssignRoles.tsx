import { ApiUsersSortField, getUsers } from '../../api/userManageApi';
import { AssignRoleType, RedirectWithUser, SortOrder, User } from '../../store/types';
import { Fade, InputLabel, MenuItem, Select, TextField } from '@material-ui/core';
import React, { useCallback, useState } from 'react';
import { useHistory, useLocation } from 'react-router-dom';

import { Alert } from '../Notifications/Notifcations';
import { DynamicTable } from '../DynamicTable/DynamicTable';
import Header from '../Header/Header';
import { ResolvedPromise } from '../../api/resolvePromise';
import { UPDATEUSER } from '../../constants/Routes';
import { useTableWrapperStyles } from '../MaterialStyles/TabbleWrapper';
import { useTranslation } from 'react-i18next';

type SortToggle = {
    sortField: ApiUsersSortField;
    sortOrder: SortOrder;
};

const AdminAssignRoles: React.FC = () => {
    const { t } = useTranslation(['common']);
    const history = useHistory<RedirectWithUser>();
    const location = useLocation<RedirectWithUser>();
    const [totalRecords, setTotalRecords] = useState<number>(0);
    const [sortToggle, setSortToggle] = useState<SortToggle>({ sortField: 'username', sortOrder: 'ASC' });
    const [error, setError] = useState<string>();
    const [dataAvailable, setDataAvailable] = useState<boolean>(false);
    const [users, setUsers] = useState<User[]>([]);
    const [role, setRole] = useState<AssignRoleType>('ADMIN');
    const [searchTerm, setSearchTerm] = useState<string>('');
    const [searchFieldInputValue, setSearchFieldInputValue] = useState<string>('');
    const pageSize = 10;
    const tableWrapperClasses = useTableWrapperStyles();

    const headers = [
        {
            Header: t('common:commonText.username'),
            accessor: 'username',
        },
        {
            Header: t('common:commonText.emailAddress'),
            accessor: 'email',
        },
        {
            Header: t('common:roles.userRoles'),
            accessor: 'roles',
        },
    ];

    const fetchData = useCallback(
        async (pageIndex: number, pageSize: number) => {
            let usersResponse: ResolvedPromise = await getUsers(
                role,
                pageIndex,
                pageSize,
                searchTerm,
                sortToggle.sortField,
                sortToggle.sortOrder
            );

            if (usersResponse.error) {
                setError(usersResponse.error);
            } else {
                let usersData: User[] = usersResponse.data.data.result as User[];

                setTotalRecords(usersResponse.data.data.totalResults);

                if (usersData.length <= 0) {
                    setDataAvailable(false);
                } else {
                    setDataAvailable(true);
                }
                setUsers(usersData);
            }
        },
        [searchTerm, sortToggle.sortField, sortToggle.sortOrder, role]
    );

    const handleSearchInput = (event: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) => {
        event.preventDefault();
        const value: string = event.currentTarget.value;
        if (value.length >= 3) {
            setSearchTerm(value);
            setSortToggle({ sortField: 'username', sortOrder: 'ASC' });
        } else {
            setSearchTerm('');
        }
        setSearchFieldInputValue(value);
    };

    const onRowClickHandler = (dataIndex: number) => {
        history.push({
            pathname: UPDATEUSER,
            state: { user: users[dataIndex], from: location.pathname },
        });
    };

    const handleRoleSelect = (event: React.ChangeEvent<{ value: unknown }>) => {
        event.preventDefault();
        setRole(event.target.value as AssignRoleType);
    };

    const onSortHandler = (sortOption: string) => {
        let derivedSortOption: ApiUsersSortField = 'username';
        derivedSortOption = sortOption as ApiUsersSortField;
        if (sortToggle.sortField === derivedSortOption!) {
            setSortToggle({
                sortField: sortToggle.sortField,
                sortOrder: sortToggle.sortOrder === 'ASC' ? 'DESC' : 'ASC',
            });
        } else {
            setSortToggle({ sortField: derivedSortOption, sortOrder: 'ASC' });
        }
    };

    if (error) {
        return <Header>{t('common:error.fetchingData')}</Header>;
    }

    return (
        <div className="loginPageWrapper">
            <Header>{t('common:pageTitle.userManagement')}</Header>

            <Fade in={true}>
                <div className={tableWrapperClasses.mainTableWrapper}>
                    <div className={tableWrapperClasses.toolBar}>
                        <div className={`${tableWrapperClasses.toolBarItem} ${tableWrapperClasses.searchFieldItem}`}>
                            <TextField
                                className="searchField"
                                label={t('common:commonText.userSearch')}
                                onChange={(event) => {
                                    handleSearchInput(event);
                                }}
                                value={searchFieldInputValue}
                                helperText={t('common:inputHelpText.userSearch')}
                                variant="outlined"
                            />
                        </div>

                        <div className={`${tableWrapperClasses.toolBarItem}`}>
                            <InputLabel className={tableWrapperClasses.label} shrink id="userRole-label">
                                {t('common:account.selectAccountType')}
                            </InputLabel>
                            <Select
                                className={`  ${tableWrapperClasses.selectDropdown} roleTypeSelect`}
                                labelId="userRole-label"
                                onChange={(event) => handleRoleSelect(event)}
                                value={role}
                                variant="outlined"
                            >
                                <MenuItem value="ADMIN">{t('common:roles.admin')}</MenuItem>
                                <MenuItem value="CUSTOMER">{t('common:roles.customer')}</MenuItem>
                                <MenuItem value="GUEST">{t('common:roles.guest')}</MenuItem>
                            </Select>
                        </div>
                    </div>

                    {!dataAvailable && (
                        <Alert severity="info">
                            {searchFieldInputValue.length >= 3
                                ? t('common:warning.noSearchResult')
                                : t('common:warning.noDataAvailable')}
                        </Alert>
                    )}

                    <DynamicTable<User>
                        className={'--userTable'}
                        columns={headers}
                        data={users}
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

export default AdminAssignRoles;
