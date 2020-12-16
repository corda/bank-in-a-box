import { By, Key, WebDriver, WebElement } from 'selenium-webdriver';
import {
    ROOTURL,
    TESTFILEDIR,
    getElementByClassName,
    getElementByCssQuery,
    getElementById,
    getElementByTagName,
    getElementByXPath,
} from './testHelpers';

import { AccountType } from '../store/types';
import AdmZip from 'adm-zip';
import fs from 'fs';

export class AccountDetailsPage {
    driver: WebDriver;

    constructor(driver: WebDriver) {
        this.driver = driver;
    }

    infoCardWithText = async (text: string): Promise<boolean> => {
        return await (
            await getElementByXPath(
                `//div[contains(@class, 'MuiCardContent-root') and .//p[contains(text(),'${text}')]]`,
                this.driver
            )
        ).isDisplayed();
    };

    confirmAccountDataRenderedCorrectly = async (custDataFromTable: TableRowDataAccount): Promise<boolean> => {
        const accountDataDisplay = await this.infoCardWithText(custDataFromTable.accountKey);
        const accountTypeDisplay = await this.infoCardWithText(custDataFromTable.type);
        const currencyTypeDisplay = await this.infoCardWithText(custDataFromTable.currency);
        const balanceDisplay = await this.infoCardWithText(custDataFromTable.balance);
        const statusDisplay = await this.infoCardWithText(custDataFromTable.status);
        const lastTransactionDisplay = await this.infoCardWithText(custDataFromTable.lastTxDate);

        return (
            accountDataDisplay &&
            accountTypeDisplay &&
            currencyTypeDisplay &&
            balanceDisplay &&
            statusDisplay &&
            lastTransactionDisplay
        );
    };

    confirmCustomerDetailsCorrect = async (
        customerName: string,
        contactNumber: string,
        email: string,
        postCode: string
    ): Promise<boolean> => {
        const customerNameDisplay = await this.infoCardWithText(customerName);
        const contactNumberDisplay = await this.infoCardWithText(contactNumber);
        const emailDisplay = await this.infoCardWithText(email);
        const postCodeDisplay = await this.infoCardWithText(postCode);

        return customerNameDisplay && contactNumberDisplay && emailDisplay && postCodeDisplay;
    };

    makeAccountActive = async () => {
        await (await getElementByCssQuery('.accountStatusSelect', this.driver)).click();
        await (await getElementByXPath(`//li[text()="ACTIVE"]`, this.driver)).click();
        return await (await getElementByCssQuery('.submitButton', this.driver)).click();
    };

    goToAccountsTab = async () => {
        return await (await getElementByClassName('accountTab', this.driver)).click();
    };
    goToCustomerTab = async () => {
        return await (await getElementByClassName('customerTab', this.driver)).click();
    };
    goToTransactionsTab = async () => {
        return await (await getElementByClassName('transactionsTab', this.driver)).click();
    };
    goToRecurringPaymentsTab = async () => {
        return await (await getElementByClassName('recurringPaymentsTab', this.driver)).click();
    };

    goToSetStatusPage = async () => {
        return await (await getElementByClassName('setStatusButton', this.driver)).click();
    };

    goToSetLimitsPage = async () => {
        return await (await getElementByClassName('setLimitsButton', this.driver)).click();
    };

    goToApproveOverDraftPage = async () => {
        return await (await getElementByClassName('approveOverdraftButton', this.driver)).click();
    };

    goToIssueLoanPage = async () => {
        return await (await getElementByClassName('issueLoanButton', this.driver)).click();
    };

    issueLoan = async (depositAmount: string) => {
        await (await getElementByCssQuery('.loanAmountInput>div>input', this.driver)).sendKeys(depositAmount);
        await (await getElementByCssQuery('.loanPeriodInput>div>input', this.driver)).sendKeys('2');
        return await (await getElementByCssQuery('.submitButton', this.driver)).click();
    };

    goToDepositPage = async () => {
        return await (await getElementByClassName('depositButton', this.driver)).click();
    };

