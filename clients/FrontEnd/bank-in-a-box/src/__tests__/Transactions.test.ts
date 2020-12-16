import 'selenium-webdriver/chrome';
import 'selenium-webdriver/firefox';
import 'chromedriver';
import 'geckodriver';

import {
    AccountDetailsPage,
    AccountsPage,
    AppBar,
    AssignRolesPage,
    CreateAccountsPage,
    CreateCustomerPage,
    CreateRecurringPaymentPage,
    CustomersPage,
    IntrabankPaymentsPage,
    LoginPage,
    NotificationService,
    RegisterPage,
    TransactionDetailsPage,
    TransactionsPage,
    UpdateCustomerPage,
} from '../testUtils/testPageComponents';
import { Builder, WebDriver } from 'selenium-webdriver';
import { TESTFILEDIR, getElementByCssQuery, getElementById } from '../testUtils/testHelpers';

import fs from 'fs';
import retry from 'jest-retries';
import { titleCase } from '../utils/Utils';

let driver: WebDriver;
jasmine.DEFAULT_TIMEOUT_INTERVAL = 1000 * 60 * 5;

const FILENAME1 = 'TxTestZip1.zip';
const FILENAME2 = 'TxTestZip2.zip';

describe('Testing Transactions Functionality', () => {
    let NOTIFICATIONSERVICE: NotificationService;
    let ACCOUNTSPAGE: AccountsPage;
    let CREATEACCOUNTSPAGE: CreateAccountsPage;
    let ACCOUNTDETAILSPAGE: AccountDetailsPage;
    let LOGINPAGE: LoginPage;
    let REGISTERPAGE: RegisterPage;
    let CUSTOMERSPAGE: CustomersPage;
    let APPBAR: AppBar;
    let ASSIGNROLEPAGE: AssignRolesPage;
    let INTRABANKPAYMENTPAGE: IntrabankPaymentsPage;
    let CREATECUSTOMERPAGE: CreateCustomerPage;
    let CREATERECURRINGPAYMENTPAGE: CreateRecurringPaymentPage;
    let TRANSACTIONSPAGE: TransactionsPage;
    let TRANSACTIONDETAILSPAGE: TransactionDetailsPage;

    let today = new Date();
    let testCustomerName1 = titleCase('Test Tramsaction1' + today.toString().replace(/ /g, ''));
    let contactNumberOnCreation1 = '3531111111';
    let emailAddressOnCreation1 = 'testcustomer1@email.com';
    let postCodeOnCreation1 = 'A2B00001';
    let customerUserName1 = 'useraccounttest1' + today.toString().replace(/ /g, '');

    let testCustomerName2 = titleCase('Test Tramsaction2' + today.toString().replace(/ /g, ''));
    let contactNumberOnCreation2 = '3531111111';
    let emailAddressOnCreation2 = 'testcustomer2@email.com';
    let postCodeOnCreation2 = 'A2B00002';
    let customerUserName2 = 'useraccounttest2' + today.toString().replace(/ /g, '');

    let customerPassword = 'password1!';
    let adminUsername = 'admin';
    let adminPassword = 'password1!';
    let depositAmount = '6000';

    beforeAll(async () => {
        driver = await new Builder().forBrowser('chrome').build();
        NOTIFICATIONSERVICE = new NotificationService(driver);
        ACCOUNTSPAGE = new AccountsPage(driver);
        CREATEACCOUNTSPAGE = new CreateAccountsPage(driver);
        ACCOUNTDETAILSPAGE = new AccountDetailsPage(driver);
        LOGINPAGE = new LoginPage(driver);
        REGISTERPAGE = new RegisterPage(driver);
        CUSTOMERSPAGE = new CustomersPage(driver);
        APPBAR = new AppBar(driver);
        ASSIGNROLEPAGE = new AssignRolesPage(driver);
        CREATECUSTOMERPAGE = new CreateCustomerPage(driver);
        INTRABANKPAYMENTPAGE = new IntrabankPaymentsPage(driver);
        CREATERECURRINGPAYMENTPAGE = new CreateRecurringPaymentPage(driver);
        TRANSACTIONSPAGE = new TransactionsPage(driver);
        TRANSACTIONDETAILSPAGE = new TransactionDetailsPage(driver);
    });

    afterAll(async () => {
        driver.quit();
        fs.unlinkSync(`${TESTFILEDIR}/${FILENAME1}`);
        fs.unlinkSync(`${TESTFILEDIR}/${FILENAME2}`);
    });

    const customerLogsIn = async (customerUserName) => {
        await LOGINPAGE.goToPage();
        await LOGINPAGE.loginUser(customerUserName, customerPassword);
        let notificationExpected = 'Successfully logged in.';
        let containsNotification = await NOTIFICATIONSERVICE.containsSuccessfulNotification(notificationExpected);
        expect(containsNotification).toEqual(true);
    };

    const adminLogsIn = async () => {
        await LOGINPAGE.goToPage();
        await LOGINPAGE.loginUser('admin', 'password1!');
        const notificationExpected = 'Successfully logged in.';
        const containsNotification = await NOTIFICATIONSERVICE.containsSuccessfulNotification(notificationExpected);
        expect(containsNotification).toEqual(true);
    };

    retry('Log in Admin', async () => {
        await adminLogsIn();
    });

    let customerIdPostCreation1 = '';

    retry('Admin creates customer 1 accounts', async () => {
        await CREATECUSTOMERPAGE.gotToPage();
        await CREATECUSTOMERPAGE.createCustomer(
            testCustomerName1,
            contactNumberOnCreation1,
            emailAddressOnCreation1,
            postCodeOnCreation1,
            FILENAME1
        );
        const notificationExpected = 'Customer created successfully.';
        const containsNotification = await NOTIFICATIONSERVICE.containsSuccessfulNotification(notificationExpected);
        expect(containsNotification).toEqual(true);
        await CUSTOMERSPAGE.goToPage();
        await CUSTOMERSPAGE.inputSearch(testCustomerName1);
        await CUSTOMERSPAGE.clickCustomerRowInTable(testCustomerName1);
        customerIdPostCreation1 = await new UpdateCustomerPage(driver).getCustomerId();
    });

    let customerIdPostCreation2 = '';

    retry('Admin creates customer 2 account ', async () => {
        await CREATECUSTOMERPAGE.gotToPage();
        await CREATECUSTOMERPAGE.createCustomer(
            testCustomerName2,
            contactNumberOnCreation2,
            emailAddressOnCreation2,
            postCodeOnCreation2,
            FILENAME2
        );
        const notificationExpected = 'Customer created successfully.';
        const containsNotification = await NOTIFICATIONSERVICE.containsSuccessfulNotification(notificationExpected);
        expect(containsNotification).toEqual(true);
        await CUSTOMERSPAGE.goToPage();
        await CUSTOMERSPAGE.inputSearch(testCustomerName2);
        await CUSTOMERSPAGE.clickCustomerRowInTable(testCustomerName2);
        customerIdPostCreation2 = await new UpdateCustomerPage(driver).getCustomerId();
    });

    retry('Admin Logs out ', async () => {
        await APPBAR.logout();
    });

    retry('Customer 1 can Register and logs out', async () => {
        //give server time to update no way to avoid this wait
        await driver.sleep(5000);
        await REGISTERPAGE.goToPage();
        await REGISTERPAGE.registerCustomer(
            customerUserName1,
            customerPassword,
            emailAddressOnCreation1,
            customerIdPostCreation1,
            FILENAME1
        );
        const notificationExpected = 'Successfully registered. You can now log in.';
        const containsNotification = await NOTIFICATIONSERVICE.containsSuccessfulNotification(notificationExpected);
        expect(containsNotification).toEqual(true);
        await APPBAR.logout();
    });

    retry('Customer 2 can Register and logs out', async () => {
        //give server time to update no way to avoid this wait
        await REGISTERPAGE.goToPage();
        await REGISTERPAGE.registerCustomer(
            customerUserName2,
            customerPassword,
            emailAddressOnCreation2,
            customerIdPostCreation2,
            FILENAME2
        );
        const notificationExpected = 'Successfully registered. You can now log in.';
        const containsNotification = await NOTIFICATIONSERVICE.containsSuccessfulNotification(notificationExpected);
        expect(containsNotification).toEqual(true);
        await APPBAR.logout();
    });

    retry('Admin Logs in and assigns customer roles and logs out', async () => {
        await LOGINPAGE.goToPage();
        await LOGINPAGE.loginUser(adminUsername, adminPassword);
        let notificationExpected = 'Successfully logged in.';
        let containsNotification = await NOTIFICATIONSERVICE.containsSuccessfulNotification(notificationExpected);
        expect(containsNotification).toEqual(true);
        await ASSIGNROLEPAGE.goToPage();
        await ASSIGNROLEPAGE.assignRole(customerUserName1, 'CUSTOMER');
        notificationExpected = 'Successfully assigned role to user';
        containsNotification = await NOTIFICATIONSERVICE.containsSuccessfulNotification(notificationExpected);
        expect(containsNotification).toEqual(true);
        await ASSIGNROLEPAGE.goToPage();
        await ASSIGNROLEPAGE.assignRole(customerUserName2, 'CUSTOMER');
        notificationExpected = 'Successfully assigned role to user';
        containsNotification = await NOTIFICATIONSERVICE.containsSuccessfulNotification(notificationExpected);
        expect(containsNotification).toEqual(true);
    });

    retry('Admin creates current account and savings accounts for each customer and logs out', async () => {
        //customer 1 curretn + savings account
        await ACCOUNTSPAGE.goToPage();
        await ACCOUNTSPAGE.navigateToCreateNewPage();
        await CREATEACCOUNTSPAGE.createCurrentAccountNoLimits(testCustomerName1);
        let notificationExpected = 'Account created successfully';
        let containsNotification = await NOTIFICATIONSERVICE.containsSuccessfulNotification(notificationExpected);
        expect(containsNotification).toEqual(true);

        await ACCOUNTSPAGE.goToPage();
        await ACCOUNTSPAGE.navigateToCreateNewPage();
        await CREATEACCOUNTSPAGE.createSavingsAccount(testCustomerName1);
        notificationExpected = 'Account created successfully';
        containsNotification = await NOTIFICATIONSERVICE.containsSuccessfulNotification(notificationExpected);
        expect(containsNotification).toEqual(true);

        //customer 2 current + savings account
        await ACCOUNTSPAGE.goToPage();
        await ACCOUNTSPAGE.navigateToCreateNewPage();
        await CREATEACCOUNTSPAGE.createCurrentAccount(testCustomerName2);
        notificationExpected = 'Account created successfully';
        containsNotification = await NOTIFICATIONSERVICE.containsSuccessfulNotification(notificationExpected);
        expect(containsNotification).toEqual(true);

        await ACCOUNTSPAGE.goToPage();
        await ACCOUNTSPAGE.navigateToCreateNewPage();
        await CREATEACCOUNTSPAGE.createSavingsAccount(testCustomerName2);
        notificationExpected = 'Account created successfully';
        containsNotification = await NOTIFICATIONSERVICE.containsSuccessfulNotification(notificationExpected);
        expect(containsNotification).toEqual(true);
    });

    let customer1CurrentAccountId = '';
    let customer2CurrentAccountId = '';
    let customer1SavingsAccountId = '';
    let customer1LoanAccountId = '';
    //This test will be refactored once all of the tabs on the account details page are implemented with cancel buttons etc
    retry('Admin makes Customers accounts active', async () => {
        await ACCOUNTSPAGE.goToPage();
        await (await getElementByCssQuery('.searchField>div>input', driver)).sendKeys(testCustomerName1);
        let custDataFromTable1 = await ACCOUNTSPAGE.selectAccount('CURRENT');
        customer1CurrentAccountId = custDataFromTable1.accountKey;
        await ACCOUNTDETAILSPAGE.goToSetStatusPage();
        await ACCOUNTDETAILSPAGE.makeAccountActive();

        await ACCOUNTSPAGE.goToPage();
        await (await getElementByCssQuery('.searchField>div>input', driver)).sendKeys(testCustomerName1);
        custDataFromTable1 = await ACCOUNTSPAGE.selectAccount('SAVINGS');
        customer1SavingsAccountId = custDataFromTable1.accountKey;
        await ACCOUNTDETAILSPAGE.goToSetStatusPage();
        await ACCOUNTDETAILSPAGE.makeAccountActive();

        await ACCOUNTSPAGE.goToPage();
        await (await getElementByCssQuery('.searchField>div>input', driver)).sendKeys(testCustomerName2);
        const custDataFromTable2 = await ACCOUNTSPAGE.selectAccount('CURRENT');
        customer2CurrentAccountId = custDataFromTable2.accountKey;
        await ACCOUNTDETAILSPAGE.goToSetStatusPage();
        await ACCOUNTDETAILSPAGE.makeAccountActive();
    });

    retry('Admin deposits money + issues loan for both current accounts and logs out', async () => {
        await ACCOUNTSPAGE.goToPage();
        await (await getElementByCssQuery('.searchField>div>input', driver)).sendKeys(testCustomerName1);
        await ACCOUNTSPAGE.selectAccount('CURRENT');
        await ACCOUNTDETAILSPAGE.goToDepositPage();
        await ACCOUNTDETAILSPAGE.depostFiat(depositAmount);
        let notificationExpected = 'Deposit was successful';
        let containsNotification = await NOTIFICATIONSERVICE.containsSuccessfulNotification(notificationExpected);
        expect(containsNotification).toEqual(true);

        await ACCOUNTDETAILSPAGE.goToIssueLoanPage();
        await ACCOUNTDETAILSPAGE.issueLoan(depositAmount);
        containsNotification = await NOTIFICATIONSERVICE.containsSuccessfulNotification(notificationExpected);
        expect(containsNotification).toEqual(true);
        notificationExpected = 'Successfully issued loan for this account';
        containsNotification = await NOTIFICATIONSERVICE.containsSuccessfulNotification(notificationExpected);
        expect(containsNotification).toEqual(true);

        await ACCOUNTSPAGE.goToPage();
        await (await getElementByCssQuery('.searchField>div>input', driver)).sendKeys(testCustomerName2);
        await ACCOUNTSPAGE.selectAccount('CURRENT');
        await ACCOUNTDETAILSPAGE.goToDepositPage();
        await ACCOUNTDETAILSPAGE.depostFiat(depositAmount);
        notificationExpected = 'Deposit was successful';
        containsNotification = await NOTIFICATIONSERVICE.containsSuccessfulNotification(notificationExpected);
        expect(containsNotification).toEqual(true);

        await ACCOUNTDETAILSPAGE.goToIssueLoanPage();
        await ACCOUNTDETAILSPAGE.issueLoan(depositAmount);
        containsNotification = await NOTIFICATIONSERVICE.containsSuccessfulNotification(notificationExpected);
        expect(containsNotification).toEqual(true);
        notificationExpected = 'Successfully issued loan for this account';
        containsNotification = await NOTIFICATIONSERVICE.containsSuccessfulNotification(notificationExpected);
        expect(containsNotification).toEqual(true);
    });

    let paymentAmount = '1000';

    retry('Customer makes successfull intrabank payment to customer 2s current account', async () => {
        await customerLogsIn(customerUserName1);
        INTRABANKPAYMENTPAGE.goToPage();
        INTRABANKPAYMENTPAGE.makeQuickPayment(customer2CurrentAccountId, paymentAmount);
        let notificationExpected = 'Intrabank payment successful';
        let containsNotification = await NOTIFICATIONSERVICE.containsSuccessfulNotification(notificationExpected);
        expect(containsNotification).toEqual(true);
    });

    retry('Customer verifies transaction is vibisible on the Accounts transactions tab page', async () => {
        await ACCOUNTSPAGE.goToPage();
        await ACCOUNTSPAGE.inputSearch(testCustomerName1);
        await ACCOUNTSPAGE.selectAccount('CURRENT');
        //need explicit wait as server does not show transaction immediatley sometimes
        await driver.sleep(2000);
        await ACCOUNTDETAILSPAGE.goToTransactionsTab();
        await TRANSACTIONSPAGE.loadTable();
        const transactionDataFromTable = await TRANSACTIONSPAGE.selectTransaction(
            customer1CurrentAccountId,
            customer2CurrentAccountId
        );
        const applink = await getElementById('header', driver);
        const actualHeaderText = await applink.getText();
        const expectedHeaderText = 'Transaction';
        expect(actualHeaderText).toEqual(expectedHeaderText);
        await driver.sleep(1000);
        const verifyTxVisible = await TRANSACTIONDETAILSPAGE.confirmTransactionDatavisibleCorrectly(
            transactionDataFromTable
        );
        expect(verifyTxVisible).toEqual(true);
        TRANSACTIONDETAILSPAGE.goToAccountsTab();
        await driver.sleep(1000);
        const verifyAccountVisible = await TRANSACTIONDETAILSPAGE.confirmAccountsDataRenderedCorrectly(
            transactionDataFromTable.accountFrom,
            transactionDataFromTable.accountTo,
            'CURRENT',
            testCustomerName1,
            testCustomerName2
        );
        expect(verifyAccountVisible).toEqual(true);
    });

    retry('Customer verifies transaction is vibisible on the transactions page', async () => {
        await TRANSACTIONSPAGE.goToPage();
        await TRANSACTIONSPAGE.inputSearch(testCustomerName1);
        const transactionDataFromTable = await TRANSACTIONSPAGE.selectTransaction(
            customer1CurrentAccountId,
            customer2CurrentAccountId
        );
        const applink = await getElementById('header', driver);
        const actualHeaderText = await applink.getText();
        const expectedHeaderText = 'Transaction';
        expect(actualHeaderText).toEqual(expectedHeaderText);
        await driver.sleep(1000);
        const verifyTxVisible = await TRANSACTIONDETAILSPAGE.confirmTransactionDatavisibleCorrectly(
            transactionDataFromTable
        );
        expect(verifyTxVisible).toEqual(true);
        TRANSACTIONDETAILSPAGE.goToAccountsTab();
        await driver.sleep(1000);
        const verifyAccountVisible = await TRANSACTIONDETAILSPAGE.confirmAccountsDataRenderedCorrectly(
            transactionDataFromTable.accountFrom,
            transactionDataFromTable.accountTo,
            'CURRENT',
            testCustomerName1,
            testCustomerName2
        );
        expect(verifyAccountVisible).toEqual(true);
    });

    retry('Customer makes successfull intrabank payment to own savings account', async () => {
        INTRABANKPAYMENTPAGE.goToPage();
        INTRABANKPAYMENTPAGE.makeQuickPayment(customer1SavingsAccountId, paymentAmount);
        let notificationExpected = 'Intrabank payment successful';
        let containsNotification = await NOTIFICATIONSERVICE.containsSuccessfulNotification(notificationExpected);
        expect(containsNotification).toEqual(true);
    });

    retry('Customer makes successfull intrabank payment to own loan account', async () => {
        await ACCOUNTSPAGE.goToPage();
        await ACCOUNTSPAGE.inputSearch(testCustomerName1);
        let loanAccountData = await ACCOUNTSPAGE.selectAccount('LOAN');
        customer1LoanAccountId = loanAccountData.accountKey;
        INTRABANKPAYMENTPAGE.goToPage();
        INTRABANKPAYMENTPAGE.makeQuickPayment(customer1LoanAccountId, paymentAmount);
        let notificationExpected = 'Intrabank payment successful';
        let containsNotification = await NOTIFICATIONSERVICE.containsSuccessfulNotification(notificationExpected);
        expect(containsNotification).toEqual(true);
    });

    retry('Customer1 creates recurring payment to Customer2', async () => {
        await CREATERECURRINGPAYMENTPAGE.goToPage();
        await CREATERECURRINGPAYMENTPAGE.makeQuickRecurringPaymentPayment(
            customer2CurrentAccountId,
            paymentAmount,
            '1',
            '10'
        );
        let notificationExpected = 'Recurring payment set up successfully';
        let containsNotification = await NOTIFICATIONSERVICE.containsSuccessfulNotification(notificationExpected);
        expect(containsNotification).toEqual(true);
    });

    retry('Customer1 creates recurring payment to own savings account', async () => {
        await CREATERECURRINGPAYMENTPAGE.goToPage();
        await CREATERECURRINGPAYMENTPAGE.makeQuickRecurringPaymentPayment(
            customer1SavingsAccountId,
            paymentAmount,
            '1',
            '10'
        );
        let notificationExpected = 'Recurring payment set up successfully';
        let containsNotification = await NOTIFICATIONSERVICE.containsSuccessfulNotification(notificationExpected);
        expect(containsNotification).toEqual(true);
    });

    retry('Customer1 creates recurring payment to own loan account', async () => {
        await CREATERECURRINGPAYMENTPAGE.goToPage();
        await CREATERECURRINGPAYMENTPAGE.makeQuickRecurringPaymentPayment(
            customer1LoanAccountId,
            paymentAmount,
            '1',
            '10'
        );
        let notificationExpected = 'Recurring payment set up successfully';
        let containsNotification = await NOTIFICATIONSERVICE.containsSuccessfulNotification(notificationExpected);
        expect(containsNotification).toEqual(true);
    });
});
