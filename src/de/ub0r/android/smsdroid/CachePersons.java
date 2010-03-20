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

import java.util.HashMap;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.Contacts;
import android.provider.Contacts.PeopleColumns;
import android.util.Log;
import android.widget.TextView;

/**
 * Cache holding persons.
 * 
 * @author flx
 */
@SuppressWarnings("deprecation")
public final class CachePersons {
	/** Tag for output. */
	private static final String TAG = "SMSdroid.cp";

	/** Error message if API5 is not available. */
	private static final String ERRORMESG = "no API5 available";

	/** Cached data. */
	private static final HashMap<String, String> CACHE = // .
	new HashMap<String, String>();

	/** {@link Uri} for persons. */
	private static final Uri API3_URI_PERSON = // .
	Contacts.Phones.CONTENT_FILTER_URL;

	/** Projection for persons query. */
	private static final String[] API3_PROJECTION = // .
	new String[] { PeopleColumns.NAME };

	/** Used {@link Uri} for query. */
	private static Uri uriPerson = API3_URI_PERSON;
	/** Used projection for query. */
	private static String[] projection = API3_PROJECTION;

	/** Use old API? */
	@SuppressWarnings("unused")
	private static boolean useNewAPI = isAvailable();

	/** Private Constructor. */
	private CachePersons() {
	}

	/**
	 * Check whether API5 is available.
	 * 
	 * @return true if API5 is available
	 */
	static boolean isAvailable() {
		try {
			if (new HelperAPI5Contacts().isAvailable()) {
				uriPerson = HelperAPI5Contacts.getUri();
				projection = HelperAPI5Contacts.getProjections();
				return true;
			}
		} catch (Throwable e) {
			Log.d(TAG, ERRORMESG, e);
			return false;
		}
		Log.d(TAG, ERRORMESG);
		return false;
	}

	/**
	 * Get name for address.
	 * 
	 * @param context
	 *            {@link Context}
	 * @param address
	 *            address
	 * @return name
	 */
	private static String getNameForAddress(final Context context,
			final String address) {
		// address contains the phone number
		Uri phoneUri = Uri.withAppendedPath(uriPerson, address);
		if (phoneUri != null) {
			Cursor phoneCursor = context.getContentResolver().query(phoneUri,
					projection, null, null, null);
			if (phoneCursor.moveToFirst()) {
				final String ret = phoneCursor.getString(0);
				if (ret != null) {
					Log.d(TAG, "resolved address: " + address + " -> " + ret);
					CACHE.put(address, ret);
				}
				return ret;
			}
		}
		return null;
	}

	/**
	 * Get a name for a person.
	 * 
	 * @param context
	 *            {@link Context}
	 * @param address
	 *            person's address
	 * @param targetView
	 *            {@link TextView} the person should be set to
	 * @return person's name
	 */
	public static String getName(final Context context, final String address,
			final TextView targetView) {
		String ret = CACHE.get(address);
		if (ret == null) {
			// TODO: for targetView != null: spawn thread to do the work
			ret = getNameForAddress(context, address);
		}
		if (targetView != null && ret != null) {
			targetView.setText(ret);
		}
		return ret;
	}
}
