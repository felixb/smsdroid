/*
 * Copyright (C) 2010 Felix Bechstein, The Android Open Source Project
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
import java.util.ArrayList;
import java.util.HashMap;

import android.app.Activity;
import android.app.Dialog;
import android.app.AlertDialog.Builder;
import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Bitmap.Config;
import android.net.Uri;
import android.provider.BaseColumns;
import android.provider.CallLog.Calls;
import android.text.TextUtils;
import de.ub0r.android.lib.DbUtils;
import de.ub0r.android.lib.Log;

/**
 * @author flx
 */
public final class ConversationProvider extends ContentProvider {
	/** Tag for output. */
	private static final String TAG = "cvp";

	/** Name of the {@link SQLiteDatabase}. */
	private static final String DATABASE_NAME = "mms.db";
	/** Version of the {@link SQLiteDatabase}. */
	private static final int DATABASE_VERSION = 20;

	/** Authority. */
	public static final String AUTHORITY = "de.ub0r.android.smsdroid."
			+ "provider.conversations";

	/**
	 * Class holding all messages.
	 * 
	 * @author flx
	 */
	public static final class Messages {
		/** Table name. */
		private static final String TABLE = "messages";
		/** {@link HashMap} for projection. */
		private static final HashMap<String, String> PROJECTION_MAP;

		/** Bitmap showing the play button. */
		public static final Bitmap BITMAP_PLAY = Bitmap.createBitmap(1, 1,
				Config.RGB_565);

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
		/** INDEX: subject. */
		public static final int INDEX_SUBJECT = 7;

		/** Id. */
		public static final String ID = BaseColumns._ID;
		/** Date. */
		public static final String DATE = Calls.DATE;
		/** Address. */
		public static final String ADDRESS = "address";
		/** thread_id. */
		public static final String THREADID = "thread_id";
		/** body. */
		public static final String BODY = "body";
		/** type. */
		public static final String TYPE = Calls.TYPE;
		/** read. */
		public static final String READ = "read";
		/** Subject. */
		public static final String SUBJECT = "subject";

		/** Type for incoming sms. */
		public static final int TYPE_SMS_IN = Calls.INCOMING_TYPE;
		/** Type for outgoing sms. */
		public static final int TYPE_SMS_OUT = Calls.OUTGOING_TYPE;
		/** Type for sms drafts. */
		public static final int TYPE_SMS_DRAFT = 3;
		/** Type for incoming mms. */
		public static final int TYPE_MMS_IN = 132;
		/** Type for outgoing mms. */
		public static final int TYPE_MMS_OUT = 128;
		/** Type for mms drafts. */
		// public static final int MMS_DRAFT = 128;
		/** Type for not yet loaded mms. */
		public static final int TYPE_MMS_TOLOAD = 130;

		/** Where clause matching sms. */
		public static final String WHERE_TYPE_SMS = TYPE + " < 100";
		/** Where clause matching sms. */
		public static final String WHERE_TYPE_MMS = TYPE + " > 100";

		/** {@link Cursor}'s projection. */
		public static final String[] PROJECTION = { //
		ID, // 0
				DATE, // 1
				ADDRESS, // 2
				THREADID, // 3
				BODY, // 4
				TYPE, // 5
				READ, // 6
				SUBJECT, // 7
		};

		/** Index for parts: id. */
		public static final int PARTS_INDEX_ID = 0;
		/** Index for parts: mid. */
		public static final int PARTS_INDEX_MID = 1;
		/** Index for parts: ct. */
		public static final int PARTS_INDEX_CT = 2;

		/** id. */
		public static final String PARTS_ID = "_id";
		/** mid. */
		public static final String PARTS_MID = "mid";
		/** ct. */
		public static final String PARTS_CT = "ct";
		/** text. */
		public static final String PARTS_TEXT = "text";

		/** {@link Uri} for parts. */
		private static final Uri URI_PARTS = Uri.parse("content://mms/part/");

		/** Cursor's projection for parts. */
		public static final String[] PROJECTION_PARTS = { //
		PARTS_ID, // 0
				PARTS_MID, // 1
				PARTS_CT, // 2
		};

		/** Remote {@link Cursor}'s projection. */
		private static final String[] ORIG_PROJECTION_SMS = PROJECTION;

		/** Remote {@link Cursor}'s projection. */
		private static final String[] ORIG_PROJECTION_MMS = new String[] { // .
		ID, // 0
				DATE, // 1
				THREADID, // 2
				READ, // 3
				TYPE, // 4
		};

		/** Default sort order. */
		public static final String DEFAULT_SORT_ORDER = DATE + " ASC";

		/** Content {@link Uri}. */
		public static final Uri CONTENT_URI = Uri.parse("content://"
				+ AUTHORITY + "/messages");
		/** Content {@link Uri}. */
		public static final Uri THREAD_URI = Uri.parse("content://" + AUTHORITY
				+ "/conversation");
		/** {@link Uri} of real sms database. */
		private static final Uri ORIG_URI_SMS = Uri.parse("content://sms/");
		/** {@link Uri} of real mms database. */
		private static final Uri ORIG_URI_MMS = Uri.parse("content://mms/");

		/** SQL WHERE: unread messages. */
		static final String SELECTION_UNREAD = "read = '0'";
		/** SQL WHERE: read messages. */
		static final String SELECTION_READ = "read = '1'";

		/**
		 * The MIME type of {@link #CONTENT_URI} providing a list of threads.
		 */
		public static final String CONTENT_TYPE = // .
		"vnd.android.cursor.dir/vnd.ub0r.message";

