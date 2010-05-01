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

import java.util.HashMap;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.provider.BaseColumns;
import android.provider.CallLog.Calls;
import android.text.TextUtils;
import android.util.Log;

/**
 * @author flx
 */
public final class ConversationProvider extends ContentProvider {
	/** Tag for output. */
	private static final String TAG = "SMSdroid.cvp";

	private static final String DATABASE_NAME = "mms.db";
	private static final int DATABASE_VERSION = 2;
	private static final String THREADS_TABLE_NAME = "threads";

	private static final HashMap<String, String> sThreadsProjectionMap;

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
	/** INDEX: count. */
	public static final int INDEX_COUNT = 7;

	/** Cursor's projection. */
	public static final String[] PROJECTION = { //
	BaseColumns._ID, // 0
			Calls.DATE, // 1
			"address", // 2
			"thread_id", // 3
			"body", // 4
			Calls.TYPE, // 5
			"read", // 6
			"count", // 7
	};

	/** URI to resolve. */
	private static final Uri URI = Uri
			.parse("content://mms-sms/conversations/");
	/** Cursor's projection (outgoing). */
	public static final String[] PROJECTION_OUT = { //
	BaseColumns._ID, // 0
			Calls.DATE, // 1
			"address", // 2
			"thread_id", // 3
			"body", // 4
			Calls.TYPE, // 5
			"read", // 6
	};

	/** Cursors row in hero phones: address. */
	static final String ADDRESS_HERO = "recipient_address AS "
			+ PROJECTION[INDEX_ADDRESS];
	/** Cursors row in hero phones: thread_id. */
	static final String THREADID_HERO = "_id AS " + PROJECTION[INDEX_THREADID];

	public static final String DEFAULT_SORT_ORDER = PROJECTION[INDEX_DATE]
			+ " DESC";

	private static final int THREADS = 1;
	private static final int THREAD_ID = 2;

	public static final String AUTHORITY = "de.ub0r.android.smsdroid.provider.conversations";

	public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY
			+ "/threads");

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

	private static final UriMatcher sUriMatcher;

	static {
		sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
		sUriMatcher.addURI(AUTHORITY, "threads", THREADS);
		sUriMatcher.addURI(AUTHORITY, "threads/#", THREAD_ID);

		sThreadsProjectionMap = new HashMap<String, String>();
		final int l = PROJECTION.length;
		for (int i = 0; i < l; i++) {
			sThreadsProjectionMap.put(PROJECTION[i], PROJECTION[i]);
		}
	}

	/**
	 * This class helps open, create, and upgrade the database file.
	 */
	private static class DatabaseHelper extends SQLiteOpenHelper {

		DatabaseHelper(final Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		@Override
		public void onCreate(final SQLiteDatabase db) {
			Log.i(TAG, "create database");
			db.execSQL("CREATE TABLE " + THREADS_TABLE_NAME + " ("
					+ PROJECTION[INDEX_ID] + " INTEGER PRIMARY KEY,"
					+ PROJECTION[INDEX_DATE] + " INTEGER,"
					+ PROJECTION[INDEX_ADDRESS] + " TEXT,"
					+ PROJECTION[INDEX_THREADID] + " INTEGER,"
					+ PROJECTION[INDEX_BODY] + " TEXT,"
					+ PROJECTION[INDEX_TYPE] + " INTEGER,"
					+ PROJECTION[INDEX_READ] + " INTEGER,"
					+ PROJECTION[INDEX_COUNT] + " INTEGER" + ");");
		}

		@Override
		public void onUpgrade(final SQLiteDatabase db, final int oldVersion,
				final int newVersion) {
			Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
					+ newVersion + ", which will destroy all old data");
			db.execSQL("DROP TABLE IF EXISTS " + THREADS_TABLE_NAME);
			this.onCreate(db);
		}
	}

	private DatabaseHelper mOpenHelper;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int delete(final Uri uri, final String selection,
			final String[] selectionArgs) {
		throw new IllegalArgumentException("method not implemented");
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getType(final Uri uri) {
		switch (sUriMatcher.match(uri)) {
		case THREADS:
			return CONTENT_TYPE;
		case THREAD_ID:
			return CONTENT_ITEM_TYPE;
		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Uri insert(final Uri uri, final ContentValues values) {
		throw new IllegalArgumentException("method not implemented");
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean onCreate() {
		this.mOpenHelper = new DatabaseHelper(this.getContext());
		return true;
	}

	private void updateSource(final SQLiteDatabase db) {
		db.delete(THREADS_TABLE_NAME, null, null);
		final ContentResolver cr = this.getContext().getContentResolver();
		Cursor cursor;
		try {
			cursor = cr.query(URI, PROJECTION_OUT, null, null,
					DEFAULT_SORT_ORDER);
		} catch (SQLException e) {
			Log.w(TAG, "error while query", e);
			PROJECTION[INDEX_ADDRESS] = ADDRESS_HERO;
			PROJECTION[INDEX_THREADID] = THREADID_HERO;
			cursor = cr.query(URI, Conversation.PROJECTION, null, null,
					DEFAULT_SORT_ORDER);
		}
		if (cursor != null && cursor.moveToFirst()) {
			do {
				ContentValues cv = new ContentValues();
				cv.put(PROJECTION[INDEX_ID], cursor.getInt(INDEX_ID));
				long d = cursor.getLong(INDEX_DATE);
				if (d < SMSdroid.MIN_DATE) {
					d *= SMSdroid.MILLIS;
				}
				cv.put(PROJECTION[INDEX_DATE], d);
				cv.put(PROJECTION[INDEX_ADDRESS], cursor
						.getString(INDEX_ADDRESS));
				cv.put(PROJECTION[INDEX_BODY], cursor.getString(INDEX_BODY));
				cv.put(PROJECTION[INDEX_THREADID], cursor
						.getInt(INDEX_THREADID));
				cv.put(PROJECTION[INDEX_TYPE], cursor.getInt(INDEX_TYPE));
				cv.put(PROJECTION[INDEX_READ], cursor.getInt(INDEX_READ));
				cv.put(PROJECTION[INDEX_COUNT], -1);
				db.insert(THREADS_TABLE_NAME, PROJECTION[INDEX_ID], cv);
			} while (cursor.moveToNext());
		}
		db.close();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Cursor query(final Uri uri, final String[] projection,
			final String selection, final String[] selectionArgs,
			final String sortOrder) {
		final SQLiteDatabase db = this.mOpenHelper.getWritableDatabase();
		this.updateSource(db);

		SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
		qb.setTables(THREADS_TABLE_NAME);

		switch (sUriMatcher.match(uri)) {
		case THREADS:
			qb.setProjectionMap(sThreadsProjectionMap);
			break;
		case THREAD_ID:
			qb.setProjectionMap(sThreadsProjectionMap);
			qb.appendWhere(PROJECTION[INDEX_ID] + "="
					+ uri.getPathSegments().get(1));
			break;
		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}

		// If no sort order is specified use the default
		String orderBy;
		if (TextUtils.isEmpty(sortOrder)) {
			orderBy = DEFAULT_SORT_ORDER;
		} else {
			orderBy = sortOrder;
		}

		// Run the query
		Cursor c = qb.query(db, projection, selection, selectionArgs, null,
				null, orderBy);

		// Tell the cursor what uri to watch, so it knows when its source data
		// changes
		c.setNotificationUri(this.getContext().getContentResolver(), uri);
		return c;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int update(final Uri uri, final ContentValues values,
			final String selection, final String[] selectionArgs) {
		throw new IllegalArgumentException("method not implemented");
	}
}
