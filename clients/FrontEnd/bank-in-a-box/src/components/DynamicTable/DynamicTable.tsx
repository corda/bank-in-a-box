import './DynamicTable.scss';

import { Fade, MenuItem, Select, Theme, createStyles, makeStyles } from '@material-ui/core';
import React, { useEffect, useState } from 'react';
import { usePagination, useSortBy, useTable } from 'react-table';

import Pagination from '@material-ui/lab/Pagination/Pagination';
import { titleCase } from '../../utils/Utils';
import { useTranslation } from 'react-i18next';

type HeaderColumns = {
    Header?: string;
    accessor: string;
    className?: string;
};

type TableProps<T> = {
    className?: string;
    columns: HeaderColumns[];
    data: T[];
    rowsPerPage: number;
    totalRecords: number;
    /*
    getRowProps should be implemented in the componenet
    used for getting the row index 
    usefull for querying parent data lists
    */
    getRowProps: (row: number) => void;
    /*
    fetchData should be implemented in parent component
    handles fetching the data based of the page index and page size
    */
    fetchData: (pageIndex: number, pageSize: number) => void;
    /*
    sortBy should be implemented in parent component
    reads the column (header) id (accessor) as a string
    should set the relevant sort data for search query based on this
    */
    sortBy: (sortField: string) => void;
};

const useSortBySelect = makeStyles((theme: Theme) =>
    createStyles({
        select: {
            display: 'block',
            marginLeft: 'auto',
            marginRight: 'auto',
            maxWidth: 300,
            [theme.breakpoints.up('sm')]: {
                marginLeft: 0,
            },
        },
    })
);

export const DynamicTable: <T extends object>(props: TableProps<T>) => React.ReactElement<TableProps<T>> = (props) => {
    //export function DynamicTable<T extends object>(props: TableProps<T>): ReactElement {
    const { className, columns, data, rowsPerPage, totalRecords, getRowProps, fetchData, sortBy } = props;
    const numOfPages = rowsPerPage ? Math.ceil(totalRecords / rowsPerPage) : 1;

    //Used for getting each page individually from the api
    //Default pageIndex which state is derived from useTable will be used for when we are using default pagination
    const [customPageIndex, setCustomPageIndex] = useState(0);
    const [sortByDropDownValue, setSortByDropDownValue] = useState(columns[0].accessor);
    const selectStyles = useSortBySelect();
    const { t } = useTranslation('common');

    const { getTableProps, getTableBodyProps, rows, headerGroups, prepareRow, page, setPageSize } = useTable(
        {
            ...props,
            columns,
            data,
            intialState: {
                pageIndex: 0,
                pageSize: rowsPerPage || totalRecords,
            },
            autoResetPage: false,
        },
        useSortBy,
        usePagination
    );

    useEffect(() => setPageSize(rowsPerPage || totalRecords), [rowsPerPage, totalRecords, setPageSize]);

    useEffect(() => {
        fetchData(customPageIndex + 1, rowsPerPage);
    }, [customPageIndex, rowsPerPage, fetchData]);

    const sortTable = (colName) => ({
        onClick: () => {
            setCustomPageIndex(0);
            sortBy(colName);
        },
        onKeyPress: () => {
            setCustomPageIndex(0);
            sortBy(colName);
        },
    });

    const sortTableWithDropDown = (event) => {
        const val = event.target.value;
        if (val === 0) {
            setCustomPageIndex(0);
            sortBy(sortByDropDownValue);
        } else {
            setCustomPageIndex(0);
            sortBy(val as string);
            setSortByDropDownValue(val as string);
        }
    };

    const selectRow = (index) => ({
        onClick: () => {
            getRowProps(index);
        },
        onKeyPress: () => {
            getRowProps(index);
        },
    });

    //Reset to page 1 when the total amount of records changes
    useEffect(() => {
        setCustomPageIndex(0);
    }, [totalRecords]);

    return (
        <div className="dynamicTableWrapper">
            <div className={`sortBySelectWrapper`}>
                <Select
                    className={selectStyles.select}
                    labelId="sort-label"
                    onClick={(event) => (event.target ? sortTableWithDropDown(event) : null)}
                    name="FromAccountSelect"
                    value={sortByDropDownValue ? sortByDropDownValue : columns[0].accessor}
                    variant="outlined"
                >
                    {columns.map((col, index) => {
                        let accessor = col.accessor;
                        if (accessor.includes('.')) {
                            var split = accessor.split('.');
                            accessor = split[split.length - 1];
                        }
                        accessor = titleCase(accessor);
                        return (
                            <MenuItem key={index} value={col.accessor}>
                                {`${t('common:inputHelpText.sortBy')} ${accessor}`}
                            </MenuItem>
                        );
                    })}
                </Select>
            </div>
            <div className="tableWrapper">
                <Fade in={true}>
                    <table className={`dynamicTable${className}`} {...getTableProps()}>
                        <thead>
                            {headerGroups.map((headerGroup) => (
                                <tr {...headerGroup.getHeaderGroupProps()}>
                                    {headerGroup.headers.map((column) => (
                                        <th
                                            tabIndex="0"
                                            className={'tableHeader'}
                                            {...column.getHeaderProps([
                                                {
                                                    className: column.className || '',
                                                    style: column.style,
                                                },
                                                column.getSortByToggleProps(sortTable(column.id)),
                                            ])}
                                        >
                                            {column.render('Header')}
                                        </th>
                                    ))}
                                </tr>
                            ))}
                        </thead>
                        <tbody {...getTableBodyProps()}>
                            {rowsPerPage
                                ? page.map((row, i) => {
                                      prepareRow(row);
                                      return (
                                          <tr tabIndex="0" {...row.getRowProps(selectRow(row.index))}>
                                              {row.cells.map((cell, index) => {
                                                  return (
                                                      <td
                                                          {...cell.getCellProps([
                                                              {
                                                                  className: cell.column.className || '',
                                                                  style: cell.column.style,
                                                              },
                                                          ])}
                                                      >
                                                          {cell.render('Cell')}
                                                      </td>
                                                  );
                                              })}
                                          </tr>
                                      );
                                  })
                                : rows.map((row, i) => {
                                      prepareRow(row);
                                      return (
                                          <tr {...row.getRowProps(getRowProps(row.index))}>
                                              {row.cells.map((cell) => {
                                                  return (
                                                      <td
                                                          {...cell.getCellProps([
                                                              {
                                                                  className: cell.column.className || '',
                                                                  style: cell.column.style,
                                                              },
                                                          ])}
                                                      >
                                                          {cell.render('Cell')}
                                                      </td>
                                                  );
                                              })}
                                          </tr>
                                      );
                                  })}
                        </tbody>
                    </table>
                </Fade>
            </div>
            <div className="pagination">
                {rowsPerPage && rowsPerPage < totalRecords ? (
                    <Pagination
                        className="paginationComponent self-center"
                        defaultPage={1}
                        siblingCount={1}
                        boundaryCount={1}
                        color="primary"
                        size="large"
                        count={numOfPages}
                        page={customPageIndex + 1}
                        onChange={(event, page) => setCustomPageIndex(page - 1)}
                    />
                ) : null}
            </div>
        </div>
    );
};
