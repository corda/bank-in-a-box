import { Redirect, Route, RouteProps } from 'react-router-dom';

import React from 'react';
import { UserType } from '../../store/types';
import { useAuthProvider } from '../../store/AuthenticationContext';

interface PrivateRouteProps extends RouteProps {
    component: any;
    requiredRoles: UserType[];
}

const PrivateRoute = (props: PrivateRouteProps) => {
    const { component: Component, requiredRoles, ...rest } = props;
    const authContext = useAuthProvider();
    if (authContext?.isFetching) {
        return <></>;
    }

    return (
        <Route
            {...rest}
            render={(props) => {
                let user = authContext?.user;
                if (user === undefined || user?.userType === UserType.NOTLOGGEDIN) {
                    // not logged in so redirect to login page with the return url
                    return <Redirect to={{ pathname: '/login', state: { from: props.location } }} />;
                }

                // route is restricted redirect to route
                if (requiredRoles.indexOf(user!.userType) === -1) {
                    return <Redirect to={{ pathname: '/', state: { from: props.location } }} />;
                }

                // check if route is restricted by user type
                if (requiredRoles && requiredRoles.indexOf(user!.userType) !== -1) {
                    // role authorised
                    return <Component {...props} />;
                }
            }}
        />
    );
};

export default PrivateRoute;
