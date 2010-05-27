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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Bitmap.Config;
import android.net.Uri;
import android.provider.CallLog.Calls;
import android.widget.Toast;

/**
 * Class holding a single message.
 * 
 * @author flx
 */
public final class Message {
	/** Tag for logging. */
	static final String TAG = "msg";

	/** Bitmap showing the play button. */
	public static final Bitmap BITMAP_PLAY = Bitmap.createBitmap(1, 1,
			Config.RGB_565);

	/** Cache size. */
	private static final int CAHCESIZE = 50;
	/** Internal Cache. */
	private static final LinkedHashMap<Integer, Message> CACHE = // .
	new LinkedHashMap<Integer, Message>(26, 0.9f, true);

	/** INDEX: id. */
	public static final int INDEX_ID = 0;
	/** INDEX: read. */
	public static final int INDEX_READ = 1;
	/** INDEX: date. */
	public static final int INDEX_DATE = 2;
	/** INDEX: thread_id. */
	public static final int INDEX_THREADID = 3;
	/** INDEX: type. */
	public static final int INDEX_TYPE = 4;
	/** INDEX: address. */
	public static final int INDEX_ADDRESS = 5;
	/** INDEX: body. */
	public static final int INDEX_BODY = 6;
	/** INDEX: subject. */
	public static final int INDEX_SUBJECT = 7;
	/** INDEX: m_type. */
	public static final int INDEX_MTYPE = 8;

	/** INDEX: mid. */
	public static final int INDEX_MID = 1;
	/** INDEX: content type. */
	public static final int INDEX_CT = 2;

	/** Cursor's projection. */
	public static final String[] PROJECTION = { //
	"_id", // 0
			"read", // 1
			Calls.DATE, // 2
			"thread_id", // 3
			Calls.TYPE, // 4
			"address", // 5
			"body", // 6
	};

	/** Cursor's projection. */
	public static final String[] PROJECTION_SMS = { //
	PROJECTION[INDEX_ID], // 0
			PROJECTION[INDEX_READ], // 1
			PROJECTION[INDEX_DATE], // 2
			PROJECTION[INDEX_THREADID], // 3
			PROJECTION[INDEX_TYPE], // 4
			PROJECTION[INDEX_ADDRESS], // 5
			PROJECTION[INDEX_BODY], // 6
	};

	/** Cursor's projection. */
	public static final String[] PROJECTION_MMS = { //
	PROJECTION[INDEX_ID], // 0
			PROJECTION[INDEX_READ], // 1
			PROJECTION[INDEX_DATE], // 2
			PROJECTION[INDEX_THREADID], // 3
			"m_type", // 4
			PROJECTION[INDEX_ID], // 5
			PROJECTION[INDEX_ID], // 6
			"sub", // 7
			"m_type", // 8
	};

	/** Cursor's projection. */
	public static final String[] PROJECTION_JOIN = { //
	PROJECTION[INDEX_ID], // 0
			PROJECTION[INDEX_READ], // 1
			PROJECTION[INDEX_DATE], // 2
			PROJECTION[INDEX_THREADID], // 3
			PROJECTION[INDEX_TYPE], // 4
			PROJECTION[INDEX_ADDRESS], // 5
			PROJECTION[INDEX_BODY], // 6
			"sub", // 7
			"m_type", // 8
	};

	/** Cursor's projection for set read/unread operations. */
	public static final String[] PROJECTION_READ = { //
	PROJECTION[INDEX_ID], // 0
			PROJECTION[INDEX_READ], // 1
			PROJECTION[INDEX_DATE], // 2
			PROJECTION[INDEX_THREADID], // 3
	};

	/** {@link Uri} for parts. */
	private static final Uri URI_PARTS = Uri.parse("content://mms/part/");

	/** Cursor's projection for parts. */
	public static final String[] PROJECTION_PARTS = { //
	"_id", // 0
			"mid", // 1
			"ct", // 2
	};

	/** SQL WHERE: unread messages. */
	static final String SELECTION_UNREAD = "read = '0'";
	/** SQL WHERE: read messages. */
	static final String SELECTION_READ = "read = '1'";

	/** Cursor's sort, upside down. */
	public static final String SORT_USD = Calls.DATE + " ASC";
	/** Cursor's sort, normal. */
	public static final String SORT_NORM = Calls.DATE + " DESC";

