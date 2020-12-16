import { Account, AccountPaginated } from '../store/types';

//This function will translate the status and type of all accounts passed in
//Expects a a translate function (t) from useTranslation() hook that is used for translation in the calling component
export const TranslateAccounts = (
    accounts: AccountPaginated[],
    t: (translate: string) => string
): AccountPaginated[] => {
    return accounts.map((acc) => {
        return { account: TranslateAccount(acc.account, t), customer: acc.customer };
    });
};

//This function will translate the status and type of am account passed in
//Expects a a translate function (t) from useTranslation() hook that is used for translation in the calling component
export const TranslateAccount = (account: Account, t: (translate: string) => string): Account => {
    const accType = account.type;
    if (accType === 'CURRENT') {
        account.type = t('common:accountType.current');
    } else if (accType === 'SAVINGS') {
        account.type = t('common:accountType.savings');
    } else if (accType === 'OVERDRAFT') {
        account.type = t('common:accountType.overdraft');
    } else if (accType === 'LOAN') {
        account.type = t('common:accountType.loan');
    }

    const accStatus = account.accountData.status;
    if (accStatus === 'PENDING') {
        account.accountData.status = t('common:accountStatus.pending');
    } else if (accStatus === 'ACTIVE') {
        account.accountData.status = t('common:accountStatus.active');
    }
    return account;
};