		/**
		 * The MIME type of a {@link #CONTENT_URI} single thread.
		 */
		public static final String CONTENT_ITEM_TYPE = // .
		"vnd.android.cursor.item/vnd.ub0r.message";

		static {
			PROJECTION_MAP = new HashMap<String, String>();
			final int l = PROJECTION.length;
			for (int i = 0; i < l; i++) {
				PROJECTION_MAP.put(PROJECTION[i], PROJECTION[i]);
			}
		}

		/**
		 * Create table in {@link SQLiteDatabase}.
		 * 
		 * @param db
		 *            {@link SQLiteDatabase}
		 */
		public static void onCreate(final SQLiteDatabase db) {
			Log.i(TAG, "create table: " + TABLE);
			db.execSQL("DROP TABLE IF EXISTS " + TABLE);
			db.execSQL("CREATE TABLE " + TABLE + " (" // .
					+ ID + " LONG," // .
					+ DATE + " LONG,"// .
					+ ADDRESS + " TEXT,"// .
					+ THREADID + " LONG,"// .
					+ BODY + " TEXT,"// .
					+ TYPE + " INTEGER," // .
					+ READ + " INTEGER," // .
					+ SUBJECT + " TEXT" // .
					+ ");");
		}

		/**
		 * Upgrade table.
		 * 
		 * @param db
		 *            {@link SQLiteDatabase}
		 * @param oldVersion
		 *            old version
		 * @param newVersion
		 *            new version
		 */
		public static void onUpgrade(final SQLiteDatabase db,
				final int oldVersion, final int newVersion) {
			Log.w(TAG, "Upgrading table: " + TABLE);
			onCreate(db);
		}

		/**
		 * Get {@link Cursor} holding MMS parts for mid.
		 * 
		 * @param cr
		 *            {@link ContentResolver}
		 * @param mid
		 *            message's id
		 * @return {@link Cursor}
		 */
		public static Cursor getPartsCursor(final ContentResolver cr, // .
				final long mid) {
			return cr.query(URI_PARTS, null, PARTS_MID + " = " + mid, null,
					null);
		}

		/**
		 * Get MMS parts as {@link Object}s (Bitmap, {@link CharSequence},
		 * {@link Intent}).
		 * 
		 * @param cr
		 *            {@link ContentResolver}
		 * @param mid
		 *            MMS's id
		 * @return {@link ArrayList} of parts
		 */
		public static ArrayList<Object> getParts(final ContentResolver cr,
				final long mid) {
			Cursor cursor = getPartsCursor(cr, mid);
			if (cursor == null || !cursor.moveToFirst()) {
				return null;
			}
			final ArrayList<Object> ret = new ArrayList<Object>();
			final int iID = cursor.getColumnIndex(PARTS_ID);
			final int iCT = cursor.getColumnIndex(PARTS_CT);
			final int iText = cursor.getColumnIndex(PARTS_TEXT);
			do {
				final long pid = cursor.getLong(iID);
				final String ct = cursor.getString(iCT);
				Log.d(TAG, "part: " + pid + " " + ct);

				// get part
				InputStream is = getPart(cr, pid);
				if (is == null) {
					Log.i(TAG, "InputStream for part " + pid + " is null");
					if (iText >= 0 && ct.startsWith("text/")) {
						ret.add(cursor.getString(iText));
					}
					continue;
				}
				if (ct == null) {
					continue;
				}
				final Intent i = Messages.getPartContentIntent(pid, ct);
				if (i != null) {
					ret.add(i);
				}
				final Object o = Messages.getPartObject(ct, is);
				if (o != null) {
					ret.add(o);
				}

				if (is != null) {
					try {
						is.close();
					} catch (IOException e) {
						Log.e(TAG, "Failed to close stream", e);
					} // Ignore
				}
			} while (cursor.moveToNext());
			return ret;
		}

		/**
		 * Get MMS part's {@link Uri}.
		 * 
		 * @param pid
		 *            part's id
		 * @return {@link Uri}
		 */
		public static Uri getPartUri(final long pid) {
			return ContentUris.withAppendedId(URI_PARTS, pid);
		}

		/**
		 * Get content's {@link Intent} for MMS part.
		 * 
		 * @param pid
		 *            part's id
		 * @param ct
		 *            part's content type
		 * @return {@link Intent}
		 */
		public static Intent getPartContentIntent(final long pid,
				final String ct) {
			if (ct.startsWith("image/")) {
				final Intent i = new Intent(Intent.ACTION_VIEW);
				i.setDataAndType(getPartUri(pid), ct);
				i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
				return i;
			} else if (ct.startsWith("video/") || ct.startsWith("audio/")) {
				final Intent i = new Intent(Intent.ACTION_VIEW);
				i.setDataAndType(getPartUri(pid), ct);
				return i;
			}
			return null;
		}

		/**
		 * Get MMS part's picture or text.
		 * 
		 * @param ct
		 *            part's content type
		 * @param is
		 *            {@link InputStream}
		 * @return {@link Bitmap} or {@link CharSequence}
		 */
		public static Object getPartObject(final String ct, // .
				final InputStream is) {
			if (is == null) {
				return null;
			}
			if (ct.startsWith("image/")) {
				return BitmapFactory.decodeStream(is);
			} else if (ct.startsWith("video/") || ct.startsWith("audio/")) {
				return BITMAP_PLAY;
			} else if (ct.startsWith("text/")) {
				return fetchPart(is);
			}
			return null;
		}

