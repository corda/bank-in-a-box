import {
    Accordion,
    AccordionDetails,
    AccordionSummary,
    Button,
    Chip,
    Divider,
    Fade,
    Paper,
    TextField,
    Theme,
    Typography,
    createStyles,
    makeStyles,
} from '@material-ui/core';
import { CachedFile, Customer, EmptyCustomerData, RedirectWithCustomer, UserType } from '../../store/types';
import { DUPLICATEATTACHMENTEXCEPTION, FILESIZERROR } from '../../constants/APIERRORS';
import {
    MINPHONENUMBERLENGHT,
    MINPOSTCODELENGHT,
    getMaxFileSizeFromErrorBytes,
    mapCustomerData,
    titleCase,
    validateContactNumber,
    validateEmail,
} from '../../utils/Utils';
import React, { useEffect, useState } from 'react';
import { getCustomerById, updateCustomerAdmin, updateCustomerCustomer, uploadAttachment } from '../../api/customerApi';
import { useHistory, useLocation } from 'react-router-dom';

import { Alert } from '../Notifications/Notifcations';
import ExpandMoreIcon from '@material-ui/icons/ExpandMore';
import FilterNoneIcon from '@material-ui/icons/FilterNone';
import Header from '../Header/Header';
import { ResolvedPromise } from '../../api/resolvePromise';
import { useAuthProvider } from '../../store/AuthenticationContext';
import useEventListener from '@use-it/event-listener';
import { useFormStyles } from '../MaterialStyles/Forms';
import { useSnackbar } from 'notistack';
import { useTranslation } from 'react-i18next';

export const useUpdateCustomerStyles = makeStyles((theme: Theme) =>
    createStyles({
        customerIdContainer: {
            display: 'flex',
        },
        idAlert: {
            width: '100%',
            marginRight: 10,
        },
        copyButton: {
            color: 'white',
            backgroundColor: '#2196f3',
            paddingLeft: 30,
            paddingRight: 30,
            order: 2,
            marginLeft: 'auto',
            '&:hover': {
                background: theme.palette.primary.light,
                color: theme.palette.primary.contrastText,
            },
            boxShadow:
                '0px 3px 5px -1px rgba(0,0,0,0.2), 0px 6px 10px 0px rgba(0,0,0,0.14), 0px 1px 18px 0px rgba(0,0,0,0.12)',
        },
    })
);