	/** Type for incoming sms. */
	public static final int SMS_IN = Calls.INCOMING_TYPE;
	/** Type for outgoing sms. */
	public static final int SMS_OUT = Calls.OUTGOING_TYPE;
	/** Type for sms drafts. */
	public static final int SMS_DRAFT = 3;
	/** Type for pending sms. */
	// TODO public static final int SMS_PENDING = 4;
	/** Type for incoming mms. */
	public static final int MMS_IN = 132;
	/** Type for outgoing mms. */
	public static final int MMS_OUT = 128;
	/** Type for mms drafts. */
	// public static final int MMS_DRAFT = 128;
	/** Type for pending mms. */
	// public static final int MMS_PENDING = 128;
	/** Type for not yet loaded mms. */
	public static final int MMS_TOLOAD = 130;

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
	/** Subject. */
	private String subject = null;
	/** Picture. */
	private Bitmap picture = null;
	/** {@link Integer} to for viewing the content. */
	private Intent contentIntent = null;

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
	private Message(final Context context, final Cursor cursor) {
		this.id = cursor.getLong(INDEX_ID);
		this.threadId = cursor.getLong(INDEX_THREADID);
		this.date = cursor.getLong(INDEX_DATE);
		if (this.date < ConversationList.MIN_DATE) {
			this.date *= ConversationList.MILLIS;
		}
		if (cursor.getColumnIndex(PROJECTION_JOIN[INDEX_TYPE]) >= 0) {
			this.address = cursor.getString(INDEX_ADDRESS);
			this.body = cursor.getString(INDEX_BODY);
			if (ConversationList.showEmoticons && this.body != null) {
				this.body = SmileyParser.getInstance(context).addSmileySpans(
						this.body);
			}
		} else {
			this.body = null;
			this.address = null;
		}
		this.type = cursor.getInt(INDEX_TYPE);
		this.read = cursor.getInt(INDEX_READ);
		if (this.body == null) {
			this.isMms = true;
			try {
				this.fetchMmsParts(context);
			} catch (OutOfMemoryError e) {
				Log.e(TAG, "error loading parts", e);
				try {
					Toast.makeText(context, e.getMessage(), Toast.LENGTH_LONG)
							.show();
				} catch (Exception e1) {
					Log.e(TAG, "error creating Toast", e1);
				}
			}
		} else {
			this.isMms = false;
		}
		try {
			this.subject = cursor.getString(INDEX_SUBJECT);
		} catch (IllegalStateException e) {
			this.subject = null;
		}
		try {
			if (cursor.getColumnCount() > INDEX_MTYPE) {
				final int t = cursor.getInt(INDEX_MTYPE);
				if (t != 0) {
					this.type = t;
				}
			}
		} catch (IllegalStateException e) {
			this.subject = null;
		}

		Log.d(TAG, "threadId: " + this.threadId);
		Log.d(TAG, "address: " + this.address);
		Log.d(TAG, "subject: " + this.subject);
		Log.d(TAG, "body: " + this.body);
		Log.d(TAG, "type: " + this.type);
	}

	/**
	 * Update {@link Message}.
	 * 
	 * @param cursor
	 *            {@link Cursor} to read from
	 */
	public void update(final Cursor cursor) {
		this.read = cursor.getInt(INDEX_READ);
		this.type = cursor.getInt(INDEX_TYPE);
		try {
			if (cursor.getColumnCount() > INDEX_MTYPE) {
				final int t = cursor.getInt(INDEX_MTYPE);
				if (t != 0) {
					this.type = t;
				}
			}
		} catch (IllegalStateException e) {
			Log.e(TAG, "wrong projection?", e);
		}
	}