    depostFiat = async (amount: string) => {
        await (await getElementByCssQuery('.depositAmountInput>div>input', this.driver)).sendKeys(amount);
        await (await getElementByCssQuery('.currencyInput', this.driver)).click();
        await (await getElementByXPath(`//li[text()="EUR"]`, this.driver)).click();
        await (await getElementByCssQuery('.submitButton', this.driver)).click();
    };

    goToWithdrawPage = async () => {
        return await (await getElementByClassName('withdrawButton', this.driver)).click();
    };
}

export class CreateAccountsPage {
    driver: WebDriver;

    constructor(driver: WebDriver) {
        this.driver = driver;
    }

    gotToPage = async () => {
        await this.driver.get(`${ROOTURL}createCustomer`);
    };

    createCurrentAccount = async (customerName: string) => {
        await (await getElementByCssQuery('.accountTypeSelect', this.driver)).click();
        await (await getElementByXPath(`//li[text()="Current account"]`, this.driver)).click();
        await (await getElementByCssQuery('.customerNameSearch>div>input', this.driver)).sendKeys(customerName);
        const resultSelect = await getElementByClassName('searchResultDropdown__result', this.driver);
        await resultSelect.click();
        await (await getElementByCssQuery('.currencyInput', this.driver)).click();
        await (await getElementByXPath(`//li[text()="EUR"]`, this.driver)).click();
        await (await getElementByCssQuery('.withdrawalDailyLimitInput>div>input', this.driver)).sendKeys('2000');
        await (await getElementByCssQuery('.transferDailyLimitInput>div>input', this.driver)).sendKeys('2000');
        return await (await getElementByClassName('submitButton', this.driver)).click();
    };

    createCurrentAccountNoLimits = async (customerName: string) => {
        await (await getElementByCssQuery('.accountTypeSelect', this.driver)).click();
        await (await getElementByXPath(`//li[text()="Current account"]`, this.driver)).click();
        await (await getElementByCssQuery('.customerNameSearch>div>input', this.driver)).sendKeys(customerName);
        const resultSelect = await getElementByClassName('searchResultDropdown__result', this.driver);
        await resultSelect.click();
        await (await getElementByCssQuery('.currencyInput', this.driver)).click();
        await (await getElementByXPath(`//li[text()="EUR"]`, this.driver)).click();
        return await (await getElementByClassName('submitButton', this.driver)).click();
    };

    createSavingsAccount = async (customerName: string) => {
        await (await getElementByCssQuery('.accountTypeSelect', this.driver)).click();
        await (await getElementByXPath(`//li[text()="Savings account"]`, this.driver)).click();
        await (await getElementByCssQuery('.customerNameSearch>div>input', this.driver)).sendKeys(customerName);
        const resultSelect = await getElementByClassName('searchResultDropdown__result', this.driver);
        await resultSelect.click();
        await (await getElementByCssQuery('.currencyInput', this.driver)).click();
        await (await getElementByXPath(`//li[text()="EUR"]`, this.driver)).click();
        await (await getElementByCssQuery('.savingsAmountInput>div>input', this.driver)).sendKeys('2000');
        try {
            await (await getElementByCssQuery('.savingsStartDateInput', this.driver)).click();
        } catch (error) {
            if (error.toString().includes('ElementClickInterceptedError:')) {
                await new NotificationService(this.driver).dismissNotifications();
                await (await getElementByCssQuery('.savingsStartDateInput', this.driver)).click();
            }
        }
        //Get the date picker header and click the next month button
        const dateHeader = await getElementByCssQuery('.MuiPickersCalendarHeader-switchHeader', this.driver);
        const buttons = await dateHeader.findElements(By.tagName('button'));
        await buttons[1].click();
        await (
            await getElementByXPath(
                `//button[contains(@class, "MuiPickersDay-day") and .//p[contains(text(),'1')]]`,
                this.driver
            )
        ).click();
        await (
            await getElementByXPath('//span[contains(@class, "MuiButton-label") and text()="OK"]', this.driver)
        ).click();
        await (await getElementByCssQuery('.savingsPeriodInput>div>input', this.driver)).sendKeys('2');
        await (await getElementByClassName('submitButton', this.driver)).click();
    };
}

