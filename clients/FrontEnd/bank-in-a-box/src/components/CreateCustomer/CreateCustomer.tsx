import { Button, Chip, Fade, Paper, TextField } from '@material-ui/core';
import { DUPLICATEATTACHMENTEXCEPTION, FILESIZERROR } from '../../constants/APIERRORS';
import {
    MINPHONENUMBERLENGHT,
    MINPOSTCODELENGHT,
    getMaxFileSizeFromErrorBytes,
    titleCase,
    validateContactNumber,
    validateEmail,
} from '../../utils/Utils';
import React, { useEffect, useState } from 'react';
import { createCustomer, uploadAttachment } from '../../api/customerApi';

import { CachedFile } from '../../store/types';
import Header from '../Header/Header';
import { ResolvedPromise } from '../../api/resolvePromise';
import useEventListener from '@use-it/event-listener';
import { useFormStyles } from '../MaterialStyles/Forms';
import { useHistory } from 'react-router-dom';
import { useSnackbar } from 'notistack';
import { useTranslation } from 'react-i18next';

const textFieldVariant = 'outlined';

const CreateCustomer: React.FC = () => {
    const history = useHistory();
    const [customerNameInput, setCustomerNameInput] = useState<string>('');
    const [contactNumberInput, setContactNumberInput] = useState<string>('');
    const [emailInput, setEmailInput] = useState<string>('');
    const [postCodeInput, setPostCodeInput] = useState<string>('');
    const [submitButtonDisabled, setSubmitButtonDisabled] = useState<boolean>(true);
    const [files, setFiles] = useState<File[]>([]);
    const [cachedFiles, setCachedFiles] = useState<CachedFile[]>([]);
    const { t } = useTranslation('common');
    const formClasses = useFormStyles();
    const { enqueueSnackbar } = useSnackbar();

    const handleKeyDown = (event: { keyCode: number }) => {
        if (event.keyCode === 13 && !submitButtonDisabled) {
            submitNewCustomer();
        }
    };

    useEventListener('keyup', handleKeyDown);

    useEffect(() => {
        const validSubmitInput = (): boolean => {
            if (
                contactNumberInput.length === 0 ||
                emailInput.length === 0 ||
                contactNumberInput.length === 0 ||
                postCodeInput.length === 0 ||
                files.length === 0 ||
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
    }, [customerNameInput, contactNumberInput, emailInput, postCodeInput, files]);

    const addFileToUpload = async (event: React.ChangeEvent<HTMLInputElement>) => {
        if (event.target.files![0] != null) {
            setFiles([...files, event.target.files![0]]);
        }
        event.target.value = '';
    };

    const removeFile = (file: File) => {
        setFiles(files.filter((item, index) => item !== file));
        setCachedFiles(cachedFiles.filter((item) => item.file !== file));
    };

    const existsInCache = (file: File): string => {
        for (const cachedFile of cachedFiles) {
            if (cachedFile.file.name === file.name) {
                return cachedFile.idHash;
            }
        }
        return '';
    };

    const submitNewCustomer = async () => {
        setSubmitButtonDisabled(true);
        let attachments: string[] = [];

        for (const file of files) {
            let cachedIdHash = existsInCache(file);
            //Skip this file as it has already been uploaded, add the cached idHash of it to the attachments array
            if (cachedIdHash.length > 0) {
                attachments.push(cachedIdHash);
                continue;
            }
            let uploadAttachmentResponse: ResolvedPromise = await uploadAttachment(customerNameInput, file);
            if (uploadAttachmentResponse.error) {
                let errorMessage = uploadAttachmentResponse.error;
                if (errorMessage.includes(DUPLICATEATTACHMENTEXCEPTION)) {
                    errorMessage = t('common:error:duplicateFile', { fileName: file.name });
                } else if (errorMessage.includes(FILESIZERROR)) {
                    errorMessage = t('common:error.fileSize', { maxSize: getMaxFileSizeFromErrorBytes(errorMessage) });
                } else {
                    errorMessage = uploadAttachmentResponse.error;
                }
                enqueueSnackbar(t('common:error.fileUploadError', { error: errorMessage }), { variant: 'error' });
                //remove file from attachments as it cannot be sent
                setFiles(files.filter((item) => item !== file));
                return;
            } else {
                //generate the fileName:filehash pair and cache it in state incase one of the queued files fails to upload
                const fileHashPair = `${file.name}:${uploadAttachmentResponse.data.data.secureHash}`;
                setCachedFiles([...cachedFiles, { idHash: fileHashPair, file: file }]);
                attachments.push(fileHashPair);
            }
            setSubmitButtonDisabled(false);
        }

        const createCustomerResponse: ResolvedPromise = await createCustomer(
            customerNameInput,
            contactNumberInput,
            emailInput,
            postCodeInput,
            attachments
        );

        if (createCustomerResponse.error) {
            enqueueSnackbar(t('common:error.createCustomerError', { error: createCustomerResponse.error }), {
                variant: 'error',
            });
        } else {
            enqueueSnackbar(t('common:success.createdCustomer'), { variant: 'success' });
            history.push('/customers');
        }
    };

    const handleFormInput = (event: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) => {
        event.preventDefault();
        const inputType = event.currentTarget.name;
        const value: string = event.currentTarget.value;
        if (inputType === 'CustomerName') {
            let textToSet = '';
            if (value.includes(' ')) {
                textToSet = titleCase(value);
            } else {
                textToSet = value;
            }
            setCustomerNameInput(textToSet);
        } else if (inputType === 'ContactNumber') {
            if (validateContactNumber(value) || value.length === 0) {
                setContactNumberInput(value);
            }
        } else if (inputType === 'EmailAddress') {
            setEmailInput(value.toLowerCase());
        } else if (inputType === 'PostCode' && value.length < 11) {
            setPostCodeInput(value);
        }
    };

    const fileInputs: JSX.Element[] = [];

    for (let i = 0; i < files.length; i++) {
        fileInputs.push(
            <Chip
                key={i}
                label={files[i] !== undefined ? files[i].name : ''}
                onDelete={() => {
                    removeFile(files[i]);
                }}
                color="primary"
                variant="outlined"
                className={formClasses.attachmentChip}
            />
        );
    }

    return (
        <div className="updateCustomerWrapper">
            <Header>{t('common:pageTitle.createCustomer')}</Header>

            <Fade in={true}>
                <Paper elevation={3} className={formClasses.inputWrapper}>
                    <div className={formClasses.column}>
                        <TextField
                            label={t('common:commonText.customerName')}
                            className={`${formClasses.columnItem} customerNameInput ${formClasses.formInput} ${
                                customerNameInput.length > 0 ? formClasses.completedInput : formClasses.incompleteInput
                            }`}
                            variant={textFieldVariant}
                            name="CustomerName"
                            value={customerNameInput}
                            onChange={(event) => {
                                handleFormInput(event);
                            }}
                            required
                        />

                        <TextField
                            label={t('common:commonText.contactNumber')}
                            className={`${formClasses.columnItem} contactNumberInput ${formClasses.formInput} ${
                                contactNumberInput.length >= MINPHONENUMBERLENGHT
                                    ? formClasses.completedInput
                                    : formClasses.incompleteInput
                            }`}
                            variant={textFieldVariant}
                            name="ContactNumber"
                            value={contactNumberInput}
                            onChange={(event) => {
                                handleFormInput(event);
                            }}
                            helperText={t('common:commonText.validPhoneNumber', {
                                minPhoneNumberLength: MINPHONENUMBERLENGHT,
                            })}
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
                            variant={textFieldVariant}
                            name="EmailAddress"
                            value={emailInput}
                            onChange={(event) => {
                                handleFormInput(event);
                            }}
                            required
                        />

                        <TextField
                            className={`${formClasses.columnItem} postCodeInput  ${formClasses.formInput} ${
                                formClasses.formInput
                            } ${
                                postCodeInput.length >= MINPOSTCODELENGHT
                                    ? formClasses.completedInput
                                    : formClasses.incompleteInput
                            }`}
                            label={t('common:commonText.postCode')}
                            variant={textFieldVariant}
                            name="PostCode"
                            value={postCodeInput}
                            onChange={(event) => {
                                handleFormInput(event);
                            }}
                            helperText={t('common:commonText.validPostCode', { minPostCodeLength: MINPOSTCODELENGHT })}
                            required
                        />

                        <input
                            accept=".zip, .jar"
                            style={{ opacity: '0', maxWidth: '0px' }}
                            id="raised-button-file"
                            type="file"
                            onChange={(event) => {
                                addFileToUpload(event);
                            }}
                        />
                        <label htmlFor="raised-button-file">
                            <Button
                                variant="contained"
                                component="span"
                                color="primary"
                                className={`fileUploadInput ${formClasses.button}`}
                            >
                                {t('common:inputHelpText.addAttachment')}
                            </Button>
                        </label>
                        {fileInputs}
                    </div>

                    <div className={formClasses.column}>
                        <div className={formClasses.formControl}>
                            <Button
                                className={`${formClasses.formControlButton} submitButton  ${formClasses.button}`}
                                variant="contained"
                                color="primary"
                                disabled={submitButtonDisabled}
                                onClick={submitNewCustomer}
                            >
                                {t('common:button.save')}
                            </Button>

                            <Button
                                className={`${formClasses.formControlButton}  ${formClasses.button}`}
                                variant="contained"
                                color="primary"
                                onClick={() => {
                                    history.push('/customers');
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

export default CreateCustomer;
