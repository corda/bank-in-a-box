import { Button, Fade, TextField } from '@material-ui/core';
import { CUSTOMERS, UPDATECUSTOMER } from '../../constants/Routes';
import React, { useEffect, useState } from 'react';

import Header from '../Header/Header';
import { UserType } from '../../store/types';
import { useAuthProvider } from '../../store/AuthenticationContext';
import useEventListener from '@use-it/event-listener';
import { useFormStyles } from '../MaterialStyles/Forms';
import { useHistory } from 'react-router-dom';
import { useSnackbar } from 'notistack';
import { useTranslation } from 'react-i18next';

const LoginPage: React.FC = () => {
    const { t } = useTranslation(['common']);
    const history = useHistory();
    const [submitButtonDisabled, setSubmitButtonDisabled] = useState<boolean>(true);
    const [userNameInput, setUserNameInput] = useState<string>('');
    const [passwordInput, setPasswordInput] = useState<string>('');
    const authContext = useAuthProvider();
    const formClasses = useFormStyles();
    const { enqueueSnackbar } = useSnackbar();

    const handleKeyDown = (event: { keyCode: number }) => {
        if (event.keyCode === 13 && !submitButtonDisabled) {
            login();
        }
    };

    useEventListener('keyup', handleKeyDown);

    const login = async () => {
        setSubmitButtonDisabled(true);
        const userType = await authContext?.login(userNameInput, passwordInput);

        if (userType === UserType.NOTLOGGEDIN) {
            enqueueSnackbar(t('common:error:login'), { variant: 'error' });
        } else {
            enqueueSnackbar(t('common:success:login'), { variant: 'success' });
        }

        if (userType === UserType.CUSTOMER) {
            history.push(UPDATECUSTOMER);
            return;
        } else if (userType === UserType.ADMIN) {
            history.push(CUSTOMERS);
            return;
        } else if (userType === UserType.GUEST) {
            history.push('/');
            return;
        }
        setSubmitButtonDisabled(false);
    };

    const handleFormInput = (event: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) => {
        event.preventDefault();
        const value = event.target.value;

        if (event.target.name === 'Username') {
            setUserNameInput(value);
        } else if (event.target.name === 'Password') {
            setPasswordInput(value);
        }
    };

    useEffect(() => {
        if (userNameInput.length > 0 && passwordInput.length > 0) {
            setSubmitButtonDisabled(false);
        } else {
            setSubmitButtonDisabled(true);
        }
    }, [userNameInput, passwordInput]);

    return (
        <div className="loginPageWrapper">
            <Header>{t('common:commonText.login')}</Header>
            <Fade in={true}>
                <div className={formClasses.column}>
                    <TextField
                        className={`${formClasses.columnItem} usernameInput ${formClasses.formInput}`}
                        label={t('common:commonText.username')}
                        onChange={(event) => {
                            handleFormInput(event);
                        }}
                        name="Username"
                        value={userNameInput}
                        required
                        variant="outlined"
                    />

                    <TextField
                        className={`${formClasses.columnItem} passwordInput ${formClasses.formInput}`}
                        label={t('common:commonText.password')}
                        onChange={(event) => {
                            handleFormInput(event);
                        }}
                        name="Password"
                        value={passwordInput}
                        required
                        type="password"
                        variant="outlined"
                    />

                    <div className={formClasses.formControl}>
                        <Button
                            className={`${formClasses.formControlButton} submitButton  ${formClasses.button}`}
                            variant="contained"
                            color="primary"
                            disabled={submitButtonDisabled}
                            onClick={login}
                        >
                            {t('common:button.save')}
                        </Button>

                        <Button
                            className={`${formClasses.formControlButton} ${formClasses.button}`}
                            size="large"
                            variant="contained"
                            onClick={() => {
                                history.push('/');
                            }}
                        >
                            {t('common:button.cancel')}
                        </Button>
                    </div>
                </div>
            </Fade>
        </div>
    );
};

export default LoginPage;
