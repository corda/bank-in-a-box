import 'react-app-polyfill/ie11';
import 'react-app-polyfill/stable';
import './index.css';
import './i18n/i18n';

import * as serviceWorker from './serviceWorker';

import App from './components/App/App';
import { AuthenticationContextProvider } from './store/AuthenticationContext';
import React from 'react';
import ReactDOM from 'react-dom';
import { BrowserRouter as Router } from 'react-router-dom';
import { ThemeProvider } from '@material-ui/core';
import theme from './components/MaterialStyles/MaterialTheme';

ReactDOM.render(
    <Router forceRefresh={true}>
        <ThemeProvider theme={theme}>
            <AuthenticationContextProvider>
                <App />
            </AuthenticationContextProvider>
        </ThemeProvider>
    </Router>,
    document.getElementById('root')
);

// If you want your app to work offline and load faster, you can change
// unregister() to register() below. Note this comes with some pitfalls.
// Learn more about service workers: https://bit.ly/CRA-PWA
serviceWorker.unregister();
