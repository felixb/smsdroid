/*
 * Copyright (C) 2010 Marek Wehmer, Felix Bechstein
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

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import de.ub0r.android.lib.Log;

/**
 * Database holding blacklisted numbers.
 * 
 * @author marek
 */
public final class DBAdapter {
	int id = 0;
	public static final String KEY_NR = "nr";
	private static final String TAG = "DBAdapter";

	private static final String DATABASE_NAME = "spamlist";
	private static final String DATABASE_TABLE = "numbers";
	private static final int DATABASE_VERSION = 1;

	private static final String DATABASE_CREATE = "CREATE TABLE IF NOT EXISTS numbers (nr varchar(50) )";

	private final Context context;

	private DatabaseHelper DBHelper;
	private SQLiteDatabase db;

	/**
	 * Default constructor.
	 * 
	 * @param ctx
	 *            {@link Context}
	 */
	public DBAdapter(final Context ctx) {
		this.context = ctx;
		this.DBHelper = new DatabaseHelper(this.context);
	}

	/**
	 * {@link DatabaseHelper} for opening the database.
	 * 
	 * @author marek
	 */
	private static class DatabaseHelper extends SQLiteOpenHelper {
		DatabaseHelper(final Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		@Override
		public void onCreate(final SQLiteDatabase db) {
			db.execSQL(DATABASE_CREATE);
		}

		@Override
		public void onUpgrade(final SQLiteDatabase db, final int oldVersion,
				final int newVersion) {
			Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
					+ newVersion + ", which will destroy all old data");
			db.execSQL("DROP TABLE IF EXISTS numbers");
			this.onCreate(db);
		}
	}

	/**
	 * Open database.
	 * 
	 * @return {@link DBAdapter}
	 * @throws SQLException
	 *             SQLException
	 */
	public DBAdapter open() throws SQLException {
		this.db = this.DBHelper.getWritableDatabase();
		return this;
	}

	/**
	 * Close database.
	 */
	public void close() {
		this.DBHelper.close();
	}

	/**
	 * Insert a number into the spam database.
	 * 
	 * @param number
	 *            number
	 * @return id in database
	 */
	public long insertNr(final String number) {
		ContentValues initialValues = new ContentValues();
		initialValues.put(KEY_NR, number);
		return this.db.insert(DATABASE_TABLE, null, initialValues);
	}

	/**
	 * Check if number is blacklisted.
	 * 
	 * @param nr
	 *            number
	 * @return true if number is blacklisted
	 */
	public boolean isInDB(final String nr) {
		Cursor cursor = this.db.rawQuery("SELECT * from " + DATABASE_TABLE
				+ " WHERE nr = \"" + nr + "\"", null);
		return cursor.moveToFirst();
	}

	/**
	 * Get all blacklisted numbers.
	 * 
	 * @return blacklist
	 */
	public int getAllEntries() {
		Cursor cursor = this.db.rawQuery("SELECT COUNT(nr) FROM "
				+ DATABASE_TABLE, null);
		Log.d(TAG, cursor.toString());
		if (cursor.moveToFirst()) {
			return cursor.getInt(0);
		}
		return cursor.getInt(0);
	}

	/**
	 * Remove number from blacklist.
	 * 
	 * @param nr
	 *            number
	 */
	public void removeNr(final String nr) {
		this.db.delete(DATABASE_TABLE, "nr = \"" + nr + "\"", null);
	}
}
