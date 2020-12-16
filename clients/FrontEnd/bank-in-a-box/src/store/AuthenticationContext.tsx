import React, { useCallback, useContext, useEffect, useState } from 'react';
import { logInUser, refreshTokens } from '../api/authApi';

import { REALTIMENOTIFSERVICE } from '../constants/SessionStorageKeys';
import { UserType } from './types';
import { axiosInstance } from '../api/apiConfig';
import jwt_decode from 'jwt-decode';
import { useHistory } from 'react-router-dom';

type TokenResponse = {
    access_token: string;
    refresh_token: string;
};

type Token = {
    user_name: string;
    customerId: string | null;
    authorities: string[];
};

export type User = {
    userName: string;
    userId: string;
    userType: UserType;
    accessToken: string;
};

type AuthContextProps =
    | {
          user: User | null;
          login: (username: string, password: string) => Promise<UserType>;
          logout: () => void;
          isFetching: boolean;
      }
    | undefined;

const AuthenticationContext = React.createContext<AuthContextProps>(undefined);

export const AuthenticationContextProvider = (props) => {
    const [user, setUser] = useState<User>({
        userName: '',
        userId: '',
        userType: UserType.NOTLOGGEDIN,
        accessToken: '',
    });
    const [isFetching, setIsFetching] = useState<boolean>(true);
    const history = useHistory();

    const login = async (username: string, password: string): Promise<UserType> => {
        const tokenResponse = await logInUser(username, password);
        if (tokenResponse.error) {
            return UserType.NOTLOGGEDIN;
        }
        return saveUser(tokenResponse.data.data as TokenResponse);
    };

    const saveUser = useCallback((tokenResponse: TokenResponse): UserType => {
        const userDetailsDecoded = jwt_decode<Token>(tokenResponse.access_token);
        const userType = resolveUserType(userDetailsDecoded);
        if (userType === UserType.ADMIN) {
            setUser({
                userName: userDetailsDecoded.user_name!,
                userId: '',
                userType: UserType.ADMIN,
                accessToken: tokenResponse.access_token,
            });
        } else {
            setUser({
                userName: userDetailsDecoded.user_name!,
                userId: userDetailsDecoded.customerId!,
                userType: userType,
                accessToken: tokenResponse.access_token,
            });
        }
        axiosInstance.defaults.headers.common['Authorization'] = `bearer ${tokenResponse.access_token}`;
        sessionStorage.setItem('refresh_token', tokenResponse.refresh_token);
        setIsFetching(false);
        return userType;
    }, []);

    const logout = async () => {
        setUser({ userName: '', userId: '', userType: UserType.NOTLOGGEDIN, accessToken: '' });
        sessionStorage.removeItem('refresh_token');
        sessionStorage.removeItem(REALTIMENOTIFSERVICE);
    };

    //On every page refresh
    //This will check to see if a refreshToken exists in sessionStorage
    //If it does will try to get the access token and user details
    useEffect(() => {
        const getUser = async () => {
            const accessTokenResponse = await refreshTokens(sessionStorage.getItem('refresh_token')!);
            if (accessTokenResponse.error) {
                history.push('/login');
                logout();
                return;
            }
            saveUser(accessTokenResponse.data.data as TokenResponse);
        };

        if (sessionStorage.getItem('refresh_token')) {
            setIsFetching(true);
            getUser();
        } else {
            setIsFetching(false);
        }
    }, [history, saveUser]);

    const resolveUserType = (jwt: Token): UserType => {
        let userType = UserType.NOTLOGGEDIN;
        for (let auth of jwt.authorities) {
            if (auth === 'GUEST') {
                userType = UserType.GUEST;
            } else if (auth === 'CUSTOMER') {
                return UserType.CUSTOMER;
            } else if (auth === 'ADMIN') {
                return UserType.ADMIN;
            }
        }
        return userType;
    };

    return (
        <AuthenticationContext.Provider
            value={{
                user,
                login,
                logout,
                isFetching,
            }}
        >
            {props.children}
        </AuthenticationContext.Provider>
    );
};

export const useAuthProvider = () => useContext(AuthenticationContext);
