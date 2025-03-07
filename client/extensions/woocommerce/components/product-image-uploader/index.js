/**
 * External dependencies
 */

import React, { Component } from 'react';
import PropTypes from 'prop-types';
import classNames from 'classnames';
import { connect } from 'react-redux';
import { head, trim, uniqueId } from 'lodash';
import Gridicon from 'calypso/components/gridicon';
import { localize } from 'i18n-calypso';

/**
 * Internal dependencies
 */
import { getSelectedSiteWithFallback } from 'woocommerce/state/sites/selectors';
import { errorNotice as errorNoticeAction } from 'calypso/state/notices/actions';
import DropZone from 'calypso/components/drop-zone';
import FilePicker from 'calypso/components/file-picker';
import getMediaErrors from 'calypso/state/selectors/get-media-errors';
import { filterItemsByMimePrefix } from 'calypso/lib/media/utils';
import { addWoocommerceProductImage } from 'calypso/state/media/thunks';

const noop = () => {};

class ProductImageUploader extends Component {
	static propTypes = {
		site: PropTypes.shape( {
			ID: PropTypes.number.isRequired,
			options: PropTypes.object.isRequired,
		} ),
		multiple: PropTypes.bool,
		compact: PropTypes.bool,
		onSelect: PropTypes.func,
		onUpload: PropTypes.func,
		onError: PropTypes.func,
		onFinish: PropTypes.func,
		addWoocommerceProductImage: PropTypes.func,
	};

	static defaultProps = {
		multiple: true,
		compact: false,
		onSelect: noop,
		onUpload: noop,
		onError: noop,
		onFinish: noop,
		addWoocommerceProductImage: noop,
	};

	UNSAFE_componentWillMount() {
		this._isMounted = true;
	}
	componentWillUnmount() {
		this._isMounted = false;
	}

	showError = ( media ) => {
		const { ID: transientId } = media;
		const { mediaValidationErrors, onError, errorNotice, translate } = this.props;

		onError( {
			file: media,
			transientId,
		} );

		let extraDetails;
		const validationError = mediaValidationErrors[ transientId ] || [];
		switch ( head( validationError ) ) {
			case 'EXCEEDS_PLAN_STORAGE_LIMIT':
			case 'NOT_ENOUGH_SPACE':
				extraDetails = translate( 'You have reached your plan storage limit.' );
				break;
		}

		let message;
		if ( media && media.file ) {
			message = translate( 'There was a problem uploading %s.', {
				args: media.file,
			} );
		} else {
			message = translate( 'There was a problem uploading your image.' );
		}

		errorNotice( ( extraDetails && message + ' ' + extraDetails ) || message );
	};

	// https://stackoverflow.com/a/20732091
	displayableFileSize( size ) {
		const i = Math.floor( Math.log( size ) / Math.log( 1024 ) );
		return (
			( size / Math.pow( 1024, i ) ).toFixed( 2 ) * 1 + ' ' + [ 'B', 'kB', 'MB', 'GB', 'TB' ][ i ]
		);
	}

	buildFilesToUpload = ( images ) => {
		const { site, errorNotice, translate } = this.props;
		const maxUploadSize = ( site.options && site.options.max_upload_size ) || null;
		const displayableFileSize = this.displayableFileSize( maxUploadSize );
		const filesToUpload = [];

		images.forEach( function ( image ) {
			if ( maxUploadSize && image.size > maxUploadSize ) {
				errorNotice(
					translate( '%(name)s exceeds the maximum upload size (%(size)s) for this site.', {
						args: {
							name: image.name,
							size: displayableFileSize,
						},
					} )
				);
				return;
			}
			filesToUpload.push( {
				ID: uniqueId( 'product-images-' ),
				fileContents: image,
				fileName: image.name,
				preview: URL.createObjectURL( image ),
			} );
		} );

		return filesToUpload;
	};

	onPick = ( files ) => {
		const { site, multiple } = this.props;
		const { onSelect, onUpload, onFinish } = this.props;

		// DropZone supplies an array, FilePicker supplies a FileList
		let images = Array.isArray( files ) ? filterItemsByMimePrefix( files, 'image' ) : [ ...files ];
		if ( ! images ) {
			return false;
		}

		if ( multiple === false ) {
			images = [ images.shift() ];
		}

		const filesToUpload = this.buildFilesToUpload( images );
		if ( filesToUpload.length === 0 ) {
			return;
		}

		onSelect( filesToUpload );

		this.props.addWoocommerceProductImage(
			filesToUpload,
			site,
			onUpload,
			this.showError,
			onFinish
		);
	};

	renderCompactUploader() {
		const { multiple } = this.props;
		return (
			<div className="product-image-uploader__picker compact">
				<FilePicker multiple={ multiple } accept="image/*" onPick={ this.onPick }>
					<Gridicon icon="add-image" size={ 24 } />
				</FilePicker>
			</div>
		);
	}

	renderUploader() {
		const { translate, multiple } = this.props;

		const addString = multiple ? translate( 'Add images' ) : translate( 'Add image' );

		return (
			<div className="product-image-uploader__wrapper">
				<div className="product-image-uploader__picker">
					<FilePicker multiple={ multiple } accept="image/*" onPick={ this.onPick }>
						<div>
							<Gridicon icon="add-image" size={ 36 } />
							<p>{ addString }</p>
						</div>
					</FilePicker>
				</div>
				<DropZone onFilesDrop={ this.onPick } />
			</div>
		);
	}

	renderChildren() {
		return React.Children.map( this.props.children, function ( child ) {
			return <div>{ child }</div>;
		} );
	}

	renderPlaceholder() {
		const { translate, compact } = this.props;

		const classes = classNames( 'product-image-uploader__wrapper placeholder', {
			compact,
		} );

		return (
			<div className={ classes }>
				<div className="product-image-uploader__picker">
					<div className="product-image-uploader__file-picker">
						<Gridicon icon="add-image" size={ 36 } />
						<p>{ ! compact && translate( 'Loading' ) }</p>
					</div>
				</div>
			</div>
		);
	}

	render() {
		const { compact, children, multiple, site } = this.props;

		if ( ! site ) {
			return this.renderPlaceholder();
		}

		if ( '' !== trim( children ) ) {
			return (
				<FilePicker multiple={ multiple } accept="image/*" onPick={ this.onPick }>
					{ this.renderChildren() }
				</FilePicker>
			);
		}

		return ( compact && this.renderCompactUploader() ) || this.renderUploader();
	}
}

function mapStateToProps( state ) {
	const site = getSelectedSiteWithFallback( state );

	return {
		site,
		mediaValidationErrors: getMediaErrors( state, site.ID ),
	};
}

export default connect( mapStateToProps, {
	errorNotice: errorNoticeAction,
	addWoocommerceProductImage,
} )( localize( ProductImageUploader ) );