export class CreateCustomerPage {
    driver: WebDriver;

    constructor(driver: WebDriver) {
        this.driver = driver;
    }

    gotToPage = async () => {
        await this.driver.get(`${ROOTURL}createCustomer`);
    };

    createCustomer = async (
        customerName: string,
        contactNumber: string,
        email: string,
        postCode: string,
        attachmentName: string
    ) => {
        const nameInput = new TextInput(this.driver, '.customerNameInput>div>input');
        await nameInput.insertText(customerName);
        const numberInput = new TextInput(this.driver, '.contactNumberInput>div>input');
        await numberInput.insertText(contactNumber);
        const emailinput = new TextInput(this.driver, '.emailAddressInput>div>input');
        await emailinput.insertText(email);
        const postcodeInput = new TextInput(this.driver, '.postCodeInput>div>input');
        await postcodeInput.insertText(postCode);
        await (await getElementByCssQuery('.fileUploadInput', this.driver)).click();
        const file = new AdmZip();
        file.addFile('somedata.txt', Buffer.from(customerName));
        fs.writeFileSync(attachmentName, file.toBuffer());
        await this.driver.switchTo().activeElement().sendKeys(`${TESTFILEDIR}/${attachmentName}`);
        const submitButton = new SubmitButton(this.driver);
        return await submitButton.click();
    };
}

export class UpdateCustomerPage {
    driver: WebDriver;

    constructor(driver: WebDriver) {
        this.driver = driver;
    }

    goToMyProfilePage = async () => {
        await this.driver.get(`${ROOTURL}updateCustomer`);
    };

    updateCustomer = async (emailAddress: string, contactNumber: string, attachmentName: string) => {
        const contactInput = new TextInput(this.driver, '.contactNumberInput>div>input');
        await contactInput.insertText(contactNumber);
        const emailinput = new TextInput(this.driver, '.emailAddressInput>div>input');
        await emailinput.click();
        await (await emailinput.getRaw()).sendKeys(Key.CONTROL + 'a');
        await (await emailinput.getRaw()).sendKeys(Key.BACK_SPACE);
        await emailinput.insertText(emailAddress);
        await (await getElementByCssQuery('.fileUploadInput', this.driver)).click();
        const file = new AdmZip();
        const today = new Date();
        file.addFile('updatedData.txt', Buffer.from(today.toString()));
        fs.writeFileSync(attachmentName, file.toBuffer());
        await this.driver.switchTo().activeElement().sendKeys(`${TESTFILEDIR}/${attachmentName}`);
        const submitButton = new SubmitButton(this.driver);
        return await submitButton.click();
    };

    verifyDetails = async (
        customerName: string,
        contactNumber: string,
        emailAddress: string,
        postCode: string,
        fileName: string,
        customerId: string
    ) => {
        const custNameDisplay = await this.inputWithValue(customerName);
        const contactNumberDisplay = await this.inputWithValue(contactNumber);
        const emailAddressDisplay = await this.inputWithValue(emailAddress);
        const postCodeDisplay = await this.inputWithValue(postCode);
        await (await getElementByCssQuery('.attachmentsAccordion', this.driver)).click();
        const idContainer = await getElementByClassName('MuiAlert-message', this.driver);
        const text = await idContainer.getText();
        const customerIdDisplayed = text.split(':')[1].trim() === customerId;
        const fileNameContainer = await getElementByCssQuery('.inputWrapper__item__attachment', this.driver);
        const fileNameText = await fileNameContainer.getText();
        const fileDisplayed = fileName === fileNameText;
        return (
            custNameDisplay &&
            contactNumberDisplay &&
            emailAddressDisplay &&
            postCodeDisplay &&
            customerIdDisplayed &&
            fileDisplayed
        );
    };

