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
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.CommonDataKinds.Phone;

/**
 * Helper class to set/unset background for api5 systems.
 * 
 * @author flx
 */
public final class ContactsWrapper5 extends ContactsWrapper {
	/** Tag for output. */
	private static final String TAG = "cw5";

	/** Projection for persons query, filter. */
	private static final String[] PROJECTION_FILTER = // .
	new String[] { ContactsContract.CommonDataKinds.Phone.LOOKUP_KEY,
			ContactsContract.PhoneLookup.DISPLAY_NAME,
			ContactsContract.CommonDataKinds.Phone.NUMBER };

	/** Length of a international prefix, + notation. */
	private static final int MIN_LEN_PLUS = "+49x".length();
	/** Length of a international prefix, 00 notation. */
	private static final int MIN_LEN_ZERO = "0049x".length();

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Bitmap loadContactPhoto(final Context context, // .
			final String contactId) {
		if (contactId == null || contactId.length() == 0) {
			return null;
		}
		try {
			final ContentResolver cr = context.getContentResolver();
			InputStream is = Contacts.openContactPhotoInputStream(cr, this
					.getContactUri(cr, contactId));
			if (is == null) {
				return null;
			}
			return BitmapFactory.decodeStream(is);
		} catch (Exception e) {
			Log.e(TAG, "error getting photo: " + contactId, e);
			return null;
		}
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
		return PROJECTION_FILTER;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Cursor getContact(final ContentResolver cr, // .
			final String number) {

		String realAddress = number;
		final int l = realAddress.length();
		if (l > MIN_LEN_PLUS && realAddress.startsWith("+")) {
			realAddress = "%" + realAddress.substring(MIN_LEN_PLUS);
		} else if (l > MIN_LEN_ZERO && realAddress.startsWith("00")) {
			realAddress = "%" + realAddress.substring(MIN_LEN_ZERO);
		} else if (realAddress.startsWith("0")) {
			realAddress = "%" + realAddress.substring(1);
		}

		Uri uri; // = Uri.withAppendedPath(
		// ContactsContract.CommonDataKinds.Phone.CONTENT_FILTER_URI,
		// number); // FIXME: this is broken in android os; issue #8255
		String[] proj; // = PROJECTION_FILTER;
		uri = Uri.withAppendedPath(
				android.provider.Contacts.Phones.CONTENT_FILTER_URL, number);
		proj = ContactsWrapper3.PROJECTION_FILTER;
		Log.d(TAG, "query: " + uri);
		Cursor c = cr.query(uri, proj, null, null, null);
		if (c.moveToFirst()) {
			// FIXME: this is an workaround!
			Cursor c0 = cr.query(Phone.CONTENT_URI, PROJECTION_FILTER,
					PROJECTION_FILTER[FILTER_INDEX_NUMBER] + " = '"
							+ c.getString(FILTER_INDEX_NUMBER) + "'", null,
					null);
			if (c0 != null && c0.moveToFirst()) {
				Log.d(TAG, "id: " + c0.getString(FILTER_INDEX_ID));
				Log.d(TAG, "name: " + c0.getString(FILTER_INDEX_NAME));
				Log.d(TAG, "number: " + c0.getString(FILTER_INDEX_NUMBER));
				return c0;
			}
		}
		return null;
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
