import * as ROUTES from './Routes';

import AccountApproveOverdraft from '../components/AccountApproveOverdraft/AccountApproveOverdraft';
import AccountDeposit from '../components/AccountDeposit/AccountDeposit';
import AccountIssueLoan from '../components/AccountIssueLoan/AccountIssueLoan';
import AccountSetLimits from '../components/AccountSetLimits/AccountSetLimits';
import AccountSetStatus from '../components/AccountSetStatus/AccountSetStatus';
import AccountView from '../components/AccountView/AccountView';
import AccountWithdraw from '../components/AccountWithdraw/AccountWithdraw';
import Accounts from '../components/Accounts/Accounts';
import AdminAssignRoles from '../components/AdminAssignRoles/AdminAssignRoles';
import CreateAccount from '../components/CreateAccount/CreateAccount';
import CreateCustomer from '../components/CreateCustomer/CreateCustomer';
import CreateRecurringPayment from '../components/CreateRecurringPayment/CreateRecurringPayment';
import Customers from '../components/Customers/Customers';
import { FunctionComponent } from 'react';
import IntrabankPayment from '../components/Payments/IntrabankPayment';
import RecurringPaymentView from '../components/RecurringPaymentView/RecurringPaymentView';
import RecurringPayments from '../components/RecurringPayments/RecurringPayments';
import TransactionView from '../components/Transaction/Transaction';
import Transactions from '../components/Transactions/Transactions';
import UpdateCustomer from '../components/UpdateCustomer/UpdateCustomer';
import UpdateUserPage from '../components/AdminAssignRoles/UpdateUserPage';
import { UserType } from '../store/types';

type Route = {
    path: string;
    roles: UserType[];
    component: FunctionComponent;
};

export const PrivateRoutes: Route[] = [
    { path: ROUTES.ACCOUNTS, roles: [UserType.ADMIN, UserType.CUSTOMER], component: Accounts },
    { path: ROUTES.ACCOUNTISSUELOAN, roles: [UserType.ADMIN], component: AccountIssueLoan },
    { path: ROUTES.ACCOUNTSETLIMITS, roles: [UserType.ADMIN, UserType.CUSTOMER], component: AccountSetLimits },
    { path: ROUTES.ACCOUNTAPPROVEOVERDRAFT, roles: [UserType.ADMIN], component: AccountApproveOverdraft },
    { path: ROUTES.ACCOUNTSETSTATUS, roles: [UserType.ADMIN], component: AccountSetStatus },
    { path: ROUTES.ACCOUNTVIEW, roles: [UserType.ADMIN, UserType.CUSTOMER], component: AccountView },
    { path: ROUTES.CREATEACCOUNT, roles: [UserType.ADMIN], component: CreateAccount },
    { path: ROUTES.CREATECUSTOMER, roles: [UserType.ADMIN], component: CreateCustomer },
    { path: ROUTES.CUSTOMERS, roles: [UserType.ADMIN], component: Customers },
    { path: ROUTES.PAYMENTS, roles: [UserType.CUSTOMER], component: IntrabankPayment },
    { path: ROUTES.RECURRINGPAYMENT, roles: [UserType.ADMIN, UserType.CUSTOMER], component: RecurringPaymentView },
    { path: ROUTES.RECURRINGPAYMENTS, roles: [UserType.ADMIN, UserType.CUSTOMER], component: RecurringPayments },
    { path: ROUTES.TRANSACTIONS, roles: [UserType.ADMIN, UserType.CUSTOMER], component: Transactions },
    { path: ROUTES.TRANSACTIONVIEW, roles: [UserType.ADMIN, UserType.CUSTOMER], component: TransactionView },
    {
        path: ROUTES.UPDATECUSTOMER,
        roles: [UserType.ADMIN, UserType.CUSTOMER, UserType.GUEST],
        component: UpdateCustomer,
    },
    { path: ROUTES.ASSIGNROLE, roles: [UserType.ADMIN], component: AdminAssignRoles },
    { path: ROUTES.DEPOSIT, roles: [UserType.ADMIN], component: AccountDeposit },
    { path: ROUTES.WITHDRAW, roles: [UserType.ADMIN], component: AccountWithdraw },
    { path: ROUTES.CREATERECURRINGPAYMENT, roles: [UserType.CUSTOMER], component: CreateRecurringPayment },
    { path: ROUTES.UPDATEUSER, roles: [UserType.ADMIN], component: UpdateUserPage },
];
