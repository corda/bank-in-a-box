import {
    AppBar,
    IconButton,
    Menu,
    MenuItem,
    Theme,
    Toolbar,
    Typography,
    createStyles,
    makeStyles,
} from '@material-ui/core';

import { AccountCircle } from '@material-ui/icons';
import ExitToAppIcon from '@material-ui/icons/ExitToApp';
import { Icon } from '@material-ui/core';
import React from 'react';
import { UserType } from '../../store/types';
import { useAuthProvider } from '../../store/AuthenticationContext';
import { useHistory } from 'react-router-dom';
import { useTranslation } from 'react-i18next';

const useStyles = makeStyles((theme: Theme) =>
    createStyles({
        appBar: {
            backgroundColor: 'white',
            color: theme.palette.primary.contrastText,
        },
        appIcon: {
            marginTop: 12,
            marginBottom: 'auto',
            width: 30,
            height: 30,
            marginLeft: 5,
        },
        binabIcon: {
            marginTop: 'auto',
            marginBottom: 'auto',
            width: 30,
            height: 30,
            marginLeft: 4,
        },
        grow: {
            flexGrow: 1,
        },
        button: {
            color: theme.palette.secondary.main,
            '&:hover': {
                color: theme.palette.primary.main,
            },
        },
        sectionDesktop: {
            display: 'flex',
        },
        title: {
            color: theme.palette.secondary.main,
            marginLeft: 8,
            fontSize: 18,
            fontWeight: 'bold',
            display: 'none',
            [theme.breakpoints.up('sm')]: {
                display: 'block',
            },
        },
        subTitle: {
            fontSize: 12,
            marginTop: 4,
            marginLeft: 6,
            display: 'none',
            color: theme.palette.secondary.main,
            [theme.breakpoints.up('sm')]: {
                display: 'block',
            },
        },
        cordaImg: {
            marginBottom: 5,
            marginLeft: 6,
            display: 'none',
            [theme.breakpoints.up('sm')]: {
                display: 'block',
            },
        },
    })
);

interface Props {
    button: React.ReactNode;
}

const TopNavWrapper: React.FC<Props> = ({ button }) => {
    const { t } = useTranslation('common');
    const authContext = useAuthProvider();
    const history = useHistory();
    const classes = useStyles();

    const [anchorEl, setAnchorEl] = React.useState<null | HTMLElement>(null);
    const isMenuOpen = Boolean(anchorEl);
    const handleProfileMenuOpen = (event: React.MouseEvent<HTMLElement>) => {
        setAnchorEl(event.currentTarget);
    };

    const handleMenuClose = () => {
        setAnchorEl(null);
    };

    const menuId = 'primary-search-account-menu';

    const renderMenu = (
        <Menu
            anchorEl={anchorEl}
            anchorOrigin={{ vertical: 'top', horizontal: 'right' }}
            id={menuId}
            keepMounted
            transformOrigin={{ vertical: 'top', horizontal: 'right' }}
            open={isMenuOpen}
            onClose={handleMenuClose}
        >
            <MenuItem
                onClick={() => {
                    history.push('/updateCustomer');
                    handleMenuClose();
                }}
            >
                {t('common:pageTitle.myProfile')}
            </MenuItem>
        </Menu>
    );

    const imageSource = (window as any).PUBLIC_URL ? (window as any).PUBLIC_URLtrim() : '';

    return (
        <div className={classes.grow}>
            <AppBar className={classes.appBar} position="static">
                <Toolbar>
                    {button}
                    <Icon className={classes.binabIcon}>
                        <img src={imageSource + 'binab.png'} alt={t('common:example')} height={30} width={30} />
                    </Icon>
                    <Typography className={classes.title} variant="h6" noWrap>
                        {t('common:appTitle')}
                    </Typography>
                    <Typography className={classes.subTitle} variant="body1" noWrap>
                        Powered By
                    </Typography>
                    <img
                        className={classes.cordaImg}
                        src={`${imageSource}cordablk.png`}
                        height={30}
                        width={75}
                        alt="Corda R3"
                    />

                    <div className={classes.grow}></div>
                    <div className={classes.sectionDesktop}>
                        {authContext?.user?.userType === UserType.CUSTOMER && (
                            <IconButton
                                className={classes.button}
                                edge="end"
                                aria-label="account of current user"
                                aria-controls={menuId}
                                aria-haspopup="true"
                                onClick={handleProfileMenuOpen}
                                color="inherit"
                            >
                                <AccountCircle />
                            </IconButton>
                        )}
                        <IconButton
                            className={`logOutButton ${classes.button}`}
                            onClick={() => {
                                authContext?.logout();
                                history.push('/login');
                            }}
                        >
                            <ExitToAppIcon />
                        </IconButton>

                        <Icon className={classes.appIcon}>
                            <img src={imageSource + 'r3.png'} alt={t('common:example')} height={30} width={30} />
                        </Icon>
                    </div>
                </Toolbar>
            </AppBar>
            {renderMenu}
        </div>
    );
};

export default TopNavWrapper;
