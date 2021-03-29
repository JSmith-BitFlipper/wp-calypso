/**
 * External dependencies
 */
import Gridicon from 'calypso/components/gridicon';
import React from 'react';
import { connect } from 'react-redux';
import { localize } from 'i18n-calypso';

/**
 * Internal dependencies
 */
import { Button, Card } from '@automattic/components';
import SectionHeader from 'calypso/components/section-header';
import { recordGoogleEvent } from 'calypso/state/analytics/actions';
import { isWebAuthnSupported } from 'calypso/lib/webauthn';
import { getCurrentUser } from 'calypso/state/current-user/selectors';
import Notice from 'calypso/components/notice';

import { httpGet, httpPost } from 'calypso/state/login/utils'; // TODO: Move these functions to own utils file
import { registrationFinish_PostFn, attestationFinish_PostFn } from 'calypso/lib/webauthn_js';

class Security2faWebauthn extends React.Component {
	state = {
		managingWebauthn: false,
		isBrowserSupported: isWebAuthnSupported(),
		errorMessage: false,
		isWebauthnEnabled: false,
	};

	getClickHandler = ( action, callback ) => {
		return ( event ) => {
			this.props.recordGoogleEvent( 'Me', 'Clicked on ' + action );

			if ( callback ) {
				callback( event );
			}
		};
	};

	manageWebauthnStart = ( event ) => {
		event.preventDefault();
		this.setState( { managingWebauthn: true } );
	};

	componentDidMount() {
		// The `componentDidMount` function should not itself be `async`
		( async () => {
			const username = this.props.currentUser.username;
			const body = await httpGet( 'https://localhost:8081', `/webauthn/is_enabled/${ username }` );

			this.setState( { isWebauthnEnabled: body.webauthn_is_enabled } );
		} )();
	}

	enableWebauthn = async ( event ) => {
		event.preventDefault();

		const username = this.props.currentUser.username;
		const webauthn_options = await httpPost( 'https://localhost:8081', '/webauthn/begin_register', {
			username: username,
		} );
		await registrationFinish_PostFn( webauthn_options, ( credentials ) =>
			httpPost( 'https://localhost:8081', '/webauthn/finish_register', {
				username: username,
				credentials: credentials,
			} )
		);
	};

	disableWebauthn = async ( event ) => {
		event.preventDefault();

		const username = this.props.currentUser.username;
		const webauthn_options = await httpPost(
			'https://localhost:8081',
			'/webauthn/begin_attestation',
			{
				auth_text: 'Confirm disable webauthn for {0}'.format( username ),
			}
		);
		await attestationFinish_PostFn( webauthn_options, ( assertion ) =>
			httpPost( 'https://localhost:8081', '/webauthn/disable', { assertion: assertion } )
		);
	};

	render() {
		const { translate } = this.props;
		const { managingWebauthn, isBrowserSupported, errorMessage, isWebauthnEnabled } = this.state;

		return (
			<div className="security-2fa-webauthn">
				<SectionHeader label={ translate( 'Webauthn Two-Factor Authentication' ) }>
					{ ! managingWebauthn && isBrowserSupported && (
						<Button
							compact
							onClick={ this.getClickHandler( 'Manage Webauthn Button', this.manageWebauthnStart ) }
						>
							{ /* eslint-disable wpcalypso/jsx-gridicon-size */ }
							<Gridicon icon="plus-small" size={ 16 } />
							{ /* eslint-enable wpcalypso/jsx-gridicon-size */ }
							{ translate( 'Manage Webauthn' ) }
						</Button>
					) }
				</SectionHeader>

				{ managingWebauthn && ! isWebauthnEnabled && (
					<Card>
						<Button
							onClick={ this.getClickHandler( 'Enable Webauthn Button', this.enableWebauthn ) }
						>
							{ translate( 'Enable Webauthn' ) }
						</Button>
					</Card>
				) }

				{ managingWebauthn && isWebauthnEnabled && (
					<Card>
						<Button
							onClick={ this.getClickHandler( 'Disable Webauthn Button', this.disableWebauthn ) }
						>
							{ translate( 'Disable Webauthn' ) }
						</Button>
					</Card>
				) }

				{ errorMessage && <Notice status="is-error" icon="notice" text={ errorMessage } /> }
				{ ! managingWebauthn && (
					<Card>
						{ isBrowserSupported && (
							<p>{ this.props.translate( 'Use Webauthn for Two-Factor Authentication.' ) }</p>
						) }
						{ ! isBrowserSupported && (
							<p>
								{ this.props.translate(
									"Your browser doesn't support the FIDO2 security key standard yet. To use a second factor security key to sign in please try a supported browsers like Chrome or Firefox."
								) }
							</p>
						) }
					</Card>
				) }
			</div>
		);
	}
}

export default connect(
	( state ) => ( {
		currentUser: getCurrentUser( state ),
	} ),
	{
		recordGoogleEvent,
	}
)( localize( Security2faWebauthn ) );