    inputWithValue = async (text: string): Promise<boolean> => {
        return await (await getElementByXPath(`//input[contains(@value, '${text}')]`, this.driver)).isDisplayed();
    };

    getCustomerId = async (): Promise<string> => {
        const idContainer = await getElementByClassName('MuiAlert-message', this.driver);
        const text = await idContainer.getText();
        return text.split(':')[1].trim();
    };
}

export class Table {
    driver: WebDriver;
    tableHeaders: string[];

    constructor(driver: WebDriver) {
        this.driver = driver;
        this.tableHeaders = [];
    }

    protected getTableBody = () => getElementByTagName('tbody', this.driver);
    protected getTableHeaders = () => getElementByTagName('thead', this.driver);

    public getColIndexOfHeader = (header: string) => {
        let colIndex: number = 0;
        this.tableHeaders.forEach((headerText, index) => {
            if (headerText === header) {
                colIndex = index;
            }
        });

        return colIndex;
    };

    setTableData = async () => {
        const ths = await this.driver.findElements(By.className('tableHeader'));
        this.tableHeaders = await Promise.all(
            ths.map(async (th) => {
                return await th.getAttribute('textContent');
            })
        );
        return;
    };

    clickFirstResult = async () => {
        const tableBody = await this.getTableBody();
        const tr = await tableBody.findElement(By.tagName('tr'));
        return await tr.click();
    };

    getTopResultTextByHeader = async (header: string): Promise<string> => {
        let colIndex: number = this.getColIndexOfHeader(header);
        const tableBody = await this.getTableBody();
        const tr = await tableBody.findElement(By.tagName('tr'));
        const tds = await tr.findElements(By.tagName('td'));
        return await tds[colIndex].getText();
    };

    confirmRowExistsWithData = async (text: string): Promise<boolean> => {
        return await (await getElementByXPath(`//td[contains(text(),'${text}')]`, this.driver)).isDisplayed();
    };

    getRowWithData = async (text: string): Promise<WebElement> => {
        const td = getElementByXPath(`.//td[contains(text(),'${text}')]`, this.driver);
        const tr = await (await td).findElement(By.xpath('./..'));
        return tr;
    };

    getRowWithTransactionData = async (accountOne: string, accountTwo: string): Promise<WebElement> => {
        return await getElementByXPath(`//tr[td[2]="${accountOne}" and td[3]="${accountTwo}"]`, this.driver);
    };
}

class SubmitButton {
    driver: WebDriver;

    constructor(driver: WebDriver) {
        this.driver = driver;
    }

    private getButton = () => getElementByCssQuery('.submitButton', this.driver);

    async enabled(): Promise<boolean> {
        const button = await this.getButton();
        return await button.isEnabled();
    }

    async click() {
        const button = await this.getButton();
        return button.click();
    }
}

class TextInput {
    driver: WebDriver;
    locator: string;

    constructor(driver: WebDriver, locator: string) {
        this.driver = driver;
        this.locator = locator;
    }

    private getTextField = () => getElementByCssQuery(this.locator, this.driver);

    click = async () => {
        const textField = await this.getTextField();
        return textField.click();
    };

    insertText = async (text: string) => {
        const textField = await this.getTextField();
        return textField.sendKeys(text);
    };

    getRaw = async () => {
        return await this.getTextField();
    };
}

export class NavBar {
    driver: WebDriver;

    constructor(driver: WebDriver) {
        this.driver = driver;
    }

    private menuButton = () => getElementByCssQuery('.menuButton', this.driver);

    //Check if can navigate to a page using the nav drawer
    naviageToPagePossible = async (buttonHref, pageHeaderExpected): Promise<boolean> => {
        await (await this.menuButton()).click();
        await this.driver.sleep(1000);
        const navButton = await getElementByCssQuery(`[href='/${buttonHref}']`, this.driver);
        await navButton.click();
        const actualUrl: string = await this.driver.getCurrentUrl();
        expect(actualUrl).toEqual(`${ROOTURL}${buttonHref}`);
        const applink = await getElementById('header', this.driver);
        const actual = await applink.getText();
        return actual === pageHeaderExpected;
    };
}

