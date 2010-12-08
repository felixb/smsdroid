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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.IntentService;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.preference.PreferenceManager;
import de.ub0r.android.lib.DbUtils;
import de.ub0r.android.lib.Log;
import de.ub0r.android.lib.apis.ContactsWrapper;
import de.ub0r.android.smsdroid.MessageProvider.Messages;
import de.ub0r.android.smsdroid.MessageProvider.Threads;

/**
 * {@link IntentService} updating contacts information.
 * 
 * @author flx
 */
public final class SyncService extends IntentService {
	/** Tag for logging. */
	private static final String TAG = "sync";

	/** Wrapper to use for contacts API. */
	private static final ContactsWrapper WRAPPER = ContactsWrapper
			.getInstance();

	/** Pattern to clean up numbers. */
	private static final Pattern PATTERN_CLEAN_NUMBER = Pattern
			.compile("<(\\+?[0-9]+)>");

	/** Preference's name: last run. */
	private static final String PREFS_LASTRUN = "cs_lastrun";

	/** Minimal time to wait between runs. */
	private static final long MIN_WAIT_TIME = 60000L;

	/** Action: sync contacts. */
	private static final String ACTION_SYNC_CONTACTS = "de.ub0r.android"
			+ ".smsdroid.SYNC_CONTACTS";
	/** Action: sync messages. */
	private static final String ACTION_SYNC_MESSAGES = "de.ub0r.android"
			+ ".smsdroid.SYNC_MESSAGES";
	/** Action: sync threads. */
	private static final String ACTION_SYNC_THREADS = "de.ub0r.android"
			+ ".smsdroid.SYNC_THREADS";

	/** Queue for sync contacts. */
	private static int queueContacts = 0;
	/** Queue for sync messages. */
	private static int queueMessages = 0;
	/** Queue for sync threads. */
	private static int queueThreads = 0;

	/**
	 * Default Constructor.
	 */
	public SyncService() {
		super("ContactsService");
	}

	/**
	 * Start this {@link IntentService} in background to sync the contacts meta
	 * data to threads.
	 * 
	 * @param context
	 *            {@link Context}
	 */
	public static synchronized void syncContacts(final Context context) {
		final SharedPreferences p = PreferenceManager
				.getDefaultSharedPreferences(context);
		final long lastRun = p.getLong(PREFS_LASTRUN, 0L);
		if (lastRun + MIN_WAIT_TIME < System.currentTimeMillis()
				&& queueContacts <= 0) {
			Log.d(TAG, "call startService: " + ACTION_SYNC_CONTACTS);
			++queueContacts;
			context.startService(new Intent(ACTION_SYNC_CONTACTS, null,
					context, SyncService.class));
		} else {
			Log.i(TAG, "skip startService: " + ACTION_SYNC_CONTACTS + " / "
					+ queueContacts);
		}
	}

	/**
	 * Start this {@link IntentService} in background to sync messages to
	 * internal database.
	 * 
	 * @param context
	 *            {@link Context}
	 */
	public static synchronized void syncMessages(final Context context) {
		if (queueMessages <= 0) {
			Log.d(TAG, "call startService: " + ACTION_SYNC_MESSAGES);
			++queueMessages;
			context.startService(new Intent(ACTION_SYNC_MESSAGES, null,
					context, SyncService.class));
		} else {
			Log.i(TAG, "skip startService: " + ACTION_SYNC_MESSAGES + " / "
					+ queueMessages);
		}
	}

	/**
	 * Start this {@link IntentService} in background to sync threads to
	 * internal database.
	 * 
	 * @param context
	 *            {@link Context}
	 * @param threadId
	 *            thread to sync
	 */
	public static synchronized void syncThreads(final Context context,
			final long threadId) {
		if (queueThreads <= 0) {
			Log.d(TAG, "call startService: " + ACTION_SYNC_THREADS);
			Log.d(TAG, "threadId: " + threadId);
			final Intent i = new Intent(ACTION_SYNC_THREADS, null, context,
					SyncService.class);
			if (threadId >= 0L) {
				i.setData(ContentUris.withAppendedId(Threads.CACHE_URI,
						threadId));
			}
			++queueThreads;
			context.startService(i);
		} else {
			Log.i(TAG, "skip startService: " + ACTION_SYNC_THREADS + " / "
					+ queueThreads);
		}
	}

