import { RecurringPayment } from '../store/types';
import { TOptions } from 'i18next';

//Expects a a translate function (t) from useTranslation() hook that is used for translation in the calling component
export const TranslateRecurringPayments = (
    transaction: RecurringPayment[],
    t: (
        translate: string,
        options?:
            | string
            | TOptions<{
                  amount: string;
              }>
    ) => string
): RecurringPayment[] => {
    return transaction.map((transaction) => {
        return TransalateRecurringPayment(transaction, t);
    });
};

//This function will translate the period  of the recurring payment passed in
//Expects a a translate function (t) from useTranslation() hook that is used for translation in the calling component
export const TransalateRecurringPayment = (
    transaction: RecurringPayment,
    t: (
        translate: string,
        options?:
            | string
            | TOptions<{
                  amount: string;
              }>
    ) => string
): RecurringPayment => {
    const amountPeriodUnit = transaction.period.split(' ');
    const amount = amountPeriodUnit[0].trim();
    transaction.period = t('common:recurringPayment.periodInDays', { amount: amount });
    return transaction;
};
