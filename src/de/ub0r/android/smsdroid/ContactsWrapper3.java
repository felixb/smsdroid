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

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.Contacts;
import android.provider.Contacts.People;
import android.provider.Contacts.PeopleColumns;
import android.provider.Contacts.PhonesColumns;
import android.provider.Contacts.People.Extensions;
import de.ub0r.android.lib.Log;

/**
 * Implement {@link ContactsWrapper} for API 3 and 4.
 * 
 * @author flx
 */
@SuppressWarnings("deprecation")
public final class ContactsWrapper3 extends ContactsWrapper {
	/** Tag for output. */
	private static final String TAG = "cw3";

	/** Projection for persons query, filter. */
	private static final String[] PROJECTION_FILTER = // .
	new String[] { Extensions.PERSON_ID, PeopleColumns.DISPLAY_NAME,
			PhonesColumns.NUMBER };

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Uri getContactUri(final ContentResolver cr, final String id) {
		return Uri.withAppendedPath(People.CONTENT_URI, id);
	}

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
			Uri uri = Uri.withAppendedPath(People.CONTENT_URI, contactId);
			return People.loadContactPhoto(context, uri,
					R.drawable.ic_contact_picture, null);
		} catch (Exception e) {
			Log.e(TAG, "error getting photo: " + contactId, e);
			return null;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Cursor getContact(final ContentResolver cr, // .
			final String number) {
		final Uri uri = Uri.withAppendedPath(
				Contacts.Phones.CONTENT_FILTER_URL, number);
		Log.d(TAG, "query: " + uri);
		Cursor c = cr.query(uri, PROJECTION_FILTER, null, null, null);
		if (c.moveToFirst()) {
			Log.d(TAG, "id: " + c.getString(FILTER_INDEX_ID));
			Log.d(TAG, "name: " + c.getString(FILTER_INDEX_NAME));
			Log.d(TAG, "number: " + c.getString(FILTER_INDEX_NUMBER));
			return c;
		}
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Intent getInsertPickIntent(final String address) {
		final Intent i = new Intent(Intent.ACTION_INSERT_OR_EDIT);
		i.setType(People.CONTENT_ITEM_TYPE);
		i.putExtra(Contacts.Intents.Insert.PHONE, address);
		i.putExtra(Contacts.Intents.Insert.PHONE_TYPE,
				Contacts.PhonesColumns.TYPE_MOBILE);
		return i;
	}
}