	/**
	 * Fetch a part.
	 * 
	 * @param is
	 *            {@link InputStream}
	 * @return part
	 */
	private CharSequence fetchPart(final InputStream is) {
		Log.d(TAG, "fetchPart(cr, is)");
		String ret = null;
		// get part
		final ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			byte[] buffer = new byte[256];
			int len = is.read(buffer);
			while (len >= 0) {
				baos.write(buffer, 0, len);
				len = is.read(buffer);
			}
			ret = new String(baos.toByteArray());
			Log.d(TAG, ret);
		} catch (IOException e) {
			Log.e(TAG, "Failed to load part data", e);
		} finally {
			if (is != null) {
				try {
					is.close();
				} catch (IOException e) {
					Log.e(TAG, "Failed to close stream", e);
				} // Ignore
			}
		}
		return ret;
	}

	/**
	 * Fetch MMS parts.
	 * 
	 * @param context
	 *            {@link Context}
	 */
	private void fetchMmsParts(final Context context) {
		final ContentResolver cr = context.getContentResolver();
		Cursor cursor = cr.query(URI_PARTS, null, PROJECTION_PARTS[INDEX_MID]
				+ " = " + this.id, null, null);
		if (cursor == null || !cursor.moveToFirst()) {
			return;
		}
		final int iID = cursor.getColumnIndex(PROJECTION_PARTS[INDEX_ID]);
		final int iCT = cursor.getColumnIndex(PROJECTION_PARTS[INDEX_CT]);
		final int iText = cursor.getColumnIndex("text");
		do {
			final int pid = cursor.getInt(iID);
			final String ct = cursor.getString(iCT);
			Log.d(TAG, "part: " + pid + " " + ct);

			// get part
			InputStream is = null;

			final Uri uri = Uri
					.withAppendedPath(URI_PARTS, String.valueOf(pid));
			try {
				is = cr.openInputStream(uri);
			} catch (IOException e) {
				Log.e(TAG, "Failed to load part data", e);
			}
			if (is == null) {
				Log.i(TAG, "InputStream for part " + pid + " is null");
				if (iText >= 0 && ct.startsWith("text/")) {
					this.body = cursor.getString(iText);
				}
				continue;
			}
			if (ct == null) {
				continue;
			}
			if (ct.startsWith("image/")) {
				this.picture = BitmapFactory.decodeStream(is);
				final Intent i = new Intent(Intent.ACTION_VIEW);
				i.setDataAndType(uri, ct);
				i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
				this.contentIntent = i;
				continue; // skip the rest
			} else if (ct.startsWith("video/") || ct.startsWith("audio/")) {
				this.picture = BITMAP_PLAY;
				final Intent i = new Intent(Intent.ACTION_VIEW);
				i.setDataAndType(uri, ct);
				this.contentIntent = i;
				continue; // skip the rest
			} else if (ct.startsWith("text/")) {
				this.body = this.fetchPart(is);
			}

			if (is != null) {
				try {
					is.close();
				} catch (IOException e) {
					Log.e(TAG, "Failed to close stream", e);
				} // Ignore
			}
		} while (cursor.moveToNext());
	}

	/**
	 * Get a {@link Message} from cache or {@link Cursor}.
	 * 
	 * @param context
	 *            {@link Context}
	 * @param cursor
	 *            {@link Cursor}
	 * @return {@link Message}
	 */
	public static Message getMessage(final Context context, // .
			final Cursor cursor) {
		synchronized (CACHE) {
			String body = cursor.getString(INDEX_BODY);
			int id = cursor.getInt(INDEX_ID);
			if (body == null) { // MMS
				id *= -1;
			}
			Message ret = CACHE.get(id);
			if (ret == null) {
				ret = new Message(context, cursor);
				CACHE.put(id, ret);
				Log.d(TAG, "cachesize: " + CACHE.size());
				while (CACHE.size() > CAHCESIZE) {
					Integer i = CACHE.keySet().iterator().next();
					Log.d(TAG, "rm msg. from cache: " + i);
					Message cc = CACHE.remove(i);
					if (cc == null) {
						Log.w(TAG, "CACHE might be inconsistent!");
						break;
					}
				}
			} else {
				ret.update(cursor);
			}
			return ret;
		}
	}

	/**
	 * @return the id
	 */
	public long getId() {
		return this.id;
	}

	/**
	 * @return the threadId
	 */
	public long getThreadId() {
		return this.threadId;
	}

	/**
	 * @return the date
	 */
	public long getDate() {
		return this.date;
	}

	/**
	 * @param context
	 *            {@link Context} to query SMS DB for an address.
	 * @return the address
	 */
	public String getAddress(final Context context) {
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
	public CharSequence getBody() {
		return this.body;
	}

	/**
	 * @return the type
	 */
	public int getType() {
		return this.type;
	}

	/**
	 * @return the read
	 */
	public int getRead() {
		return this.read;
	}

	/**
	 * @return is this message a MMS?
	 */
	public boolean isMMS() {
		return this.isMms;
	}

	/**
	 * @return the subject
	 */
	public String getSubject() {
		return this.subject;
	}

	/**
	 * @return the picture
	 */
	public Bitmap getPicture() {
		return this.picture;
	}

	/**
	 * @return {@link Intent} to the content
	 */
	public Intent getContentIntent() {
		return this.contentIntent;
	}

	/**
	 * @return {@link Uri} of this {@link Message}
	 */
	public Uri getUri() {
		if (this.isMms) {
			return Uri.parse("content://mms/" + this.id);
		} else {
			return Uri.parse("content://sms/" + this.id);
		}
	}
}
