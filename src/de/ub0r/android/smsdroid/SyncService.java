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
import android.net.Uri;
import android.preference.PreferenceManager;
import de.ub0r.android.lib.Log;
import de.ub0r.android.lib.apis.ContactsWrapper;
import de.ub0r.android.smsdroid.MessageProvider.Messages;
import de.ub0r.android.smsdroid.MessageProvider.Threads;

/**
 * {@link IntentService} updating contacts information.
 * 
 * @author flx
 */
public final class SyncService extends IntentService {
	/** Tag for logging. */
	private static final String TAG = "sync";

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

	/** Action: sync contacts. */
	private static final String ACTION_SYNC_CONTACTS = "de.ub0r.android"
			+ ".smsdroid.SYNC_CONTACTS";
	/** Action: sync messages. */
	private static final String ACTION_SYNC_MESSAGES = "de.ub0r.android"
			+ ".smsdroid.SYNC_MESSAGES";
	/** Action: sync threads. */
	private static final String ACTION_SYNC_THREADS = "de.ub0r.android"
			+ ".smsdroid.SYNC_THREADS";

	/**
	 * Default Constructor.
	 */
	public SyncService() {
		super("ContactsService");
	}

	/**
	 * Start this {@link IntentService} in background to sync the contacts meta
	 * data to threads.
	 * 
	 * @param context
	 *            {@link Context}
	 */
	public static void syncContacts(final Context context) {
		final SharedPreferences p = PreferenceManager
				.getDefaultSharedPreferences(context);
		final long lastRun = p.getLong(PREFS_LASTRUN, 0L);
		if (lastRun + MIN_WAIT_TIME < System.currentTimeMillis()) {
			Log.d(TAG, "call startService: " + ACTION_SYNC_CONTACTS);
			context.startService(new Intent(ACTION_SYNC_CONTACTS, null,
					context, SyncService.class));
		} else {
			Log.d(TAG, "skip startService: " + ACTION_SYNC_CONTACTS);
		}
	}

	/**
	 * Start this {@link IntentService} in background to sync messages to
	 * internal database.
	 * 
	 * @param context
	 *            {@link Context}
	 */
	public static void syncMessages(final Context context) {
		Log.d(TAG, "call startService: " + ACTION_SYNC_MESSAGES);
		context.startService(new Intent(ACTION_SYNC_MESSAGES, null, context,
				SyncService.class));
	}

	/**
	 * Start this {@link IntentService} in background to sync threads to
	 * internal database.
	 * 
	 * @param context
	 *            {@link Context}
	 * @param threadId
	 *            thread to sync
	 */
	public static void syncThreads(final Context context, final long threadId) {
		Log.d(TAG, "call startService: " + ACTION_SYNC_THREADS);
		Log.d(TAG, "threadId: " + threadId);
		final Intent i = new Intent(ACTION_SYNC_THREADS, null, context,
				SyncService.class);
		if (threadId >= 0L) {
			i.setData(ContentUris.withAppendedId(Threads.CACHE_URI, threadId));
		}
		context.startService(i);
	}

	@Override
	protected void onHandleIntent(final Intent intent) {
		Log.i(TAG, "start SyncService: " + intent);
		final String action = intent.getAction();
		final Uri uri = intent.getData();
		if (action == null) {
			return;
		} else if (action.equals(ACTION_SYNC_CONTACTS)) {
			this.syncContacts(intent);
		} else if (action.equals(ACTION_SYNC_MESSAGES)) {
			this.syncMessages(intent);
		} else if (action.equals(ACTION_SYNC_THREADS)) {
			if (uri == null) {
				this.syncThreads(intent);
			} else {
				final long threadId = ContentUris.parseId(uri);
				this.syncThread(threadId);
			}
		} else {
			return;
		}

		// FIXME this.getContentResolver().notifyChange(Messages.CONTENT_URI,
		// null);
	}

	/**
	 * Sync contacts meta data to threads.
	 * 
	 * @param intent
	 *            {@link Intent} which started the service
	 */
	private void syncContacts(final Intent intent) {
		Log.d(TAG, "syncContacts(" + intent + ")");
		final SharedPreferences p = PreferenceManager
				.getDefaultSharedPreferences(this);
		final long lastRun = p.getLong(PREFS_LASTRUN, 0L);
		if (lastRun + MIN_WAIT_TIME > System.currentTimeMillis()) {
			Log.d(TAG, "skip syncContacts(" + intent + ")");
			return;
		}

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
		p.edit().putLong(PREFS_LASTRUN, System.currentTimeMillis()).commit();
	}

