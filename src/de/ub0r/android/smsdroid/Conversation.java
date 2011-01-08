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

import java.util.LinkedHashMap;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.provider.BaseColumns;
import android.provider.CallLog.Calls;
import de.ub0r.android.lib.Log;
import de.ub0r.android.lib.apis.Contact;

/**
 * Class holding a single conversation.
 * 
 * @author flx
 */
public final class Conversation {
	/** Tag for logging. */
	static final String TAG = "con";

	/** Cache size. */
	private static final int CAHCESIZE = 50;
	/** Internal Cache. */
	private static final LinkedHashMap<Integer, Conversation> CACHE = // .
	new LinkedHashMap<Integer, Conversation>(26, 0.9f, true);

	/** No photo available. */
	public static final Bitmap NO_PHOTO = Bitmap.createBitmap(1, 1,
			Bitmap.Config.RGB_565);

	/** {@link Uri} to all threads. */
	static final Uri URI_SIMPLE = Uri.parse("content://mms-sms/conversations")
			.buildUpon().appendQueryParameter("simple", "true").build();

	/** Id. */
	public static final String ID = BaseColumns._ID;
	/** Date. */
	public static final String DATE = Calls.DATE;
	/** count. */
	public static final String COUNT = "message_count";
	/** number id. */
	public static final String NID = "recipient_ids";
	/** body. */
	public static final String BODY = "snippet";
	/** read. */
	public static final String READ = "read";

	/** INDEX: id. */
	public static final int INDEX_SIMPLE_ID = 0;
	/** INDEX: date. */
	public static final int INDEX_SIMPLE_DATE = 1;
	/** INDEX: count. */
	public static final int INDEX_SIMPLE_COUNT = 2;
	/** INDEX: person id. */
	public static final int INDEX_SIMPLE_NID = 3;
	/** INDEX: body. */
	public static final int INDEX_SIMPLE_BODY = 4;
	/** INDEX: read. */
	public static final int INDEX_SIMPLE_READ = 5;

	/** Cursor's projection. */
	public static final String[] PROJECTION_SIMPLE = { //
	ID, // 0
			DATE, // 1
			COUNT, // 2
			NID, // 3
			BODY, // 4
			READ, // 5
	};

	/** Date format. //TODO: move me to xml */
	static final String DATE_FORMAT = "dd.MM. kk:mm";

	/** Time of valid cache. */
	private static long validCache = 0;

	/** Id. */
	private int id;
	/** ThreadId. */
	private int threadId;
	/** Contact. */
	private Contact contact;
	/** NumerId. */
	// private long numberId;
	/** Contact's id. */
	// private String contactId;
	/** Date. */
	private long date;
	/** Address. */
	// private String address;
	/** Body. */
	private String body;
	/** Read status. */
	private int read;
	/** Message count. */
	private int count = -1;
	/** Last update. */
	private long lastUpdate = 0L;

