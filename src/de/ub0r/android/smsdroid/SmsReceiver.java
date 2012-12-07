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

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.provider.CallLog.Calls;
import android.support.v4.app.NotificationCompat;
import android.telephony.gsm.SmsMessage;
import de.ub0r.android.lib.Log;
import de.ub0r.android.lib.Utils;

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
	private static final String ACTION_SMS = "android.provider.Telephony.SMS_RECEIVED";
	/** Intent.action for receiving MMS. */
	private static final String ACTION_MMS = "android.provider.Telephony.WAP_PUSH_RECEIVED";

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

	/** Red lights. */
	static final int RED = 0xFFFF0000;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final void onReceive(final Context context, final Intent intent) {
		Log.d(TAG, "onReceive()");
		final PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
		final PowerManager.WakeLock wakelock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
		wakelock.acquire();
		Log.i(TAG, "got wakelock");
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
		if (action.equals(SenderActivity.MESSAGE_SENT_ACTION)) {
			this.handleSent(context, intent, this.getResultCode());
		} else {
			boolean silent = false;
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
					final String s = smsMessage[0].getOriginatingAddress();
					final SpamDB db = new SpamDB(context);
					db.open();
					if (db.isInDB(smsMessage[0].getOriginatingAddress())) {
						Log.d(TAG, "Message from " + s + " filtered.");
						silent = true;
					} else {
						Log.d(TAG, "Message from " + s + " NOT filtered.");
					}
					db.close();
				}
			} else if (action.equals(ACTION_MMS)) {
				t = MMS_BODY;
			}

			if (!silent) {
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
		}
		wakelock.release();
		Log.i(TAG, "wakelock released");
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
	private static int[] getUnreadSMS(final ContentResolver cr, final String text) {
		Log.d(TAG, "getUnreadSMS(cr, " + text + ")");
		Cursor cursor = cr.query(URI_SMS, Message.PROJECTION, Message.SELECTION_READ_UNREAD,
				Message.SELECTION_UNREAD, SORT);
		if (cursor == null || cursor.isClosed() || cursor.getCount() == 0 || !cursor.moveToFirst()) {
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
		final int count = cursor.getCount();
		if (cursor != null && !cursor.isClosed()) {
			cursor.close();
		}
		return new int[] { tid, count };
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
	private static int[] getUnreadMMS(final ContentResolver cr, final String text) {
		Log.d(TAG, "getUnreadMMS(cr, " + text + ")");
		Cursor cursor = cr.query(URI_MMS, Message.PROJECTION_READ, Message.SELECTION_READ_UNREAD,
				Message.SELECTION_UNREAD, null);
		if (cursor == null || cursor.isClosed() || cursor.getCount() == 0 || !cursor.moveToFirst()) {
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
		if (d < ConversationListActivity.MIN_DATE) {
			d *= ConversationListActivity.MILLIS;
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
		final int count = cursor.getCount();
		if (cursor != null && !cursor.isClosed()) {
			cursor.close();
		}
		return new int[] { tid, count };
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
	private static int[] getUnread(final ContentResolver cr, final String text) {
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
	static final int updateNewMessageNotification(final Context context, final String text) {
		Log.d(TAG, "updNewMsgNoti(" + context + "," + text + ")");
		final NotificationManager mNotificationMgr = (NotificationManager) context
				.getSystemService(Context.NOTIFICATION_SERVICE);
		final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		final boolean enableNotifications = prefs.getBoolean(
				PreferencesActivity.PREFS_NOTIFICATION_ENABLE, true);
		final boolean privateNotification = prefs.getBoolean(
				PreferencesActivity.PREFS_NOTIFICATION_PRIVACY, false);
		final boolean showPhoto = !privateNotification
				&& prefs.getBoolean(PreferencesActivity.PREFS_CONTACT_PHOTO, true);
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
			final Intent i = new Intent(context, ConversationListActivity.class);
			// add pending intent
			i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			pIntent = PendingIntent.getActivity(context, 0, i, PendingIntent.FLAG_CANCEL_CURRENT);
		} else {
			NotificationCompat.Builder nb = new NotificationCompat.Builder(context);
			boolean showNotification = true;
			Intent i;
			if (tid >= 0) {
				uri = Uri.parse(MessageListActivity.URI + tid);
				i = new Intent(Intent.ACTION_VIEW, uri, context, MessageListActivity.class);
				pIntent = PendingIntent.getActivity(context, 0, i,
						PendingIntent.FLAG_UPDATE_CURRENT);

				if (enableNotifications) {
					final Conversation conv = Conversation.getConversation(context, tid, true);
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
						showNotification = true;
						nb.setSmallIcon(PreferencesActivity.getNotificationIcon(context));
						nb.setTicker(a);
						nb.setWhen(lastUnreadDate);
						if (l == 1) {
							String body;
							if (privateNotification) {
								body = context.getString(R.string.new_message);
							} else {
								body = lastUnreadBody;
							}
							if (body == null) {
								body = context.getString(R.string.mms_conversation);
							}
							nb.setContentTitle(a);
							nb.setContentText(body);
							nb.setContentIntent(pIntent);
						} else {
							nb.setContentTitle(a);
							nb.setContentText(context.getString(R.string.new_messages, l));
							nb.setContentIntent(pIntent);
						}
						if (showPhoto // just for the speeeeed
								&& Utils.isApi(Build.VERSION_CODES.HONEYCOMB)) {
							conv.getContact().update(context, false, true);
							Drawable d = conv.getContact().getAvatar(context, null);
							if (d instanceof BitmapDrawable) { // FIXME
								nb.setLargeIcon(((BitmapDrawable) d).getBitmap());
							}
						}
					}
				}
			} else {
				uri = Uri.parse(MessageListActivity.URI);
				i = new Intent(Intent.ACTION_VIEW, uri, context, ConversationListActivity.class);
				pIntent = PendingIntent.getActivity(context, 0, i,
						PendingIntent.FLAG_UPDATE_CURRENT);

				if (enableNotifications) {
					showNotification = true;
					nb.setSmallIcon(PreferencesActivity.getNotificationIcon(context));
					nb.setTicker(context.getString(R.string.new_messages_));
					nb.setWhen(lastUnreadDate);
					nb.setContentTitle(context.getString(R.string.new_messages_));
					nb.setContentText(context.getString(R.string.new_messages, l));
					nb.setContentIntent(pIntent);
					nb.setNumber(l);
				}
			}
			// add pending intent
			i.setFlags(i.getFlags() | Intent.FLAG_ACTIVITY_NEW_TASK);

			if (enableNotifications && showNotification) {
				int[] ledFlash = PreferencesActivity.getLEDflash(context);
				nb.setLights(PreferencesActivity.getLEDcolor(context), ledFlash[0], ledFlash[1]);
				final SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(context);
				if (text != null) {
					final boolean vibrate = p.getBoolean(PreferencesActivity.PREFS_VIBRATE, false);
					final String s = p.getString(PreferencesActivity.PREFS_SOUND, null);
					Uri sound;
					if (s == null || s.length() <= 0) {
						sound = null;
					} else {
						sound = Uri.parse(s);
					}
					if (vibrate) {
						final long[] pattern = PreferencesActivity.getVibratorPattern(context);
						if (pattern.length == 1 && pattern[0] == 0) {
							nb.setDefaults(Notification.DEFAULT_VIBRATE);
						} else {
							nb.setVibrate(pattern);
						}
					}
					nb.setSound(sound);
				}
			}
			Log.d(TAG, "uri: " + uri);
			mNotificationMgr.cancel(NOTIFICATION_ID_NEW);
			if (enableNotifications && showNotification) {
				mNotificationMgr.notify(NOTIFICATION_ID_NEW, nb.getNotification());
			}
		}
		Log.d(TAG, "return " + ret + " (2)");
		AppWidgetManager.getInstance(context).updateAppWidget(
				new ComponentName(context, WidgetProvider.class),
				WidgetProvider.getRemoteViews(context, l, pIntent));
		return ret;
	}

	/**
	 * Update failed message notification.
	 * 
	 * @param context
	 *            {@link Context}
	 * @param uri
	 *            {@link Uri} to message
	 */
	private void updateFailedNotification(final Context context, final Uri uri) {
		Log.d(TAG, "updateFailedNotification: " + uri);
		final Cursor c = context.getContentResolver().query(uri, Message.PROJECTION_SMS, null,
				null, null);
		if (c != null && c.moveToFirst()) {
			final int id = c.getInt(Message.INDEX_ID);
			final int tid = c.getInt(Message.INDEX_THREADID);
			final String body = c.getString(Message.INDEX_BODY);
			final long date = c.getLong(Message.INDEX_DATE);

			Conversation conv = Conversation.getConversation(context, tid, true);

			final NotificationManager mNotificationMgr = (NotificationManager) context
					.getSystemService(Context.NOTIFICATION_SERVICE);
			final SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(context);
			final boolean privateNotification = p.getBoolean(
					PreferencesActivity.PREFS_NOTIFICATION_PRIVACY, false);
			Intent intent;
			if (conv == null) {
				intent = new Intent(Intent.ACTION_VIEW, null, context, SenderActivity.class);
			} else {
				intent = new Intent(Intent.ACTION_VIEW, conv.getUri(), context,
						MessageListActivity.class);
			}
			intent.putExtra(Intent.EXTRA_TEXT, body);

			String title = context.getString(R.string.error_sending_failed);
			String text = null;
			final Notification n = new Notification(android.R.drawable.stat_sys_warning, title,
					date);

			if (privateNotification) {
				title += "!";
			} else if (conv == null) {
				title += "!";
				text = body;
			} else {
				title += ": " + conv.getContact().getDisplayName();
				text = body;
			}
			n.setLatestEventInfo(context, title, text, PendingIntent.getActivity(context, 0,
					intent, PendingIntent.FLAG_CANCEL_CURRENT));
			n.flags |= Notification.FLAG_AUTO_CANCEL;
			n.flags |= Notification.FLAG_SHOW_LIGHTS;
			n.ledARGB = RED;
			int[] ledFlash = PreferencesActivity.getLEDflash(context);
			n.ledOnMS = ledFlash[0];
			n.ledOffMS = ledFlash[1];
			final boolean vibrate = p.getBoolean(PreferencesActivity.PREFS_VIBRATE, false);
			final String s = p.getString(PreferencesActivity.PREFS_SOUND, null);
			Uri sound;
			if (s == null || s.length() <= 0) {
				sound = null;
			} else {
				sound = Uri.parse(s);
			}
			if (vibrate) {
				final long[] pattern = PreferencesActivity.getVibratorPattern(context);
				if (pattern.length == 1 && pattern[0] == 0) {
					n.defaults |= Notification.DEFAULT_VIBRATE;
				} else {
					n.vibrate = pattern;
				}
			}
			n.sound = sound;
			mNotificationMgr.notify(id, n);
		}
		if (c != null && !c.isClosed()) {
			c.close();
		}
	}

	/**
	 * Handle sent message.
	 * 
	 * @param context
	 *            {@link Context}
	 * @param intent
	 *            {@link Intent}
	 * @param resultCode
	 *            message status
	 */
	private void handleSent(final Context context, final Intent intent, final int resultCode) {
		final Uri uri = intent.getData();
		Log.d(TAG, "sent message: " + uri + ", rc: " + resultCode);
		if (uri == null) {
			Log.w(TAG, "handleSent(null)");
			return;
		}

		if (resultCode == Activity.RESULT_OK) {
			final ContentValues cv = new ContentValues(1);
			cv.put(SenderActivity.TYPE, Message.SMS_OUT);
			context.getContentResolver().update(uri, cv, null, null);
		} else {
			this.updateFailedNotification(context, uri);
		}
	}
}
