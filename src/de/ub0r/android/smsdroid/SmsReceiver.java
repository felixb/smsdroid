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

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.CallLog.Calls;
import android.telephony.gsm.SmsMessage;
import android.util.Log;

/**
 * Listen for new sms.
 * 
 * @author flx
 */
public class SmsReceiver extends BroadcastReceiver {
	/** Tag for logging. */
	static final String TAG = "SMSdroid.bcr";
	/** URI to get messages from. */
	static final Uri URI = Uri.parse("content://sms/inbox");

	/** Sort the newest message first. */
	private static final String SORT = Calls.DATE + " DESC";

	/** Delay for spinlock, waiting for new messages. */
	private static final long SLEEP = 200;
	/** Number of maximal spins. */
	private static final int MAX_SPINS = 40;

	/** ID for new message notification. */
	private static final int NOTIFICATION_ID_NEW = 1;
	/** LED color for notification. */
	private static final int NOTIFICATION_LED_COLOR = 0xff00ff00;
	/** LED blink on (ms) for notification. */
	private static final int NOTIFICATION_LED_ON = 500;
	/** LED blink off (ms) for notification. */
	private static final int NOTIFICATION_LED_OFF = 2000;

	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings("deprecation")
	@Override
	public final void onReceive(final Context context, final Intent intent) {
		Log.d(TAG, "got intent: " + intent.getAction());
		if (!PreferenceManager.getDefaultSharedPreferences(context).getBoolean(
				Preferences.PREFS_NOTIFICATION_ENABLE, true)) {
			return;
		}
		Bundle b = intent.getExtras();
		Object[] messages = (Object[]) b.get("pdus");
		SmsMessage[] smsMessage = new SmsMessage[messages.length];
		int l = messages.length;
		for (int i = 0; i < l; i++) {
			smsMessage[i] = SmsMessage.createFromPdu((byte[]) messages[i]);
		}
		long t = -1;
		if (l > 0) {
			t = smsMessage[0].getTimestampMillis();
		}

		int count = MAX_SPINS;
		do {
			try {
				Thread.sleep(SLEEP);
			} catch (InterruptedException e) {
				Log.d(TAG, "interrupted in spinlock", e);
				e.printStackTrace();
			}
			--count;
		} while (updateNewMessageNotification(context, t) <= 0 && count > 0);
	}

	/**
	 * Update new message {@link Notification}.
	 * 
	 * @param context
	 *            {@link Context}
	 * @param time
	 *            timestamp of the last assumed unread message
	 * @return number of unread messages
	 */
	static final int updateNewMessageNotification(final Context context,
			final long time) {
		final NotificationManager mNotificationMgr = // .
		(NotificationManager) context
				.getSystemService(Context.NOTIFICATION_SERVICE);
		if (!PreferenceManager.getDefaultSharedPreferences(context).getBoolean(
				Preferences.PREFS_NOTIFICATION_ENABLE, true)) {
			mNotificationMgr.cancelAll();
			return -1;
		}
		final Cursor cursor = context.getContentResolver().query(URI,
				MessageListAdapter.PROJECTION,
				MessageListAdapter.SELECTION_UNREAD, null, SORT);
		final int l = cursor.getCount();
		int ret = l;
		if (time > 0 || l == 0) {
			mNotificationMgr.cancel(NOTIFICATION_ID_NEW);
		}
		if (l > 0) {
			Notification n = null;
			cursor.moveToFirst();
			final long d = cursor.getLong(MessageListAdapter.INDEX_DATE);
			if (time > 0) {
				if (time <= d) {
					ret = l;
				} else {
					return -1;
				}
			}
			if (l == 1) {
				final String r = cursor
						.getString(MessageListAdapter.INDEX_ADDRESS);
				String rr = CachePersons.getName(context, r, null);
				if (rr == null) {
					rr = r;
				}
				final String t = cursor
						.getString(MessageListAdapter.INDEX_BODY);
				final String th = cursor
						.getString(MessageListAdapter.INDEX_THREADID);
				n = new Notification(R.drawable.stat_notify_sms, rr, System
						.currentTimeMillis());
				final Intent i = new Intent(Intent.ACTION_VIEW, Uri
						.parse(MessageList.URI + th), context,
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
				final Intent i = new Intent(Intent.ACTION_VIEW, Uri
						.parse(MessageList.URI), context, SMSdroid.class);
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
			n.ledARGB = NOTIFICATION_LED_COLOR;
			n.ledOnMS = NOTIFICATION_LED_ON;
			n.ledOffMS = NOTIFICATION_LED_OFF;
			final SharedPreferences p = PreferenceManager
					.getDefaultSharedPreferences(context);
			if (time > 0) {
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
					n.defaults |= Notification.DEFAULT_VIBRATE;
				}
				n.sound = sound;
			}
			mNotificationMgr.notify(NOTIFICATION_ID_NEW, n);
		}
		return ret;
	}
}
