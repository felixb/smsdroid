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

/**
 * @author flx
 */
public final class ConversationProvider extends ContentProvider {
	/** Tag for output. */
	private static final String TAG = "cvp";

	/** Name of the {@link SQLiteDatabase}. */
	private static final String DATABASE_NAME = "mms.db";
	/** Version of the {@link SQLiteDatabase}. */
	private static final int DATABASE_VERSION = 5;
	/** Table name for threads. */
	private static final String THREADS_TABLE_NAME = "threads";

	/** {@link HashMap} for projection. */
	private static final HashMap<String, String> THREADS_PROJECTION_MAP;

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
	/** INDEX: person id. */
	public static final int INDEX_PID = 6;
	/** INDEX: name. */
	public static final int INDEX_NAME = 7;

	/** Cursor's projection. */
	public static final String[] PROJECTION = { //
	BaseColumns._ID, // 0
			Calls.DATE, // 1
			"address", // 2
			"thread_id", // 3
			"body", // 4
			Calls.TYPE, // 5
			"pid", // 6
			"name", // 7
	};

	/** ORIG_URI to resolve. */
	public static final Uri ORIG_URI = Uri
			.parse("content://mms-sms/conversations/");
	/** Cursor's projection (outgoing). */
	private static final String[] PROJECTION_OUT = { //
	BaseColumns._ID, // 0
			Calls.DATE, // 1
			"address", // 2
			"thread_id", // 3
			"body", // 4
			Calls.TYPE, // 5
	};

	/** Cursors row in hero phones: address. */
	static final String ADDRESS_HERO = "recipient_address AS "
			+ PROJECTION[INDEX_ADDRESS];
	/** Cursors row in hero phones: thread_id. */
	static final String THREADID_HERO = "_id AS " + PROJECTION[INDEX_THREADID];

	/** Default sort order. */
	public static final String DEFAULT_SORT_ORDER = PROJECTION[INDEX_DATE]
			+ " DESC";

	/** Internal id: threads. */
	private static final int THREADS = 1;
	/** Internal id: single thread. */
	private static final int THREAD_ID = 2;

	/** Authority. */
	public static final String AUTHORITY = "de.ub0r.android.smsdroid."
			+ "provider.conversations";

	/** Content {@link Uri}. */
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

	/** {@link UriMatcher}. */
	private static final UriMatcher URI_MATCHER;

