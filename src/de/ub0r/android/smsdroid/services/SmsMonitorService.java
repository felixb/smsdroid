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
package de.ub0r.android.smsdroid.services;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.provider.CallLog.Calls;
import android.util.Log;
import de.ub0r.android.smsdroid.CachePersons;
import de.ub0r.android.smsdroid.MessageList;
import de.ub0r.android.smsdroid.MessageListAdapter;
import de.ub0r.android.smsdroid.Preferences;
import de.ub0r.android.smsdroid.R;
import de.ub0r.android.smsdroid.SMSdroid;

/**
 * Monitor for changes on {@link ContentResolver}.
 * 
 * @author <author of sms-popup>, flx
 */
public class SmsMonitorService extends Service {
	/** Tag for output. */
	private static final String TAG = "SMSdroid.sms";

	/** Sort the newest message first. */
	private static final String SORT = Calls.DATE + " DESC";

	/** Delay for spinlock, waiting for new messages. */
	private static final long SLEEP = 500;
	/** Number of maximal spins. */
	private static final int MAX_SPINS = 15;

	/** ID for new message notification. */
	private static final int NOTIFICATION_ID_NEW = 1;

	/** {@link Uri} to observe. */
	static final Uri URI = Uri.parse("content://sms/");

	private ContentResolver crSMS;
	private SmsContentObserver observerSMS = null;
	private Context context;

	@Override
	public void onCreate() {
		super.onCreate();
		this.context = this.getApplicationContext();
		Log.d(TAG, "SmsMonitorService created");
		this.registerSMSObserver();
	}

	@Override
	public void onDestroy() {
		this.unregisterSMSObserver();
		super.onDestroy();
	}

	@Override
	public IBinder onBind(final Intent intent) {
		return null;
	}

	/*
	 * Registers the observer for SMS changes
	 */
	private void registerSMSObserver() {
		if (this.observerSMS == null) {
			this.observerSMS = new SmsContentObserver(new Handler());
			this.crSMS = this.getContentResolver();
			this.crSMS.registerContentObserver(URI, true, this.observerSMS);
			Log.d(TAG, "SMS Observer registered.");
		}
	}

	/**
	 * Unregisters the observer for call log changes.
	 */
	private void unregisterSMSObserver() {
		if (this.crSMS != null) {
			this.crSMS.unregisterContentObserver(this.observerSMS);
		}
		if (this.observerSMS != null) {
			this.observerSMS = null;
		}
		Log.d(TAG, "Unregistered SMS Observer");
	}

	private class SmsContentObserver extends ContentObserver {
		public SmsContentObserver(final Handler handler) {
			super(handler);
		}

		@Override
		public void onChange(final boolean selfChange) {
			super.onChange(selfChange);
			updateNewMessageNotification(SmsMonitorService.this.context);
		}
	}

	/**
	 * Update new message {@link Notification}.
	 * 
	 * @param context
	 *            {@link Context}
	 * @return number of unread messages
	 */
	static final int updateNewMessageNotification(final Context context) {
		Log.d(TAG, "updNewMsgNoti()");
		final NotificationManager mNotificationMgr = // .
		(NotificationManager) context
				.getSystemService(Context.NOTIFICATION_SERVICE);
		if (!PreferenceManager.getDefaultSharedPreferences(context).getBoolean(
				Preferences.PREFS_NOTIFICATION_ENABLE, true)) {
			mNotificationMgr.cancelAll();
			Log.d(TAG, "no notification needed, return -1");
			return -1;
		}
		final Cursor cursor = context.getContentResolver().query(URI,
				MessageListAdapter.PROJECTION,
				MessageListAdapter.SELECTION_UNREAD, null, SORT);
		final int l = cursor.getCount();
		Log.d(TAG, "l: " + l);
		int ret = l;
		Uri uri = null;
		if (l == 0) {
			mNotificationMgr.cancel(NOTIFICATION_ID_NEW);
		} else {
			Notification n = null;
			cursor.moveToFirst();
			final String t = cursor.getString(MessageListAdapter.INDEX_BODY);
			Log.d(TAG, "t: " + t);
			if (l == 1) {
				final String a = cursor
						.getString(MessageListAdapter.INDEX_ADDRESS);
				Log.d(TAG, "p: " + a);
				String rr = CachePersons.getName(context, a, null);
				if (rr == null) {
					rr = a;
				}
				final String th = cursor
						.getString(MessageListAdapter.INDEX_THREADID);
				n = new Notification(R.drawable.stat_notify_sms, rr, System
						.currentTimeMillis());
				uri = Uri.parse(MessageList.URI + th);
				final Intent i = new Intent(Intent.ACTION_VIEW, uri, context,
						MessageList.class);
				// add pending intent
				// i.setFlags(i.getFlags() | Intent.FLAG_ACTIVITY_NEW_TASK);
				final PendingIntent cIntent = PendingIntent.getActivity(
						context, 0, i, PendingIntent.FLAG_CANCEL_CURRENT);
				n.setLatestEventInfo(context, rr, t, cIntent);
			} else {
				n = new Notification(R.drawable.stat_notify_sms, context
						.getString(R.string.new_messages_), System
						.currentTimeMillis());
				uri = Uri.parse(MessageList.URI);
				final Intent i = new Intent(Intent.ACTION_VIEW, uri, context,
						SMSdroid.class);
				// add pending intent
				// i.setFlags(i.getFlags() | Intent.FLAG_ACTIVITY_NEW_TASK);

				final PendingIntent cIntent = PendingIntent.getActivity(
						context, 0, i, PendingIntent.FLAG_CANCEL_CURRENT);
				n.setLatestEventInfo(context, context
						.getString(R.string.new_messages_), String.format(
						context.getString(R.string.new_messages), l), cIntent);
				n.number = l;
			}
			n.flags |= Notification.FLAG_SHOW_LIGHTS;
			n.ledARGB = Preferences.getLEDcolor(context);
			int[] ledFlash = Preferences.getLEDflash(context);
			n.ledOnMS = ledFlash[0];
			n.ledOffMS = ledFlash[1];
			final SharedPreferences p = PreferenceManager
					.getDefaultSharedPreferences(context);
			if (SmsReceiver.isDirty()) {
				final boolean vibrate = p.getBoolean(Preferences.PREFS_VIBRATE,
						false);
				final String s = p.getString(Preferences.PREFS_SOUND, null);
				Uri sound;
				if (s == null || s.length() <= 0) {
					sound = null;
				} else {
					sound = Uri.parse(s);
				}
				if (vibrate) {
					final long[] pattern = Preferences
							.getVibratorPattern(context);
					if (pattern.length == 1 && pattern[0] == 0) {
						n.defaults |= Notification.DEFAULT_VIBRATE;
					} else {
						n.vibrate = pattern;
					}
				}
				n.sound = sound;
			}
			mNotificationMgr.notify(NOTIFICATION_ID_NEW, n);
		}
		Log.d(TAG, "return " + ret + " (2)");
		AppWidgetManager.getInstance(context).updateAppWidget(
				new ComponentName(context, WidgetProvider.class),
				WidgetProvider.getRemoteViews(context, l, uri));
		return ret;
	}
}
