import {
    Account,
    AccountPaginated,
    Attachment,
    BLANKFIELD,
    CurrencyType,
    Customer,
    RecurringPayment,
    Transaction,
} from '../store/types';

export const currencyTypes: CurrencyType[] = ['AUD', 'CAD', 'CHF', 'EUR', 'GBP', 'JPY', 'NZD', 'USD'];

export const MINPHONENUMBERLENGHT = 10;

export const MINPOSTCODELENGHT = 4;

export const validateContactNumber = (text: string) => {
    if ((text.includes('+') && text.indexOf('+') > 0) || text.split('+').length - 1 > 1) {
        return false;
    }
    var numbers = /[0-9 +]+$/;
    if (text.match(numbers) && text.length < 20) {
        return true;
    } else {
        return false;
    }
};

export const validateStringIsNumber = (text: string) => {
    var numbers = /^[0-9]+$/;
    if (text.match(numbers)) {
        return true;
    } else {
        return false;
    }
};

export const mapCustomersResponse = (responsData): Customer[] => {
    return responsData.map(
        (customer: Customer): Customer => {
            return mapCustomerData(customer);
        }
    );
};

export const mapCustomerData = (dataObject): Customer => {
    return {
        contactNumber: dataObject.contactNumber,
        createdOn: new Date(dataObject.createdOn).toString(),
        customerId: dataObject.customerId,
        customerName: dataObject.customerName,
        emailAddress: dataObject.emailAddress,
        modifiedOn: new Date(dataObject.modifiedOn).toString(),
        postCode: dataObject.postCode,
        attachments: mapAttachments(dataObject.attachments),
    };
};

const mapAttachments = (attachmentsObject): Attachment[] => {
    let attachments: Attachment[] = [];
    if (attachmentsObject.length > 0) {
        attachmentsObject.map((attachment) => {
            return attachments.push({
                attachmentHash: attachment.attachmentHash,
                customer: mapCustomerData(attachment.customer),
                id: attachment.id,
                name: attachment.name,
            });
        });
    }
    return attachments;
};

export const mapAccounts = (accountsResults): AccountPaginated[] => {
    return accountsResults.map(
        (result): AccountPaginated => {
            return mapAccountFromPaginated(result);
        }
    );
};

export const mapAccountFromPaginated = (accountData): AccountPaginated => {
    const mappedAccount: AccountPaginated = {
        account: mapAccount(accountData.first),
        customer: mapCustomerData(accountData.second),
    };
    return mappedAccount;
};

export const mapAccount = (account): Account => {
    const mappedAccount = account as Account;

    const balCur = mappedAccount.accountData.balance.split('of');
    const balance = balCur[0];
    const currency = balCur[1];
    mappedAccount.accountData.currency = currency.trim();
    mappedAccount.accountData.balance = balance.trim();
    mappedAccount.type = mappedAccount.type.toUpperCase();
    mappedAccount.accountData.txDate = new Date(mappedAccount.accountData.txDate).toString();
    //The data for daily limits is set in cents so normalize it to match other values
    if (mappedAccount.transferDailyLimit) {
        mappedAccount.transferDailyLimit = mappedAccount.transferDailyLimit / 100;
    }
    if (mappedAccount.withdrawalDailyLimit) {
        mappedAccount.withdrawalDailyLimit = mappedAccount.withdrawalDailyLimit / 100;
    }

    if (mappedAccount.approvedOverdraftLimit) {
        mappedAccount.approvedOverdraftLimit = mappedAccount.approvedOverdraftLimit / 100;
    }

    if (mappedAccount.overdraftBalance) {
        mappedAccount.overdraftBalance = mappedAccount.overdraftBalance / 100;
    }

    return mappedAccount;
};

export const mapTransactions = (transactions: Transaction[]): Transaction[] => {
    transactions.forEach((transaction) => {
        const amount = parseFloat(transaction.amount);
        transaction.amount = (amount / 100).toString();
        transaction.txDate = new Date(transaction.txDate).toString();

        if (transaction.accountFrom === null) {
            transaction.accountFrom = BLANKFIELD;
        }

        if (transaction.accountTo === null) {
            transaction.accountTo = BLANKFIELD;
        }
    });

    return transactions;
};

export const mapRecurringPayments = (recurringPayments: RecurringPayment[]): RecurringPayment[] => {
    return recurringPayments.map((rp) => {
        return mapRecurringPayment(rp);
    });
};

export const mapRecurringPayment = (recurringPayment: RecurringPayment): RecurringPayment => {
    recurringPayment.amount = (parseFloat(recurringPayment.amount) / 100).toString();
    recurringPayment.txDate = new Date(recurringPayment.txDate).toString();
    return recurringPayment;
};

export const LSACCOUNTTABKEY = 'SELECTEDACCOUNTTAB';

export const filterForCurrentAccounts = (data: Account[]): Account[] => {
    return data.filter((acc) => acc.type === 'CURRENT' || acc.type === 'OVERDRAFT');
};

export const filterForActiveCurrentAccounts = (data: Account[]): Account[] => {
    return data.filter(
        (acc) => (acc.type === 'CURRENT' || acc.type === 'OVERDRAFT') && acc.accountData.status === 'ACTIVE'
    );
};

export const filterForActiveAccounts = (data: Account[]): Account[] => {
    return data.filter((acc) => acc.accountData.status === 'ACTIVE');
};

export const ZeroOrEmptyString = (val: string): boolean => {
    if (val === '0' || val.length <= 0) {
        return true;
    } else {
        return false;
    }
};

export const titleCase = (str: string) => {
    let splitStr = str.toLowerCase().split(' ');
    for (let i = 0; i < splitStr.length; i++) {
        splitStr[i] = splitStr[i].charAt(0).toUpperCase() + splitStr[i].substring(1);
    }
    return splitStr.join(' ');
};

export const validateEmail = (email: string) => {
    if (
        /^(([^<>()[\]\\.,;:\s@"]+(\.[^<>()[\]\\.,;:\s@"]+)*)|(".+"))@((\[[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}])|(([a-zA-Z\-0-9]+\.)+[a-zA-Z]{2,}))$/.test(
            email
        ) &&
        email.length <= 254
    ) {
        return true;
    }
    return false;
};

export const getMaxFileSizeFromErrorBytes = (error: string) => {
    if (error.includes('request was rejected')) {
        return error.split('maximum (')[1].trim().split(')')[0];
    } else {
        return error.split('of')[1].trim().split(' ')[0];
    }
};
