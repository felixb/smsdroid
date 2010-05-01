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

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import de.ub0r.android.smsdroid.Conversation;
import de.ub0r.android.smsdroid.ConversationsAdapter;
import de.ub0r.android.smsdroid.Message;
import de.ub0r.android.smsdroid.SMSdroid;

/**
 * @author flx
 */
public final class AsyncHelper extends AsyncTask<Void, Void, Void> {
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
	 */
	public static void fillConversation(final Context context,
			final Conversation c) {
		if (context == null || c == null || c.getThreadId() < 0) {
			return;
		}
		new AsyncHelper(context, c).execute((Void) null);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected Void doInBackground(final Void... arg0) {
		Uri uri = Uri.parse("content://mms-sms/conversations/"
				+ this.mConversation.getThreadId());
		Cursor cursor = this.context.getContentResolver().query(uri,
				Message.PROJECTION, null, null, null);

		// count
		this.mConversation.setCount(cursor.getCount());

		// address
		String address = this.mConversation.getAddress();
		if (address == null) {
			if (cursor.moveToLast()) {
				do {
					address = cursor.getString(Message.INDEX_ADDRESS);
				} while (address == null && cursor.moveToPrevious());
			}
		}
		cursor.close();

		// read
		cursor = this.context.getContentResolver().query(uri,
				Message.PROJECTION,
				Message.PROJECTION[Message.INDEX_READ] + " = 0", null, null);
		if (cursor.getCount() == 0) {
			this.mConversation.setRead(1);
		} else {
			this.mConversation.setRead(0);
		}
		cursor.close();
		cursor = null;

		// name
		if (this.mConversation.getName() == null) {
			this.mConversation.setName(Persons.getName(this.context, address,
					false));
		}

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
