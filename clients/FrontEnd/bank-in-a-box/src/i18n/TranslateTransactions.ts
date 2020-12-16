import { Transaction } from '../store/types';

//Expects a a translate function (t) from useTranslation() hook that is used for translation in the calling component
export const TranslateTransactions = (transaction: Transaction[], t: (translate: string) => string): Transaction[] => {
    return transaction.map((transaction) => {
        return TranslateTransaction(transaction, t);
    });
};

//This function will translate the type of a transaction passed in
//Expects a a translate function (t) from useTranslation() hook that is used for translation in the calling component
export const TranslateTransaction = (transaction: Transaction, t: (translate: string) => string): Transaction => {
    const transactionType = transaction.txType;
    if (transactionType === 'DEPOSIT') {
        transaction.txType = t('common:transactionType.deposit');
    } else if (transactionType === 'TRANSFER') {
        transaction.txType = t('common:transactionType.transfer');
    } else if (transactionType === 'WITHDRAWAL') {
        transaction.txType = t('common:transactionType.withdrawal');
    }
    return transaction;
};
