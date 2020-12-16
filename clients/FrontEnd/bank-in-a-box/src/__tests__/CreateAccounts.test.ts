import 'selenium-webdriver/chrome';
import 'selenium-webdriver/firefox';
import 'chromedriver';
import 'geckodriver';

import {
    AccountDetailsPage,
    AccountsPage,
    AppBar,
    ApproveOverdraftPage,
    AssignRolesPage,
    CreateAccountsPage,
    CreateCustomerPage,
    CustomersPage,
    LoginPage,
    NotificationService,
    RegisterPage,
    UpdateCustomerPage,
} from '../testUtils/testPageComponents';
import { Builder, By, WebDriver } from 'selenium-webdriver';
import {
    ROOTURL,
    TESTFILEDIR,
    getElementByCssQuery,
    getElementById,
    getElementByXPath,
} from '../testUtils/testHelpers';

import { AccountType } from '../store/types';
import fs from 'fs';
import retry from 'jest-retries';
import { titleCase } from '../utils/Utils';

let driver: WebDriver;
jasmine.DEFAULT_TIMEOUT_INTERVAL = 1000 * 60 * 5;

const ACCOUNTTESTFILENAME = 'AccountsTestZip.zip';

describe('Testing Accounts Functionality', () => {
    let APPROVEOVERDRAFTPAGE: ApproveOverdraftPage;
    let NOTIFICATIONSERVICE: NotificationService;
    let ACCOUNTSPAGE: AccountsPage;
    let CREATEACCOUNTSPAGE: CreateAccountsPage;
    let ACCOUNTDETAILSPAGE: AccountDetailsPage;
    let LOGINPAGE: LoginPage;
    let REGISTERPAGE: RegisterPage;
    let CUSTOMERSPAGE: CustomersPage;
    let APPBAR: AppBar;
    let ASSIGNROLEPAGE: AssignRolesPage;

    let today = new Date();
    let testCustomerName = titleCase('Test Account' + today.toString().replace(/ /g, ''));
    let contactNumberOnCreation = '3531111111';
    let emailAddressOnCreation = 'testcustomer@email.com';
    let postCodeOnCreation = 'A2B0000';
    let customerUserName = 'useraccounttest' + today.toString().replace(/ /g, '');
    let customerPassword = 'password1!';
    let adminUsername = 'admin';
    let adminPassword = 'password1!';
    let currentAccountId = '';
    let savingsAccountId = '';
    let loanAccountId = '';

    beforeAll(async () => {
        driver = await new Builder().forBrowser('chrome').build();
        NOTIFICATIONSERVICE = new NotificationService(driver);
        APPROVEOVERDRAFTPAGE = new ApproveOverdraftPage(driver);
        ACCOUNTSPAGE = new AccountsPage(driver);
        CREATEACCOUNTSPAGE = new CreateAccountsPage(driver);
        ACCOUNTDETAILSPAGE = new AccountDetailsPage(driver);
        LOGINPAGE = new LoginPage(driver);
        REGISTERPAGE = new RegisterPage(driver);
        CUSTOMERSPAGE = new CustomersPage(driver);
        APPBAR = new AppBar(driver);
        ASSIGNROLEPAGE = new AssignRolesPage(driver);
    });

    afterAll(async () => {
        driver.quit();
        fs.unlinkSync(`${TESTFILEDIR}/${ACCOUNTTESTFILENAME}`);
    });

    const checkCurrentAccountViewable = async () => {
        await ACCOUNTSPAGE.goToPage();
        await ACCOUNTSPAGE.inputSearch(testCustomerName);
        const custDataFromTable = await ACCOUNTSPAGE.selectAccount('CURRENT');
        currentAccountId = custDataFromTable.accountKey;
        const applink = await getElementById('header', driver);
        const actualHeaderText = await applink.getText();
        const expectedHeaderText = 'Account';
        expect(actualHeaderText).toEqual(expectedHeaderText);
        const verifyDataVisible = await ACCOUNTDETAILSPAGE.confirmAccountDataRenderedCorrectly(custDataFromTable);
        expect(verifyDataVisible).toEqual(true);
    };

    const checkCustomerDetailsForAccount = async (accountType: AccountType) => {
        await ACCOUNTSPAGE.goToPage();
        await ACCOUNTSPAGE.inputSearch(testCustomerName);
        const custDataFromTable = await ACCOUNTSPAGE.selectAccount(accountType);
        const applink = await getElementById('header', driver);
        const actualHeaderText = await applink.getText();
        const expectedHeaderText = 'Account';
        expect(actualHeaderText).toEqual(expectedHeaderText);
        const verifyDataVisible = await ACCOUNTDETAILSPAGE.confirmAccountDataRenderedCorrectly(custDataFromTable);
        expect(verifyDataVisible).toEqual(true);
        await ACCOUNTDETAILSPAGE.goToCustomerTab();
        const verifyCustomerDataVisible = await ACCOUNTDETAILSPAGE.confirmCustomerDetailsCorrect(
            testCustomerName,
            contactNumberOnCreation,
            emailAddressOnCreation,
            postCodeOnCreation
        );
        expect(verifyCustomerDataVisible).toEqual(true);
    };

    const checkSavingsAccountViewable = async () => {
        await ACCOUNTSPAGE.goToPage();
        await ACCOUNTSPAGE.inputSearch(testCustomerName);
        const custDataFromTable = await ACCOUNTSPAGE.selectAccount('SAVINGS');
        savingsAccountId = custDataFromTable.accountKey;
        const applink = await getElementById('header', driver);
        const actualHeaderText = await applink.getText();
        const expectedHeaderText = 'Account';
        expect(actualHeaderText).toEqual(expectedHeaderText);
        const verifyDataVisible = await ACCOUNTDETAILSPAGE.confirmAccountDataRenderedCorrectly(custDataFromTable);
        expect(verifyDataVisible).toEqual(true);
    };

    const checkLoanAccountViewable = async () => {
        await ACCOUNTSPAGE.goToPage();
        await ACCOUNTSPAGE.inputSearch(testCustomerName);
        const custDataFromTable = await ACCOUNTSPAGE.selectAccount('LOAN');
        loanAccountId = custDataFromTable.accountKey;
        const applink = await getElementById('header', driver);
        const actualHeaderText = await applink.getText();
        const expectedHeaderText = 'Account';
        expect(actualHeaderText).toEqual(expectedHeaderText);
        const verifyDataVisible = await ACCOUNTDETAILSPAGE.confirmAccountDataRenderedCorrectly(custDataFromTable);
        expect(verifyDataVisible).toEqual(true);
    };

    retry('Log in Admin', async () => {
        await LOGINPAGE.goToPage();
        await LOGINPAGE.loginUser('admin', 'password1!');
        const notificationExpected = 'Successfully logged in.';
        const containsNotification = await NOTIFICATIONSERVICE.containsSuccessfulNotification(notificationExpected);
        expect(containsNotification).toEqual(true);
    });

    retry('Intialize page and check to make sure that the header is present', async () => {
        await ACCOUNTSPAGE.goToPage();
        const applink = await getElementById('header', driver);
        const actual = await applink.getText();
        const expected = 'Accounts';
        expect(actual).toEqual(expected);
    });

    let customerIdPostCreation = '';

    retry('Creating a new customer for testing accounts on', async () => {
        const CREATECUSTOMERPAGE = new CreateCustomerPage(driver);
        await CREATECUSTOMERPAGE.gotToPage();
        await CREATECUSTOMERPAGE.createCustomer(
            testCustomerName,
            contactNumberOnCreation,
            emailAddressOnCreation,
            postCodeOnCreation,
            ACCOUNTTESTFILENAME
        );
        const notificationExpected = 'Customer created successfully.';
        const containsNotification = await NOTIFICATIONSERVICE.containsSuccessfulNotification(notificationExpected);
        expect(containsNotification).toEqual(true);
        await CUSTOMERSPAGE.goToPage();
        await CUSTOMERSPAGE.inputSearch(testCustomerName);
        await CUSTOMERSPAGE.clickCustomerRowInTable(testCustomerName);
        customerIdPostCreation = await new UpdateCustomerPage(driver).getCustomerId();
        await APPBAR.logout();
    });

    retry('Customer can Register', async () => {
        await driver.get(ROOTURL);
        //give server time to update no way to avoid this wait
        await driver.sleep(5000);
        await REGISTERPAGE.goToPage();
        await REGISTERPAGE.registerCustomer(
            customerUserName,
            customerPassword,
            emailAddressOnCreation,
            customerIdPostCreation,
            ACCOUNTTESTFILENAME
        );
        const notificationExpected = 'Successfully registered. You can now log in.';
        const containsNotification = await NOTIFICATIONSERVICE.containsSuccessfulNotification(notificationExpected);
        expect(containsNotification).toEqual(true);
        await APPBAR.logout();
    });

    retry('Admin Logs in and assigns customer role', async () => {
        await driver.get(ROOTURL);
        await LOGINPAGE.goToPage();
        await LOGINPAGE.loginUser(adminUsername, adminPassword);
        let notificationExpected = 'Successfully logged in.';
        let containsNotification = await NOTIFICATIONSERVICE.containsSuccessfulNotification(notificationExpected);
        expect(containsNotification).toEqual(true);
        await ASSIGNROLEPAGE.goToPage();
        await ASSIGNROLEPAGE.assignRole(customerUserName, 'CUSTOMER');
        notificationExpected = 'Successfully assigned role to user';
        containsNotification = await NOTIFICATIONSERVICE.containsSuccessfulNotification(notificationExpected);
        expect(containsNotification).toEqual(true);
    });

    retry('Creating Current Account Is Possible', async () => {
        await ACCOUNTSPAGE.goToPage();
        await ACCOUNTSPAGE.navigateToCreateNewPage();
        await CREATEACCOUNTSPAGE.createCurrentAccount(testCustomerName);
        const notificationExpected = 'Account created successfully';
        const containsNotification = await NOTIFICATIONSERVICE.containsSuccessfulNotification(notificationExpected);
        expect(containsNotification).toEqual(true);
    });

    retry('Creating Savings Account Is Possible', async () => {
        await ACCOUNTSPAGE.goToPage();
        await ACCOUNTSPAGE.navigateToCreateNewPage();
        await CREATEACCOUNTSPAGE.createSavingsAccount(testCustomerName);
        const notificationExpected = 'Account created successfully';
        const containsNotification = await NOTIFICATIONSERVICE.containsSuccessfulNotification(notificationExpected);
        expect(containsNotification).toEqual(true);
    });

    retry('Check if Currnet Account is Viewable', async () => {
        await checkCurrentAccountViewable();
    });

    retry('Check if Savings Account is Viewable', async () => {
        await checkSavingsAccountViewable();
    });

    //This test will be refactored once all of the tabs on the account details page are implemented with cancel buttons etc
    retry('Navigate to Current Account', async () => {
        await driver.get(`${ROOTURL}accounts`);
        await (await getElementByCssQuery('.searchField>div>input', driver)).sendKeys(testCustomerName);
        const tableBody = await getElementByXPath(`//td[contains(text(),'CURRENT')]`, driver);
        const tr = await tableBody.findElement(By.xpath('./..'));
        await tr.click();
        let header = await getElementById('header', driver);
        let actualHeaderText = await header.getText();
        let expectedHeaderText = 'Account';
        expect(actualHeaderText).toEqual(expectedHeaderText);
    });

    retry('Check if Can Cancel Overdraft Approval', async () => {
        await ACCOUNTDETAILSPAGE.goToApproveOverDraftPage();
        await APPROVEOVERDRAFTPAGE.cancelOverDraftApproval();
        const header = await getElementById('header', driver);
        const actualHeaderText = await header.getText();
        const expectedHeaderText = 'Account';
        expect(actualHeaderText).toEqual(expectedHeaderText);
    });

    retry('Check if Can Approve Overdraft', async () => {
        await ACCOUNTDETAILSPAGE.goToApproveOverDraftPage();
        await APPROVEOVERDRAFTPAGE.inputOverdraftAmount('1000');
        await APPROVEOVERDRAFTPAGE.submitOverdraft();
        const notificationExpected = 'Successfully approved overdraft for this account.';
        const containsNotification = await NOTIFICATIONSERVICE.containsSuccessfulNotification(notificationExpected);
        expect(containsNotification).toEqual(true);
    });

    retry('Check if Can Set Status SUSPENDED', async () => {
        await ACCOUNTDETAILSPAGE.goToSetStatusPage();
        await (await getElementByCssQuery('.accountStatusSelect', driver)).click();
        await (await getElementByXPath(`//li[text()="SUSPENDED"]`, driver)).click();
        await (await getElementByCssQuery('.submitButton', driver)).click();
        const notificationExpected = 'Successfully set status to SUSPENDED';
        const containsNotification = await NOTIFICATIONSERVICE.containsSuccessfulNotification(notificationExpected);
        expect(containsNotification).toEqual(true);
    });

    retry('Check if Can Set Status ACTIVE', async () => {
        await ACCOUNTDETAILSPAGE.goToSetStatusPage();
        await (await getElementByCssQuery('.accountStatusSelect', driver)).click();
        await (await getElementByXPath(`//li[text()="ACTIVE"]`, driver)).click();
        await (await getElementByCssQuery('.submitButton', driver)).click();
        const notificationExpected = 'Successfully set status to ACTIVE';
        const containsNotification = await NOTIFICATIONSERVICE.containsSuccessfulNotification(notificationExpected);
        expect(containsNotification).toEqual(true);
    });

    retry('Check if Can Set Limits', async () => {
        await ACCOUNTDETAILSPAGE.goToSetLimitsPage();
        const withdrawalDailyLimitInput = await (
            await getElementByCssQuery('.withdrawalDailyLimitInput>div>input', driver)
        ).sendKeys('4000');
        const transferDailyLimitInput = await (
            await getElementByCssQuery('.transferDailyLimitInput>div>input', driver)
        ).sendKeys('4000');
        await (await getElementByCssQuery('.submitButton', driver)).click();
        const notificationExpected = 'Successfully set account limits';
        const containsNotification = await NOTIFICATIONSERVICE.containsSuccessfulNotification(notificationExpected);
        expect(containsNotification).toEqual(true);
    });

    const transactionAmount = '2000';

    retry('Check if Can Issue Loan', async () => {
        await ACCOUNTDETAILSPAGE.goToIssueLoanPage();
        await (await getElementByCssQuery('.loanAmountInput>div>input', driver)).sendKeys(transactionAmount);
        await (await getElementByCssQuery('.loanPeriodInput>div>input', driver)).sendKeys('2');
        await (await getElementByCssQuery('.submitButton', driver)).click();
        const notificationExpected = 'Successfully issued loan for this account';
        const containsNotification = await NOTIFICATIONSERVICE.containsSuccessfulNotification(notificationExpected);
        expect(containsNotification).toEqual(true);
    });

    retry('Check if Can Deposit Amount', async () => {
        await ACCOUNTDETAILSPAGE.goToDepositPage();
        await (await getElementByCssQuery('.depositAmountInput>div>input', driver)).sendKeys(transactionAmount);
        await (await getElementByCssQuery('.currencyInput', driver)).click();
        await (await getElementByXPath(`//li[text()="EUR"]`, driver)).click();
        await (await getElementByCssQuery('.submitButton', driver)).click();
        const notificationExpected = 'Deposit was successful';
        const containsNotification = await NOTIFICATIONSERVICE.containsSuccessfulNotification(notificationExpected);
        expect(containsNotification).toEqual(true);
    });

    //Current account at this point should have 4000 balance

    retry('Check if Can Withdraw Amount', async () => {
        await ACCOUNTDETAILSPAGE.goToWithdrawPage();
        await (await getElementByCssQuery('.withdrawAmountInput>div>input', driver)).sendKeys(transactionAmount);
        await (await getElementByCssQuery('.currencyInput', driver)).click();
        await (await getElementByXPath(`//li[text()="EUR"]`, driver)).click();
        await (await getElementByCssQuery('.submitButton', driver)).click();
        const notificationExpected = 'Withdrawal was successful';
        const containsNotification = await NOTIFICATIONSERVICE.containsSuccessfulNotification(notificationExpected);
        expect(containsNotification).toEqual(true);
    });

    //Current account at this point should have 2000 balance + 1000 over draft

    let overDraftWithdrawAmount = '3000';

    retry('Check if Overdraft Withdraw Works', async () => {
        await ACCOUNTDETAILSPAGE.goToWithdrawPage();
        await (await getElementByCssQuery('.withdrawAmountInput>div>input', driver)).sendKeys(overDraftWithdrawAmount);
        await (await getElementByCssQuery('.currencyInput', driver)).click();
        await (await getElementByXPath(`//li[text()="EUR"]`, driver)).click();
        await (await getElementByCssQuery('.submitButton', driver)).click();
        const notificationExpected = 'Withdrawal was successful';
        const containsNotification = await NOTIFICATIONSERVICE.containsSuccessfulNotification(notificationExpected);
        expect(containsNotification).toEqual(true);
    });

    //Current account should have 0 balance at this point

    retry('Check if Further withdrawals fail due to insufficient funds', async () => {
        await ACCOUNTDETAILSPAGE.goToWithdrawPage();
        await (await getElementByCssQuery('.withdrawAmountInput>div>input', driver)).sendKeys('3000');
        await (await getElementByCssQuery('.currencyInput', driver)).click();
        await (await getElementByXPath(`//li[text()="EUR"]`, driver)).click();
        await (await getElementByCssQuery('.submitButton', driver)).click();
        const notificationExpected =
            'This account has insufficient funds to complete this transaction, please check the balance and try again.';
        const containsNotification = await NOTIFICATIONSERVICE.containsErrorNotificaiton(notificationExpected);
        expect(containsNotification).toEqual(true);
        await (await getElementByCssQuery('.cancelButton', driver)).click();
    });

    retry('Check if Issuing loan fails due to account being in overdraft', async () => {
        await ACCOUNTDETAILSPAGE.goToIssueLoanPage();
        await (await getElementByCssQuery('.loanAmountInput>div>input', driver)).sendKeys(transactionAmount);
        await (await getElementByCssQuery('.loanPeriodInput>div>input', driver)).sendKeys('2');
        await (await getElementByCssQuery('.submitButton', driver)).click();
        const notificationExpected =
            'Error issuing loan. Error: Cannot issue loan as account is currently in deficit, possibly in overdraft.';
        const containsNotification = await NOTIFICATIONSERVICE.containsErrorNotificaiton(notificationExpected);
        expect(containsNotification).toEqual(true);
    });

    retry('Check if Loan Account is Viewable', async () => {
        await checkLoanAccountViewable();
    });

    retry('Check if can see customer details for savings account type', async () => {
        await checkCustomerDetailsForAccount('SAVINGS');
    });

    retry('Check if can see customer details for current account type', async () => {
        await checkCustomerDetailsForAccount('CURRENT');
    });

    retry('Check if can see customer details for loan account type', async () => {
        await checkCustomerDetailsForAccount('LOAN');
    });

    retry('Customer Logs in', async () => {
        await driver.get(ROOTURL);
        //Give server time to update
        await driver.sleep(2000);
        await LOGINPAGE.goToPage();
        await LOGINPAGE.loginUser(customerUserName, customerPassword);
        let notificationExpected = 'Successfully logged in.';
        let containsNotification = await NOTIFICATIONSERVICE.containsSuccessfulNotification(notificationExpected);
        expect(containsNotification).toEqual(true);
    });

    retry('Customer Checks if Currnet Account is Viewable', async () => {
        await checkCurrentAccountViewable();
    });

    retry('Customer Checks if Savings Account is Viewable', async () => {
        await checkSavingsAccountViewable();
    });

    retry('Customer Checks if Loan Account is Viewable', async () => {
        await checkLoanAccountViewable();
    });

    retry('Customer Checks if can see customer details for savings account type', async () => {
        await checkCustomerDetailsForAccount('SAVINGS');
    });

    retry('Customer Checks if can see customer details for current account type', async () => {
        await checkCustomerDetailsForAccount('CURRENT');
    });

    retry('Customer Checks if can see customer details for loan account type', async () => {
        await checkCustomerDetailsForAccount('LOAN');
    });
});
