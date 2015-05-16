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
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.RemoteViews;

import de.ub0r.android.logg0r.Log;

/**
 * A widget provider.
 */
public final class WidgetProvider extends AppWidgetProvider {

    /**
     * Tag for output.
     */
    private static final String TAG = "wdp";

    /**
     * {@inheritDoc}
     */
    @Override
    public void onUpdate(final Context context, final AppWidgetManager appWidgetManager,
            final int[] appWidgetIds) {
        Log.d(TAG, "onUpdate");
        SmsReceiver.updateNewMessageNotification(context, null);
    }

    /**
     * Get {@link RemoteViews}.
     *
     * @param context {@link Context}
     * @param count   number of unread messages
     * @param pIntent {@link PendingIntent}
     * @return {@link RemoteViews}
     */
    static RemoteViews getRemoteViews(final Context context, final int count,
            final PendingIntent pIntent) {
        final SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(context);
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget);
        views.setTextViewText(R.id.text1, String.valueOf(count));
        if (count == 0) {
            views.setViewVisibility(R.id.text1, View.GONE);
        } else {
            views.setViewVisibility(R.id.text1, View.VISIBLE);
        }
        if (p.getBoolean(PreferencesActivity.PREFS_HIDE_WIDGET_LABEL, false)) {
            views.setViewVisibility(R.id.label, View.GONE);
        } else {
            views.setViewVisibility(R.id.label, View.VISIBLE);
        }
        if (pIntent != null) {
            views.setOnClickPendingIntent(R.id.widget, pIntent);
            Log.d(TAG, "set pending intent: ", pIntent.toString());
        }
        return views;
    }
}
