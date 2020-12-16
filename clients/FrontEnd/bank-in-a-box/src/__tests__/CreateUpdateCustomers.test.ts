import 'selenium-webdriver/chrome';
import 'selenium-webdriver/firefox';
import 'chromedriver';
import 'geckodriver';

import { Builder, WebDriver } from 'selenium-webdriver';
import {
    CreateCustomerPage,
    CustomersPage,
    LoginPage,
    NotificationService,
    UpdateCustomerPage,
} from '../testUtils/testPageComponents';
import { ROOTURL, TESTFILEDIR, getElementById } from '../testUtils/testHelpers';

import fs from 'fs';
import retry from 'jest-retries';
import { titleCase } from '../utils/Utils';

let driver: WebDriver;
jasmine.DEFAULT_TIMEOUT_INTERVAL = 1000 * 60 * 5;

const CUSTOMERTESTFILENAME = 'CustomerTestZip.zip';

describe('Testing Customers Behaviours', () => {
    let today: Date;
    let testCustomerName: string;
    let contactNumberOnCreation: string;
    let emailAddressOnCreation: string;
    let CUSTOMERSPAGE: CustomersPage;
    let CREATECUSTOMERPAGE: CreateCustomerPage;
    let NOTIFICATIONSERVICE: NotificationService;
    let UPDATECUSTOMERPAGE: UpdateCustomerPage;
    let LOGINPAGE: LoginPage;

    beforeAll(async () => {
        driver = await new Builder().forBrowser('chrome').build();
        today = new Date();
        testCustomerName = titleCase('Test Customer' + today.toString().replace(/ /g, ''));
        contactNumberOnCreation = '3531111111';
        emailAddressOnCreation = 'testcustomer@email.com';
        CUSTOMERSPAGE = new CustomersPage(driver);
        CREATECUSTOMERPAGE = new CreateCustomerPage(driver);
        NOTIFICATIONSERVICE = new NotificationService(driver);
        UPDATECUSTOMERPAGE = new UpdateCustomerPage(driver);
        LOGINPAGE = new LoginPage(driver);
    });

    afterAll(async () => {
        driver.quit();
        fs.unlinkSync(`${TESTFILEDIR}/${CUSTOMERTESTFILENAME}`);
    });

    retry('Log in Admin', async () => {
        await LOGINPAGE.goToPage();
        await LOGINPAGE.loginUser('admin', 'password1!');
        const notificationExpected = 'Successfully logged in.';
        const containsNotification = await NOTIFICATIONSERVICE.containsSuccessfulNotification(notificationExpected);
        expect(containsNotification).toEqual(true);
    });

    retry('initialises the context and go to home page', async () => {
        await CUSTOMERSPAGE.goToPage();
    });

    retry('Check to make sure that the header is present', async () => {
        const applink = await getElementById('header', driver);
        const actual = await applink.getText();
        const expected = 'Customers';
        expect(actual).toEqual(expected);
    });

    retry('Navigate to create to new customer is possible', async () => {
        await CUSTOMERSPAGE.navigateToCreateNewPage();
        const actualUrl: string = await driver.getCurrentUrl();
        expect(actualUrl).toEqual(`${ROOTURL}createCustomer`);
        const applink = await getElementById('header', driver);
        const actualHeaderText = await applink.getText();
        const expectedHeaderText = 'Create Customer';
        expect(actualHeaderText).toEqual(expectedHeaderText);
    });

    retry('Creating a new customer is possible', async () => {
        await CREATECUSTOMERPAGE.gotToPage();
        await CREATECUSTOMERPAGE.createCustomer(
            testCustomerName,
            contactNumberOnCreation,
            emailAddressOnCreation,
            '55555',
            CUSTOMERTESTFILENAME
        );
        const notificationExpected = 'Customer created successfully.';
        const containsNotification = await NOTIFICATIONSERVICE.containsSuccessfulNotification(notificationExpected);
        expect(containsNotification).toEqual(true);
    });

    retry('Check if created customer is present in the customer database', async () => {
        await CUSTOMERSPAGE.goToPage();
        await CUSTOMERSPAGE.inputSearch(testCustomerName);
        const customerNameFromTable = await CUSTOMERSPAGE.confirmCustomerNameInTable(testCustomerName);
        expect(customerNameFromTable).toEqual(true);
    });

    const contactNumberAfterUpdate = '35322222222';
    const emailAddressAfterUpdate = 'testcustomer2@email.com';

    retry('Check if can update customer', async () => {
        await CUSTOMERSPAGE.goToPage();
        await CUSTOMERSPAGE.inputSearch(testCustomerName);
        await CUSTOMERSPAGE.clickCustomerRowInTable(testCustomerName);
        const applink = await getElementById('header', driver);
        const actualHeaderText = await applink.getText();
        const expectedHeaderText = 'Update Customer';
        expect(actualHeaderText).toEqual(expectedHeaderText);
        UPDATECUSTOMERPAGE.updateCustomer(emailAddressAfterUpdate, contactNumberAfterUpdate, CUSTOMERTESTFILENAME);
        const notificationExpected = 'Customer updated successfully';
        const containsNotification = await NOTIFICATIONSERVICE.containsSuccessfulNotification(notificationExpected);
        expect(containsNotification).toEqual(true);
    });
});
