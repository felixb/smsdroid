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
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.CallLog.Calls;
import android.telephony.gsm.SmsMessage;
import de.ub0r.android.lib.Log;

/**
 * Listen for new sms.
 * 
 * @author flx
 */
@SuppressWarnings("deprecation")
public class SmsReceiver extends BroadcastReceiver {
	/** Tag for logging. */
	static final String TAG = "bcr";
	/** {@link Uri} to get messages from. */
	private static final Uri URI_SMS = Uri.parse("content://sms/");
	/** {@link Uri} to get messages from. */
	private static final Uri URI_MMS = Uri.parse("content://mms/");

	/** Intent.action for receiving SMS. */
	private static final String ACTION_SMS = // .
	"android.provider.Telephony.SMS_RECEIVED";
	/** Intent.action for receiving MMS. */
	private static final String ACTION_MMS = // .
	"android.provider.Telephony.WAP_PUSH_RECEIVED";

	/** An unreadable MMS body. */
	private static final String MMS_BODY = "<MMS>";

	/** Index: thread id. */
	private static final int ID_TID = 0;
	/** Index: count. */
	private static final int ID_COUNT = 1;

	/** Sort the newest message first. */
	private static final String SORT = Calls.DATE + " DESC";

	/** Delay for spinlock, waiting for new messages. */
	private static final long SLEEP = 500;
	/** Number of maximal spins. */
	private static final int MAX_SPINS = 15;

	/** ID for new message notification. */
	private static final int NOTIFICATION_ID_NEW = 1;