class TableWrapper {
    table: Table;
    driver: WebDriver;
    path: string;

    constructor(driver: WebDriver, path: string) {
        this.driver = driver;
        this.path = path;
        this.table = new Table(driver);
    }

    goToPage = async () => {
        await this.driver.get(`${ROOTURL}${this.path}`);
        await this.driver.sleep(1000);
        return await this.table.setTableData();
    };

    navigateToCreateNewPage = async () => {
        const custButton = await getElementByClassName('createNewButtonItem', this.driver);
        await custButton.click();
    };

    inputSearch = async (text: string) => {
        const textField = new TextInput(this.driver, '.searchField>div>input');
        return textField.insertText(text);
    };

    clickFirstResult = async () => {
        return await this.table.clickFirstResult();
    };
}

export class CustomersPage extends TableWrapper {
    constructor(driver: WebDriver) {
        super(driver, 'customers');
    }

    confirmCustomerNameInTable = async (name: string): Promise<boolean> => {
        return await this.table.confirmRowExistsWithData(name);
    };

    clickCustomerRowInTable = async (name: string) => {
        return await (await this.table.getRowWithData(name)).click();
    };

    clickFirstResult = async () => {
        return await this.table.clickFirstResult();
    };
}

export class LoginPage {
    driver: WebDriver;
    constructor(driver: WebDriver) {
        this.driver = driver;
    }

    goToPage = async () => {
        return await this.driver.get(`${ROOTURL}login`);
    };

    loginUser = async (username: string, password: string) => {
        const userNameInput = new TextInput(this.driver, '.usernameInput>div>input');
        await userNameInput.insertText(username);
        const passwordInput = new TextInput(this.driver, '.passwordInput>div>input');
        await passwordInput.insertText(password);
        const submitButton = new SubmitButton(this.driver);
        return await submitButton.click();
    };
}

export class RegisterPage {
    driver: WebDriver;
    constructor(driver: WebDriver) {
        this.driver = driver;
    }

    goToPage = async () => {
        return await this.driver.get(`${ROOTURL}register`);
    };

    registerCustomer = async (
        username: string,
        password: string,
        email: string,
        customerId: string,
        attachmentName: string
    ) => {
        const userNameInput = new TextInput(this.driver, '.usernameInput>div>input');
        await userNameInput.insertText(username);
        const emailInput = new TextInput(this.driver, '.emailAddressInput>div>input');
        await emailInput.insertText(email);
        const passwordInput = new TextInput(this.driver, '.passwordInput>div>input');
        await passwordInput.insertText(password);
        const passwordConfirmInput = new TextInput(this.driver, '.passwordConfirmInput>div>input');
        await passwordConfirmInput.insertText(password);
        const custmomerIdInput = new TextInput(this.driver, '.customerIdInput>div>input');
        await custmomerIdInput.insertText(customerId);
        await (await getElementByCssQuery('.fileUploadInput', this.driver)).click();
        await this.driver.switchTo().activeElement().sendKeys(`${TESTFILEDIR}/${attachmentName}`);
        const submitButton = new SubmitButton(this.driver);
        return await submitButton.click();
    };

    registerAdmin = async (username: string, email: string, password: string) => {
        const userNameInput = new TextInput(this.driver, '.usernameInput>div>input');
        await userNameInput.insertText(username);
        const emailInput = new TextInput(this.driver, '.emailAddressInput>div>input');
        await emailInput.insertText(email);
        const passwordInput = new TextInput(this.driver, '.passwordInput>div>input');
        await passwordInput.insertText(password);
        const passwordConfirmInput = new TextInput(this.driver, '.passwordConfirmInput>div>input');
        await passwordConfirmInput.insertText(password);
        const submitButton = new SubmitButton(this.driver);
        return await submitButton.click();
    };
}

