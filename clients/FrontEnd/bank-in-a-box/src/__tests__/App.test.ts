import 'selenium-webdriver/chrome';
import 'selenium-webdriver/firefox';
import 'chromedriver';
import 'geckodriver';

import { Builder, WebDriver } from 'selenium-webdriver';
import { LoginPage, NavBar, NotificationService } from '../testUtils/testPageComponents';
import { ROOTURL, getElementById } from '../testUtils/testHelpers';

import retry from 'jest-retries';

let driver: WebDriver;
jasmine.DEFAULT_TIMEOUT_INTERVAL = 1000 * 60 * 5;

//Test Suite Based on AG-248 Requirnments and expected user interaction
describe('Testing Home Page And Navigation', () => {
    let NAVBAR: NavBar;
    let LOGINPAGE: LoginPage;
    let NOTIFICATIONSERVICE: NotificationService;
    beforeAll(async () => {
        driver = await new Builder().forBrowser('chrome').build();
        LOGINPAGE = new LoginPage(driver);
        NOTIFICATIONSERVICE = new NotificationService(driver);
        NAVBAR = new NavBar(driver);
    });

    afterAll(async () => {
        driver.close();
        driver.quit();
    });

    //Later will need to add login tests when auth is implemented
    retry('initialises the context and go to home page', async () => {
        await driver.get(ROOTURL);
    });

    retry('Check to make sure that the header is present', async () => {
        const applink = await getElementById('header', driver);
        const actual = await applink.getText();
        const expected = 'Welcome to Bank in a Box';
        expect(actual).toEqual(expected);
    });

    retry('Navigate to Register is possible', async () => {
        const canNavigate = await NAVBAR.naviageToPagePossible('register', 'Register');
        expect(canNavigate).toEqual(true);
    });

    retry('Admin can login', async () => {
        await LOGINPAGE.goToPage();
        await LOGINPAGE.loginUser('admin', 'password1!');
        let notificationExpected = 'Successfully logged in.';
        let containsNotification = await NOTIFICATIONSERVICE.containsSuccessfulNotification(notificationExpected);
        expect(containsNotification).toEqual(true);
    });

    retry('Navigate to Assign User Roles is possible', async () => {
        const canNavigate = await NAVBAR.naviageToPagePossible('assignRole', 'User Management');
        expect(canNavigate).toEqual(true);
    });

    retry('Navigate to Customers is possible', async () => {
        const canNavigate = await NAVBAR.naviageToPagePossible('customers', 'Customers');
        expect(canNavigate).toEqual(true);
    });

    retry('Navigate to Accounts is possible', async () => {
        const canNavigate = await NAVBAR.naviageToPagePossible('accounts', 'Accounts');
        expect(canNavigate).toEqual(true);
    });

    retry('Navigate to Accounts is possible', async () => {
        const canNavigate = await NAVBAR.naviageToPagePossible('transactions', 'Transactions');
        expect(canNavigate).toEqual(true);
    });

    retry('Navigate to Accounts is possible', async () => {
        const canNavigate = await NAVBAR.naviageToPagePossible('recurringpayments', 'Recurring Payments');
        expect(canNavigate).toEqual(true);
    });
});
