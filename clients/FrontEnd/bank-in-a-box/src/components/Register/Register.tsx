import { Button, Chip, Fade, Paper, TextField } from '@material-ui/core';
import {
    COULDNOTEXECUTESTATEMENT,
    CUSTOMERIDFIELD,
    FILESIZERROR,
    INVALIDUUID,
    SAMEUSERNAME,
} from '../../constants/APIERRORS';
import React, { useEffect, useState } from 'react';
import { getMaxFileSizeFromErrorBytes, validateEmail } from '../../utils/Utils';

import Header from '../Header/Header';
import { registerUser } from '../../api/authApi';
import useEventListener from '@use-it/event-listener';
import { useFormStyles } from '../MaterialStyles/Forms';
import { useHistory } from 'react-router-dom';
import { useSnackbar } from 'notistack';
import { useTranslation } from 'react-i18next';

const Register: React.FC = () => {
    const history = useHistory();
    const [usernameInput, setUsernameInput] = useState<string>('');
    const [emailInput, setEmailInput] = useState<string>('');
    const [passwordInput, setPasswordInput] = useState<string>('');
    const [confirmPasswordInput, setConfirmPasswordInput] = useState<string>('');
    const [customerIdInput, setCustomerIdInput] = useState<string>('');
    const [submitButtonDisabled, setSubmitButtonDisabled] = useState<boolean>(true);
    const [file, setFile] = useState<File>();
    const { t } = useTranslation('common');
    const formClasses = useFormStyles();
    const { enqueueSnackbar } = useSnackbar();

    const handleKeyDown = (event: { keyCode: number }) => {
        if (event.keyCode === 13 && !submitButtonDisabled) {
            register();
        }
    };

    useEventListener('keyup', handleKeyDown);

    useEffect(() => {
        const validSubmitInput = (): boolean => {
            if (
                passwordInput.length === 0 ||
                emailInput.length === 0 ||
                passwordInput.length === 0 ||
                confirmPasswordInput.length === 0 ||
                passwordInput !== confirmPasswordInput ||
                !validateEmail(emailInput)
            ) {
                return false;
            }
            return true;
        };

        if (validSubmitInput()) {
            setSubmitButtonDisabled(false);
        } else {
            setSubmitButtonDisabled(true);
        }
    }, [usernameInput, passwordInput, emailInput, confirmPasswordInput, file]);

    const register = async () => {
        setSubmitButtonDisabled(true);
        const registerReponse = await registerUser(usernameInput, passwordInput, emailInput, customerIdInput, file);

        if (registerReponse.error) {
            let errorMessage = registerReponse.error;
            if (errorMessage.includes(FILESIZERROR)) {
                errorMessage = t('common:error.fileSize', { maxSize: getMaxFileSizeFromErrorBytes(errorMessage) });
            }
            if (errorMessage.includes(COULDNOTEXECUTESTATEMENT) && errorMessage.includes(CUSTOMERIDFIELD)) {
                errorMessage = t('common:error.registerSameCustId', { custId: customerIdInput });
            }
            if (errorMessage.includes(SAMEUSERNAME)) {
                errorMessage = t('common:error.registerSameUsername', { username: usernameInput });
            }
            if (errorMessage.includes(INVALIDUUID)) {
                errorMessage = t('common:error.invalidUuid');
            }
            enqueueSnackbar(t('common:error.registerError', { error: errorMessage }), { variant: 'error' });
        } else {
            enqueueSnackbar(t('common:success.registered'), { variant: 'success' });
            history.push('/login');
        }
        setSubmitButtonDisabled(false);
    };

    const handleFormInput = (event: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) => {
        event.preventDefault();
        const inputType = event.currentTarget.name;
        const value: string = event.currentTarget.value;
        if (inputType === 'UserName') {
            setUsernameInput(value);
        } else if (inputType === 'EmailAddress') {
            setEmailInput(value);
        } else if (inputType === 'Password') {
            setPasswordInput(value);
        } else if (inputType === 'ConfirmPassword') {
            setConfirmPasswordInput(value);
        } else if (inputType === 'CustomerId') {
            setCustomerIdInput(value);
        }
    };

    const fileInput: JSX.Element =
        file !== undefined ? (
            <Chip
                label={file?.name}
                onDelete={() => {
                    setFile(undefined);
                }}
                color="primary"
                variant="outlined"
                className={formClasses.attachmentChip}
            />
        ) : (
            <></>
        );

    const passwordAccepted = passwordInput.length > 0 && passwordInput === confirmPasswordInput ? true : false;

    return (
        <div className="registerWrapper">
            <Header>{t('common:pageTitle.register')}</Header>
            <Fade in={true}>
                <Paper elevation={3} className={formClasses.inputWrapper}>
                    <div className={formClasses.column}>
                        <TextField
                            className={`${formClasses.columnItem} usernameInput ${formClasses.formInput} ${
                                usernameInput.length > 0 ? formClasses.completedInput : formClasses.incompleteInput
                            }`}
                            label={t('common:commonText.username')}
                            onChange={(event) => {
                                handleFormInput(event);
                            }}
                            name="UserName"
                            value={usernameInput}
                            variant="outlined"
                            required
                        />

                        <TextField
                            className={`${formClasses.columnItem} emailAddressInput ${formClasses.formInput} ${
                                validateEmail(emailInput) ? formClasses.completedInput : formClasses.incompleteInput
                            }`}
                            label={
                                !validateEmail(emailInput) && emailInput.length > 0
                                    ? t('common:error.invalidEmail')
                                    : t('common:commonText.emailAddress')
                            }
                            onChange={(event) => {
                                handleFormInput(event);
                            }}
                            name="EmailAddress"
                            value={emailInput}
                            variant="outlined"
                            required
                        />

                        <TextField
                            className={`${formClasses.columnItem} passwordInput ${
                                passwordAccepted ? formClasses.completedInput : formClasses.incompleteInput
                            } ${formClasses.formInput}`}
                            label={t('common:commonText.password')}
                            onChange={(event) => {
                                handleFormInput(event);
                            }}
                            name="Password"
                            type="password"
                            value={passwordInput}
                            variant="outlined"
                            required
                        />

                        <TextField
                            className={`${formClasses.columnItem} passwordConfirmInput  ${
                                passwordAccepted ? formClasses.completedInput : formClasses.incompleteInput
                            } ${formClasses.formInput}`}
                            label={t('common:commonText.passwordConfirm')}
                            onChange={(event) => {
                                handleFormInput(event);
                            }}
                            name="ConfirmPassword"
                            type="password"
                            helperText={
                                passwordInput !== confirmPasswordInput ? t('common:commonText.passwordsMustMatch') : ''
                            }
                            value={confirmPasswordInput}
                            variant="outlined"
                            required
                        />

                        <TextField
                            className={`${formClasses.columnItem} customerIdInput ${formClasses.formInput}`}
                            label={t('common:inputHelpText.registerCustomerId')}
                            onChange={(event) => {
                                handleFormInput(event);
                            }}
                            name="CustomerId"
                            value={customerIdInput}
                            variant="outlined"
                        />

                        <input
                            accept=".zip, .jar"
                            style={{ opacity: '0', maxWidth: '0px' }}
                            id="raised-button-file"
                            type="file"
                            onChange={(event) => {
                                setFile(event.target.files![0]);
                                event.target.value = '';
                            }}
                        />
                        <label htmlFor="raised-button-file">
                            <Button
                                variant="contained"
                                component="span"
                                color="primary"
                                className={`fileUploadInput ${formClasses.button}`}
                            >
                                {t('common:inputHelpText.registerFile')}
                            </Button>
                        </label>
                        {fileInput}
                    </div>

                    <div className={formClasses.column}>
                        <div className={formClasses.formControl}>
                            <Button
                                className={`${formClasses.formControlButton} submitButton`}
                                variant="contained"
                                disabled={submitButtonDisabled}
                                color="primary"
                                onClick={register}
                            >
                                {t('common:button.save')}
                            </Button>

                            <Button
                                className={`${formClasses.formControlButton} cancelButton`}
                                color="primary"
                                variant="contained"
                                onClick={() => {
                                    history.push('/login');
                                }}
                            >
                                {t('common:button.cancel')}
                            </Button>
                        </div>
                    </div>
                </Paper>
            </Fade>
        </div>
    );
};

export default Register;
