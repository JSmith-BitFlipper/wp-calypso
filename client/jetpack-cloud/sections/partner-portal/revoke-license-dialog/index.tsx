/**
 * External dependencies
 */
import React, { ReactElement, useCallback } from 'react';
import { useDispatch } from 'react-redux';
import { useMutation } from 'react-query';
import { useTranslate } from 'i18n-calypso';

/**
 * Internal dependencies
 */
import { Button, Dialog } from '@automattic/components';
import Gridicon from 'calypso/components/gridicon';
import { APILicense } from 'calypso/state/partner-portal/types';
import { wpcomJetpackLicensing as wpcomJpl } from 'calypso/lib/wp';
import { errorNotice } from 'calypso/state/notices/actions';
import { noop } from 'calypso/jetpack-cloud/sections/partner-portal/utils';

/**
 * Style dependencies
 */
import './style.scss';

interface Props {
	licenseKey: string;
	product: string;
	onClose: ( action?: string ) => void;
	[ rest: string ]: any;
}

function mutationRevokeLicense( licenseKey: string ): Promise< APILicense > {
	return wpcomJpl.req.post( {
		method: 'DELETE',
		apiNamespace: 'wpcom/v2',
		path: '/jetpack-licensing/license',
		body: { license_key: licenseKey },
	} );
}

export default function RevokeLicenseDialog( {
	licenseKey,
	product,
	onClose,
	...rest
}: Props ): ReactElement {
	let close = noop;
	const translate = useTranslate();
	const dispatch = useDispatch();
	const mutation = useMutation( mutationRevokeLicense, {
		onSuccess: ( license ) => {
			console.log( license );
			close();
			// TODO refresh listing?
		},
		onError: ( error: Error ) => {
			dispatch( errorNotice( error.message ) );
		},
	} );

	close = useCallback( () => {
		if ( ! mutation.isLoading ) {
			onClose();
		}
	}, [ onClose, mutation.isLoading ] );

	const revoke = useCallback( () => {
		mutation.mutate( licenseKey );
	}, [ licenseKey, mutation.mutate ] );

	// TODO there seems to be a Chrome bug that keeps considering the button :hover-ed when pointer-events: none is applied on click (i.e. while hovered).
	// TODO the above applies to the issue license button as well.
	const buttons = [
		<Button disabled={ false } onClick={ close }>
			{ translate( 'Go back' ) }
		</Button>,
		<Button primary scary busy={ mutation.isLoading } onClick={ revoke }>
			{ translate( 'Revoke License' ) }
		</Button>,
	];

	return (
		<Dialog
			isVisible={ true }
			buttons={ buttons }
			additionalClassNames="revoke-license-dialog"
			onClose={ close }
			{ ...rest }
		>
			<h2 className="revoke-license-dialog__heading">
				{ translate( 'Are you sure you want to revoke this license?' ) }
			</h2>

			<p>
				{ translate(
					'A revoked license cannot be reused and the associated site will no longer have access to the provisioned product. You will stop being billing for this license immediately.'
				) }
			</p>

			<ul>
				<li>
					<strong>{ translate( 'Site:' ) }</strong> { 'mygroovysite.co.uk' }
				</li>
				<li>
					<strong>{ translate( 'Product:' ) }</strong> { product }
				</li>
				<li>
					<strong>{ translate( 'License:' ) }</strong> <code>{ licenseKey }</code>
				</li>
			</ul>

			<p className="revoke-license-dialog__warning">
				<Gridicon icon="info-outline" size={ 18 } />

				{ translate( 'Please note this action cannot be undone.' ) }
			</p>
		</Dialog>
	);
}