	/** Last unread message's date. */
	private static long lastUnreadDate = 0L;
	/** Last unread message's body. */
	private static String lastUnreadBody = null;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final void onReceive(final Context context, final Intent intent) {
		final String action = intent.getAction();
		Log.d(TAG, "got intent: " + action);
		try {
			Log.d(TAG, "sleep(" + SLEEP + ")");
			Thread.sleep(SLEEP);
		} catch (InterruptedException e) {
			Log.d(TAG, "interrupted in spinlock", e);
			e.printStackTrace();
		}
		String t = null;
		if (action.equals(ACTION_SMS)) {
			Bundle b = intent.getExtras();
			Object[] messages = (Object[]) b.get("pdus");
			SmsMessage[] smsMessage = new SmsMessage[messages.length];
			int l = messages.length;
			for (int i = 0; i < l; i++) {
				smsMessage[i] = SmsMessage.createFromPdu((byte[]) messages[i]);
			}
			t = null;
			if (l > 0) {
				t = smsMessage[0].getDisplayMessageBody();
				// ! Check in blacklist db - filter spam
				boolean q = false;
				final String s = smsMessage[0].getOriginatingAddress();
				final SpamDB db = new SpamDB(context);
				db.open();
				if (db.isInDB(smsMessage[0].getOriginatingAddress())) {
					Log.d(TAG, "Message from " + s + " filtered.");
					q = true;
				} else {
					Log.d(TAG, "Message from " + s + " NOT filtered.");
				}
				// db.getEntrieCount();
				db.close();
				if (q) {
					return;
				}
			}
		} else if (action.equals(ACTION_MMS)) {
			t = MMS_BODY;
		}

		Log.d(TAG, "t: " + t);
		int count = MAX_SPINS;
		do {
			Log.d(TAG, "spin: " + count);
			try {
				Log.d(TAG, "sleep(" + SLEEP + ")");
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
	 * Get unread SMS.
	 * 
	 * @param cr
	 *            {@link ContentResolver} to query
	 * @param text
	 *            text of the last assumed unread message
	 * @return [thread id (-1 if there are more), number of unread messages (-1
	 *         if text does not match newest message)]
	 */
	private static int[] getUnreadSMS(final ContentResolver cr,
			final String text) {
		Log.d(TAG, "getUnreadSMS(cr, " + text + ")");
		Cursor cursor = cr.query(URI_SMS, Message.PROJECTION,
				Message.SELECTION_READ_UNREAD, Message.SELECTION_UNREAD, SORT);
		if (cursor == null || cursor.getCount() == 0 || !cursor.moveToFirst()) {
			if (text != null) { // try again!
				if (cursor != null && !cursor.isClosed()) {
					cursor.close();
				}
				return new int[] { -1, -1 };
			} else {
				if (cursor != null && !cursor.isClosed()) {
					cursor.close();
				}
				return new int[] { 0, 0 };
			}
		}
		final String t = cursor.getString(Message.INDEX_BODY);
		if (text != null && (t == null || !t.startsWith(text))) {
			if (cursor != null && !cursor.isClosed()) {
				cursor.close();
			}
			return new int[] { -1, -1 }; // try again!
		}
		final long d = cursor.getLong(Message.INDEX_DATE);
		if (d > lastUnreadDate) {
			lastUnreadDate = d;
			lastUnreadBody = t;
		}
		int tid = cursor.getInt(Message.INDEX_THREADID);
		while (cursor.moveToNext() && tid > -1) {
			// check if following messages are from the same thread
			if (tid != cursor.getInt(Message.INDEX_THREADID)) {
				tid = -1;
			}
		}
		if (cursor != null && !cursor.isClosed()) {
			cursor.close();
		}
		return new int[] { tid, cursor.getCount() };
	}

	/**
	 * Get unread MMS.
	 * 
	 * @param cr
	 *            {@link ContentResolver} to query
	 * @param text
	 *            text of the last assumed unread message
	 * @return [thread id (-1 if there are more), number of unread messages]
	 */
	private static int[] getUnreadMMS(final ContentResolver cr,
			final String text) {
		Log.d(TAG, "getUnreadMMS(cr, " + text + ")");
		Cursor cursor = cr.query(URI_MMS, Message.PROJECTION_READ,
				Message.SELECTION_READ_UNREAD, Message.SELECTION_UNREAD, null);
		if (cursor == null || cursor.getCount() == 0 || !cursor.moveToFirst()) {
			if (text == MMS_BODY) {
				if (cursor != null && !cursor.isClosed()) {
					cursor.close();
				}
				return new int[] { -1, -1 }; // try again!
			} else {
				if (cursor != null && !cursor.isClosed()) {
					cursor.close();
				}
				return new int[] { 0, 0 };
			}
		}
		int tid = cursor.getInt(Message.INDEX_THREADID);
		long d = cursor.getLong(Message.INDEX_DATE);
		if (d < ConversationList.MIN_DATE) {
			d *= ConversationList.MILLIS;
		}
		if (d > lastUnreadDate) {
			lastUnreadDate = d;
			lastUnreadBody = null;
		}
		while (cursor.moveToNext() && tid > -1) {
			// check if following messages are from the same thread
			if (tid != cursor.getInt(Message.INDEX_THREADID)) {
				tid = -1;
			}
		}
		if (cursor != null && !cursor.isClosed()) {
			cursor.close();
		}
		return new int[] { tid, cursor.getCount() };
	}

	/**
	 * Get unread messages (MMS and SMS).
	 * 
	 * @param cr
	 *            {@link ContentResolver} to query
	 * @param text
	 *            text of the last assumed unread message
	 * @return [thread id (-1 if there are more), number of unread messages (-1
	 *         if text does not match newest message)]
	 */
	private static int[] getUnread(final ContentResolver cr, // .
			final String text) {
		Log.d(TAG, "getUnread(cr, " + text + ")");
		lastUnreadBody = null;
		lastUnreadDate = 0L;
		String t = text;
		if (t == MMS_BODY) {
			t = null;
		}
		final int[] retSMS = getUnreadSMS(cr, t);
		if (retSMS[ID_COUNT] == -1) {
			// return to retry
			return new int[] { -1, -1 };
		}
		final int[] retMMS = getUnreadMMS(cr, text);
		if (retMMS[ID_COUNT] == -1) {
			// return to retry
			return new int[] { -1, -1 };
		}
		final int[] ret = new int[] { -1, retSMS[ID_COUNT] + retMMS[ID_COUNT] };
		if (retMMS[ID_TID] <= 0 || retSMS[ID_TID] == retMMS[ID_TID]) {
			ret[ID_TID] = retSMS[ID_TID];
		} else if (retSMS[ID_TID] <= 0) {
			ret[ID_TID] = retMMS[ID_TID];
		}
		return ret;
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
		final SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(context);
		final boolean enableNotifications = prefs.getBoolean(
				Preferences.PREFS_NOTIFICATION_ENABLE, true);
		final boolean privateNotification = prefs.getBoolean(
				Preferences.PREFS_NOTIFICATION_PRIVACY, false);
		if (!enableNotifications) {
			mNotificationMgr.cancelAll();
			Log.d(TAG, "no notification needed!");
		}
		final int[] status = getUnread(context.getContentResolver(), text);
		final int l = status[ID_COUNT];
		final int tid = status[ID_TID];

		Log.d(TAG, "l: " + l);
		if (l < 0) {
			return l;
		}
		int ret = l;
		if (enableNotifications && (text != null || l == 0)) {
			mNotificationMgr.cancel(NOTIFICATION_ID_NEW);
		}
		Uri uri = null;
		PendingIntent pIntent;
		if (l == 0) {
			final Intent i = new Intent(context, ConversationList.class);
			// add pending intent
			i.setFlags(i.getFlags() | Intent.FLAG_ACTIVITY_CLEAR_TOP);
			pIntent = PendingIntent.getActivity(context, 0, i,
					PendingIntent.FLAG_CANCEL_CURRENT);
		} else {
			Notification n = null;
			Intent i;
			if (tid >= 0) {
				uri = Uri.parse(MessageList.URI + tid);
				i = new Intent(Intent.ACTION_VIEW, uri, context,
						MessageList.class);
				pIntent = PendingIntent.getActivity(context, 0, i,
						PendingIntent.FLAG_CANCEL_CURRENT);

				if (enableNotifications) {
					final Conversation conv = Conversation.getConversation(
							context, tid, true);
					if (conv != null) {
						String a;
						if (privateNotification) {
							if (l == 1) {
								a = context.getString(R.string.new_message_);
							} else {
								a = context.getString(R.string.new_messages_);
							}
						} else {
							a = conv.getContact().getDisplayName();
						}
						n = new Notification(Preferences
								.getNotificationItem(context), a,
								lastUnreadDate);
						if (l == 1) {
							String body;
							if (privateNotification) {
								body = context.getString(R.string.new_message);
							} else {
								body = lastUnreadBody;
							}
							if (body == null) {
								body = context
										.getString(R.string.mms_conversation);
							}
							n.setLatestEventInfo(context, a, body, pIntent);
						} else {
							n.setLatestEventInfo(context, a, String
									.format(context
											.getString(R.string.new_messages),
											l), pIntent);
						}
					}
				}
			} else {
				uri = Uri.parse(MessageList.URI);
				i = new Intent(Intent.ACTION_VIEW, uri, context, // .
						ConversationList.class);
				pIntent = PendingIntent.getActivity(context, 0, i,
						PendingIntent.FLAG_CANCEL_CURRENT);

				if (enableNotifications) {
					n = new Notification(R.drawable.stat_notify_sms, context
							.getString(R.string.new_messages_), lastUnreadDate);
					n.setLatestEventInfo(context, context
							.getString(R.string.new_messages_), String.format(
							context.getString(R.string.new_messages), l),
							pIntent);
					n.number = l;
				}
			}
			// add pending intent
			i.setFlags(i.getFlags() | Intent.FLAG_ACTIVITY_NEW_TASK);

			if (enableNotifications && n != null) {
				n.flags |= Notification.FLAG_SHOW_LIGHTS;
				n.ledARGB = Preferences.getLEDcolor(context);
				int[] ledFlash = Preferences.getLEDflash(context);
				n.ledOnMS = ledFlash[0];
				n.ledOffMS = ledFlash[1];
				final SharedPreferences p = PreferenceManager
						.getDefaultSharedPreferences(context);
				if (text != null) {
					final boolean vibrate = p.getBoolean(
							Preferences.PREFS_VIBRATE, false);
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
			}
			Log.d(TAG, "uri: " + uri);
			mNotificationMgr.cancel(NOTIFICATION_ID_NEW);
			if (enableNotifications && n != null) {
				mNotificationMgr.notify(NOTIFICATION_ID_NEW, n);
			}
		}
		Log.d(TAG, "return " + ret + " (2)");
		AppWidgetManager.getInstance(context).updateAppWidget(
				new ComponentName(context, WidgetProvider.class),
				WidgetProvider.getRemoteViews(context, l, pIntent));
		return ret;
	}
}
