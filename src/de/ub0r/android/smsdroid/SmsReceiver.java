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

import de.ub0r.android.smsdroid.cache.Persons;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
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
@SuppressWarnings("deprecation")
public class SmsReceiver extends BroadcastReceiver {
	/** Tag for logging. */
	static final String TAG = "SMSdroid.bcr";
	/** URI to get messages from. */
	static final Uri URI = Uri.parse("content://sms/inbox");

	/** Sort the newest message first. */
	private static final String SORT = Calls.DATE + " DESC";

	/** Delay for spinlock, waiting for new messages. */
	private static final long SLEEP = 500;
	/** Number of maximal spins. */
	private static final int MAX_SPINS = 15;

	/** ID for new message notification. */
	private static final int NOTIFICATION_ID_NEW = 1;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final void onReceive(final Context context, final Intent intent) {
		Log.d(TAG, "got intent: " + intent.getAction());
		if (!PreferenceManager.getDefaultSharedPreferences(context).getBoolean(
				Preferences.PREFS_NOTIFICATION_ENABLE, true)) {
			Log.d(TAG, "no notification needed");
			return;
		}
		Bundle b = intent.getExtras();
		Object[] messages = (Object[]) b.get("pdus");
		SmsMessage[] smsMessage = new SmsMessage[messages.length];
		int l = messages.length;
		for (int i = 0; i < l; i++) {
			smsMessage[i] = SmsMessage.createFromPdu((byte[]) messages[i]);
		}
		String t = null;
		if (l > 0) {
			t = smsMessage[0].getDisplayMessageBody();
		}

		Log.d(TAG, "l: " + l);
		Log.d(TAG, "t: " + t);
		int count = MAX_SPINS;
		do {
			Log.d(TAG, "spin: " + count);
			try {
				Thread.sleep(SLEEP);
			} catch (InterruptedException e) {
				Log.d(TAG, "interrupted in spinlock", e);
				e.printStackTrace();
			}
			--count;
		} while (updateNewMessageNotification(context, t) <= 0 && count > 0);
		if (count == 0) { // use messages as they are available
			updateNewMessageNotification(context, null);
		}
	}

	/**
	 * Update new message {@link Notification}.
	 * 
	 * @param context
	 *            {@link Context}
	 * @param text
	 *            text of the last assumed unread message
	 * @return number of unread messages
	 */
	static final int updateNewMessageNotification(final Context context,
			final String text) {
		Log.d(TAG, "updNewMsgNoti(" + context + "," + text + ")");
		final NotificationManager mNotificationMgr = // .
		(NotificationManager) context
				.getSystemService(Context.NOTIFICATION_SERVICE);
		if (!PreferenceManager.getDefaultSharedPreferences(context).getBoolean(
				Preferences.PREFS_NOTIFICATION_ENABLE, true)) {
			mNotificationMgr.cancelAll();
			Log.d(TAG, "no notification needed, return -1");
			return -1; // FIXME: notification != widget
		}
		final Cursor cursor = context.getContentResolver().query(URI,
				Message.PROJECTION, Message.SELECTION_UNREAD, null, SORT);
		final int l = cursor.getCount();
		Log.d(TAG, "l: " + l);
		int ret = l;
		if (text != null || l == 0) {
			mNotificationMgr.cancel(NOTIFICATION_ID_NEW);
		}
		Uri uri = null;
		PendingIntent pIntent;
		if (l == 0) {
			final Intent i = new Intent(context, SMSdroid.class);
			// add pending intent
			i.setFlags(i.getFlags() | Intent.FLAG_ACTIVITY_NEW_TASK);
			pIntent = PendingIntent.getActivity(context, 0, i,
					PendingIntent.FLAG_CANCEL_CURRENT);
		} else {
			Notification n = null;
			cursor.moveToFirst();
			final String t = cursor.getString(Message.INDEX_BODY);
			Log.d(TAG, "t: " + t);
			if (text != null) {
				if (t.startsWith(text)) {
					ret = l;
				} else {
					Log.d(TAG, "return -1 (1)");
					return -1;
				}
			}
			if (l == 1) {
				final String a = cursor.getString(Message.INDEX_ADDRESS);
				Log.d(TAG, "p: " + a);
				String rr = Persons.getName(context, a, null);
				if (rr == null) {
					rr = a;
				}
				final String th = cursor.getString(Message.INDEX_THREADID);
				n = new Notification(R.drawable.stat_notify_sms, rr, System
						.currentTimeMillis());
				uri = Uri.parse(MessageList.URI + th);
				final Intent i = new Intent(Intent.ACTION_VIEW, uri, context,
						MessageList.class);
				// add pending intent
				i.setFlags(i.getFlags() | Intent.FLAG_ACTIVITY_NEW_TASK);
				pIntent = PendingIntent.getActivity(context, 0, i,
						PendingIntent.FLAG_CANCEL_CURRENT);
				n.setLatestEventInfo(context, rr, t, pIntent);
			} else {
				n = new Notification(R.drawable.stat_notify_sms, context
						.getString(R.string.new_messages_), System
						.currentTimeMillis());
				uri = Uri.parse(MessageList.URI);
				final Intent i = new Intent(Intent.ACTION_VIEW, uri, context,
						SMSdroid.class);
				// add pending intent
				i.setFlags(i.getFlags() | Intent.FLAG_ACTIVITY_NEW_TASK);
				pIntent = PendingIntent.getActivity(context, 0, i,
						PendingIntent.FLAG_CANCEL_CURRENT);
				n.setLatestEventInfo(context, context
						.getString(R.string.new_messages_), String.format(
						context.getString(R.string.new_messages), l), pIntent);
				n.number = l;
			}
			n.flags |= Notification.FLAG_SHOW_LIGHTS;
			n.ledARGB = Preferences.getLEDcolor(context);
			int[] ledFlash = Preferences.getLEDflash(context);
			n.ledOnMS = ledFlash[0];
			n.ledOffMS = ledFlash[1];
			final SharedPreferences p = PreferenceManager
					.getDefaultSharedPreferences(context);
			if (text != null) {
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
			Log.d(TAG, "uri: " + uri);
			mNotificationMgr.cancel(NOTIFICATION_ID_NEW);
			mNotificationMgr.notify(NOTIFICATION_ID_NEW, n);
		}
		Log.d(TAG, "return " + ret + " (2)");
		AppWidgetManager.getInstance(context).updateAppWidget(
				new ComponentName(context, WidgetProvider.class),
				WidgetProvider.getRemoteViews(context, l, pIntent));
		return ret;
	}
}
