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

import java.util.concurrent.RejectedExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;
import de.ub0r.android.lib.Log;
import de.ub0r.android.lib.apis.ContactsWrapper;

/**
 * @author flx
 */
public final class AsyncHelper extends AsyncTask<Void, Void, Void> {
	/** Tag for logging. */
	static final String TAG = "ash";

	/** Pattern to clean up numbers. */
	private static final Pattern PATTERN_CLEAN_NUMBER = Pattern
			.compile("<(\\+?[0-9]+)>");

	/** Wrapper to use for contacts API. */
	private static final ContactsWrapper WRAPPER = ContactsWrapper
			.getInstance();

	/** {@link ConversationAdapter} to invalidate on new data. */
	private static ConversationAdapter adapter = null;

	/** {@link Context}. */
	private final Context context;
	/** {@link Conversation}. */
	private final Conversation conv;

	/** Changed anything? */
	private boolean changed = false;

	/**
	 * Fill {@link Conversation}.
	 * 
	 * @param c
	 *            {@link Context}
	 * @param con
	 *            {@link Conversation}
	 */

	private AsyncHelper(final Context c, final Conversation con) {
		this.context = c;
		this.conv = con;
	}

	/**
	 * Fill Conversations data. If needed: spawn threads.
	 * 
	 * @param context
	 *            {@link Context}
	 * @param c
	 *            {@link Conversation}
	 * @param sync
	 *            fetch of information
	 */
	public static void fillConversation(final Context context,
			final Conversation c, final boolean sync) {
		Log.d(TAG, "fillConversation(ctx, conv, " + sync + ")");
		if (context == null || c == null || c.getThreadId() < 0) {
			return;
		}
		AsyncHelper helper = new AsyncHelper(context, c);
		if (sync) {
			helper.doInBackground((Void) null);
		} else {
			try {
				helper.execute((Void) null);
			} catch (RejectedExecutionException e) {
				Log.e(TAG, "rejected exceution", e);
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected Void doInBackground(final Void... arg0) {
		if (this.conv == null) {
			return null;
		}
		this.changed = this.conv.getContact().update(this.context, true,
				ConversationList.showContactPhoto);
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onPostExecute(final Void result) {
		if (this.changed && adapter != null) {
			adapter.notifyDataSetChanged();
		}
	}

	/**
	 * Set {@link ConversationAdapter} to invalidate data after refreshing.
	 * 
	 * @param a
	 *            {@link ConversationAdapter}
	 */
	public static void setAdapter(final ConversationAdapter a) {
		adapter = a;
	}

	/**
	 * Get a contact's name by address.
	 * 
	 * @param context
	 *            {@link Context}
	 * @param address
	 *            address
	 * @return name
	 */
	public static String getContactName(final Context context,
			final String address) {
		Log.d(TAG, "getContactName(ctx, " + address + ")");
		if (address == null) {
			return null;
		}
		Cursor cursor = getContact(context, address);
		if (cursor == null) {
			return null;
		}
		return cursor.getString(ContactsWrapper.FILTER_INDEX_NAME);
	}

	/**
	 * Get (id, name) for address.
	 * 
	 * @param context
	 *            {@link Context}
	 * @param address
	 *            address
	 * @return {@link Cursor}
	 */
	private static synchronized Cursor getContact(final Context context,
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
		}
		// address contains the phone number
		try {
			final Cursor cursor = WRAPPER.getContact(context
					.getContentResolver(), realAddress);
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
