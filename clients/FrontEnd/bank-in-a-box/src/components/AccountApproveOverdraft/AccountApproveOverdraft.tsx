import { Account, EmptyAccountData, RedirectWithAccount } from '../../store/types';
import { Button, Fade, TextField } from '@material-ui/core';
import React, { useEffect, useState } from 'react';
import { ZeroOrEmptyString, validateStringIsNumber } from '../../utils/Utils';
import { useHistory, useLocation } from 'react-router-dom';

import Header from '../Header/Header';
import { approveOverdraft } from '../../api/accountApi';
import useEventListener from '@use-it/event-listener';
import { useFormStyles } from '../MaterialStyles/Forms';
import { useSnackbar } from 'notistack';
import { useTranslation } from 'react-i18next';

const AccountApproveOverdraft: React.FC = () => {
    const location = useLocation<RedirectWithAccount>();
    const history = useHistory();
    const { t } = useTranslation('common');
    const formClasses = useFormStyles();

    const setInitialAccountData = (): Account => {
        //If the user is navigating here from accountDetails page (selecting an account)
        if (location?.state?.account) {
            window.scroll(0, 0);
            return location.state.account;
            //If the user tried to navigate to this page by url (without a selected account)
            //Will be pushed back to homepage
        } else {
            history.push('/');
            return EmptyAccountData;
        }
    };

    const [account] = useState<Account>(setInitialAccountData());
    const [inputValue, setInputValue] = useState<string>('');
    const [submitButtonDisabled, setSubmitButtonDisabled] = useState<boolean>(true);

    const { enqueueSnackbar } = useSnackbar();

    const handleKeyDown = (event: { keyCode: number }) => {
        if (event.keyCode === 13 && !submitButtonDisabled) {
            submitOverdraftUpdate();
        }
    };

    useEventListener('keyup', handleKeyDown);

    const handleInput = (event: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) => {
        const inputVal = event.target.value;
        if (inputVal.length > 0 && !validateStringIsNumber(inputVal)) {
            return;
        }
        setInputValue(inputVal);
    };

    const goToPageWithAccount = (path: string) => {
        history.push({
            pathname: path,
            state: { account: account, from: location.pathname },
        });
    };

    const submitOverdraftUpdate = async () => {
        if (inputValue.length <= 0) {
            return;
        }
        setSubmitButtonDisabled(true);
        const response = await approveOverdraft(account.accountData.accountId, parseFloat(inputValue) * 100);

        if (response.error) {
            enqueueSnackbar(t('common:error.approveOverdraft', { error: response.error }), { variant: 'error' });
            setSubmitButtonDisabled(false);
            return;
        } else {
            enqueueSnackbar(t('common:success.approvedOverDraft'), { variant: 'success' });
            goToPageWithAccount('/accountView');
        }
    };

    useEffect(() => {
        window.scrollTo(0, 0);
    }, [location]);

    useEffect(() => {
        if (!ZeroOrEmptyString(inputValue)) {
            setSubmitButtonDisabled(false);
        } else {
            setSubmitButtonDisabled(true);
        }
    }, [inputValue]);

    return (
        <div className="approveOverdraftWrapper">
            <Header>{t('common:pageTitle.approveOverdraft')}</Header>
            <Fade in={true}>
                <div className={formClasses.inputWrapper}>
                    <div className={formClasses.column}>
                        <TextField
                            className={`${formClasses.columnItem} ${formClasses.formInput} overdraftLimit ${
                                ZeroOrEmptyString(inputValue) ? formClasses.incompleteInput : formClasses.completedInput
                            } `}
                            label={t('common:inputHelpText.overDraftLimit')}
                            onChange={(event) => {
                                handleInput(event);
                            }}
                            name={t('common:account.overdraftLimit')}
                            value={inputValue}
                            required
                            variant="outlined"
                            helperText={`${t('common:inputHelpText.overdraftLimit')}. ${
                                ZeroOrEmptyString(inputValue) ? t('common:inputHelpText.cannotBeZero') : ''
                            }`}
                            autoComplete="new-password"
                        />

                        <div className={formClasses.formControl}>
                            <Button
                                className={`${formClasses.formControlButton} submitButton ${formClasses.button}`}
                                variant="contained"
                                color="primary"
                                onClick={() => submitOverdraftUpdate()}
                                disabled={submitButtonDisabled}
                            >
                                {t('common:button.save')}
                            </Button>

                            <Button
                                className={`${formClasses.formControlButton}  ${formClasses.button} cancelButton`}
                                variant="contained"
                                color="primary"
                                onClick={() => goToPageWithAccount('/accountView')}
                            >
                                {t('common:button.cancel')}
                            </Button>
                        </div>
                    </div>
                </div>
            </Fade>
        </div>
    );
};

export default AccountApproveOverdraft;