type Role = 'CUSTOMER' | 'ADMIN' | 'GUEST';

export class AssignRolesPage extends TableWrapper {
    constructor(driver: WebDriver) {
        super(driver, 'assignRole');
    }

    assignRole = async (username: string, role: Role) => {
        await (await getElementByCssQuery('.roleTypeSelect', this.driver)).click();
        await (await getElementByXPath(`//li[text()="GUEST"]`, this.driver)).click();
        await (await getElementByCssQuery('.searchField>div>input', this.driver)).sendKeys(username);
        await this.clickUserRowInTable(username);
        await (await getElementByCssQuery('.roleTypeSelect', this.driver)).click();
        await (await getElementByXPath(`//li[text()="${role}"]`, this.driver)).click();
        await (await getElementByCssQuery('.assignRoleButton', this.driver)).click();
    };

    revokeUser = async (username: string, role: Role) => {
        await (await getElementByCssQuery('.roleTypeSelect', this.driver)).click();
        await (await getElementByXPath(`//li[text()="${role}"]`, this.driver)).click();
        await (await getElementByCssQuery('.searchField>div>input', this.driver)).sendKeys(username);
        await this.clickUserRowInTable(username);
        await (await getElementByCssQuery('.revokeButton', this.driver)).click();
    };

    clickUserRowInTable = async (name: string) => {
        return await (await this.table.getRowWithData(name)).click();
    };
}

type TableRowDataAccount = {
    accountKey: string;
    type: string;
    currency: string;
    balance: string;
    status: string;
    lastTxDate: string;
    customerName: string;
};

export class AccountsPage extends TableWrapper {
    constructor(driver: WebDriver) {
        super(driver, 'accounts');
    }

    selectAccount = async (accountType: AccountType): Promise<TableRowDataAccount> => {
        const row = await this.table.getRowWithData(accountType);
        const tds = await row.findElements(By.tagName('td'));
        const accountKey = await tds[this.table.getColIndexOfHeader('Account Key')].getText();
        const customerName = await tds[this.table.getColIndexOfHeader('Customer Name')].getText();
        const type = await tds[this.table.getColIndexOfHeader('Type')].getText();
        const status = await tds[this.table.getColIndexOfHeader('Status')].getText();
        const currency = await tds[this.table.getColIndexOfHeader('Currency')].getText();
        const balance = await tds[this.table.getColIndexOfHeader('Balance')].getText();
        const lastTxDate = await tds[this.table.getColIndexOfHeader('Last TX Date')].getText();
        await row.click();
        return {
            accountKey: accountKey,
            type: type,
            customerName: customerName,
            status: status,
            currency: currency,
            balance: balance,
            lastTxDate: lastTxDate,
        };
    };
}

export class ApproveOverdraftPage {
    driver: WebDriver;
    constructor(driver: WebDriver) {
        this.driver = driver;
    }

    async submitOverdraft() {
        const submitButton = new SubmitButton(this.driver);
        return await submitButton.click();
    }

    async cancelOverDraftApproval() {
        return (await getElementByCssQuery('.cancelButton', this.driver)).click();
    }

    async inputOverdraftAmount(amount: string) {
        const textField = new TextInput(this.driver, '.overdraftLimit>div>input');
        return await textField.insertText(amount);
    }
}

export class NotificationService {
    driver: WebDriver;

    constructor(driver: WebDriver) {
        this.driver = driver;
    }

    async containsSuccessfulNotification(text: string): Promise<boolean> {
        const snackBar = await getElementByXPath(
            `//div[contains(@class,'SnackbarItem-variantSuccess') and .//*[contains(text(),"${text}")]]`,
            this.driver
        );
        return await snackBar.isDisplayed();
    }

    async containsInfoNotification(text: string): Promise<boolean> {
        const snackBar = await getElementByXPath(
            `//div[contains(@class,'SnackbarItem-variantInfo') and .//*[contains(text(),"${text}")]]`,
            this.driver
        );
        return await snackBar.isDisplayed();
    }

