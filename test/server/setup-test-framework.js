/**
 * External dependencies
 */
import * as nock from 'nock';
import chai from 'chai';
import sinonChai from 'sinon-chai';

// Ensure babel transpilations use the right browserslist environment. By default
// it will use NODE_ENV (equal to `test`), and because that env is not defined in
// `package.json`, it will use browserslist defaults.
process.env.BROWSERSLIST_ENV = 'production';

chai.use( sinonChai );

// Disables all network requests for all tests.
nock.disableNetConnect();

beforeAll( () => {
	// reactivate nock on test start
	if ( ! nock.isActive() ) {
		nock.activate();
	}
} );

afterAll( () => {
	// helps clean up nock after each test run and avoid memory leaks
	nock.restore();
	nock.cleanAll();
} );
