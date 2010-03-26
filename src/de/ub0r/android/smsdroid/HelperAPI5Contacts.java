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
import java.lang.reflect.Method;

import android.app.Notification;
import android.app.Service;
import android.content.ContentUris;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Contacts;
import android.util.Log;

/**
 * Helper class to set/unset background for api5 systems.
 * 
 * @author flx
 */
public final class HelperAPI5Contacts {
	/** Tag for output. */
	private static final String TAG = "SMSdroid.api5c";

	/** Error message if API5 is not available. */
	private static final String ERRORMESG = "no API5c available";

	/** {@link Uri} for persons, content filter. */
	private static final Uri API5_URI_CONTENT_FILTER = // .
	ContactsContract.CommonDataKinds.Phone.CONTENT_FILTER_URI;

	/** Projection for persons query, filter. */
	private static final String[] API5_PROJECTION_FILTER = // .
	new String[] { ContactsContract.Data.CONTACT_ID,
			ContactsContract.Data.DISPLAY_NAME };

	/**
	 * Check whether API5 is available.
	 * 
	 * @return true if API5 is available
	 */
	boolean isAvailable() {
		try {
			Method mDebugMethod = Service.class.getMethod("startForeground",
					new Class[] { Integer.TYPE, Notification.class });
			/* success, this is a newer device */
			if (mDebugMethod != null) {
				return true;
			}
		} catch (Throwable e) {
			Log.d(TAG, ERRORMESG, e);
			throw new VerifyError(ERRORMESG);
		}
		Log.d(TAG, ERRORMESG);
		throw new VerifyError(ERRORMESG);
	}

	/**
	 * Load ContactPhoto from database.
	 * 
	 * @param context
	 *            {@link Context}
	 * @param contactId
	 *            id of contact
	 * @return {@link Bitmap}
	 */
	static Bitmap loadContactPhoto(final Context context, // .
			final long contactId) {
		InputStream is = Contacts.openContactPhotoInputStream(context
				.getContentResolver(), ContentUris.withAppendedId(
				Contacts.CONTENT_URI, contactId));
		if (is == null) {
			return null;
		}
		return BitmapFactory.decodeStream(is);
	}

	/**
	 * Get Persons {@link Uri}, filter.
	 * 
	 * @return {@link Uri}
	 */
	static Uri getUriFilter() {
		return API5_URI_CONTENT_FILTER;
	}

	/**
	 * Get projection.
	 * 
	 * @return projection
	 */
	static String[] getProjectionFilter() {
		return API5_PROJECTION_FILTER;
	}
}