    async containsErrorNotificaiton(text: string): Promise<boolean> {
        const snackBar = await getElementByXPath(
            `//div[contains(@class,'SnackbarItem-variantError') and .//*[contains(text(),"${text}")]]`,
            this.driver
        );
        return await snackBar.isDisplayed();
    }

    async dismissNotifications() {
        //using the default notistack styles to locate parent element
        //const notifDismissParent = await getElementByClassName('makeStyles-right-41', this.driver);
        const notifDismissButtons = await this.driver.findElements(By.id('dismissNotificationButton'));
        for (let button of notifDismissButtons) {
            await button.click();
        }
        return;
    }
}

export class AppBar {
    driver: WebDriver;

    constructor(driver: WebDriver) {
        this.driver = driver;
    }

    async logout() {
        try {
            await (await getElementByClassName('logOutButton', this.driver)).click();
        } catch (error) {
            if (error.toString().includes('ElementClickInterceptedError:')) {
                await new NotificationService(this.driver).dismissNotifications();
                await (await getElementByClassName('logOutButton', this.driver)).click();
            }
        }
    }
}

export class IntrabankPaymentsPage {
    driver: WebDriver;
    constructor(driver: WebDriver) {
        this.driver = driver;
    }

    goToPage = async () => {
        return await this.driver.get(`${ROOTURL}payments`);
    };

    /**This is a quick payment and the first current account should be auto selected as payment from account */
    makeQuickPayment = async (toAccount: string, amount: string) => {
        await (await getElementByCssQuery('.toAccountInput>div>input', this.driver)).sendKeys(Key.CONTROL + 'a');
        await (await getElementByCssQuery('.toAccountInput>div>input', this.driver)).sendKeys(Key.DELETE);
        await (await getElementByCssQuery('.toAccountInput>div>input', this.driver)).sendKeys(toAccount);
        await (await getElementByCssQuery('.amountInput>div>input', this.driver)).sendKeys(amount);
        const submitButton = new SubmitButton(this.driver);
        return await submitButton.click();
    };
}

export class CreateRecurringPaymentPage {
    driver: WebDriver;
    constructor(driver: WebDriver) {
        this.driver = driver;
    }

    goToPage = async () => {
        return await this.driver.get(`${ROOTURL}createRecurringPayment`);
    };

    /**This is a quick payment and the first current account should be auto selected as payment from account */
    makeQuickRecurringPaymentPayment = async (
        toAccount: string,
        amount: string,
        everyDays: string,
        iterationCount: string
    ) => {
        await (await getElementByCssQuery('.toAccountInput>div>input', this.driver)).sendKeys(Key.CONTROL + 'a');
        await (await getElementByCssQuery('.toAccountInput>div>input', this.driver)).sendKeys(Key.DELETE);
        await (await getElementByCssQuery('.toAccountInput>div>input', this.driver)).sendKeys(toAccount);
        await (await getElementByCssQuery('.amountInput>div>input', this.driver)).sendKeys(amount);
        await (await getElementByCssQuery('.periodInDays>div>input', this.driver)).sendKeys(everyDays);
        await (await getElementByCssQuery('.numberOfIterations>div>input', this.driver)).sendKeys(iterationCount);
        try {
            await (await getElementByCssQuery('.startDateInput', this.driver)).click();
        } catch (error) {
            if (error.toString().includes('ElementClickInterceptedError:')) {
                await new NotificationService(this.driver).dismissNotifications();
                await (await getElementByCssQuery('.startDateInput', this.driver)).click();
            }
        }
        const dateHeader = await getElementByCssQuery('.MuiPickersCalendarHeader-switchHeader', this.driver);
        const buttons = await dateHeader.findElements(By.tagName('button'));
        await buttons[1].click();
        await (
            await getElementByXPath(
                `//button[contains(@class, "MuiPickersDay-day") and .//p[contains(text(),'1')]]`,
                this.driver
            )
        ).click();
        await (
            await getElementByXPath('//span[contains(@class, "MuiButton-label") and text()="OK"]', this.driver)
        ).click();
        const submitButton = new SubmitButton(this.driver);
        return await submitButton.click();
    };
}