	/**
	 * Sync messages to internal database.
	 * 
	 * @param intent
	 *            {@link Intent} which started the service
	 */
	private void syncMessages(final Intent intent) {
		Log.d(TAG, "syncMessages(" + intent + ")");
	}

	/**
	 * Sync messages to internal database.
	 * 
	 * @param intent
	 *            {@link Intent} which started the service
	 */
	private void syncThreads(final Intent intent) {
		Log.d(TAG, "syncThreads(" + intent + ")");

		final String[] mproj = new String[] { Messages.THREADID };
		final String[] cproj = new String[] { Threads.ID };
		final ContentResolver cr = this.getContentResolver();
		final Cursor mcursor = cr.query(Messages.CACHE_THREADS_URI, mproj,
				null, null, Messages.THREADID + " ASC");
		final Cursor ccursor = cr.query(Threads.CACHE_URI, cproj, null, null,
				Threads.ID + " ASC");
		if (mcursor != null && ccursor != null && mcursor.moveToFirst()) {
			ccursor.moveToFirst();
			long mtid = -1L;
			do {
				mtid = mcursor.getLong(0);
				long ctid = -1L;
				Log.d(TAG, "mtid: " + mtid);
				if (!ccursor.isAfterLast()) {
					do {
						ctid = ccursor.getLong(0);
						Log.d(TAG, "ctid: " + ctid);
						if (ctid < mtid) {
							Log.d(TAG, "delete: tid=" + ctid);
							cr.delete(Threads.CACHE_URI, Threads.ID + " = "
									+ ctid, null);
						} else {
							break;
						}
					} while (ccursor.moveToNext());
				}
				this.syncThread(mtid);
				if (ctid == mtid) {
					ccursor.moveToNext();
				}
			} while (mcursor.moveToNext());
			Log.d(TAG, "delete: tid>" + mtid);
			cr.delete(Threads.CACHE_URI, Threads.ID + " > " + mtid, null);
		}
		if (mcursor != null && !mcursor.isClosed()) {
			mcursor.close();
		}
		if (ccursor != null && !ccursor.isClosed()) {
			ccursor.close();
		}

		syncContacts(this);
	}

	/**
	 * Update Threads table from Messages.
	 * 
	 * @param threadId
	 *            thread's ID, -1 for all
	 */
	private void syncThread(final long threadId) {
		Log.d(TAG, "syncThread(" + threadId + ")");
		final ContentResolver cr = this.getContentResolver();
		final String[] proj = new String[] {// .
		Messages.DATE, // 0
				Messages.BODY, // 1
				Messages.ADDRESS, // 2
				Messages.TYPE, // 3
		};

		Cursor cursor = cr.query(Messages.CACHE_URI, proj, Messages.THREADID
				+ " = " + threadId, null, Messages.DATE + " DESC");
		final ContentValues values = new ContentValues();
		if (cursor != null && cursor.moveToFirst()) {
			values.put(Threads.DATE, cursor.getLong(0));
			values.put(Threads.BODY, cursor.getString(1));
			values.put(Threads.ADDRESS, cursor.getString(2));
			values.put(Threads.TYPE, cursor.getInt(3));
			values.put(Threads.COUNT, cursor.getCount());
		}
		if (cursor != null && !cursor.isClosed()) {
			cursor.close();
		}
		cursor = cr.query(Messages.CACHE_URI, proj, Messages.THREADID + " = "
				+ threadId + " AND " + Messages.READ + " = 0", null, null);
		if (cursor != null && cursor.getCount() > 0) {
			values.put(Threads.READ, 0);
		} else if (values.size() > 0) {
			values.put(Threads.READ, 1);
		}
		if (cursor != null && !cursor.isClosed()) {
			cursor.close();
		}
		if (values.size() > 0) {
			Log.d(TAG, "update thread: " + threadId + "/ " + values);
			int ret = cr.update(Threads.CACHE_URI, values, Threads.ID + " = "
					+ threadId, null);
			if (ret <= 0) {
				Log.d(TAG, "insert thread: " + threadId);
				values.put(Threads.ID, threadId);
				cr.insert(Threads.CACHE_URI, values);
			}
		}
		Log.d(TAG, "exit syncThread(" + threadId + ")");
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
