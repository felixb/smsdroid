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

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CallLog.Calls;
import android.util.Log;

/**
 * Class holding a single message.
 * 
 * @author flx
 */
public class Message {
	/** Tag for logging. */
	static final String TAG = "SMSdroid.msg";

	/** INDEX: id. */
	public static final int INDEX_ID = 0;
	/** INDEX: date. */
	public static final int INDEX_DATE = 1;
	/** INDEX: address. */
	public static final int INDEX_ADDRESS = 2;
	/** INDEX: thread_id. */
	public static final int INDEX_THREADID = 3;
	/** INDEX: body. */
	public static final int INDEX_BODY = 4;
	/** INDEX: type. */
	public static final int INDEX_TYPE = 5;
	/** INDEX: read. */
	public static final int INDEX_READ = 6;

	/** Cursor's projection. */
	public static final String[] PROJECTION = { //
	"_id", // 0
			Calls.DATE, // 1
			"address", // 2
			"thread_id", // 3
			"body", // 4
			Calls.TYPE, // 5
			"read", // 6
	};

	/** SQL WHERE: unread messages. */
	static final String SELECTION_UNREAD = "read = '0'";
	/** SQL WHERE: read messages. */
	static final String SELECTION_READ = "read = '1'";

	/** Cursor's sort, upside down. */
	public static final String SORT_USD = Calls.DATE + " ASC";
	/** Cursor's sort, normal. */
	public static final String SORT_NORM = Calls.DATE + " DESC";

	/** Id. */
	private long id;
	/** ThreadId. */
	private long threadId;
	/** Date. */
	private long date;
	/** Address. */
	private String address;
	/** Body. */
	private CharSequence body;
	/** Type. */
	private int type;
	/** Read status. */
	private int read;

	/** Is this message a MMS? */
	private final boolean isMms;

	/**
	 * Default constructor.
	 * 
	 * @param context
	 *            {@link Context} to spawn the {@link SmileyParser}.
	 * @param cursor
	 *            {@link Cursor} to read the data
	 */
	public Message(final Context context, final Cursor cursor) {
		this.id = cursor.getLong(INDEX_ID);
		this.threadId = cursor.getLong(INDEX_THREADID);
		this.date = cursor.getLong(INDEX_DATE);
		if (this.date < SMSdroid.MIN_DATE) {
			this.date *= SMSdroid.MILLIS;
		}
		this.address = cursor.getString(INDEX_ADDRESS);
		this.body = cursor.getString(INDEX_BODY);
		if (SMSdroid.showEmoticons) {
			this.body = SmileyParser.getInstance(context).addSmileySpans(
					this.body);
		}
		this.type = cursor.getInt(INDEX_TYPE);
		this.read = cursor.getInt(INDEX_READ);
		if (this.body == null) {
			this.isMms = true;
		} else {
			this.isMms = false;
		}
	}

	/**
	 * @return the id
	 */
	public final long getId() {
		return this.id;
	}

	/**
	 * @return the threadId
	 */
	public final long getThreadId() {
		return this.threadId;
	}

	/**
	 * @return the date
	 */
	public final long getDate() {
		return this.date;
	}

	/**
	 * @param context
	 *            {@link Context} to query SMS DB for an address.
	 * @return the address
	 */
	public final String getAddress(final Context context) {
		// TODO: cache address for thread
		if (this.address == null && context != null) {
			final String select = Message.PROJECTION[// .
					Message.INDEX_THREADID]
					+ " = '" + this.getThreadId()
					+ "' and "
					+ Message.PROJECTION[Message.INDEX_ADDRESS] + " != ''";
			Log.d(TAG, "select: " + select);
			final Cursor cur = context.getContentResolver().query(
					Uri.parse("content://sms/"), Message.PROJECTION, select,
					null, null);
			if (cur != null && cur.moveToFirst()) {
				this.address = cur.getString(Message.INDEX_ADDRESS);
				Log.d(TAG, "found address: " + this.address);
			}
			cur.close();
		}
		return this.address;
	}

	/**
	 * @return the body
	 */
	public final CharSequence getBody() {
		return this.body;
	}

	/**
	 * @return the type
	 */
	public final int getType() {
		return this.type;
	}

	/**
	 * @return the read
	 */
	public final int getRead() {
		return this.read;
	}

	/**
	 * @return is this message a MMS?
	 */
	public final boolean isMMS() {
		return this.isMms;
	}

	/**
	 * @return {@link Uri} of this message
	 */
	public final Uri getUri() {
		if (this.isMms) {
			return Uri.parse("content://mms/" + this.id);
		} else {
			return Uri.parse("content://sms/" + this.id);
		}
	}
}
