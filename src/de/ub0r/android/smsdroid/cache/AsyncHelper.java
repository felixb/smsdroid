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

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;
import de.ub0r.android.smsdroid.Conversation;
import de.ub0r.android.smsdroid.ConversationProvider;
import de.ub0r.android.smsdroid.ConversationsAdapter;
import de.ub0r.android.smsdroid.Message;
import de.ub0r.android.smsdroid.SMSdroid;

/**
 * @author flx
 */
public final class AsyncHelper extends AsyncTask<Void, Void, Void> {
	/** Tag for logging. */
	static final String TAG = "SMSdroid.ash";

	/** {@link ConversationsAdapter} to invalidate on new data. */
	private static ConversationsAdapter adapter = null;

	/** {@link Context}. */
	private final Context context;
	/** {@link Conversation}. */
	private final Conversation mConversation;

	/**
	 * Fill {@link Conversation}.
	 * 
	 * @param c
	 *            {@link Context}
	 * @param conv
	 *            {@link Conversation}
	 */

	private AsyncHelper(final Context c, final Conversation conv) {
		this.context = c;
		this.mConversation = conv;
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
			helper.execute((Void) null);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected Void doInBackground(final Void... arg0) {
		ContentValues cv = new ContentValues();
		Uri uri = this.mConversation.getUri();
		Cursor cursor = this.context.getContentResolver().query(uri,
				Message.PROJECTION_JOIN, null, null, null);

		// count
		this.mConversation.setCount(cursor.getCount());

		// address
		String address = this.mConversation.getAddress();
		Log.d(TAG, "address: " + address);
		if (address == null) {
			if (cursor.moveToLast()) {
				do {
					address = cursor.getString(Message.INDEX_ADDRESS);
				} while (address == null && cursor.moveToPrevious());
			}
			if (address != null) {
				this.mConversation.setAddress(address);
				Log.d(TAG, "new address: " + address);
				cv.put(ConversationProvider.PROJECTION[// .
						ConversationProvider.INDEX_ADDRESS], address);
			}
		}
		if (this.mConversation.getBody() == null && cursor.moveToLast()) {
			final Message m = Message.getMessage(this.context, cursor);
			final CharSequence b = m.getBody();
			if (b != null) {
				this.mConversation.setBody(b.toString());
			}
		}

		// read
		cursor = this.context.getContentResolver().query(uri,
				Message.PROJECTION,
				Message.PROJECTION[Message.INDEX_READ] + " = 0", null, null);
		if (cursor.getCount() == 0) {
			this.mConversation.setRead(1);
		} else {
			this.mConversation.setRead(0);
		}

		// name
		if (this.mConversation.getName() == null) {
			String n = Persons.getName(this.context, address, false);
			if (n != null) {
				this.mConversation.setName(n);
				cv.put(ConversationProvider.PROJECTION[// .
						ConversationProvider.INDEX_NAME], n);
			}
		}
		if (cv.size() > 0) {
			this.context.getContentResolver().update(
					this.mConversation.getInternalUri(), cv, null, null);
		}
		cursor.close();
		cursor = null;

		// photo
		if (SMSdroid.showContactPhoto && // .
				this.mConversation.getPhoto() == null) {
			this.mConversation.setPhoto(Persons.getPicture(this.context,
					address));
		}
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onPostExecute(final Void result) {
		if (adapter != null) {
			adapter.notifyDataSetChanged();
		}
	}

	/**
	 * Set {@link ConversationsAdapter} to invalidate data after refreshing.
	 * 
	 * @param a
	 *            {@link ConversationsAdapter}
	 */
	public static void setAdapter(final ConversationsAdapter a) {
		adapter = a;
	}
}
