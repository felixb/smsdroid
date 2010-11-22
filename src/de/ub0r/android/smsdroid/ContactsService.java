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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.IntentService;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.preference.PreferenceManager;
import de.ub0r.android.lib.Log;
import de.ub0r.android.lib.apis.ContactsWrapper;
import de.ub0r.android.smsdroid.ConversationProvider.Threads;

/**
 * {@link IntentService} updating contacts information.
 * 
 * @author flx
 */
public final class ContactsService extends IntentService {
	/** Tag for logging. */
	private static final String TAG = "cs";

	/** Wrapper to use for contacts API. */
	private static final ContactsWrapper WRAPPER = ContactsWrapper
			.getInstance();

	/** Pattern to clean up numbers. */
	private static final Pattern PATTERN_CLEAN_NUMBER = Pattern
			.compile("<(\\+?[0-9]+)>");

	/** Preference's name: last run. */
	private static final String PREFS_LASTRUN = "cs_lastrun";

	/** Minimal time to wait between runs. */
	private static final long MIN_WAIT_TIME = 60000L;

	/**
	 * Default Constructor.
	 */
	public ContactsService() {
		super("ContactsService");
	}

	/**
	 * Start this {@link IntentService}.
	 * 
	 * @param context
	 *            {@link Context}
	 */
	public static void startService(final Context context) {
		final SharedPreferences p = PreferenceManager
				.getDefaultSharedPreferences(context);
		final long lastRun = p.getLong(PREFS_LASTRUN, 0L);
		if (lastRun + MIN_WAIT_TIME < System.currentTimeMillis()) {
			context.startService(new Intent(context, ContactsService.class));
		}
	}

	@Override
	protected void onHandleIntent(final Intent intent) {
		Log.i(TAG, "start ContactsService: " + intent);
		int changed = 0;
		final ContentResolver cr = this.getContentResolver();
		Cursor cursor = cr.query(Threads.CONTENT_URI, Threads.PROJECTION, null,
				null, null);
		if (cursor != null && cursor.moveToFirst()) {
			do {
				String address = cursor.getString(Threads.INDEX_ADDRESS);
				if (address == null) {
					continue;
				}
				address = address.trim();
				if (address.length() == 0) {
					continue;
				}
				final Cursor contact = getContact(cr, address);
				if (contact == null || !contact.moveToFirst()) {
					continue;
				}
				final long tid = cursor.getLong(Threads.INDEX_ID);
				final String name = contact
						.getString(ContactsWrapper.FILTER_INDEX_NAME);
				final String pid = contact
						.getString(ContactsWrapper.FILTER_INDEX_ID);
				final ContentValues values = new ContentValues(2);
				if (name != null
						&& !name.equals(cursor.getString(Threads.INDEX_NAME))) {
					values.put(Threads.NAME, name);
				}
				if (pid != null
						&& !pid.equals(cursor.getString(Threads.INDEX_PID))) {
					values.put(Threads.PID, pid);
				}
				if (values.size() > 0) {
					changed += cr.update(ContentUris.withAppendedId(
							Threads.CONTENT_URI, tid), values, null, null);
				}
				if (!contact.isClosed()) {
					contact.close();
				}
			} while (cursor.moveToNext());
		}
		if (cursor != null && !cursor.isClosed()) {
			cursor.close();
		}
		final SharedPreferences p = PreferenceManager
				.getDefaultSharedPreferences(this);
		p.edit().putLong(PREFS_LASTRUN, System.currentTimeMillis()).commit();
	}

	/**
	 * Get (id, name) for address.
	 * 
	 * @param cr
	 *            {@link ContentResolver}
	 * @param address
	 *            address
	 * @return {@link Cursor}
	 */
	private static synchronized Cursor getContact(final ContentResolver cr,
			final String address) {
		Log.d(TAG, "getContact(ctx, " + address + ")");
		if (address == null) {
			return null;
		}
		// clean up number
		String realAddress = address;
		final Matcher m = PATTERN_CLEAN_NUMBER.matcher(realAddress);
		if (m.find()) {
			realAddress = m.group(1);
			Log.d(TAG, "real address: " + realAddress);
		}
		// address contains the phone number
		try {
			final Cursor cursor = WRAPPER.getContact(cr, realAddress);
			if (cursor != null && cursor.moveToFirst()) {
				return cursor;
			}
		} catch (Exception e) {
			Log.e(TAG, "failed to fetch contact", e);
		}
		Log.d(TAG, "nothing found!");
		return null;
	}
}