	@Override
	protected void onHandleIntent(final Intent intent) {
		Log.i(TAG, "start SyncService: " + intent);
		final String action = intent.getAction();
		final Uri uri = intent.getData();
		if (action == null) {
			return;
		} else if (action.equals(ACTION_SYNC_CONTACTS)) {
			this.syncContacts(intent);
			queueContacts = 0;
		} else if (action.equals(ACTION_SYNC_MESSAGES)) {
			this.syncMessages(intent);
			queueMessages = 0;
		} else if (action.equals(ACTION_SYNC_THREADS)) {
			if (uri == null) {
				this.syncThreads(intent);
			} else {
				final long threadId = ContentUris.parseId(uri);
				this.syncThread(threadId);
			}
			queueThreads = 0;
		} else {
			return;
		}

		// FIXME this.getContentResolver().notifyChange(Messages.CONTENT_URI,
		// null);
	}

	/**
	 * Sync contacts meta data to threads.
	 * 
	 * @param intent
	 *            {@link Intent} which started the service
	 */
	private void syncContacts(final Intent intent) {
		Log.d(TAG, "syncContacts(" + intent + ")");
		final SharedPreferences p = PreferenceManager
				.getDefaultSharedPreferences(this);
		final long lastRun = p.getLong(PREFS_LASTRUN, 0L);
		if (lastRun + MIN_WAIT_TIME > System.currentTimeMillis()) {
			Log.d(TAG, "skip syncContacts(" + intent + ")");
			return;
		}

		int changed = 0;
		final ContentResolver cr = this.getContentResolver();
		Cursor cursor = cr.query(Threads.CONTENT_URI, Threads.PROJECTION, null,
				null, null);
		if (cursor != null && cursor.moveToFirst()) {
			do {
				String address = cursor.getString(Threads.INDEX_ADDRESS);
				if (address == null) {
					continue;
				}
				address = address.trim();
				if (address.length() == 0) {
					continue;
				}
				final Cursor contact = getContact(cr, address);
				if (contact == null || !contact.moveToFirst()) {
					continue;
				}
				final long tid = cursor.getLong(Threads.INDEX_ID);
				final String name = contact
						.getString(ContactsWrapper.FILTER_INDEX_NAME);
				final String pid = contact
						.getString(ContactsWrapper.FILTER_INDEX_ID);
				final ContentValues values = new ContentValues(2);
				if (name != null
						&& !name.equals(cursor.getString(Threads.INDEX_NAME))) {
					values.put(Threads.NAME, name);
				}
				if (pid != null
						&& !pid.equals(cursor.getString(Threads.INDEX_PID))) {
					values.put(Threads.PID, pid);
				}
				if (values.size() > 0) {
					changed += cr.update(ContentUris.withAppendedId(
							Threads.CONTENT_URI, tid), values, null, null);
				}
				if (!contact.isClosed()) {
					contact.close();
				}
			} while (cursor.moveToNext());
		}
		if (cursor != null && !cursor.isClosed()) {
			cursor.close();
		}
		p.edit().putLong(PREFS_LASTRUN, System.currentTimeMillis()).commit();
	}

	/**
	 * Sync messages to internal database.
	 * 
	 * @param intent
	 *            {@link Intent} which started the service
	 */
	private void syncMessages(final Intent intent) {
		Log.d(TAG, "syncMessages(" + intent + ")");

		boolean changed = false;
		final ContentResolver cr = this.getContentResolver();

		// get last internal message
		final Cursor lcursor = cr.query(Messages.CACHE_URI,
				Messages.PROJECTION, null, null, Messages.DATE + " DESC");
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
		changed |= this.getNewSMS(cr, lmaxdate);
		changed |= this.getNewMMS(cr, lmaxdate);

		if (changed) {
			this.syncThreads(intent);
			// FIXME: cr.notifyChange(Messages.CONTENT_URI, null);
		}
	}

