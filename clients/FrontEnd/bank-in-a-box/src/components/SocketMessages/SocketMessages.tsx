import React, { useEffect } from 'react';

import { REALTIMENOTIFSERVICE } from '../../constants/SessionStorageKeys';
import { generatei18nSocketNotifcation } from '../../i18n/TranslateGenerateNotif';
import { useAuthProvider } from '../../store/AuthenticationContext';
import { useSnackbar } from 'notistack';
import { useTranslation } from 'react-i18next';

const SocketMessages: React.FC = () => {
    const { enqueueSnackbar } = useSnackbar();
    const authUser = useAuthProvider();
    const { t } = useTranslation('common');

    useEffect(() => {
        const socket = new WebSocket(`ws://${(window as any).REACT_APP_APIHOST!.trim()}ws`);

        socket.onopen = function (event) {
            socket.send(`${authUser?.user?.accessToken}`);
            let connectedToService = sessionStorage.getItem(REALTIMENOTIFSERVICE);
            if (!(connectedToService === 'CONNECTED')) {
                enqueueSnackbar(t('common:socketNotification.realtimeServiceConnected'), {
                    variant: 'info',
                });
                sessionStorage.setItem(REALTIMENOTIFSERVICE, 'CONNECTED');
            }
        };

        socket.onmessage = function (event) {
            let data = JSON.parse(event.data);
            let userMessage = generatei18nSocketNotifcation(
                data.messageType,
                data.accountId,
                data.propertyName,
                data.newPropertyValue,
                t
            );
            enqueueSnackbar(userMessage, {
                variant: 'info',
                autoHideDuration: 10000,
            });
        };
        // eslint-disable-next-line
    }, [enqueueSnackbar, t]);

    return null;
};

export default SocketMessages;
