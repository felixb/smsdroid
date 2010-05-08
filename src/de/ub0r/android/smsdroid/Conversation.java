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

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.BaseColumns;
import android.provider.CallLog.Calls;

/**
 * Class holding a single conversation.
 * 
 * @author flx
 */
public final class Conversation {
	/** Tag for logging. */
	static final String TAG = "con";

	/** Internal Cache. */
	private static final HashMap<Integer, Conversation> CACHE = // .
	new HashMap<Integer, Conversation>();

	/** No contact available. */
	public static final String NO_CONTACT = "-1";
	/** No photo available. */
	public static final Bitmap NO_PHOTO = Bitmap.createBitmap(1, 1,
			Bitmap.Config.RGB_565);

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

	/** Time of valid cache. */
	private static long validCache = 0;

	/** Id. */
	private int id;
	/** ThreadId. */
	private int threadId;
	/** Contact's id. */
	private String personId;
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
	/** Message count. */
	private int count = -1;
	/** Last update. */
	private long lastUpdate = 0;

	/** Name. */
	private String name = null;
	/** Photo. */
	private Bitmap photo = null;

	/**
	 * Default constructor.
	 * 
	 * @param context
	 *            {@link Context}
	 * @param cursor
	 *            {@link Cursor} to read the data
	 * @param sync
	 *            fetch of information
	 */
	private Conversation(final Context context, final Cursor cursor,
			final boolean sync) {
		this.id = cursor.getInt(INDEX_ID);
		this.threadId = cursor.getInt(INDEX_THREADID);
		this.date = cursor.getLong(INDEX_DATE);
		this.address = cursor.getString(INDEX_ADDRESS);
		this.body = cursor.getString(INDEX_BODY);
		this.type = cursor.getInt(INDEX_TYPE);
		// this.read = cursor.getInt(INDEX_READ);
		this.read = 1;
		int idName = cursor.getColumnIndex(ConversationProvider.PROJECTION[// .
				ConversationProvider.INDEX_NAME]);
		if (idName >= 0) {
			this.name = cursor.getString(idName);
		}
		int idPid = cursor.getColumnIndex(ConversationProvider.PROJECTION[// .
				ConversationProvider.INDEX_PID]);
		if (idPid >= 0) {
			this.personId = cursor.getString(idPid);
		}

		AsyncHelper.fillConversation(context, this, sync);
		this.lastUpdate = System.currentTimeMillis();
	}

	/**
	 * Update data.
	 * 
	 * @param context
	 *            {@link Context}
	 * @param cursor
	 *            {@link Cursor} to read from.
	 * @param sync
	 *            fetch of information
	 */
	private void update(final Context context, final Cursor cursor,
			final boolean sync) {
		long d = cursor.getLong(INDEX_DATE);
		if (d != this.date) {
			this.id = cursor.getInt(INDEX_ID);
			this.date = d;
			this.body = cursor.getString(INDEX_BODY);
			this.type = cursor.getInt(INDEX_TYPE);
			int idName = cursor.getColumnIndex(ConversationProvider.// .
					PROJECTION[ConversationProvider.INDEX_NAME]);
			if (idName >= 0) {
				this.name = cursor.getString(idName);
			}
		}
		// this.read = cursor.getInt(INDEX_READ);
		if (this.lastUpdate < validCache) {
			AsyncHelper.fillConversation(context, this, sync);
		}
		this.lastUpdate = System.currentTimeMillis();
	}

	/**
	 * Get a {@link Conversation}.
	 * 
	 * @param context
	 *            {@link Context}
	 * @param cursor
	 *            {@link Cursor} to read the data from
	 * @param sync
	 *            fetch of information
	 * @return {@link Conversation}
	 */
	public static Conversation getConversation(final Context context,
			final Cursor cursor, final boolean sync) {
		synchronized (CACHE) {
			Conversation ret = CACHE.get(cursor
					.getInt(ConversationProvider.INDEX_THREADID));
			if (ret == null) {
				ret = new Conversation(context, cursor, sync);
				CACHE.put(ret.getThreadId(), ret);
			} else {
				ret.update(context, cursor, sync);
			}
			return ret;
		}
	}

