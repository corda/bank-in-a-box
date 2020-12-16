import './App.scss';

import * as ROUTES from '../../constants/Routes';

import React, { useState } from 'react';
import { Route, BrowserRouter as Switch } from 'react-router-dom';
import { Theme, createStyles, makeStyles } from '@material-ui/core';

import Button from '@material-ui/core/Button';
import DrawerNav from '../DrawerNav/DrawerNav';
import Footer from '../Footer/Footer';
import HomePage from '../HomePage/HomePage';
import Loading from '../Loading/Loading';
import LoginPage from '../LoginPage/LoginPage';
import MenuOpenIcon from '@material-ui/icons/MenuOpen';
import NavBar from '../NavBar/NavBar';
import PrivateRoute from '../PrivateRoute/PrivateRoute';
import { PrivateRoutes } from '../../constants/RouteRoles';
import Register from '../Register/Register';
import SnackbarCloseButton from '../Notifications/SnackBarCloseButton';
import { SnackbarProvider } from 'notistack';
import SocketMessages from '../SocketMessages/SocketMessages';
import TopNavWrapper from '../TopNavWrapper/TopNavWrapper';
import { UserType } from '../../store/types';
import { useAuthProvider } from '../../store/AuthenticationContext';
import { usePromiseTracker } from 'react-promise-tracker';
import { useTranslation } from 'react-i18next';

const useStyles = makeStyles((theme: Theme) =>
    createStyles({
        menuButton: {
            color: theme.palette.primary.contrastText,
            fontWeight: 'bold',
            fontSize: 18,

            '&:hover': {
                color: theme.palette.primary.main,
            },
        },
        notifBase: {
            '& #client-snackbar': {
                maxWidth: 400,
            },
        },
    })
);

const App: React.FC = () => {
    // Used for controlling the drawer nav bar when burger menu is available
    const [drawerOpen, setDrawerOpen] = useState(false);
    const classes = useStyles();
    const { t } = useTranslation('common');
    const closeDrawer = () => {
        setDrawerOpen(!drawerOpen);
    };
    const { promiseInProgress } = usePromiseTracker({
        delay: parseInt((window as any).REACT_APP_PROMISE_TRACKER_DELAY!.trim()),
    });

    const authContext = useAuthProvider();

    const mainContent = (
        <Switch>
            <Route exact path="/" component={HomePage}></Route>
            <Route exact path={ROUTES.LOGIN} component={LoginPage}></Route>
            <Route exact path={ROUTES.REGISTER} component={Register}></Route>
            {PrivateRoutes.map((route, index) => {
                return (
                    <PrivateRoute
                        key={index}
                        exact
                        path={route.path}
                        requiredRoles={route.roles}
                        component={route.component}
                    />
                );
            })}
        </Switch>
    );

    return (
        <SnackbarProvider
            id="snackBarProvider"
            className="snackBarProvider"
            dense
            maxSnack={5}
            anchorOrigin={{
                vertical: 'top',
                horizontal: 'right',
            }}
            classes={{ root: classes.notifBase }}
            action={(k) => <SnackbarCloseButton snackKey={k} />}
        >
            <div className="App">
                <TopNavWrapper
                    button={
                        <Button
                            className={`menuButton ${classes.menuButton}`}
                            startIcon={<MenuOpenIcon />}
                            color="primary"
                            onClick={() => {
                                setDrawerOpen(!drawerOpen);
                            }}
                        >
                            {t('common:commonText.menu')}
                        </Button>
                    }
                ></TopNavWrapper>
                {promiseInProgress && <Loading />}
                <div className="mainContent">{mainContent}</div>
                <DrawerNav open={drawerOpen} closeDrawer={closeDrawer}>
                    <NavBar />
                </DrawerNav>

                {authContext?.user?.userType === UserType.CUSTOMER && <SocketMessages />}

                <Footer copyright="© 2020 R3. All rights reserved.">
                    <a rel="noopener noreferrer" href="/#" title="Terms and Conditions">
                        {t('common:commonText.termsAndConditions')}
                    </a>
                    <a rel="noopener noreferrer" href="/#" title="Privacy Policy">
                        {t('common:commonText.privacyPolicy')}
                    </a>
                </Footer>
            </div>
        </SnackbarProvider>
    );
};

export default App;
