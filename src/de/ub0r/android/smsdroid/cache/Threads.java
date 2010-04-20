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
package de.ub0r.android.smsdroid.cache;

import java.util.HashMap;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;
import de.ub0r.android.smsdroid.Message;

/**
 * Cache holding threads.
 * 
 * @author flx
 */
public final class Threads {
	/** Cached person. */
	private static class Thread {
		/** Thread's address. */
		String address = null;
		/** Thread's count. */
		int count = -1;
		/** Timestamp of last read. */
		long lastCheck = -1;
	}

	/** Tag for output. */
	private static final String TAG = "SMSdroid.ct";

	/** Cached data. */
	private static final HashMap<Long, Thread> CACHE = // .
	new HashMap<Long, Thread>();

	/** Time of valid cache. */
	private static long validCache = 0;

	/** Private Constructor. */
	private Threads() {
	}

	/**
	 * Get data from DB.
	 * 
	 * @param context
	 *            {@link Context}
	 * @param id
	 *            thread id
	 * @return {@link Thread}
	 */
	private static Thread getData(final Context context, final long id,
			final Thread thread) {
		Log.d(TAG, "id: " + id);
		// address contains the phone number
		Uri uri = Uri.parse("content://mms-sms/conversations/" + id);
		try {
			Cursor cursor;
			cursor = context.getContentResolver().query(uri,
					Message.PROJECTION, null, null, null);
			if (cursor != null && cursor.moveToFirst()) {
				Thread t = thread;
				if (t == null) {
					t = new Thread();
				}
				t.count = cursor.getCount();
				if (t.address != null) {
					String a = null;
					do {
						a = cursor.getString(Message.INDEX_ADDRESS);
					} while (a == null && cursor.moveToNext());
					t.address = a;
				}
				cursor.close();
				t.lastCheck = System.currentTimeMillis();
				return t;
			}
			cursor.close();
		} catch (Exception e) {
			Log.e(TAG, "failed to fetch thread", e);
		}
		return null;
	}

	/**
	 * Add a new {@link Thread} to Cache.
	 * 
	 * @param id
	 *            {@link Thread}'s id
	 * @param thread
	 *            {@link Thread}
	 * @return Person
	 */
	private static Thread newEntry(final long id, final Thread thread) {
		Log.d(TAG, "put thread to cache: " + id);
		Thread t = thread;
		if (t == null) {
			t = new Thread();
		}
		CACHE.put(id, t);
		Log.d(TAG, id + ": " + t.address + " (" + t.count + ")");
		return t;
	}

	/**
	 * Get an address for a thread id.
	 * 
	 * @param context
	 *            {@link Context}
	 * @param id
	 *            thread id
	 * @return address
	 */
	public static String getAddress(final Context context, final long id) {
		if (id < 0) {
			return null;
		}
		Thread t = CACHE.get(id);
		if (t == null) {
			t = getData(context, id, null);
			if (t != null) {
				newEntry(id, t);
			}
		}
		if (t != null) {
			return t.address;
		}
		return null;
	}

	/**
	 * Get number of messages in a thread.
	 * 
	 * @param context
	 *            {@link Context}
	 * @param id
	 *            thread id
	 * @return count
	 */
	public static int getCount(final Context context, final long id) {
		// TODO: need something to invalidate cache
		if (id < 0) {
			return -1;
		}
		Thread t = CACHE.get(id);
		if (t == null) {
			t = getData(context, id, null);
			if (t != null) {
				newEntry(id, t);
			}
		} else {
			getData(context, id, null);
		}
		if (t != null) {
			if (t.lastCheck < validCache) { // requery DB for count
				getData(context, id, t);
			}
			return t.count;
		}
		return -1;
	}

	/**
	 * Invalidate Count Cache.
	 */
	public static void invalidate() {
		validCache = System.currentTimeMillis();
	}

	/**
	 * Check if {@link Thread} is in cache.
	 * 
	 * @param id
	 *            {@link Thread}'s id
	 * @return true if {@link Thread} is in cache
	 */
	public static boolean poke(final long id) {
		Thread t = CACHE.get(id);
		if (t == null) { // not in cache
			return false;
		}
		if (t.lastCheck < validCache) {
			return false;
		}
		return true;
	}
}
