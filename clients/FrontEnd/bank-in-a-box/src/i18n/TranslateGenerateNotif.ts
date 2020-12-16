import { TOptions } from 'i18next';

type ChangedAccountProperties = {
    name: string;
    value: string;
};

export const generatei18nSocketNotifcation = (
    messageType: string,
    accountId: string,
    propertyName: string | null,
    newPropertyValue: string | null,
    t: (
        translate: string,
        options?:
            | string
            | TOptions<{
                  [key: string]: string;
              }>
    ) => string
): string => {
    let socketNotif = '';

    if (messageType === 'ACCOUNT_CREATED') {
        socketNotif = t('common:socketNotification.accountCreated', { accountId: accountId });
    } else if (messageType === 'PROPERTY_CHANGED') {
        let properties = translateChangedProperties(propertyName!, newPropertyValue!, t);
        socketNotif = t('common:socketNotification.propertyChanged', {
            accountId: accountId,
            propertyName: properties.name,
            propertyValue: properties.value,
        });
    }

    return socketNotif;
};

export const translateChangedProperties = (
    propertyName: string,
    newPropertyValue: string,
    t: (translate: string) => string
): ChangedAccountProperties => {
    let changedProps: ChangedAccountProperties = { name: '', value: '' };

    switch (propertyName) {
        case 'Status': {
            changedProps.name = t('common:commonText.status');
            break;
        }
        case 'TransferDailyLimit': {
            changedProps.name = t('common:account.transferDailyLimit');
            newPropertyValue === '0'
                ? (changedProps.value = t('common:commonText.unlimitedAmount'))
                : (changedProps.value = (parseInt(newPropertyValue) / 100).toString());

            return changedProps;
        }
        case 'WithdrawalDailyLimit': {
            changedProps.name = t('common:account.withdrawalDailyLimit');
            newPropertyValue === '0'
                ? (changedProps.value = t('common:commonText.unlimitedAmount'))
                : (changedProps.value = (parseInt(newPropertyValue) / 100).toString());

            return changedProps;
        }
        case 'ApprovedOverdraftLimit': {
            changedProps.name = t('common:account.overdraftLimit');
            changedProps.value = (parseInt(newPropertyValue) / 100).toString();
            return changedProps;
        }
        case 'Balance': {
            changedProps.name = t('common:commonText.balance');
            changedProps.value = newPropertyValue;
            return changedProps;
        }
    }

    switch (newPropertyValue) {
        case 'ACTIVE': {
            changedProps.value = t('common:accountStatus.active');
            break;
        }
        case 'SUSPENDED': {
            changedProps.value = t('common:accountStatus.suspended');
            break;
        }
        case 'PENDING': {
            changedProps.value = t('common:accountStatus.pending');
            break;
        }
        default: {
            changedProps.value = newPropertyValue;
        }
    }

    return changedProps;
};
