import 'selenium-webdriver/chrome';
import 'selenium-webdriver/firefox';
import 'chromedriver';
import 'geckodriver';

import {
    AppBar,
    AssignRolesPage,
    CreateCustomerPage,
    CustomersPage,
    LoginPage,
    NotificationService,
    RegisterPage,
    UpdateCustomerPage,
    UrlNav,
} from '../testUtils/testPageComponents';
import { Builder, WebDriver } from 'selenium-webdriver';
import { ROOTURL, TESTFILEDIR, getElementByCssQuery, getElementById } from '../testUtils/testHelpers';

import fs from 'fs';
import retry from 'jest-retries';
import { titleCase } from '../utils/Utils';

let driver: WebDriver;
jasmine.DEFAULT_TIMEOUT_INTERVAL = 1000 * 60 * 5;
describe('Testing Auth flows + Registering Users', () => {
    let today = new Date();
    let testCustomerName = titleCase('Auth Test' + today.toString().replace(/ /g, ''));
    let contactNumberOnCreation = '3531111111';
    let emailAddressOnCreation = 'testcustomer@email.com';
    let customerUserName = 'user' + today.toString().replace(/ /g, '');
    let customerPassword = 'password1!';
    let postCodeOnCreation = 'A2B0000';
    let adminUsername = 'admin';
    let adminPassword = 'password1!';
    let APPBAR: AppBar;
    let LOGINPAGE: LoginPage;
    let REGISTERPAGE: RegisterPage;
    let NOTIFICATIONSERVICE: NotificationService;
    let CUSTOMERSPAGE: CustomersPage;
    let CREATECUSTOMERPAGE: CreateCustomerPage;
    let CUSTOMERDOCNAME: string;
    let UPDATECUSTOMERPAGE: UpdateCustomerPage;
    let ASSIGNROLEPAGE: AssignRolesPage;
    let URLNAV: UrlNav;
    beforeAll(async () => {
        driver = await new Builder().forBrowser('chrome').build();
        LOGINPAGE = new LoginPage(driver);
        REGISTERPAGE = new RegisterPage(driver);
        CUSTOMERDOCNAME = 'AUTHTESTDOC.zip';
        NOTIFICATIONSERVICE = new NotificationService(driver);
        CUSTOMERSPAGE = new CustomersPage(driver);
        CREATECUSTOMERPAGE = new CreateCustomerPage(driver);
        UPDATECUSTOMERPAGE = new UpdateCustomerPage(driver);
        ASSIGNROLEPAGE = new AssignRolesPage(driver);
        APPBAR = new AppBar(driver);
        URLNAV = new UrlNav(driver);
    });

    afterAll(async () => {
        fs.unlinkSync(`${TESTFILEDIR}/${CUSTOMERDOCNAME}`);
        driver.close();
        driver.quit();
    });

    retry('initialises the context and go to home page', async () => {
        await driver.get(ROOTURL);
    });

    let customerIdPostCreation = '';

    retry('Admin Creates a new customer', async () => {
        await LOGINPAGE.goToPage();
        await LOGINPAGE.loginUser(adminUsername, adminPassword);
        let notificationExpected = 'Successfully logged in.';
        let containsNotification = await NOTIFICATIONSERVICE.containsSuccessfulNotification(notificationExpected);
        expect(containsNotification).toEqual(true);
        await CUSTOMERSPAGE.goToPage();
        await CUSTOMERSPAGE.navigateToCreateNewPage();
        await CREATECUSTOMERPAGE.createCustomer(
            testCustomerName,
            contactNumberOnCreation,
            emailAddressOnCreation,
            postCodeOnCreation,
            CUSTOMERDOCNAME
        );
        notificationExpected = 'Customer created successfully.';
        containsNotification = await NOTIFICATIONSERVICE.containsSuccessfulNotification(notificationExpected);
        expect(containsNotification).toEqual(true);
        await CUSTOMERSPAGE.goToPage();
        await CUSTOMERSPAGE.inputSearch(testCustomerName);
        await CUSTOMERSPAGE.clickCustomerRowInTable(testCustomerName);
        customerIdPostCreation = await UPDATECUSTOMERPAGE.getCustomerId();
        await APPBAR.logout();
    });

    retry('Customer can Register', async () => {
        //give server time to update no way to avoid this wait
        await driver.sleep(2000);
        await REGISTERPAGE.goToPage();
        await REGISTERPAGE.registerCustomer(
            customerUserName,
            customerPassword,
            emailAddressOnCreation,
            customerIdPostCreation,
            CUSTOMERDOCNAME
        );
        const notificationExpected = 'Successfully registered. You can now log in.';
        const containsNotification = await NOTIFICATIONSERVICE.containsSuccessfulNotification(notificationExpected);
        expect(containsNotification).toEqual(true);
    });

    const adminRegisterUserName = titleCase('admin' + today.toString().replace(/ /g, ''));
    const adminRegisterEmailAddress = 'admin@gmail.com';
    const adminRegisterPasword = 'password1!';

    retry('Admin can Register', async () => {
        //give server time to update no way to avoid this wait
        await driver.sleep(2000);
        await REGISTERPAGE.goToPage();
        await REGISTERPAGE.registerAdmin(adminRegisterUserName, adminRegisterEmailAddress, adminRegisterPasword);
        const notificationExpected = 'Successfully registered. You can now log in.';
        const containsNotification = await NOTIFICATIONSERVICE.containsSuccessfulNotification(notificationExpected);
        expect(containsNotification).toEqual(true);
    });

    retry('Admin Logs in verfifies guest', async () => {
        //Give server time to update
        await driver.sleep(2000);
        await LOGINPAGE.goToPage();
        await LOGINPAGE.loginUser(adminRegisterUserName, adminRegisterPasword);
        let notificationExpected = 'Successfully logged in.';
        let containsNotification = await NOTIFICATIONSERVICE.containsSuccessfulNotification(notificationExpected);
        expect(containsNotification).toEqual(true);
        const snackBarInfo = await getElementByCssQuery('.MuiAlert-message', driver);
        const text = await await snackBarInfo.getText();
        const expectedText = `Your account is currently under review by an administrator.`;
        expect(expectedText).toEqual(text);
        await APPBAR.logout();
    });

    retry('Customer Logs in verfifies guest', async () => {
        //Give server time to update
        await driver.sleep(2000);
        await LOGINPAGE.goToPage();
        await LOGINPAGE.loginUser(customerUserName, customerPassword);
        let notificationExpected = 'Successfully logged in.';
        let containsNotification = await NOTIFICATIONSERVICE.containsSuccessfulNotification(notificationExpected);
        expect(containsNotification).toEqual(true);
        const snackBarInfo = await getElementByCssQuery('.MuiAlert-message', driver);
        const text = await await snackBarInfo.getText();
        const expectedText = `Your account with the customer ID of ${customerIdPostCreation.toLowerCase()} is currently under review by an administrator.`;
        expect(expectedText).toEqual(text);
        await APPBAR.logout();
    });

    retry('Admin logs in assigns customer role', async () => {
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

    retry('Main Admin assigns admin role', async () => {
        await ASSIGNROLEPAGE.goToPage();
        await ASSIGNROLEPAGE.assignRole(adminRegisterUserName, 'ADMIN');
        let notificationExpected = 'Successfully assigned role to user';
        let containsNotification = await NOTIFICATIONSERVICE.containsSuccessfulNotification(notificationExpected);
        expect(containsNotification).toEqual(true);
    });

    retry('Customer Logs in verfifies customer role', async () => {
        await driver.sleep(2000);
        await LOGINPAGE.goToPage();
        await LOGINPAGE.loginUser(customerUserName, customerPassword);
        let notificationExpected = 'Successfully logged in.';
        let containsNotification = await NOTIFICATIONSERVICE.containsSuccessfulNotification(notificationExpected);
        expect(containsNotification).toEqual(true);
        notificationExpected = `You are now connected to Corda's vault update realtime notification service!`;
        containsNotification = await NOTIFICATIONSERVICE.containsInfoNotification(notificationExpected);
        expect(containsNotification).toEqual(true);
        await UPDATECUSTOMERPAGE.goToMyProfilePage();
        let custIdOnPage = await UPDATECUSTOMERPAGE.getCustomerId();
        expect(customerIdPostCreation).toEqual(custIdOnPage);
    });

    retry('Customer verifies no access to admin routes', async () => {
        let canNavigate = await URLNAV.naviageToPagePossible('assignRole', 'User Management');
        expect(canNavigate).toEqual(false);
        canNavigate = await URLNAV.naviageToPagePossible('createCustomer', 'Create Customer');
        expect(canNavigate).toEqual(false);
        canNavigate = await URLNAV.naviageToPagePossible('createAccount', 'User Account');
        expect(canNavigate).toEqual(false);
        canNavigate = await URLNAV.naviageToPagePossible('updateUser', 'Assign User Roles');
        expect(canNavigate).toEqual(false);
        await APPBAR.logout();
    });

    retry('Admin Logs in verfifies admin role', async () => {
        await driver.sleep(2000);
        await LOGINPAGE.goToPage();
        await LOGINPAGE.loginUser(adminRegisterUserName, adminRegisterPasword);
        let notificationExpected = 'Successfully logged in.';
        let containsNotification = await NOTIFICATIONSERVICE.containsSuccessfulNotification(notificationExpected);
        expect(containsNotification).toEqual(true);
        await ASSIGNROLEPAGE.goToPage();
        const headerText = await getElementById('header', driver);
        const actual = await headerText.getText();
        const expected = 'User Management';
        expect(expected).toEqual(actual);
    });

    retry('Admin verifies no access to customer routes', async () => {
        let canNavigate = await URLNAV.naviageToPagePossible('payments', 'Intrabank Payment');
        expect(canNavigate).toEqual(false);
        canNavigate = await URLNAV.naviageToPagePossible('createRecurringPayment', 'Create Recurring Payment');
        expect(canNavigate).toEqual(false);
        canNavigate = await URLNAV.naviageToPagePossible('updateCustomer', 'Update Customer');
        expect(canNavigate).toEqual(false);
    });

    retry('New Admin Revokes Customers Role', async () => {
        await ASSIGNROLEPAGE.goToPage();
        await ASSIGNROLEPAGE.revokeUser(customerUserName, 'CUSTOMER');
        let notificationExpected = 'Successfully revoked role for user';
        let containsNotification = await NOTIFICATIONSERVICE.containsSuccessfulNotification(notificationExpected);
        expect(containsNotification).toEqual(true);
        await APPBAR.logout();
    });

    retry('Main Admin Revokes Admin Role', async () => {
        await LOGINPAGE.goToPage();
        await LOGINPAGE.loginUser(adminUsername, adminPassword);
        let notificationExpected = 'Successfully logged in.';
        let containsNotification = await NOTIFICATIONSERVICE.containsSuccessfulNotification(notificationExpected);
        expect(containsNotification).toEqual(true);
        await ASSIGNROLEPAGE.goToPage();
        await ASSIGNROLEPAGE.revokeUser(adminRegisterUserName, 'ADMIN');
        notificationExpected = 'Successfully revoked role for user';
        containsNotification = await NOTIFICATIONSERVICE.containsSuccessfulNotification(notificationExpected);
        expect(containsNotification).toEqual(true);
    });

    retry('Admin Logs in verfifies guest', async () => {
        //Give server time to update
        await driver.sleep(2000);
        await LOGINPAGE.goToPage();
        await LOGINPAGE.loginUser(adminRegisterUserName, adminRegisterPasword);
        let notificationExpected = 'Successfully logged in.';
        let containsNotification = await NOTIFICATIONSERVICE.containsSuccessfulNotification(notificationExpected);
        expect(containsNotification).toEqual(true);
        const snackBarInfo = await getElementByCssQuery('.MuiAlert-message', driver);
        const text = await await snackBarInfo.getText();
        const expectedText = `Your account is currently under review by an administrator.`;
        expect(expectedText).toEqual(text);
        await APPBAR.logout();
    });

    retry('Customer Logs in verfifies guest', async () => {
        await driver.sleep(2000);
        await LOGINPAGE.goToPage();
        await LOGINPAGE.loginUser(customerUserName, customerPassword);
        let notificationExpected = 'Successfully logged in.';
        let containsNotification = await NOTIFICATIONSERVICE.containsSuccessfulNotification(notificationExpected);
        expect(containsNotification).toEqual(true);
        const snackBarInfo = await getElementByCssQuery('.MuiAlert-message', driver);
        const text = await await snackBarInfo.getText();
        const expectedText = `Your account with the customer ID of ${customerIdPostCreation.toLowerCase()} is currently under review by an administrator.`;
        expect(expectedText).toEqual(text);
        await APPBAR.logout();
    });

    retry('Can not log in with invalid credentials', async () => {
        //Give server time to update
        await driver.sleep(2000);
        await LOGINPAGE.goToPage();
        await LOGINPAGE.loginUser('fakeuser', 'password');
        let notificationExpected = 'Invalid credentials. Please try again or contact an administrator.';
        let containsNotification = await NOTIFICATIONSERVICE.containsErrorNotificaiton(notificationExpected);
        expect(containsNotification).toEqual(true);
    });
});
