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

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

/**
 * A widget provider.
 */
public final class WidgetProvider extends AppWidgetProvider {
	/** Tag for output. */
	private static final String TAG = "SMSdroid.wdp";

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onUpdate(final Context context,
			final AppWidgetManager appWidgetManager, final int[] appWidgetIds) {
		Log.d(TAG, "onUpdate");
		SmsReceiver.updateNewMessageNotification(context, null);
	}

	/**
	 * Get {@link RemoteViews}.
	 * 
	 * @param context
	 *            {@link Context}
	 * @return {@link RemoteViews}
	 */
	static RemoteViews getRemoteViews(final Context context) {
		final Cursor cursor = context.getContentResolver().query(
				SmsReceiver.URI, MessageListAdapter.PROJECTION,
				MessageListAdapter.SELECTION_UNREAD, null, null);
		final int l = cursor.getCount();
		Log.d(TAG, "l: " + l);
		return getRemoteViews(context, l, null);
	}

	/**
	 * Get {@link RemoteViews}.
	 * 
	 * @param context
	 *            {@link Context}
	 * @param count
	 *            number of unread messages
	 * @param uri
	 *            {@link Uri} to link
	 * @return {@link RemoteViews}
	 */
	static RemoteViews getRemoteViews(final Context context, final int count,
			final Uri uri) {
		Intent intent;
		if (uri == null) {
			intent = new Intent(context, SMSdroid.class);
		} else {
			if (uri.toString().equals(MessageList.URI)) {
				intent = new Intent(Intent.ACTION_VIEW, uri, context,
						SMSdroid.class);
			} else {
				intent = new Intent(Intent.ACTION_VIEW, uri, context,
						MessageList.class);
			}
		}
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		PendingIntent pendingIntent = PendingIntent.getActivity(context, 0,
				intent, PendingIntent.FLAG_CANCEL_CURRENT);
		RemoteViews views = new RemoteViews(context.getPackageName(),
				R.layout.widget);
		views.setTextViewText(R.id.text1, String.valueOf(count));
		if (count == 0) {
			views.setViewVisibility(R.id.text1, View.GONE);
		} else {
			views.setViewVisibility(R.id.text1, View.VISIBLE);
		}
		views.setOnClickPendingIntent(R.id.widget, pendingIntent);
		Log.d(TAG, "set pending intent: " + pendingIntent.toString());
		return views;
	}
}
