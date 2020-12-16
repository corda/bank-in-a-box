import * as React from 'react';

import { IconButton } from '@material-ui/core';
import { Close as IconClose } from '@material-ui/icons';
import { useSnackbar } from 'notistack';

type Props = {
    snackKey: React.ReactText;
};

const SnackbarCloseButton: React.FC<Props> = ({ snackKey }) => {
    const { closeSnackbar } = useSnackbar();

    return (
        <IconButton id="dismissNotificationButton" onClick={() => closeSnackbar(snackKey)}>
            <IconClose />
        </IconButton>
    );
};

export default SnackbarCloseButton;