		/**
		 * Fetch a part.
		 * 
		 * @param is
		 *            {@link InputStream}
		 * @return part
		 */
		private static CharSequence fetchPart(final InputStream is) {
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
		 * Get MMS part as {@link InputStream}.
		 * 
		 * @param cr
		 *            {@link ContentResolver}
		 * @param pid
		 *            part's id
		 * @return {@link InputStream}
		 */
		public static InputStream getPart(final ContentResolver cr,
				final long pid) {
			InputStream is = null;
			final Uri uri = getPartUri(pid);
			try {
				is = cr.openInputStream(uri);
			} catch (IOException e) {
				Log.e(TAG, "Failed to load part data", e);
			}
			return is;
		}

		/** Default constructor. */
		private Messages() {
			// nothing here.
		}
	}

	/**
	 * Class holding all threads.
	 * 
	 * @author flx
	 */
	public static final class Threads {
		/** Table name. */
		private static final String TABLE = "threads";
		/** {@link HashMap} for projection. */
		private static final HashMap<String, String> PROJECTION_MAP;

		/** No photo available. */
		public static final Bitmap NO_PHOTO = Bitmap.createBitmap(1, 1,
				Bitmap.Config.RGB_565);

		/** INDEX: id. */
		public static final int INDEX_ID = 0;
		/** INDEX: date. */
		public static final int INDEX_DATE = 1;
		/** INDEX: address. */
		public static final int INDEX_ADDRESS = 2;
		/** INDEX: body. */
		public static final int INDEX_BODY = 3;
		/** INDEX: type. */
		public static final int INDEX_TYPE = 4;
		/** INDEX: person id. */
		public static final int INDEX_PID = 5;
		/** INDEX: name. */
		public static final int INDEX_NAME = 6;
		/** INDEX: count. */
		public static final int INDEX_COUNT = 7;
		/** INDEX: read. */
		public static final int INDEX_READ = 8;

		/** Id. */
		public static final String ID = BaseColumns._ID;
		/** Date. */
		public static final String DATE = Calls.DATE;
		/** Address. */
		public static final String ADDRESS = "address";
		/** body. */
		public static final String BODY = "body";
		/** type. */
		public static final String TYPE = Calls.TYPE;
		/** person id. */
		public static final String PID = "pid";
		/** name. */
		public static final String NAME = "name";
		/** count. */
		public static final String COUNT = "count";
		/** read. */
		public static final String READ = "read";

		/** Cursor's projection. */
		public static final String[] PROJECTION = { //
		ID, // 0
				DATE, // 1
				ADDRESS, // 2
				BODY, // 3
				TYPE, // 4
				PID, // 5
				NAME, // 6
				COUNT, // 7
				READ, // 8
		};

		/** ORIG_URI to resolve. */
		public static final Uri ORIG_URI = Uri
				.parse("content://mms-sms/conversations/");

		/** Default sort order. */
		public static final String DEFAULT_SORT_ORDER = DATE + " DESC";

		/** Content {@link Uri}. */
		public static final Uri CONTENT_URI = Uri.parse("content://"
				+ AUTHORITY + "/threads");

		/**
		 * The MIME type of {@link #CONTENT_URI} providing a list of threads.
		 */
		public static final String CONTENT_TYPE = // .
		"vnd.android.cursor.dir/vnd.ub0r.thread";

		/**
		 * The MIME type of a {@link #CONTENT_URI} single thread.
		 */
		public static final String CONTENT_ITEM_TYPE = // .
		"vnd.android.cursor.item/vnd.ub0r.thread";

		static {
			PROJECTION_MAP = new HashMap<String, String>();
			final int l = PROJECTION.length;
			for (int i = 0; i < l; i++) {
				PROJECTION_MAP.put(PROJECTION[i], PROJECTION[i]);
			}
		}

		/**
		 * Create table in {@link SQLiteDatabase}.
		 * 
		 * @param db
		 *            {@link SQLiteDatabase}
		 */
		public static void onCreate(final SQLiteDatabase db) {
			Log.i(TAG, "create table: " + TABLE);
			db.execSQL("DROP TABLE IF EXISTS " + TABLE);
			db.execSQL("CREATE TABLE " + TABLE + " (" // .
					+ ID + " LONG," // .
					+ DATE + " LONG,"// .
					+ ADDRESS + " TEXT,"// .
					+ BODY + " TEXT,"// .
					+ TYPE + " INTEGER," // .
					+ PID + " TEXT," // .
					+ NAME + " TEXT,"// .
					+ COUNT + " INTEGER," // .
					+ READ + " INTEGER" // .
					+ ");");
		}

		/**
		 * Upgrade table.
		 * 
		 * @param db
		 *            {@link SQLiteDatabase}
		 * @param oldVersion
		 *            old version
		 * @param newVersion
		 *            new version
		 */
		public static void onUpgrade(final SQLiteDatabase db,
				final int oldVersion, final int newVersion) {
			Log.w(TAG, "Upgrading table: " + TABLE);
			onCreate(db);
		}

		/** Default constructor. */
		private Threads() {
			// nothing here.
		}
	}

	/** Internal id: threads. */
	private static final int THREADS = 1;
	/** Internal id: single thread. */
	private static final int THREAD_ID = 2;
	/** Internal id: messages. */
	private static final int MESSAGES = 3;
	/** Internal id: single message. */
	private static final int MESSAGE_ID = 4;
	/** Internal id: messages from a single thread. */
	private static final int MESSAGES_TID = 5;

	/** {@link UriMatcher}. */
	private static final UriMatcher URI_MATCHER;

	static {
		URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
		URI_MATCHER.addURI(AUTHORITY, "threads", THREADS);
		URI_MATCHER.addURI(AUTHORITY, "threads/#", THREAD_ID);
		URI_MATCHER.addURI(AUTHORITY, "messages", MESSAGES);
		URI_MATCHER.addURI(AUTHORITY, "messages/#", MESSAGE_ID);
		URI_MATCHER.addURI(AUTHORITY, "conversation/#", MESSAGES_TID);
	}

