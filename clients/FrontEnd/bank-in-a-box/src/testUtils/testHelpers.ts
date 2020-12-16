import { By, WebDriver, until } from 'selenium-webdriver';

import { Driver } from 'selenium-webdriver/chrome';

/**
 * Set the url path of where the web app is hosted to execute tests there.
 */
export const ROOTURL = 'http://localhost:3003/';

/**
 * Set the absolute path of where the generated zip files will be stored during automated e2e tests
 * Recommended: Root path of this project
 * Replace <ROOT ABSOLUTE PATH> with for example: C:/Documents/Github
 */
export const TESTFILEDIR = '<ROOT ABSOLUTE PATH>/refapp/clients/FrontEnd/bank-in-a-box';

const waitUntilTime: number = 10000;

export async function getElementById(id: string, driver: WebDriver) {
    const el = await driver.wait(until.elementLocated(By.id(id)), waitUntilTime);
    return await driver.wait(until.elementIsVisible(el), waitUntilTime);
}

export async function getElementByClassName(id: string, driver: WebDriver) {
    const el = await driver.wait(until.elementLocated(By.className(id)), waitUntilTime);
    return await driver.wait(until.elementIsVisible(el), waitUntilTime);
}

export async function getElementByXPath(xpath: string, driver: WebDriver) {
    const el = await driver.wait(until.elementLocated(By.xpath(xpath)), waitUntilTime);
    return await driver.wait(until.elementIsVisible(el), waitUntilTime);
}

export async function getElementByCssQuery(selector: string, driver: WebDriver) {
    const el = await driver.wait(until.elementLocated(By.css(selector)), waitUntilTime);
    return await driver.wait(until.elementIsVisible(el), waitUntilTime);
}

export async function getElementByTagName(selector: string, driver: WebDriver) {
    const el = await driver.wait(until.elementLocated(By.tagName(selector)), waitUntilTime);
    return await driver.wait(until.elementIsVisible(el), waitUntilTime);
}

export async function elementExists(id: string, driver: Driver, by: (queryString: string) => By): Promise<boolean> {
    return await driver.findElement(by(id)).then(
        function (webElement) {
            return true;
        },
        function (err) {
            if (err.state && err.state === 'no such element') {
                return false;
            } else {
                return false;
            }
        }
    );
}
