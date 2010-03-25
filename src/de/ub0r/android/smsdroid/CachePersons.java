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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.Contacts;
import android.provider.Contacts.People;
import android.provider.Contacts.PeopleColumns;
import android.provider.Contacts.People.Extensions;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * Cache holding persons.
 * 
 * @author flx
 */
@SuppressWarnings("deprecation")
public final class CachePersons {
	/** Cached person. */
	private static class Person {
		/** Persons's ID. */
		long id = -1;
		/** Persons's name. */
		String name = null;
		/** There is no picture. */
		boolean noPicutre = false;
		/** Person's profile picture. */
		Bitmap picture = null;
	}

	/** Tag for output. */
	private static final String TAG = "SMSdroid.cp";

	/** Error message if API5 is not available. */
	private static final String ERRORMESG = "no API5 available";

	/** Pattern to clean up numbers. */
	private static final Pattern PATTERN_CLEAN_NUMBER = Pattern
			.compile("<([0-9]+)>");

	/** Cached data. */
	private static final HashMap<String, Person> CACHE = // .
	new HashMap<String, Person>();

	/** {@link Uri} for persons. */
	private static final Uri API3_URI_PERSON = // .
	Contacts.Phones.CONTENT_FILTER_URL;

	/** Projection for persons query. */
	private static final String[] API3_PROJECTION = // .
	new String[] { Extensions.PERSON_ID, PeopleColumns.DISPLAY_NAME };

	/** Index of id. */
	private static final int INDEX_ID = 0;
	/** Index of name. */
	private static final int INDEX_NAME = 1;

	/** Used {@link Uri} for query. */
	private static Uri uriPerson = API3_URI_PERSON;
	/** Used projection for query. */
	private static String[] projection = API3_PROJECTION;

	/** Use old API? */
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
	 * @return Person
	 */
	private static Person getNameForAddress(final Context context,
			final String address) {
		// clean up number
		final Matcher m = PATTERN_CLEAN_NUMBER.matcher(address);
		String realAddress = address;
		if (m.find()) {
			realAddress = m.group(1);
		}
		// address contains the phone number
		Uri uri = Uri.withAppendedPath(uriPerson, realAddress);
		if (uri != null) {
			Cursor cursor = context.getContentResolver().query(uri, projection,
					null, null, null);
			if (cursor.moveToFirst()) {
				final Person p = new Person();
				p.id = cursor.getLong(INDEX_ID);
				p.name = cursor.getString(INDEX_NAME);
				cursor.close();
				return p;
			}
			cursor.close();
		}
		return null;
	}

	/**
	 * Get picture for person.
	 * 
	 * @param context
	 *            {@link Context}
	 * @param person
	 *            Person
	 * @return {@link Bitmap}
	 */
	private static Bitmap getPictureForPerson(final Context context,
			final Person person) {
		if (person.noPicutre) {
			return null;
		}
		Bitmap b = null;
		if (useNewAPI) {
			b = HelperAPI5Contacts.loadContactPhoto(context, person.id);
		} else {
			Uri uri = ContentUris.withAppendedId(People.CONTENT_URI, person.id);
			Log.d(TAG, "load pic: " + uri.toString());
			b = People.loadContactPhoto(context, uri,
					R.drawable.ic_contact_picture, null);
		}
		person.picture = b;
		if (b == null) {
			person.noPicutre = true;
		} else {
			Log.d(TAG, "got picture for " + person.name + "/" + person.id);
		}
		return b;
	}

	/**
	 * Add a new Person to Cache.
	 * 
	 * @param address
	 *            person's address
	 * @param person
	 *            {@link Person}
	 * @return Person
	 */
	private static Person newEntry(final String address, final Person person) {
		Log.d(TAG, "put person to cache: " + address);
		Person p = person;
		if (p == null) {
			p = new Person();
		}
		CACHE.put(address, p);
		if (p.name != null) {
			Log.d(TAG, address + ": " + p.name);
			Log.d(TAG, address + ": " + p.id);
		}
		return p;
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
		Person p = CACHE.get(address);
		if (p == null) {
			// TODO: for targetView != null: spawn thread to do the work
			p = getNameForAddress(context, address);
			if (p != null) {
				newEntry(address, p);
			}
		}
		if (p != null) {
			if (targetView != null && p.name != null) {
				targetView.setText(p.name);
			}
			return p.name;
		} else {
			return null;
		}
	}

	/**
	 * Get id of a person.
	 * 
	 * @param context
	 *            {@link Context}
	 * @param address
	 *            person's address
	 * @return person's id
	 */
	public static long getID(final Context context, final String address) {
		Person p = CACHE.get(address);
		if (p == null) {
			getName(context, address, null); // try to get contact from database
			p = CACHE.get(address);
		}
		if (p != null) {
			return p.id;
		}
		return -1;
	}

	/**
	 * Get a picture for a person.
	 * 
	 * @param context
	 *            {@link Context}
	 * @param address
	 *            person's address
	 * @param targetView
	 *            {@link ImageView} the person should be set to
	 * @return person's picture
	 */
	public static Bitmap getPicture(final Context context,
			final String address, final ImageView targetView) {

		Person p = CACHE.get(address);
		if (p == null) {
			getName(context, address, null); // try to get contact from database
			p = CACHE.get(address);
		}
		Bitmap b = null;
		if (p != null) {
			b = getPictureForPerson(context, p);
		}
		if (targetView != null) {
			if (b != null) {
				targetView.setImageBitmap(b);
			} else {
				targetView.setImageResource(R.drawable.ic_contact_picture);
			}
		}
		return b;
	}
}
