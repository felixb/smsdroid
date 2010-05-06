/*
 * Copyright (C) 2010 Felix Bechstein
 * 
 * This file is part of SMSdroid.
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 3 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program; If not, see <http://www.gnu.org/licenses/>.
 */
package de.ub0r.android.smsdroid;

import java.io.InputStream;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Contacts;

/**
 * Helper class to set/unset background for api5 systems.
 * 
 * @author flx
 */
public final class ContactsWrapper5 extends ContactsWrapper {
	/** Projection for persons query, filter. */
	// FIXME: use LOOKUP_KEY instead
	private static final String[] API5_PROJECTION_FILTER = // .
	new String[] { ContactsContract.CommonDataKinds.Phone.LOOKUP_KEY,
			ContactsContract.PhoneLookup.DISPLAY_NAME,
			ContactsContract.CommonDataKinds.Phone.NUMBER };

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Bitmap loadContactPhoto(final Context context, // .
			final String contactId) {
		final ContentResolver cr = context.getContentResolver();
		InputStream is = Contacts.openContactPhotoInputStream(cr, this
				.getContactUri(cr, contactId));
		if (is == null) {
			return null;
		}
		return BitmapFactory.decodeStream(is);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Uri getUriFilter() {
		return ContactsContract.CommonDataKinds.Phone.CONTENT_URI;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Uri getContactUri(final ContentResolver cr, final String id) {
		return Contacts.lookupContact(cr, Uri.withAppendedPath(
				Contacts.CONTENT_LOOKUP_URI, id));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String[] getProjectionFilter() {
		return API5_PROJECTION_FILTER;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Intent getInsertPickIntent(final String address) {
		final Intent i = new Intent(Intent.ACTION_INSERT_OR_EDIT);
		i.setType(ContactsContract.Contacts.CONTENT_ITEM_TYPE);
		i.putExtra(ContactsContract.Intents.Insert.PHONE, address);
		i.putExtra(ContactsContract.Intents.Insert.PHONE_TYPE,
				ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE);
		return i;
	}

}
