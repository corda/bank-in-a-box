// jest-dom adds custom jest matchers for asserting on DOM nodes.
// allows you to do things like:
// expect(element).toHaveTextContent(/react/i)
// learn more: https://github.com/testing-library/jest-dom

import '@testing-library/jest-dom/extend-expect';

import * as failFast from 'jasmine-fail-fast';

//The bellow usage of failFast should be used when you want jest to stop executing after a singe failed test.
//It will skip every subsequent individual test


if (JSON.parse(process.env.npm_config_argv!).original.includes('--bail')) {
    const jasmineEnv = (jasmine as any).getEnv();
    jasmineEnv.addReporter(failFast.init());
}