	/**
	 * Get a {@link Conversation}.
	 * 
	 * @param context
	 *            {@link Context}
	 * @param threadId
	 *            threadId
	 * @param forceUpdate
	 *            force an update of that {@link Conversation}
	 * @return {@link Conversation}
	 */
	public static Conversation getConversation(final Context context,
			final int threadId, final boolean forceUpdate) {
		synchronized (CACHE) {
			Conversation ret = CACHE.get(threadId);
			if (ret == null || ret.getAddress() == null || forceUpdate) {
				Cursor cursor = context.getContentResolver().query(
						ConversationProvider.CONTENT_URI,
						ConversationProvider.PROJECTION,
						ConversationProvider.PROJECTION[// .
								ConversationProvider.INDEX_THREADID]
								+ " = " + threadId, null, null);
				if (cursor != null && cursor.moveToFirst()) {
					return getConversation(context, cursor, true);
				} else {
					Log.e(TAG, "did not found conversation: " + threadId);
				}
			}
			return ret;
		}
	}

	/**
	 * Invalidate Cache.
	 */
	public static void invalidate() {
		validCache = System.currentTimeMillis();
	}

	/**
	 * @return the id
	 */
	public int getId() {
		return this.id;
	}

	/**
	 * @return the personId
	 */
	public String getPersonId() {
		return this.personId;
	}

	/**
	 * Set the personId.
	 * 
	 * @param pid
	 *            the personId
	 */
	public void setPersonId(final String pid) {
		this.personId = pid;
	}

	/**
	 * @return the threadId
	 */
	public int getThreadId() {
		return this.threadId;
	}

	/**
	 * @return the date
	 */
	public long getDate() {
		return this.date;
	}

	/**
	 * @return the address
	 */
	public String getAddress() {
		return this.address;
	}

	/**
	 * Set {@link Conversation}'s address.
	 * 
	 * @param a
	 *            address
	 */
	public void setAddress(final String a) {
		this.address = a;
	}

	/**
	 * @return the body
	 */
	public String getBody() {
		return this.body;
	}

	/**
	 * Set the body.
	 * 
	 * @param b
	 *            body
	 */
	public void setBody(final String b) {
		this.body = b;
	}

	/**
	 * @return the type
	 */
	public int getType() {
		return this.type;
	}

	/**
	 * @return the read status
	 */
	public int getRead() {
		return this.read;
	}

	/**
	 * Set {@link Conversation}'s read status.
	 * 
	 * @param status
	 *            read status
	 */
	public void setRead(final int status) {
		this.read = status;
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * @return name, address or "..."
	 */
	public String getDisplayName() {
		if (this.name != null) {
			return this.name;
		} else if (this.address != null) {
			return this.address;
		} else {
			return "...";
		}
	}

	/**
	 * @param n
	 *            the name to set
	 */
	public void setName(final String n) {
		this.name = n;
	}

	/**
	 * @return the photo
	 */
	public Bitmap getPhoto() {
		return this.photo;
	}

	/**
	 * @param img
	 *            the photo to set
	 */
	public void setPhoto(final Bitmap img) {
		this.photo = img;
	}

	/**
	 * @return the count
	 */
	public int getCount() {
		return this.count;
	}

	/**
	 * @param c
	 *            the count to set
	 */
	public void setCount(final int c) {
		this.count = c;
	}

	/**
	 * @return {@link Uri} of this {@link Conversation}
	 */
	public Uri getUri() {
		return Uri.withAppendedPath(SMSdroid.URI, // .
				String.valueOf(this.threadId));
	}

	/**
	 * @return {@link Uri} of this {@link Conversation} represented in the
	 *         internal DataBase
	 */
	public Uri getInternalUri() {
		return Uri.withAppendedPath(ConversationProvider.CONTENT_URI, String
				.valueOf(this.threadId));
	}
}
