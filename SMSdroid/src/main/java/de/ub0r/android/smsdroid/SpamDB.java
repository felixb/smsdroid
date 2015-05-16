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
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import de.ub0r.android.logg0r.Log;

/**
 * Database holding blacklisted numbers.
 *
 * @author marek, flx
 */
public final class SpamDB {

    /**
     * TAG for debug out.
     */
    private static final String TAG = "blacklist";

    /**
     * Name of {@link SQLiteDatabase}.
     */
    private static final String DATABASE_NAME = "spamlist";

    /**
     * Version of {@link SQLiteDatabase}.
     */
    private static final int DATABASE_VERSION = 1;

    /**
     * Table in {@link SQLiteDatabase}.
     */
    private static final String DATABASE_TABLE = "numbers";

    /**
     * Key in table.
     */
    public static final String KEY_NR = "nr";

    /**
     * Projection.
     */
    public static final String[] PROJECTION = new String[]{KEY_NR};

    /**
     * SQL to create {@link SQLiteDatabase}.
     */
    private static final String DATABASE_CREATE
            = "CREATE TABLE IF NOT EXISTS numbers (nr varchar(50) )";

    /**
     * {@link DatabaseHelper}.
     */
    private final DatabaseHelper dbHelper;

    /**
     * {@link SQLiteDatabase}.
     */
    private SQLiteDatabase db;

    /**
     * Default constructor.
     *
     * @param context {@link Context}
     */
    public SpamDB(final Context context) {
        dbHelper = new DatabaseHelper(context);
    }

    /**
     * {@link DatabaseHelper} for opening the database.
     *
     * @author marek
     */
    private static class DatabaseHelper extends SQLiteOpenHelper {

        /**
         * Default constructor.
         *
         * @param context {@link Context}
         */
        DatabaseHelper(final Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(final SQLiteDatabase db) {
            db.execSQL(DATABASE_CREATE);
        }

        @Override
        public void onUpgrade(final SQLiteDatabase db, final int oldVersion, final int newVersion) {
            Log.w(TAG, "Upgrading database from version ", oldVersion, " to ", newVersion
                    + ", which will destroy all old data");
            db.execSQL("DROP TABLE IF EXISTS numbers");
            onCreate(db);
        }
    }

    /**
     * Open database.
     *
     * @return {@link SpamDB}
     */
    public SpamDB open() {
        db = dbHelper.getWritableDatabase();
        return this;
    }

    /**
     * Close database.
     */
    public void close() {
        dbHelper.close();
    }

    /**
     * Insert a number into the spam database.
     *
     * @param nr number
     * @return id in database
     */
    public long insertNr(final String nr) {
        if (nr == null) {
            return -1L;
        }
        ContentValues initialValues = new ContentValues();
        initialValues.put(KEY_NR, nr);
        return db.insert(DATABASE_TABLE, null, initialValues);
    }

    /**
     * Check if number is blacklisted.
     *
     * @param nr number
     * @return true if number is blacklisted
     */
    public boolean isInDB(final String nr) {
        Log.d(TAG, "isInDB(", nr, ")");
        if (nr == null) {
            return false;
        }
        final Cursor cursor = db.query(DATABASE_TABLE, PROJECTION, KEY_NR + " = ?",
                new String[]{nr}, null, null, null);
        final boolean ret = cursor.moveToFirst();
        if (!cursor.isClosed()) {
            cursor.close();
        }
        return ret;
    }

    /**
     * Get all blacklisted numbers.
     *
     * @return blacklist
     */
    public int getEntrieCount() {
        final Cursor cursor = db.rawQuery("SELECT COUNT(nr) FROM " + DATABASE_TABLE, null);
        Log.d(TAG, cursor.toString());
        int ret = 0;
        if (cursor.moveToFirst()) {
            ret = cursor.getInt(0);
        }
        if (!cursor.isClosed()) {
            cursor.close();
        }
        return ret;
    }

    /**
     * Get all entries from blacklist.
     *
     * @return array of entries
     */
    public String[] getAllEntries() {
        final Cursor cursor = db.query(DATABASE_TABLE, PROJECTION, null, null, null, null,
                null);
        if (cursor == null) {
            return null;
        }
        final String[] ret = new String[cursor.getCount()];
        if (cursor.moveToFirst()) {
            int i = 0;
            do {
                ret[i] = cursor.getString(0);
                Log.d(TAG, "spam: ", ret[i]);
                ++i;
            } while (cursor.moveToNext());
        }
        if (!cursor.isClosed()) {
            cursor.close();
        }
        return ret;
    }

    /**
     * Remove number from blacklist.
     *
     * @param nr number
     */
    public void removeNr(final String nr) {
        if (nr == null) {
            return;
        }
        db.delete(DATABASE_TABLE, KEY_NR + " = ?", new String[]{nr});
    }
}
