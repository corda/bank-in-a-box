export type Customer = {
    contactNumber: string;
    createdOn: string;
    customerId: string;
    customerName: string;
    emailAddress: string;
    modifiedOn: string;
    postCode: string;
    attachments: Attachment[];
};

export type Attachment = {
    attachmentHash: string;
    name: string;
    customer?: Customer;
    id: number;
};

export type CachedFile = {
    idHash: string;
    file: File;
};

export type Transaction = {
    txId: string;
    accountFrom: string | null;
    accountTo: string | null;
    currency: string;
    txDate: string;
    txType: string;
    amount: string;
};

export const EmptyTransaction: Transaction = {
    txId: '',
    accountFrom: null,
    accountTo: null,
    currency: '',
    txDate: '',
    txType: '',
    amount: '',
};

export type AccountPaginated = {
    account: Account;
    customer: Customer;
};

export type Account = {
    accountData: {
        accountId: string;
        accountInfo: {
            name: string;
            host: string;
            identifier: {
                externalId: string | null;
                id: string;
            };
        };
        customerId: string | null;
        balance: string;
        currency: string;
        txDate: string;
        status: string;
    };
    linearId: {
        externalId: string | null;
        id: string;
    };
    period: string | null;
    savingsEndDate: string | null;
    participants: string;
    type: string;
    transferDailyLimit: number | null;
    withdrawalDailyLimit: number | null;
    approvedOverdraftLimit: number | null;
    overdraftBalance: number | null;
};

export const EmptyCustomerData: Customer = {
    contactNumber: '',
    createdOn: '',
    customerId: '',
    customerName: '',
    emailAddress: '',
    modifiedOn: '',
    postCode: '',
    attachments: [],
};

export const EmptyAccountData: Account = {
    accountData: {
        accountId: '',
        accountInfo: {
            name: '',
            host: '',
            identifier: {
                externalId: null,
                id: '',
            },
        },
        customerId: '',
        balance: '',
        currency: '',
        txDate: '',
        status: '',
    },
    withdrawalDailyLimit: null,
    transferDailyLimit: null,
    type: '',
    period: null,
    savingsEndDate: null,
    approvedOverdraftLimit: null,
    overdraftBalance: null,
    participants: '',
    linearId: {
        externalId: null,
        id: '',
    },
};

export type RecurringPayment = {
    accountFrom: string;
    accountTo: string;
    amount: string;
    dateStart: string;
    period: string;
    iterationNum: number;
    recurringPaymentId: string;
    error: string;
    logId: string;
    txDate: string;
};

export const EmptyRecurringPayment: RecurringPayment = {
    accountFrom: '',
    accountTo: '',
    amount: '',
    dateStart: '',
    period: '',
    iterationNum: 0,
    recurringPaymentId: '',
    error: '',
    logId: '',
    txDate: '',
};

export type RedirectWithRecurringPayment = {
    recurringPayment: RecurringPayment;
    from: string;
};

export type RedirectWithCustomer = {
    customer: Customer;
    from: string;
};

export type RedirectWithAccount = {
    account: Account;
    from: string;
};

export type RedirectWithTransaction = {
    transaction: Transaction;
    from: string;
};

export type RedirectWithUser = {
    user: User;
    from: string;
};

export enum UserType {
    ADMIN = 'ADMIN',
    CUSTOMER = 'CUSTOMER',
    GUEST = 'GUEST',
    NOTLOGGEDIN = 'NOTLOGGEDIN',
}

export type User = {
    username: string;
    email: string;
    roles: string;
};

export type AssignRoleType = 'ADMIN' | 'CUSTOMER' | 'GUEST';

export const BLANKFIELD = '---------';

export type SortOrder = 'ASC' | 'DESC';

export type AccountType = 'CURRENT' | 'SAVINGS' | 'OVERDRAFT' | 'LOAN';

export type CurrencyType = 'AUD' | 'CAD' | 'CHF' | 'EUR' | 'GBP' | 'JPY' | 'NZD' | 'USD';

export type AccountStatus = 'ACTIVE' | 'PENDING' | 'SUSPENDED';