	/** Name. */
	// private String name = null;
	/** Photo. */
	// private Bitmap photo = null;

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
		this.threadId = cursor.getInt(INDEX_SIMPLE_ID);
		this.date = cursor.getLong(INDEX_SIMPLE_DATE);
		this.body = cursor.getString(INDEX_SIMPLE_BODY);
		this.read = cursor.getInt(INDEX_SIMPLE_READ);
		this.count = cursor.getInt(INDEX_SIMPLE_COUNT);
		this.contact = new Contact(cursor.getInt(INDEX_SIMPLE_NID));

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
		long d = cursor.getLong(INDEX_SIMPLE_DATE);
		if (d != this.date) {
			this.id = cursor.getInt(INDEX_SIMPLE_ID);
			this.date = d;
			this.body = cursor.getString(INDEX_SIMPLE_BODY);
		}
		this.count = cursor.getInt(INDEX_SIMPLE_COUNT);
		this.read = cursor.getInt(INDEX_SIMPLE_READ);
		final int nid = cursor.getInt(INDEX_SIMPLE_NID);
		if (nid != this.contact.getRecipientId()) {
			this.contact = new Contact(nid);
		}
		if (this.lastUpdate < validCache) {
			AsyncHelper.fillConversation(context, this, sync);
			this.lastUpdate = System.currentTimeMillis();
		}
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
			Conversation ret = CACHE.get(cursor.getInt(INDEX_SIMPLE_ID));
			if (ret == null) {
				ret = new Conversation(context, cursor, sync);
				CACHE.put(ret.getThreadId(), ret);
				Log.d(TAG, "cachesize: " + CACHE.size());
				while (CACHE.size() > CAHCESIZE) {
					Integer i = CACHE.keySet().iterator().next();
					Log.d(TAG, "rm con. from cache: " + i);
					Conversation cc = CACHE.remove(i);
					if (cc == null) {
						Log.w(TAG, "CACHE might be inconsistent!");
						break;
					}
				}
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
				Cursor cursor = context.getContentResolver().query(URI_SIMPLE,
						PROJECTION_SIMPLE, ID + " = ?",
						new String[] { String.valueOf(threadId) }, null);
				if (cursor != null && cursor.moveToFirst()) {
					ret = getConversation(context, cursor, true);
				} else {
					Log.e(TAG, "did not found conversation: " + threadId);
				}
				if (cursor != null && !cursor.isClosed()) {
					cursor.close();
				}
			}
			return ret;
		}
	}

	/**
	 * Flush all cached conversations.
	 */
	public static void flushCache() {
		synchronized (CACHE) {
			CACHE.clear();
		}
	}

	/**
	 * Remove a {@link Conversation} from cache.
	 * 
	 * @param threadId
	 *            threadId
	 */
	public static void removeConversation(final int threadId) {
		synchronized (CACHE) {
			final Conversation c = CACHE.get(threadId);
			if (c == null) {
				CACHE.remove(c);
			}
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
	 * @return the numberId
	 */
	@Deprecated
	public long getNumberId() {
		return this.contact.getRecipientId();
	}

	/**
	 * Set the numberId.
	 * 
	 * @param nid
	 *            the numberId
	 */
	public void setNumberId(final long nid) {
		this.contact = new Contact(nid);
	}

	/**
	 * @return the contactId
	 */
	@Deprecated
	public String getContactId() {
		return this.contact.getLookUpKey();
	}

	/**
	 * Set the contactId.
	 * 
	 * @param pid
	 *            the contactId
	 */
	@Deprecated
	public void setContactId(final String pid) {
		// FIXME this.contactId = pid;
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
	 * Get {@link Contact}.
	 * 
	 * @return {@link Contact}
	 */
	public Contact getContact() {
		return this.contact;
	}

	/**
	 * @return the address
	 */
	@Deprecated
	public String getAddress() {
		return this.contact.getNumber();
	}

	/**
	 * Set {@link Conversation}'s address.
	 * 
	 * @param a
	 *            address
	 */
	@Deprecated
	public void setAddress(final String a) {
		this.contact.setNumber(a);
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
	@Deprecated
	public String getName() {
		return this.contact.getName();
	}

	/**
	 * @return name, address or "..."
	 */
	@Deprecated
	public String getDisplayName() {
		return this.contact.getDisplayName();
	}

	/**
	 * @return "name &lt;address&gt;"
	 */
	@Deprecated
	public String getFullDisplayName() {
		return this.contact.getNameAndNumber();
	}

	/**
	 * @param n
	 *            the name to set
	 */
	@Deprecated
	public void setName(final String n) {
		this.contact.setName(n);
	}

	/**
	 * Get {@link Contact}'s avatar.
	 * 
	 * @param context
	 *            {@link Context}
	 * @param defaultValue
	 *            default {@link Drawable}
	 * @return {@link Contact}'s avatar
	 */
	@Deprecated
	public Drawable getPhoto(final Context context, // .
			final Drawable defaultValue) {
		return this.contact.getAvatar(context, defaultValue);
	}

	/**
	 * @param img
	 *            the photo to set
	 */
	@Deprecated
	public void setPhoto(final Bitmap img) {

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
		return Uri.withAppendedPath(ConversationList.URI, // .
				String.valueOf(this.threadId));
	}
}
