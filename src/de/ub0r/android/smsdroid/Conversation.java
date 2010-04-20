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

import android.database.Cursor;
import android.provider.BaseColumns;
import android.provider.CallLog.Calls;

/**
 * Class holding a single conversation.
 * 
 * @author flx
 */
public class Conversation {
	/** Tag for logging. */
	static final String TAG = "SMSdroid.con";

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

	/** Dateformat. //TODO: move me to xml */
	static final String DATE_FORMAT = "dd.MM. kk:mm";

	/** Cursor's projection. */
	public static final String[] PROJECTION = { //
	BaseColumns._ID, // 0
			Calls.DATE, // 1
			"address", // 2
			"thread_id", // 3
			"body", // 4
			Calls.TYPE, // 5
			"read", // 6
	};

	/** Cursors row in hero phones: address. */
	static final String ADDRESS_HERO = "recipient_address";
	/** Cursors row in hero phones: thread_id. */
	static final String THREADID_HERO = "_id";

	/** Id. */
	private long id;
	/** ThreadId. */
	private long threadId;
	/** Date. */
	private long date;
	/** Address. */
	private String address;
	/** Body. */
	private String body;
	/** Type. */
	private int type;
	/** Read status. */
	private int read;

	/**
	 * Default constructor.
	 * 
	 * @param cursor
	 *            {@link Cursor} to read the data
	 */
	public Conversation(final Cursor cursor) {
		this.id = cursor.getLong(INDEX_ID);
		this.threadId = cursor.getLong(INDEX_THREADID);
		this.date = cursor.getLong(INDEX_DATE);
		if (this.date < SMSdroid.MIN_DATE) {
			this.date *= SMSdroid.MILLIS;
		}
		this.address = cursor.getString(INDEX_ADDRESS);
		this.body = cursor.getString(INDEX_BODY);
		this.type = cursor.getInt(INDEX_TYPE);
		this.read = cursor.getInt(INDEX_READ);
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
	 * @return the address
	 */
	public final String getAddress() {
		return this.address;
	}

	/**
	 * Set {@link Conversation}'s address.
	 * 
	 * @param a
	 *            address
	 */
	public final void setAddress(final String a) {
		this.address = a;
	}

	/**
	 * @return the body
	 */
	public final String getBody() {
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
}