type TableRowDataTransaction = {
    transactionId: string;
    accountFrom: string;
    accountTo: string;
    amount: string;
    type: string;
    txDate: string;
};

export class TransactionsPage extends TableWrapper {
    constructor(driver: WebDriver) {
        super(driver, 'transactions');
    }

    loadTable = async () => {
        await this.driver.sleep(1000);
        return await this.table.setTableData();
    };

    selectTransaction = async (accountOne: string, accountTwo: string): Promise<TableRowDataTransaction> => {
        const row = await this.table.getRowWithTransactionData(accountOne, accountTwo);
        const tds = await row.findElements(By.tagName('td'));
        const transactionId = await tds[this.table.getColIndexOfHeader('Transaction ID')].getText();
        const accountFrom = await tds[this.table.getColIndexOfHeader('Account from')].getText();
        const accountTo = await tds[this.table.getColIndexOfHeader('Account to')].getText();
        const amount = await tds[this.table.getColIndexOfHeader('Amount')].getText();
        const type = await tds[this.table.getColIndexOfHeader('Type')].getText();
        const txDate = await tds[this.table.getColIndexOfHeader('Transaction date')].getText();
        await row.click();
        return {
            transactionId: transactionId,
            accountFrom: accountFrom,
            accountTo: accountTo,
            amount: amount,
            type: type,
            txDate: txDate,
        };
    };
}

export class TransactionDetailsPage {
    driver: WebDriver;

    constructor(driver: WebDriver) {
        this.driver = driver;
    }

    infoCardWithText = async (text: string): Promise<boolean> => {
        return await (
            await getElementByXPath(
                `//div[contains(@class, 'MuiCardContent-root') and .//p[contains(text(),'${text}')]]`,
                this.driver
            )
        ).isDisplayed();
    };

    confirmTransactionDatavisibleCorrectly = async (transactionData: TableRowDataTransaction): Promise<boolean> => {
        const transactionIdVisible = await this.infoCardWithText(transactionData.transactionId);
        const transactionTypeVisible = await this.infoCardWithText(transactionData.type);
        const transactionDateDisplay = await this.infoCardWithText(transactionData.txDate);
        const amountDisplay = await this.infoCardWithText(transactionData.amount);

        return transactionIdVisible && transactionTypeVisible && transactionDateDisplay && amountDisplay;
    };

    confirmAccountsDataRenderedCorrectly = async (
        accountFrom: string,
        accountTo: string,
        ownAccountType: string,
        customerNameFrom: string,
        customerNameTo: string
    ): Promise<boolean> => {
        const acountFromVisible = await this.infoCardWithText(accountFrom);
        const accountToVisible = await this.infoCardWithText(accountTo);
        const ownAccountTypeDisplay = await this.infoCardWithText(ownAccountType);
        const customerNameFromDisplay = await this.infoCardWithText(customerNameFrom);
        const customerNameToDisplay = await this.infoCardWithText(customerNameTo);

        return (
            acountFromVisible &&
            accountToVisible &&
            ownAccountTypeDisplay &&
            customerNameFromDisplay &&
            customerNameToDisplay
        );
    };

    goToAccountsTab = async () => {
        return await (await getElementByClassName('accountsTab', this.driver)).click();
    };
}

export class UrlNav {
    driver: WebDriver;

    constructor(driver: WebDriver) {
        this.driver = driver;
    }

    //Check if can navigate to a page using the url bar
    naviageToPagePossible = async (path, pageHeaderExpected): Promise<boolean> => {
        this.driver.get(`${ROOTURL}${path}`);
        await this.driver.sleep(1000);
        const applink = await getElementById('header', this.driver);
        const actual = await applink.getText();
        return actual === pageHeaderExpected;
    };
}
