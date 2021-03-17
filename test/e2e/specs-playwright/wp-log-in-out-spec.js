/**
 * External dependencies
 */

import playwright from 'playwright';

import LoginPage from '../lib/pages/login-page';

describe( `Auth Screen @canary @parallel`, function () {
<<<<<<< HEAD
	this.timeout( 30000 );
	let page;
	let browser;

	before( 'Start browser', async function () {
		browser = await playwright.chromium.launch( {
			headless: false,
		} );
		const browserContext = await browser.newContext();
		page = await browserContext.newPage();
=======
	this.timeout( mochaTimeOut );

	let browserContext;
	let page;

	before( 'Start browser', async function () {
		browserContext = await browserManager.newBrowserContext();
	} );

	beforeEach( 'Open new test tab', async function () {
		page = await browserContext.newPage();
		this.currentTest.page = page;
>>>>>>> ab56b7968b... Implement ability to save screenshot using Playwright.
	} );

	describe( 'Loading the log-in page', function () {
		step( 'Can see the log in page', async function () {
			const url = LoginPage.getLoginURL();
			// Waits for network activity to cease.
			// Only as a proof of concept. In a production test, should check
			// for the presence of elements.
			await page.goto( url, { waitUntill: 'networkidle' } );
			return await page.click( 'invalid_selector' );
		} );
	} );
<<<<<<< HEAD

	after( 'close browser', function () {
		browser.close();
	} );
=======
>>>>>>> ab56b7968b... Implement ability to save screenshot using Playwright.
} );
