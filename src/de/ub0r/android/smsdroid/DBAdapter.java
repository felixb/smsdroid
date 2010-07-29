package de.ub0r.android.smsdroid;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DBAdapter {
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

	public DBAdapter(final Context ctx) {
		this.context = ctx;
		this.DBHelper = new DatabaseHelper(this.context);
	}

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

	public DBAdapter open() throws SQLException {
		this.db = this.DBHelper.getWritableDatabase();
		return this;
	}

	public void close() {
		this.DBHelper.close();
	}

	// insert a number into the spam database
	public long insertNr(final String number) {
		ContentValues initialValues = new ContentValues();
		initialValues.put(KEY_NR, number);
		return this.db.insert(DATABASE_TABLE, null, initialValues);
	}

	public boolean isInDB(final String nr) {
		Cursor cursor = this.db.rawQuery("SELECT * from numbers WHERE nr = \""
				+ nr + "\"", null);
		return cursor.moveToFirst();
	}

	public int getAllEntries() {
		Cursor cursor = this.db.rawQuery("SELECT COUNT(nr) FROM numbers", null);
		Log.d(TAG, cursor.toString());
		if (cursor.moveToFirst()) {
			return cursor.getInt(0);
		}
		return cursor.getInt(0);
	}

}