const UpdateCustomer: React.FC = () => {
    const location = useLocation<RedirectWithCustomer>();
    const history = useHistory();
    const { t } = useTranslation('common');
    const authContext = useAuthProvider();
    const formClasses = useFormStyles();
    const updateCustomerClasses = useUpdateCustomerStyles();

    const handleKeyDown = (event: { keyCode: number }) => {
        if (event.keyCode === 13 && !submitButtonDisabled) {
            submitUpdate();
        }
    };
    useEventListener('keyup', handleKeyDown);

    const setInitialCustomerData = (): Customer => {
        //If the user is navigating here from customers page (selecting a customer)
        if (location?.state?.customer) {
            return location.state.customer;
            //If the user tried to navigate to this page by url (without a selected customer)
            //Will be pushed back to homepage
        } else if (authContext?.user?.userType === UserType.CUSTOMER) {
            const tempCustomerWithId = EmptyCustomerData;
            tempCustomerWithId.customerId = authContext.user.userId;
            return tempCustomerWithId;
        } else {
            history.push('/');
            return EmptyCustomerData;
        }
    };

    const [customer, setCustomer] = useState<Customer>(setInitialCustomerData());
    const [customerNameInput, setCustomerNameInput] = useState<string>(customer.customerName);
    const [contactNumberInput, setContactNumberInput] = useState<string>(customer.contactNumber);
    const [emailInput, setEmailInput] = useState<string>(customer.emailAddress);
    const [postCodeInput, setPostCodeInput] = useState<string>(customer.postCode);
    const [submitButtonDisabled, setSubmitButtonDisabled] = useState<boolean>(true);
    const [files, setFiles] = useState<File[]>([]);
    const [cachedFiles, setCachedFiles] = useState<CachedFile[]>([]);
    const { enqueueSnackbar } = useSnackbar();

    useEffect(() => {
        const updateCustomer = async () => {
            if (customer.customerId.length <= 0) {
                return;
            }
            const customerData: ResolvedPromise = await getCustomerById(customer.customerId);
            if (customerData.error) {
                enqueueSnackbar(t('common:error.serverContactError', { error: customerData.error }), {
                    variant: 'error',
                });
                history.push('/');
            } else {
                const customerMapped = mapCustomerData(customerData.data.data);
                setCustomer(customerMapped);
                updateInputs(customerMapped);
            }
        };
        updateCustomer();
    }, [customer.customerId, history, t, enqueueSnackbar]);

    useEffect(() => {
        const validSubmitInput = (): boolean => {
            if (
                (contactNumberInput === customer.contactNumber &&
                    emailInput === customer.emailAddress &&
                    files.length === 0 &&
                    postCodeInput === customer.postCode &&
                    customerNameInput === customer.customerName) ||
                contactNumberInput.length === 0 ||
                emailInput.length === 0 ||
                customerNameInput.length === 0 ||
                postCodeInput.length === 0 ||
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
    }, [customerNameInput, contactNumberInput, emailInput, postCodeInput, customer, files]);

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

    const submitUpdate = async () => {
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
                    errorMessage = t('common:error.duplicateFile', { fileName: file.name });
                } else if (errorMessage.includes(FILESIZERROR)) {
                    errorMessage = t('common:error.fileSize', { maxSize: getMaxFileSizeFromErrorBytes(errorMessage) });
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

        let customerData: ResolvedPromise;

        if (authContext?.user?.userType === UserType.ADMIN) {
            customerData = await updateCustomerAdmin(
                customer.customerId,
                customerNameInput,
                postCodeInput,
                contactNumberInput,
                emailInput,
                attachments
            );
        } else {
            customerData = await updateCustomerCustomer(
                customer.customerId,
                contactNumberInput,
                emailInput,
                attachments
            );
        }
        if (customerData.error) {
            enqueueSnackbar(t('common:error.failedToUpdateCustomer', { error: customerData.error }), {
                variant: 'error',
            });
        } else {
            enqueueSnackbar(t('common:success.customerUpdated'), { variant: 'success' });
            if (authContext?.user?.userType === UserType.ADMIN) {
                history.push('/customers');
            } else {
                history.push('/updateCustomer');
            }
        }
    };

    const updateInputs = (customer: Customer) => {
        setCustomerNameInput(customer.customerName);
        setPostCodeInput(customer.postCode);
        setContactNumberInput(customer.contactNumber);
        setEmailInput(customer.emailAddress);
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

    const copyToClipBoard = () => {
        var dummy = document.createElement('textarea');
        document.body.appendChild(dummy);
        dummy.value = customer.customerId;
        dummy.select();
        document.execCommand('copy');
        document.body.removeChild(dummy);
    };

    return (
        <div className="customerWrapper">
            <Header>
                {authContext?.user?.userType === UserType.CUSTOMER
                    ? t('common:pageTitle.myProfile')
                    : t('common:pageTitle.updateCustomer')}
            </Header>

            <Fade in={true}>
                <Paper elevation={3} className={formClasses.inputWrapper}>
                    <div className={formClasses.column}>
                        <TextField
                            className={`${formClasses.columnItem} customerNameInput ${formClasses.formInput} ${
                                customerNameInput.length > 0 ? formClasses.completedInput : formClasses.incompleteInput
                            }`}
                            label={t('common:commonText.customerName')}
                            onChange={(event) => {
                                handleFormInput(event);
                            }}
                            name="CustomerName"
                            variant="outlined"
                            disabled={authContext?.user?.userType !== UserType.ADMIN}
                            value={customerNameInput}
                        />
                        <TextField
                            label={t('common:commonText.contactNumber')}
                            className={`${formClasses.columnItem} contactNumberInput ${formClasses.formInput} ${
                                contactNumberInput.length >= MINPHONENUMBERLENGHT
                                    ? formClasses.completedInput
                                    : formClasses.incompleteInput
                            }`}
                            variant="outlined"
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
                            onChange={(event) => {
                                handleFormInput(event);
                            }}
                            name="EmailAddress"
                            variant="outlined"
                            required
                            value={emailInput}
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
                            variant="outlined"
                            name="PostCode"
                            value={postCodeInput}
                            onChange={(event) => {
                                handleFormInput(event);
                            }}
                            helperText={
                                authContext?.user?.userType === UserType.CUSTOMER
                                    ? t('common:commonText.customerChangePostCode')
                                    : t('common:commonText.validPostCode', { minPostCodeLength: MINPOSTCODELENGHT })
                            }
                            required
                            disabled={authContext?.user?.userType !== UserType.ADMIN}
                        />

                        <Accordion defaultExpanded={true} className={formClasses.columnItem}>
                            <AccordionSummary
                                expandIcon={<ExpandMoreIcon />}
                                aria-controls="panel1a-content"
                                id="panel1a-header"
                            >
                                <Typography>{t('common:customer.attachments')}</Typography>
                            </AccordionSummary>
                            {customer.attachments.map((attachment, index) => {
                                return (
                                    <>
                                        <Divider />
                                        <AccordionDetails key={index}>
                                            <Typography>{attachment.name}</Typography>
                                        </AccordionDetails>
                                    </>
                                );
                            })}
                        </Accordion>

                        {authContext?.user?.userType === UserType.ADMIN && (
                            <>
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
                            </>
                        )}
                    </div>

                    <div className={formClasses.column}>
                        <TextField
                            label={t('common:customer.createdOnDate')}
                            type="datetime"
                            value={new Date(customer.createdOn)}
                            className={`${formClasses.columnItem} ${formClasses.formInput}`}
                            InputLabelProps={{
                                shrink: true,
                            }}
                            disabled
                        />

                        <TextField
                            label={t('common:customer.modifiedOnDate')}
                            type="datetime"
                            value={new Date(customer.modifiedOn)}
                            className={`${formClasses.columnItem} ${formClasses.formInput}`}
                            InputLabelProps={{
                                shrink: true,
                            }}
                            disabled
                        />

                        <div className={updateCustomerClasses.customerIdContainer}>
                            <Alert severity="info" className={`customerIdDisplay ${updateCustomerClasses.idAlert}`}>
                                {t('common:customer.customerId') + ' : ' + customer.customerId}
                            </Alert>
                            {document.queryCommandSupported('copy') && (
                                <Button
                                    className={updateCustomerClasses.copyButton}
                                    aria-label="upload picture"
                                    component="span"
                                    onClick={copyToClipBoard}
                                    startIcon={<FilterNoneIcon />}
                                >
                                    {t('common:commonText.copy')}
                                </Button>
                            )}
                        </div>

                        <div className={formClasses.formControl}>
                            <Button
                                className={`${formClasses.formControlButton} submitButton ${formClasses.button}`}
                                variant="contained"
                                color="primary"
                                disabled={submitButtonDisabled}
                                onClick={submitUpdate}
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

export default UpdateCustomer;
