import { AssignRoleType, RedirectWithUser, User } from '../../store/types';
import { Button, Fade, MenuItem, Paper, Select, TextField } from '@material-ui/core';
import React, { useEffect, useState } from 'react';
import { useHistory, useLocation } from 'react-router-dom';

import { Alert } from '../Notifications/Notifcations';
import Header from '../Header/Header';
import { assignRoleToUser } from '../../api/authApi';
import { revokeUser } from '../../api/userManageApi';
import { useFormStyles } from '../MaterialStyles/Forms';
import { useSnackbar } from 'notistack';
import { useTranslation } from 'react-i18next';

const UpdateUserPage: React.FC = () => {
    const [submitButtonDisabled, setSubmitButtonDisabled] = useState<boolean>(false);
    const [role, setRole] = useState<AssignRoleType>('ADMIN');
    const [revokable, setRevokable] = useState<boolean>(false);
    const location = useLocation<RedirectWithUser>();
    const history = useHistory();
    const { t } = useTranslation('common');
    const { enqueueSnackbar } = useSnackbar();
    const formClasses = useFormStyles();

    const SetDefaultUser = (): User => {
        //If the user is navigating here from customers page (selecting a customer)
        if (location?.state?.user) {
            const user = location.state.user;
            return user;
            //If the user tried to navigate to this page by url (without a selected customer)
            //Will be pushed back to homepage
        } else {
            history.push('/');
            return { username: '', email: '', roles: '' };
        }
    };

    const [user] = useState<User>(SetDefaultUser());

    const revokeRole = async () => {
        setSubmitButtonDisabled(true);
        let roleToRevoke = '';
        if (user.roles.includes('ADMIN')) {
            roleToRevoke = 'ADMIN';
        } else {
            roleToRevoke = 'CUSTOMER';
        }

        const revokeResponse = await revokeUser(user.username, roleToRevoke as AssignRoleType);

        if (revokeResponse.error) {
            enqueueSnackbar(t('common:error.revokeRole', { error: revokeResponse.error }), { variant: 'error' });
        } else {
            history.push('/assignRole');
            enqueueSnackbar(t('common:success.revokeRole'), { variant: 'success' });
        }
        setSubmitButtonDisabled(false);
    };

    const assignRole = async () => {
        setSubmitButtonDisabled(true);
        const assignRoleResponse = await assignRoleToUser(user.username, role);
        if (assignRoleResponse.error) {
            enqueueSnackbar(t('common:error.assignRole', { error: assignRoleResponse.error }), { variant: 'error' });
        } else {
            history.push('/assignRole');
            enqueueSnackbar(t('common:success.assignRole'), { variant: 'success' });
        }
        setSubmitButtonDisabled(false);
    };

    const handleRoleSelect = (event: React.ChangeEvent<{ value: unknown }>) => {
        event.preventDefault();
        setRole(event.target.value as AssignRoleType);
    };

    useEffect(() => {
        if (user.roles.includes('ADMIN') || user.roles.includes('CUSTOMER')) {
            setRevokable(true);
        }
    }, [user]);

    return (
        <div className="loginPageWrapper">
            <Header>{t('common:pageTitle.assignRoles')}</Header>
            <Fade in={true}>
                <Paper elevation={3} className={formClasses.inputWrapper}>
                    <div className={formClasses.column}>
                        <TextField
                            className={`${formClasses.columnItem} ${formClasses.formInput} usernameInput`}
                            label={t('common:commonText.username')}
                            name="Username"
                            value={user.username}
                            variant="outlined"
                            disabled
                        />

                        <Select
                            className={`${formClasses.columnItem}  ${formClasses.formInput} roleTypeSelect`}
                            label={t('common:account.selectAccountType')}
                            onChange={(event) => handleRoleSelect(event)}
                            value={role}
                            variant="outlined"
                        >
                            <MenuItem value="ADMIN">{t('common:roles.admin')}</MenuItem>
                            <MenuItem value="CUSTOMER">{t('common:roles.customer')}</MenuItem>
                        </Select>

                        {user.roles.includes('ADMIN') && (
                            <Alert severity="warning">{t('common:warning.revokingAdmin')}</Alert>
                        )}

                        <div className={formClasses.formControl}>
                            <Button
                                className={`${formClasses.formControlButton} assignRoleButton ${formClasses.button}`}
                                color="primary"
                                variant="contained"
                                disabled={submitButtonDisabled}
                                onClick={assignRole}
                            >
                                {t('common:button.save')}
                            </Button>
                            {revokable && (
                                <Button
                                    className={`${formClasses.formControlButton} revokeButton ${formClasses.button}`}
                                    color="primary"
                                    variant="contained"
                                    disabled={submitButtonDisabled}
                                    onClick={revokeRole}
                                >
                                    {t('common:roles.revoke')}
                                </Button>
                            )}
                            <Button
                                className={`${formClasses.formControlButton} cancelButton ${formClasses.button}`}
                                color="primary"
                                variant="contained"
                                onClick={() => {
                                    history.push('/assignRole');
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

export default UpdateUserPage;