	/**
	 * This class helps open, create, and upgrade the database file.
	 */
	private static class DatabaseHelper extends SQLiteOpenHelper {

		/**
		 * Default Constructor.
		 * 
		 * @param context
		 *            {@link Context}
		 */
		DatabaseHelper(final Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void onCreate(final SQLiteDatabase db) {
			Log.i(TAG, "create database");
			Threads.onCreate(db);
			Messages.onCreate(db);
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void onUpgrade(final SQLiteDatabase db, final int oldVersion,
				final int newVersion) {
			Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
					+ newVersion + ", which will destroy all old data");
			Threads.onUpgrade(db, oldVersion, newVersion);
			Messages.onUpgrade(db, oldVersion, newVersion);
		}
	}

	/** {@link DatabaseHelper}. */
	private DatabaseHelper mOpenHelper;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int delete(final Uri uri, final String selection,
			final String[] selectionArgs) {
		Log.d(TAG, "delete: " + uri + " w: " + selection);
		final SQLiteDatabase db = this.mOpenHelper.getWritableDatabase();
		final ContentResolver cr = this.getContext().getContentResolver();
		int ret = 0;
		switch (URI_MATCHER.match(uri)) {
		case MESSAGES:
			ret = db.delete(Messages.TABLE, selection, selectionArgs);
			cr.delete(Messages.ORIG_URI_SMS, selection, selectionArgs);
			cr.delete(Messages.ORIG_URI_MMS, selection, selectionArgs);
			this.updateThreads(db);
			break;
		case MESSAGE_ID:
			final long mid = ContentUris.parseId(uri);
			ret = db.delete(Messages.TABLE, DbUtils.sqlAnd(Messages.ID + "="
					+ mid, selection), selectionArgs);
			if (mid >= 0L) {
				cr.delete(ContentUris
						.withAppendedId(Messages.ORIG_URI_SMS, mid), selection,
						selectionArgs);
			} else {
				cr.delete(ContentUris.withAppendedId(Messages.ORIG_URI_MMS, -1L
						* mid), selection, selectionArgs);
			}
			this.updateThreads(db);
			break;
		case MESSAGES_TID:
		case THREAD_ID:
			final long tid = ContentUris.parseId(uri);
			ret = db.delete(Messages.TABLE, DbUtils.sqlAnd(Messages.THREADID
					+ "=" + tid, selection), selectionArgs);
			ret += db.delete(Threads.TABLE, DbUtils.sqlAnd(Threads.ID + "="
					+ tid, selection), selectionArgs);
			cr.delete(ContentUris.withAppendedId(Threads.CONTENT_URI, tid),
					selection, selectionArgs);
			break;
		case THREADS:
			ret = db.delete(Threads.TABLE, selection, selectionArgs);
			cr.delete(Threads.CONTENT_URI, selection, selectionArgs);
			break;
		default:
			throw new IllegalArgumentException("Unknown Uri " + uri);
		}
		if (ret > 0) {
			cr.notifyChange(Messages.CONTENT_URI, null);
			this.sync(db);
		}
		Log.d(TAG, "exit delete(): " + ret);
		return ret;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getType(final Uri uri) {
		switch (URI_MATCHER.match(uri)) {
		case MESSAGES:
			return Messages.CONTENT_TYPE;
		case MESSAGE_ID:
			return Messages.CONTENT_ITEM_TYPE;
		case THREADS:
			return Threads.CONTENT_TYPE;
		case THREAD_ID:
			return Threads.CONTENT_ITEM_TYPE;
		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Uri insert(final Uri uri, final ContentValues values) {
		throw new IllegalArgumentException("Method not implemented");
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean onCreate() {
		this.mOpenHelper = new DatabaseHelper(this.getContext());
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Cursor query(final Uri uri, final String[] projection,
			final String selection, final String[] selectionArgs,
			final String sortOrder) {
		Log.d(TAG, "query: " + uri + " w: " + selection);
		final SQLiteDatabase db = this.mOpenHelper.getWritableDatabase();
		this.sync(db);

		SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
		// If no sort order is specified use the default
		String orderBy = sortOrder;

		switch (URI_MATCHER.match(uri)) {
		case MESSAGE_ID:
			qb.appendWhere(Messages.ID + "=" + ContentUris.parseId(uri));
			qb.setTables(Messages.TABLE);
			qb.setProjectionMap(Messages.PROJECTION_MAP);
			if (TextUtils.isEmpty(orderBy)) {
				orderBy = Messages.DEFAULT_SORT_ORDER;
			}
			break;
		case MESSAGES_TID:
			qb.appendWhere(Messages.THREADID + "=" + ContentUris.parseId(uri));
			qb.setTables(Messages.TABLE);
			qb.setProjectionMap(Messages.PROJECTION_MAP);
			if (TextUtils.isEmpty(orderBy)) {
				orderBy = Messages.DEFAULT_SORT_ORDER;
			}
			break;
		case MESSAGES:
			qb.setTables(Messages.TABLE);
			qb.setProjectionMap(Messages.PROJECTION_MAP);
			if (TextUtils.isEmpty(orderBy)) {
				orderBy = Messages.DEFAULT_SORT_ORDER;
			}
			break;
		case THREAD_ID:
			qb.appendWhere(Threads.ID + "=" + ContentUris.parseId(uri));
		case THREADS:
			qb.setTables(Threads.TABLE);
			qb.setProjectionMap(Threads.PROJECTION_MAP);
			if (TextUtils.isEmpty(orderBy)) {
				orderBy = Threads.DEFAULT_SORT_ORDER;
			}
			break;
		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}

		// Run the query
		Cursor c = qb.query(db, projection, selection, selectionArgs, null,
				null, orderBy);

		// Tell the cursor what uri to watch, so it knows when its source data
		// changes
		c.setNotificationUri(this.getContext().getContentResolver(), uri);
		Log.d(TAG, "exit query(): " + c.getCount());
		return c;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int update(final Uri uri, final ContentValues values,
			final String selection, final String[] selectionArgs) {
		Log.d(TAG, "update: " + uri + " w: " + selection + " " + values);
		final SQLiteDatabase db = this.mOpenHelper.getWritableDatabase();
		final ContentResolver cr = this.getContext().getContentResolver();
		int ret = 0;
		switch (URI_MATCHER.match(uri)) {
		case MESSAGES:
			ret = db.update(Messages.TABLE, values, selection, selectionArgs);
			cr.update(Messages.ORIG_URI_SMS, values, selection, selectionArgs);
			cr.update(Messages.ORIG_URI_MMS, values, selection, selectionArgs);
			break;
		case MESSAGE_ID:
			final long mid = ContentUris.parseId(uri);
			ret = db.update(Messages.TABLE, values, DbUtils.sqlAnd(Messages.ID
					+ "=" + mid, selection), selectionArgs);
			if (mid >= 0L) {
				cr.update(ContentUris
						.withAppendedId(Messages.ORIG_URI_SMS, mid), values,
						selection, selectionArgs);
			} else {
				cr.update(ContentUris.withAppendedId(Messages.ORIG_URI_MMS, -1L
						* mid), values, selection, selectionArgs);
			}
			break;
		case THREADS:
			ret = db.update(Threads.TABLE, values, selection, selectionArgs);
			cr.update(Threads.ORIG_URI, values, selection, null);
			break;
		case MESSAGES_TID:
		case THREAD_ID:
			final long tid = ContentUris.parseId(uri);
			ret = db.update(Threads.TABLE, values, DbUtils.sqlAnd(Threads.ID
					+ "=" + tid, selection), selectionArgs);
			ret += db.update(Messages.TABLE, values, DbUtils.sqlAnd(
					Messages.THREADID + "=" + tid, selection), selectionArgs);
			cr.update(ContentUris.withAppendedId(Threads.ORIG_URI, tid),
					values, selection, null);
			break;
		default:
			throw new IllegalArgumentException("Unknown Uri " + uri);
		}
		if (ret > 0) {
			cr.notifyChange(Messages.CONTENT_URI, null);
			this.updateThreads(db);
		}
		Log.d(TAG, "exit update(): " + ret);
		return ret;
	}

	/**
	 * Add a SMS to internal database.
	 * 
	 * @param db
	 *            {@link SQLiteDatabase}
	 * @param rcursor
	 *            {@link Cursor}
	 * @return added?
	 */
	private boolean addSMS(final SQLiteDatabase db, final Cursor rcursor) {
		boolean ret = false;
		final ContentValues values = new ContentValues();
		final long mid = rcursor.getLong(Messages.INDEX_ID);
		final long tid = rcursor.getLong(Messages.INDEX_THREADID);
		values.put(Messages.ID, mid);
		values.put(Messages.ADDRESS, rcursor.getString(Messages.INDEX_ADDRESS));
		values.put(Messages.BODY, rcursor.getString(Messages.INDEX_BODY));
		values.put(Messages.DATE, rcursor.getLong(Messages.INDEX_DATE));
		values.put(Messages.THREADID, tid);
		values.put(Messages.TYPE, rcursor.getInt(Messages.INDEX_TYPE));
		values.put(Messages.READ, rcursor.getInt(Messages.INDEX_READ));
		final int i = db.update(Messages.TABLE, values, Messages.ID + " = "
				+ mid, null);
		if (i > 0) {
			Log.d(TAG, "update sms: " + mid + "/" + tid + " " + values);
			ret = true;
		} else {
			Log.d(TAG, "add sms: " + mid + "/" + tid + " " + values);
			final long l = db.insert(Messages.TABLE, null, values);
			if (l >= 0L) {
				ret = true;
			}
		}
		return ret;
	}

	/**
	 * Add a MMS to internal database.
	 * 
	 * @param db
	 *            {@link SQLiteDatabase}
	 * @param rcursor
	 *            {@link Cursor}
	 * @return added?
	 */
	private boolean addMMS(final SQLiteDatabase db, final Cursor rcursor) {
		boolean ret = false;
		final ContentValues values = new ContentValues();
		final int iMId = rcursor.getColumnIndex(Messages.ID);
		final int iThreadId = rcursor.getColumnIndex(Messages.THREADID);
		final int iDate = rcursor.getColumnIndex(Messages.DATE);
		final int iType = rcursor.getColumnIndex(Messages.TYPE);
		final int iRead = rcursor.getColumnIndex(Messages.READ);
		final int iText = rcursor.getColumnIndex("text");
		final long mid = rcursor.getLong(iMId);
		final long tid = rcursor.getLong(iThreadId);
		long date = rcursor.getLong(iDate);
		date = getDate(date);
		values.put(Messages.ID, -1 * mid);
		values.put(Messages.DATE, date);
		values.put(Messages.THREADID, tid);
		values.put(Messages.TYPE, rcursor.getInt(iType));
		values.put(Messages.READ, rcursor.getInt(iRead));
		if (iText >= 0) {
			final String text = rcursor.getString(iText);
			values.put(Messages.BODY, text);
		}
		final int i = db.update(Messages.TABLE, values, Messages.ID + " = "
				+ mid, null);
		if (i > 0) {
			Log.d(TAG, "update mms: " + mid + "/" + tid + " " + values);
			ret = true;
		} else {
			Log.d(TAG, "add mms: " + mid + "/" + tid + " " + values);
			final long l = db.insert(Messages.TABLE, null, values);
			if (l >= 0L) {
				ret = true;
			}
		}
		return ret;
	}

	/**
	 * Get new SMS.
	 * 
	 * @param db
	 *            {@link SQLiteDatabase}
	 * @param cr
	 *            {@link ContentResolver}
	 * @param date
	 *            date of newest internal message
	 * @return database changed?
	 */
	private boolean getNewSMS(final SQLiteDatabase db,
			final ContentResolver cr, final long date) {
		boolean ret = false;
		// get new sms
		Cursor rcursor = cr.query(Messages.ORIG_URI_SMS,
				Messages.ORIG_PROJECTION_SMS, Messages.DATE + " > " + date,
				null, null);
		if (rcursor != null && rcursor.moveToFirst()) {
			do {
				ret |= this.addSMS(db, rcursor);
			} while (rcursor.moveToNext());
		}
		if (rcursor != null && !rcursor.isClosed()) {
			rcursor.close();
		}

		// check message count and check all messages if not equal
		final String sortOrder = " DESC, " + Messages.ID + " DESC";
		rcursor = cr.query(Messages.ORIG_URI_SMS, Messages.ORIG_PROJECTION_SMS,
				null, null, Messages.DATE + sortOrder);
		Cursor lcursor = db.query(Messages.TABLE, Messages.PROJECTION,
				Messages.WHERE_TYPE_SMS, null, null, null, Messages.DATE
						+ sortOrder);
		if (rcursor == null || lcursor == null) {
			return false;
		}
		if (!rcursor.moveToFirst()) {
			// no remote message: delete all
			int r = db.delete(Messages.TABLE, Messages.WHERE_TYPE_SMS, null);
			if (r > 0) {
				ret = true;
				lcursor.requery();
			}
		}
		int rcount = rcursor.getCount();
		int lcount = lcursor.getCount();
		if (rcount != lcount) {
			rcursor.moveToFirst();
			lcursor.moveToFirst();
			// walk through all messages
			do {
				long rdate = rcursor.getLong(Messages.INDEX_DATE);
				Log.d(TAG, "rdate: " + rdate);
				do {
					long ldate;
					if (lcursor.isAfterLast() || lcount == 0) {
						ldate = -1L;
					} else {
						ldate = lcursor.getLong(Messages.INDEX_DATE);
					}
					Log.d(TAG, "ldate: " + ldate);
					Log.d(TAG, "rdate-ldate: " + (rdate - ldate));
					if (ldate < rdate) {
						// add sms and check next remote
						ret |= this.addSMS(db, rcursor);
						break;
					} else if (ldate > rdate) {
						// delete local sms and check next local
						db.delete(Messages.TABLE,
								Messages.DATE + " = " + ldate, null);
						ret = true;
					} else {
						// check both next
						lcursor.moveToNext();
						break;
					}
				} while (lcursor.moveToNext());
			} while (rcursor.moveToNext());
		}
		if (!rcursor.isClosed()) {
			rcursor.close();
		}
		if (!lcursor.isClosed()) {
			lcursor.close();
		}

		// check read messages
		// set all internal messages to read
		ContentValues values = new ContentValues();
		values.put(Messages.READ, 1);
		int i = db.update(Messages.TABLE, values, Messages.READ + " = 0"
				+ " AND " + Messages.WHERE_TYPE_MMS, null);
		if (i > 0) {
			ret = true;
		}
		// set unread messages unread internally
		rcursor = cr.query(Messages.ORIG_URI_SMS, Messages.ORIG_PROJECTION_SMS,
				Messages.READ + " = 0", null, null);
		if (rcursor != null && rcursor.moveToFirst()) {
			ret = true;
			values.put(Messages.READ, 0);
			do {
				db.update(Messages.TABLE, values, Messages.ID + " = "
						+ rcursor.getLong(Messages.INDEX_ID), null);
			} while (rcursor.moveToNext());
		}
		if (rcursor != null && !rcursor.isClosed()) {
			rcursor.close();
		}

		// check draft messages
		// set all internal drafts as sent
		values = new ContentValues();
		values.put(Messages.TYPE, Messages.TYPE_SMS_OUT);
		i = db.update(Messages.TABLE, values, Messages.TYPE + " = "
				+ Messages.TYPE_SMS_DRAFT, null);
		if (i > 0) {
			ret = true;
		}
		// set draft messages as draft internally
		rcursor = cr.query(Messages.ORIG_URI_SMS, Messages.ORIG_PROJECTION_SMS,
				Messages.TYPE + " = " + Messages.TYPE_SMS_DRAFT, null, null);
		if (rcursor != null && rcursor.moveToFirst()) {
			ret = true;
			values.put(Messages.TYPE, Messages.TYPE_SMS_DRAFT);
			do {
				db.update(Messages.TABLE, values, Messages.ID + " = "
						+ rcursor.getLong(Messages.INDEX_ID), null);
			} while (rcursor.moveToNext());
		}
		if (rcursor != null && !rcursor.isClosed()) {
			rcursor.close();
		}
		return ret;
	}

	/**
	 * Get new MMS.
	 * 
	 * @param db
	 *            {@link SQLiteDatabase}
	 * @param cr
	 *            {@link ContentResolver}
	 * @param date
	 *            date of newest internal message
	 * @return database changed?
	 */
	private boolean getNewMMS(final SQLiteDatabase db,
			final ContentResolver cr, final long date) {
		boolean ret = false;
		// get new mms
		Cursor rcursor = cr.query(Messages.ORIG_URI_MMS, null, Messages.DATE
				+ " > " + (date / ConversationList.MILLIS), null, null);
		if (rcursor != null && rcursor.moveToFirst()) {
			do {
				ret |= this.addMMS(db, rcursor);
			} while (rcursor.moveToNext());
		}
		if (rcursor != null && !rcursor.isClosed()) {
			rcursor.close();
		}

		// check message count and check all messages if not equal
		final String sortOrder = " DESC, " + Messages.ID + " DESC";
		rcursor = cr.query(Messages.ORIG_URI_MMS, null, null, null,
				Messages.DATE + sortOrder);
		Cursor lcursor = db.query(Messages.TABLE, Messages.PROJECTION,
				Messages.WHERE_TYPE_MMS, null, null, null, Messages.DATE
						+ sortOrder);
		if (rcursor == null || lcursor == null) {
			return false;
		}
		if (!rcursor.moveToFirst()) {
			// no remote message: delete all
			final int r = db.delete(Messages.TABLE, Messages.WHERE_TYPE_MMS,
					null);
			if (r > 0) {
				ret = true;
				lcursor.requery();
			}
		}
		final int iMId = rcursor.getColumnIndex(Messages.ID);
		final int iThreadId = rcursor.getColumnIndex(Messages.THREADID);
		final int iDate = rcursor.getColumnIndex(Messages.DATE);
		final int iType = rcursor.getColumnIndex(Messages.TYPE);
		final int iRead = rcursor.getColumnIndex(Messages.READ);
		final int iText = rcursor.getColumnIndex("text");
		int rcount = rcursor.getCount();
		int lcount = lcursor.getCount();
		if (rcount != lcount) {
			rcursor.moveToFirst();
			lcursor.moveToFirst();
			// walk through all messages
			do {
				long rdate = getDate(rcursor.getLong(iDate));
				Log.d(TAG, "rdate: " + rdate);
				do {
					long ldate;
					if (lcursor.isAfterLast() || lcount == 0) {
						ldate = -1L;
					} else {
						ldate = lcursor.getLong(Messages.INDEX_DATE);
					}
					Log.d(TAG, "ldate: " + ldate);
					Log.d(TAG, "rdate-ldate: " + (rdate - ldate));
					if (ldate < rdate) {
						// add mms and check next remote
						ret |= this.addMMS(db, rcursor);
						break;
					} else if (ldate > rdate) {
						// delete local mms and check next local
						db.delete(Messages.TABLE,
								Messages.DATE + " = " + ldate, null);
						ret = true;
					} else {
						// check both next
						lcursor.moveToNext();
						break;
					}
				} while (lcursor.moveToNext());
			} while (rcursor.moveToNext());
		}
		if (!rcursor.isClosed()) {
			rcursor.close();
		}
		if (!lcursor.isClosed()) {
			lcursor.close();
		}

		// check read messages
		// set all internal messages to read
		ContentValues values = new ContentValues();
		values.put(Messages.READ, 1);
		int i = db.update(Messages.TABLE, values, Messages.READ + " = 0"
				+ " AND " + Messages.WHERE_TYPE_MMS, null);
		if (i > 0) {
			ret = true;
		}
		// set unread messages unread internally
		rcursor = cr.query(Messages.ORIG_URI_MMS, null, Messages.READ + " = 0",
				null, null);
		if (rcursor != null && rcursor.moveToFirst()) {
			ret = true;
			values.put(Messages.READ, 0);
			do {
				db.update(Messages.TABLE, values, Messages.ID + " = "
						+ (-1L * rcursor.getLong(iMId)), null);
			} while (rcursor.moveToNext());
		}
		if (rcursor != null && !rcursor.isClosed()) {
			rcursor.close();
		}

		return ret;
	}

	/**
	 * Update internal {@link SQLiteDatabase} from external
	 * {@link ConversationProvider}.
	 * 
	 * @param db
	 *            {@link SQLiteDatabase}.
	 */
	private synchronized void sync(final SQLiteDatabase db) {
		boolean changed = false;
		final ContentResolver cr = this.getContext().getContentResolver();

		// get last internal message
		final Cursor lcursor = db.query(Messages.TABLE, Messages.PROJECTION,
				null, null, null, null, Messages.DATE + " DESC");
		long lmaxdate = -1L;
		if (lcursor == null) {
			Log.e(TAG, "lcursor = null");
			return;
		}
		if (lcursor.moveToFirst()) {
			lmaxdate = lcursor.getLong(Messages.INDEX_DATE);
		}
		if (!lcursor.isClosed()) {
			lcursor.close();
		}
		// get new messages
		changed |= this.getNewSMS(db, cr, lmaxdate);
		changed |= this.getNewMMS(db, cr, lmaxdate);

		if (changed) {
			this.updateThreads(db);
			cr.notifyChange(Messages.CONTENT_URI, null);
		}
	}

	/**
	 * Update Threads table from Messages.
	 * 
	 * @param db
	 *            {@link SQLiteDatabase}
	 * @param threadId
	 *            thread's ID, -1 for all
	 */
	private void updateThread(final SQLiteDatabase db, final long threadId) {
		final String[] proj = new String[] {// .
		Messages.DATE, // 0
				Messages.BODY, // 1
				Messages.ADDRESS, // 2
				"min(" + Messages.READ + ")", // 3
				Messages.TYPE, // 4
				"count(" + Messages.ID + ")", // 5
		};

		final Cursor cursor = db.query(Messages.TABLE, proj, Messages.THREADID
				+ " = " + threadId, null, null, null, Messages.DATE + " ASC");
		if (cursor != null && cursor.moveToFirst()) {
			final ContentValues values = new ContentValues();
			values.put(Threads.DATE, cursor.getLong(0));
			values.put(Threads.BODY, cursor.getString(1));
			values.put(Threads.ADDRESS, cursor.getString(2));
			values.put(Threads.READ, cursor.getInt(3));
			values.put(Threads.TYPE, cursor.getInt(4));
			values.put(Threads.COUNT, cursor.getInt(5));
			Log.d(TAG, "update thread: " + threadId + "/ " + values);
			int ret = db.update(Threads.TABLE, values, Threads.ID + " = "
					+ threadId, null);
			if (ret <= 0) {
				Log.d(TAG, "insert thread: " + threadId);
				values.put(Threads.ID, threadId);
				db.insert(Threads.TABLE, null, values);
			}
		}
		if (cursor != null && !cursor.isClosed()) {
			cursor.close();
		}

	}

	/**
	 * Update Threads table from Messages.
	 * 
	 * @param db
	 *            {@link SQLiteDatabase}
	 */
	private void updateThreads(final SQLiteDatabase db) {
		final String[] mproj = new String[] { Messages.THREADID };
		final String[] cproj = new String[] { Threads.ID };
		final Cursor mcursor = db.query(Messages.TABLE, mproj, null, null,
				Messages.THREADID, null, Messages.THREADID + " ASC");
		final Cursor ccursor = db.query(Threads.TABLE, cproj, null, null, null,
				null, Threads.ID + " ASC");
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
							db.delete(Threads.TABLE, Threads.ID + " = " + ctid,
									null);
						} else {
							break;
						}
					} while (ccursor.moveToNext());
				}
				this.updateThread(db, mtid);
				if (ctid == mtid) {
					ccursor.moveToNext();
				}
			} while (mcursor.moveToNext());
			Log.d(TAG, "delete: tid>" + mtid);
			db.delete(Threads.TABLE, Threads.ID + " > " + mtid, null);
		}
		if (mcursor != null && !mcursor.isClosed()) {
			mcursor.close();
		}
		if (ccursor != null && !ccursor.isClosed()) {
			ccursor.close();
		}

		// TODO: trigger service to update names, pid, picture..
	}

	/**
	 * Get display name.
	 * 
	 * @param address
	 *            address
	 * @param name
	 *            name
	 * @param full
	 *            true for "name &lt;address&gt;"
	 * @return display name
	 */
	public static String getDisplayName(final String address,
			final String name, final boolean full) {
		if (name == null || name.trim().length() == 0) {
			return address;
		}
		if (full) {
			return name + "<" + address + ">";
		}
		return name;
	}

	/**
	 * Mark all messages with a given {@link Uri} as read.
	 * 
	 * @param context
	 *            {@link Context}
	 * @param uri
	 *            {@link Uri}
	 * @param read
	 *            read status
	 */
	public static void markRead(final Context context, final Uri uri,
			final int read) {
		if (uri == null) {
			return;
		}
		final String select = Messages.SELECTION_UNREAD.replace("0", String
				.valueOf(1 - read));
		final ContentResolver cr = context.getContentResolver();
		final Cursor cursor = cr.query(uri, Messages.PROJECTION, select, null,
				null);
		if (cursor != null && cursor.getCount() <= 0) {
			if (uri.equals(Messages.CONTENT_URI)) {
				SmsReceiver.updateNewMessageNotification(context, null);
			}
			cursor.close();
			return;
		}
		if (cursor != null && !cursor.isClosed()) {
			cursor.close();
		}

		final ContentValues cv = new ContentValues();
		cv.put(Messages.READ, read);
		cr.update(uri, cv, select, null);
		SmsReceiver.updateNewMessageNotification(context, null);
	}

	/**
	 * Delete messages with a given {@link Uri}.
	 * 
	 * @param context
	 *            {@link Context}
	 * @param uri
	 *            {@link Uri}
	 * @param title
	 *            title of {@link Dialog}
	 * @param message
	 *            message of the {@link Dialog}
	 * @param activity
	 *            {@link Activity} to finish when deleting.
	 */
	public static void deleteMessages(final Context context, final Uri uri,
			final int title, final int message, final Activity activity) {
		final ContentResolver cr = context.getContentResolver();
		final Cursor cursor = cr.query(uri, Messages.PROJECTION, null, null,
				null);
		if (cursor == null) {
			return;
		}
		final int l = cursor.getCount();
		if (cursor != null && !cursor.isClosed()) {
			cursor.close();
		}
		if (l == 0) {
			return;
		}

		final Builder builder = new Builder(context);
		builder.setTitle(title);
		builder.setMessage(message);
		builder.setNegativeButton(android.R.string.no, null);
		builder.setPositiveButton(android.R.string.yes,
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(final DialogInterface dialog,
							final int which) {
						cr.delete(uri, null, null);
						if (activity != null) {
							activity.finish();
						}
						SmsReceiver.updateNewMessageNotification(context, null);
					}
				});
		builder.create().show();
	}

	/**
	 * Fix MMS date.
	 * 
	 * @param date
	 *            date
	 * @return date as milliseconds since epoch
	 */
	public static long getDate(final long date) {
		if (date > ConversationList.MIN_DATE) {
			return date;
		}
		return date * ConversationList.MILLIS;
	}
}