	static {
		URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
		URI_MATCHER.addURI(AUTHORITY, "threads", THREADS);
		URI_MATCHER.addURI(AUTHORITY, "threads/#", THREAD_ID);

		THREADS_PROJECTION_MAP = new HashMap<String, String>();
		final int l = PROJECTION.length;
		for (int i = 0; i < l; i++) {
			THREADS_PROJECTION_MAP.put(PROJECTION[i], PROJECTION[i]);
		}
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
			db.execSQL("CREATE TABLE " + THREADS_TABLE_NAME + " ("
					+ PROJECTION[INDEX_ID] + " INTEGER PRIMARY KEY,"
					+ PROJECTION[INDEX_DATE] + " INTEGER,"
					+ PROJECTION[INDEX_ADDRESS] + " TEXT,"
					+ PROJECTION[INDEX_THREADID] + " INTEGER,"
					+ PROJECTION[INDEX_BODY] + " TEXT,"
					+ PROJECTION[INDEX_TYPE] + " INTEGER,"
					+ PROJECTION[INDEX_PID] + " TEXT," // .
					+ PROJECTION[INDEX_NAME] + " TEXT" + ");");
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void onUpgrade(final SQLiteDatabase db, final int oldVersion,
				final int newVersion) {
			Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
					+ newVersion + ", which will destroy all old data");
			db.execSQL("DROP TABLE IF EXISTS " + THREADS_TABLE_NAME);
			this.onCreate(db);
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
		if (uri.equals(CONTENT_URI)) {
			final SQLiteDatabase db = this.mOpenHelper.getWritableDatabase();
			int ret = db.delete(THREADS_TABLE_NAME, selection, selectionArgs);
			return ret;
		} else {
			throw new IllegalArgumentException("method not implemented");
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getType(final Uri uri) {
		switch (URI_MATCHER.match(uri)) {
		case THREADS:
			return CONTENT_TYPE;
		case THREAD_ID:
			return CONTENT_ITEM_TYPE;
		default:
			throw new IllegalArgumentException("Unknown ORIG_URI " + uri);
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

	/**
	 * Update a row in internal {@link SQLiteDatabase}.
	 * 
	 * @param db
	 *            {@link SQLiteDatabase}
	 * @param cout
	 *            {@link Cursor} from external {@link ConversationProvider}
	 * @param din
	 *            date from newest message in internal {@link SQLiteDatabase}
	 * @return true if any change happend
	 */
	private boolean updateRow(final SQLiteDatabase db, final Cursor cout,
			final long din) {
		long dout = cout.getLong(INDEX_DATE);
		if (dout < SMSdroid.MIN_DATE) {
			dout *= SMSdroid.MILLIS;
		}
		if (dout > din) {
			int tid = cout.getInt(INDEX_THREADID);
			ContentValues cv = new ContentValues();
			cv.put(PROJECTION[INDEX_DATE], dout);
			String a = cout.getString(INDEX_ADDRESS);
			if (a != null) {
				cv.put(PROJECTION[INDEX_ADDRESS], a);
				cv.put(PROJECTION[INDEX_NAME], (String) null);
				cv.put(PROJECTION[INDEX_PID], "");
			}
			a = null;
			cv.put(PROJECTION[INDEX_BODY], cout.getString(INDEX_BODY));
			cv.put(PROJECTION[INDEX_THREADID], tid);
			cv.put(PROJECTION[INDEX_TYPE], cout.getInt(INDEX_TYPE));
			if (db.update(THREADS_TABLE_NAME, cv, PROJECTION[INDEX_THREADID]
					+ " = " + tid, // .
					null) == 0) {
				Log.d(TAG, "insert row for thread: " + tid);
				db.insert(THREADS_TABLE_NAME, PROJECTION[INDEX_ID], cv);
			} else {
				Log.d(TAG, "update row for thread: " + tid);
			}
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Update internal {@link SQLiteDatabase} from external
	 * {@link ConversationProvider}.
	 * 
	 * @param db
	 *            {@link SQLiteDatabase}.
	 */
	private void updateSource(final SQLiteDatabase db) {
		final ContentResolver cr = this.getContext().getContentResolver();
		Cursor cin = db.query(THREADS_TABLE_NAME, PROJECTION, null, null, null,
				null, DEFAULT_SORT_ORDER);
		Cursor cout;
		try {
			cout = cr.query(ORIG_URI, PROJECTION_OUT, null, null,
					DEFAULT_SORT_ORDER);
		} catch (SQLException e) {
			Log.w(TAG, "error while query", e);
			PROJECTION_OUT[INDEX_ADDRESS] = ADDRESS_HERO;
			PROJECTION_OUT[INDEX_THREADID] = THREADID_HERO;
			cout = cr.query(ORIG_URI, PROJECTION_OUT, null, null,
					DEFAULT_SORT_ORDER);
		}

		if (cout != null && cout.moveToFirst()) {
			if (cin == null) {
				Log.e(TAG, "cursor in == null");
				return;
			}
			long din = 0;
			if (cin.moveToFirst()) {
				din = cin.getLong(INDEX_DATE);
			}
			// hunt for new sms
			do {
				if (!this.updateRow(db, cout, din)) {
					break;
				}
			} while (cout.moveToNext());

			// hunt for new mms
			if (!cout.moveToLast()) {
				Log.e(TAG, "error moving cursor to end");
			}
			do {
				long dout = cout.getLong(INDEX_DATE);
				Log.d(TAG, "din:  " + din);
				Log.d(TAG, "dout: " + dout);
				if (dout * SMSdroid.MILLIS < din) {
					continue;
				} else if (dout > SMSdroid.MIN_DATE) {
					break;
				} else if (!this.updateRow(db, cout, din)) {
					break;
				}
			} while (cout.moveToPrevious());
		}
		if (cin != null && !cin.isClosed()) {
			cin.close();
		}
		if (cout != null && !cout.isClosed()) {
			cout.close();
		}
		// internal db does have at least all the messages from external db
		cin = db.query(THREADS_TABLE_NAME, PROJECTION, null, null, null, null,
				PROJECTION[INDEX_THREADID] + " DESC");
		cout = cr.query(ORIG_URI, PROJECTION_OUT, null, null,
				PROJECTION[INDEX_THREADID] + " DESC");
		if (cout != null && cin != null && // .
				cout.requery() && cin.requery()) {
			if (cout.getCount() != cin.getCount()) {
				// hunt for deleted threads
				if (!cout.moveToFirst()) {
					Log.d(TAG, "delete all rows");
					db.delete(THREADS_TABLE_NAME, null, null);
				} else if (!cin.moveToFirst()) {
					Log.e(TAG, "error selecting first row");
				} else {
					do {
						final int tidin = cin.getInt(INDEX_THREADID);
						int tidout;
						if (cout.isAfterLast()) {
							tidout = -1;
						} else {
							tidout = cout.getInt(INDEX_THREADID);
						}
						if (tidin != tidout) {
							Conversation.removeConversation(tidin);
							Log.d(TAG, "delete row: " + tidin);
							db.delete(THREADS_TABLE_NAME,
									PROJECTION[INDEX_THREADID] + " = " + tidin,
									null);
						} else {
							cout.moveToNext();
						}
					} while (cin.moveToNext());
				}
			}
		}
		if (cin != null && !cin.isClosed()) {
			cin.close();
		}
		if (cout != null && !cout.isClosed()) {
			cout.close();
		}
		return;
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

		switch (URI_MATCHER.match(uri)) {
		case THREADS:
			qb.setProjectionMap(THREADS_PROJECTION_MAP);
			break;
		case THREAD_ID:
			qb.setProjectionMap(THREADS_PROJECTION_MAP);
			qb.appendWhere(PROJECTION[INDEX_ID] + "="
					+ uri.getPathSegments().get(1));
			break;
		default:
			throw new IllegalArgumentException("Unknown ORIG_URI " + uri);
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
		int tid = -1;
		try {
			tid = Integer.parseInt(uri.getLastPathSegment());
		} catch (NumberFormatException e) {
			Log.e(TAG, "not a number: " + uri, e);
			throw new IllegalArgumentException("method not implemented");
		}
		final SQLiteDatabase db = this.mOpenHelper.getWritableDatabase();
		int ret = db.update(THREADS_TABLE_NAME, values,
				PROJECTION[INDEX_THREADID] + " = " + tid, null);
		return ret;
	}
}