	/**
	 * Sync messages to internal database.
	 * 
	 * @param intent
	 *            {@link Intent} which started the service
	 */
	private void syncThreads(final Intent intent) {
		Log.d(TAG, "syncThreads(" + intent + ")");

		final String[] mproj = new String[] { Messages.THREADID };
		final String[] cproj = new String[] { Threads.ID };
		final ContentResolver cr = this.getContentResolver();
		final Cursor mcursor = cr.query(Messages.CACHE_THREADS_URI, mproj,
				null, null, Messages.THREADID + " ASC");
		final Cursor ccursor = cr.query(Threads.CACHE_URI, cproj, null, null,
				Threads.ID + " ASC");
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
							cr.delete(Threads.CACHE_URI, Threads.ID + " = "
									+ ctid, null);
						} else {
							break;
						}
					} while (ccursor.moveToNext());
				}
				this.syncThread(mtid);
				if (ctid == mtid) {
					ccursor.moveToNext();
				}
			} while (mcursor.moveToNext());
			Log.d(TAG, "delete: tid>" + mtid);
			cr.delete(Threads.CACHE_URI, Threads.ID + " > " + mtid, null);
		}
		if (mcursor != null && !mcursor.isClosed()) {
			mcursor.close();
		}
		if (ccursor != null && !ccursor.isClosed()) {
			ccursor.close();
		}

		syncContacts(this);
	}

	/**
	 * Update Threads table from Messages.
	 * 
	 * @param threadId
	 *            thread's ID, -1 for all
	 */
	private void syncThread(final long threadId) {
		Log.d(TAG, "syncThread(" + threadId + ")");
		final ContentResolver cr = this.getContentResolver();
		final String[] proj = new String[] {// .
		Messages.DATE, // 0
				Messages.BODY, // 1
				Messages.ADDRESS, // 2
				Messages.TYPE, // 3
		};

		Cursor cursor = cr.query(Messages.CACHE_URI, proj, Messages.THREADID
				+ " = " + threadId, null, Messages.DATE + " DESC");
		final ContentValues values = new ContentValues();
		if (cursor != null && cursor.moveToFirst()) {
			values.put(Threads.DATE, cursor.getLong(0));
			values.put(Threads.BODY, cursor.getString(1));
			values.put(Threads.ADDRESS, cursor.getString(2));
			values.put(Threads.TYPE, cursor.getInt(3));
			values.put(Threads.COUNT, cursor.getCount());
		}
		if (cursor != null && !cursor.isClosed()) {
			cursor.close();
		}
		cursor = cr.query(Messages.CACHE_URI, proj, Messages.THREADID + " = "
				+ threadId + " AND " + Messages.READ + " = 0", null, null);
		if (cursor != null && cursor.getCount() > 0) {
			values.put(Threads.READ, 0);
		} else if (values.size() > 0) {
			values.put(Threads.READ, 1);
		}
		if (cursor != null && !cursor.isClosed()) {
			cursor.close();
		}
		if (values.size() > 0) {
			Log.d(TAG, "update thread: " + threadId + "/ " + values);
			int ret = cr.update(Threads.CACHE_URI, values, Threads.ID + " = "
					+ threadId, null);
			if (ret <= 0) {
				Log.d(TAG, "insert thread: " + threadId);
				values.put(Threads.ID, threadId);
				cr.insert(Threads.CACHE_URI, values);
			}
		}
		Log.d(TAG, "exit syncThread(" + threadId + ")");
	}

	/**
	 * Get (id, name) for address.
	 * 
	 * @param cr
	 *            {@link ContentResolver}
	 * @param address
	 *            address
	 * @return {@link Cursor}
	 */
	private static synchronized Cursor getContact(final ContentResolver cr,
			final String address) {
		Log.d(TAG, "getContact(ctx, " + address + ")");
		if (address == null) {
			return null;
		}
		// clean up number
		String realAddress = address;
		final Matcher m = PATTERN_CLEAN_NUMBER.matcher(realAddress);
		if (m.find()) {
			realAddress = m.group(1);
			Log.d(TAG, "real address: " + realAddress);
		}
		// address contains the phone number
		try {
			final Cursor cursor = WRAPPER.getContact(cr, realAddress);
			if (cursor != null && cursor.moveToFirst()) {
				return cursor;
			}
		} catch (Exception e) {
			Log.e(TAG, "failed to fetch contact", e);
		}
		Log.d(TAG, "nothing found!");
		return null;
	}

	/**
	 * Add a SMS to internal database.
	 * 
	 * @param cr
	 *            {@link ContentResolver}
	 * @param rcursor
	 *            {@link Cursor}
	 * @return added?
	 */
	private boolean addSMS(final ContentResolver cr, final Cursor rcursor) {
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
		final int i = cr.update(Messages.CACHE_URI, values, Messages.ID + " = "
				+ mid, null);
		if (i > 0) {
			Log.d(TAG, "update sms: " + mid + "/" + tid + " " + values);
			ret = true;
		} else {
			Log.d(TAG, "add sms: " + mid + "/" + tid + " " + values);
			final Uri u = cr.insert(Messages.CACHE_URI, values);
			if (u != null) {
				ret = true;
			}
		}
		return ret;
	}

	/**
	 * Add a MMS to internal database.
	 * 
	 * @param cr
	 *            {@link ContentResolver}
	 * @param rcursor
	 *            {@link Cursor}
	 * @return added?
	 */
	private boolean addMMS(final ContentResolver cr, final Cursor rcursor) {
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
		values.put(Messages.ID, -1L * mid);
		values.put(Messages.DATE, date);
		values.put(Messages.THREADID, tid);
		values.put(Messages.TYPE, rcursor.getInt(iType));
		values.put(Messages.READ, rcursor.getInt(iRead));
		if (iText >= 0) {
			final String text = rcursor.getString(iText);
			values.put(Messages.BODY, text);
		}
		final int i = cr.update(Messages.CACHE_URI, values, Messages.ID + " = "
				+ -1L * mid, null);
		if (i > 0) {
			Log.d(TAG, "update mms: " + mid + "/" + tid + " " + values);
			ret = true;
		} else {
			Log.d(TAG, "add mms: " + mid + "/" + tid + " " + values);
			final Uri u = cr.insert(Messages.CACHE_URI, values);
			if (u != null) {
				ret = true;
			}
		}
		return ret;
	}

	/**
	 * Fix MMS date.
	 * 
	 * @param date
	 *            date
	 * @return date as milliseconds since epoch
	 */
	private static long getDate(final long date) {
		if (date > ConversationList.MIN_DATE) {
			return date;
		}
		return date * ConversationList.MILLIS;
	}

	/**
	 * Get new SMS.
	 * 
	 * @param cr
	 *            {@link ContentResolver}
	 * @param date
	 *            date of newest internal message
	 * @return database changed?
	 */
	private boolean getNewSMS(final ContentResolver cr, final long date) {
		boolean ret = false;
		// get new sms
		Cursor rcursor = cr.query(Messages.ORIG_URI_SMS,
				Messages.ORIG_PROJECTION_SMS, Messages.DATE + " > " + date,
				null, null);
		if (rcursor != null && rcursor.moveToFirst()) {
			do {
				ret |= this.addSMS(cr, rcursor);
			} while (rcursor.moveToNext());
		}
		if (rcursor != null && !rcursor.isClosed()) {
			rcursor.close();
		}

		// check message count and check all messages if not equal
		final String sortOrder = " DESC, " + Messages.ID + " DESC";
		rcursor = cr.query(Messages.ORIG_URI_SMS, Messages.ORIG_PROJECTION_SMS,
				null, null, Messages.DATE + sortOrder);
		Cursor lcursor = cr.query(Messages.CACHE_URI, Messages.PROJECTION,
				Messages.WHERE_TYPE_SMS, null, Messages.DATE + sortOrder);
		if (rcursor == null || lcursor == null) {
			return false;
		}
		if (!rcursor.moveToFirst()) {
			// no remote message: delete all
			int r = cr
					.delete(Messages.CACHE_URI, Messages.WHERE_TYPE_SMS, null);
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
						ret |= this.addSMS(cr, rcursor);
						break;
					} else if (ldate > rdate) {
						// delete local sms and check next local
						cr.delete(Messages.CACHE_URI, Messages.DATE + " = "
								+ ldate, null);
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
		ContentValues values = new ContentValues();
		values.put(Messages.READ, 1);
		rcursor = cr.query(Messages.ORIG_URI_SMS, Messages.ORIG_PROJECTION_SMS,
				Messages.READ + " = 0", null, null);
		int i;
		if (rcursor == null || !rcursor.moveToFirst()) {
			i = cr.update(Messages.CACHE_URI, values, Messages.READ + " = 0",
					null);
		} else {
			StringBuffer sb = new StringBuffer();
			do {
				final long l = rcursor.getLong(Messages.INDEX_ID);
				if (sb.length() > 0) {
					sb.append(" OR ");
				}
				sb.append(Messages.ID + " = " + l);
			} while (rcursor.moveToNext());
			final String w0 = sb.toString();
			final String w1 = w0.replaceAll(" OR ", " AND ").replaceAll(" = ",
					" != ");
			sb = null;
			i = cr.update(Messages.CACHE_URI, values, DbUtils.sqlAnd(
					Messages.READ + " = 0", w1), null);
			values.put(Messages.READ, 0);
			i += cr.update(Messages.CACHE_URI, values, DbUtils.sqlAnd(
					Messages.READ + " = 1", w0), null);

		}
		if (rcursor != null && !rcursor.isClosed()) {
			rcursor.close();
		}
		if (i > 0) {
			ret = true;
		}

		// check draft messages
		// set all internal drafts as sent
		values = new ContentValues();
		values.put(Messages.TYPE, Messages.TYPE_SMS_OUT);
		rcursor = cr.query(Messages.ORIG_URI_SMS, Messages.ORIG_PROJECTION_SMS,
				Messages.TYPE + " = " + Messages.TYPE_SMS_DRAFT, null, null);
		if (rcursor == null || !rcursor.moveToFirst()) {
			i = cr.update(Messages.CACHE_URI, values, Messages.TYPE + " = "
					+ Messages.TYPE_SMS_DRAFT, null);
		} else {
			StringBuffer sb = new StringBuffer();
			do {
				final long l = rcursor.getLong(Messages.INDEX_ID);
				if (sb.length() > 0) {
					sb.append(" AND ");
				}
				sb.append(Messages.ID + " != " + l);
			} while (rcursor.moveToNext());
			final String w1 = sb.toString();
			sb = null;
			i = cr.update(Messages.CACHE_URI, values, DbUtils.sqlAnd(
					Messages.TYPE + " = " + Messages.TYPE_SMS_DRAFT, w1), null);
		}
		if (rcursor != null && !rcursor.isClosed()) {
			rcursor.close();
		}
		if (i > 0) {
			ret = true;
		}
		return ret;
	}

	/**
	 * Get new MMS.
	 * 
	 * @param cr
	 *            {@link ContentResolver}
	 * @param date
	 *            date of newest internal message
	 * @return database changed?
	 */
	private boolean getNewMMS(final ContentResolver cr, final long date) {
		boolean ret = false;
		// get new mms
		Cursor rcursor = cr.query(Messages.ORIG_URI_MMS, null, Messages.DATE
				+ " > " + (date / ConversationList.MILLIS), null, null);
		if (rcursor != null && rcursor.moveToFirst()) {
			do {
				// FIXME ret |= this.addMMS(cr, rcursor);
			} while (rcursor.moveToNext());
		}
		if (rcursor != null && !rcursor.isClosed()) {
			rcursor.close();
		}

		// check message count and check all messages if not equal
		final String sortOrder = " DESC, " + Messages.ID + " DESC";
		rcursor = cr.query(Messages.ORIG_URI_MMS, null, null, null,
				Messages.DATE + sortOrder);
		Cursor lcursor = cr.query(Messages.CACHE_URI, Messages.PROJECTION,
				Messages.WHERE_TYPE_MMS, null, Messages.DATE + sortOrder);
		if (rcursor == null || lcursor == null) {
			return false;
		}
		if (!rcursor.moveToFirst()) {
			// no remote message: delete all
			final int r = cr.delete(Messages.CACHE_URI,
					Messages.WHERE_TYPE_MMS, null);
			if (r > 0) {
				ret = true;
				lcursor.requery();
			}
		}
		final int iMId = rcursor.getColumnIndex(Messages.ID);
		// final int iThreadId = rcursor.getColumnIndex(Messages.THREADID);
		final int iDate = rcursor.getColumnIndex(Messages.DATE);
		// final int iType = rcursor.getColumnIndex(Messages.TYPE);
		// final int iRead = rcursor.getColumnIndex(Messages.READ);
		// final int iText = rcursor.getColumnIndex("text");
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
						// FIXME ret |= this.addMMS(cr, rcursor);
						break;
					} else if (ldate > rdate) {
						// delete local mms and check next local
						cr.delete(Messages.CACHE_URI, Messages.DATE + " = "
								+ ldate, null);
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
		int i = cr.update(Messages.CACHE_URI, values, Messages.READ + " = 0"
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
				cr.update(Messages.CACHE_URI, values, Messages.ID + " = "
						+ (-1L * rcursor.getLong(iMId)), null);
			} while (rcursor.moveToNext());
		}
		if (rcursor != null && !rcursor.isClosed()) {
			rcursor.close();
		}

		return ret;
	}
}
