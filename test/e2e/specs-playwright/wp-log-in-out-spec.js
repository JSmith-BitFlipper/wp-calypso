/**
 * External dependencies
 */
import config from 'config';

/**
 * Internal dependencies
 */
import * as browserManager from '../lib/browser-manager';

import LoginPage from '../lib/pages/login-page';

const mochaTimeOut = config.get( 'mochaTimeoutMS' );

describe( `Auth Screen @canary @parallel`, function () {
	this.timeout( mochaTimeOut );

	let browserContext;
	let page;

	before( 'Start browser', async function () {
		browserContext = await browserManager.newBrowserContext();
	} );

	beforeEach( 'Open new test tab', async function () {
		page = await browserContext.newPage();
		this.currentTest.page = page;
	} );

	describe( 'Loading the log-in page', function () {
		step( 'Can see the log in page', async function () {
			const url = LoginPage.getLoginURL();
			// Waits for network activity to cease.
			// Only as a proof of concept. In a production test, should check
			// for the presence of elements.
			return await page.goto( url, { waitUntill: 'networkidle' } );
		} );
	} );
} );
