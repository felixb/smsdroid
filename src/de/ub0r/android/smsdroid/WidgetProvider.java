/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.ub0r.android.smsdroid;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.database.Cursor;
import android.content.pm.PackageManager;
import android.os.SystemClock;
import android.util.Log;
import android.widget.RemoteViews;

/**
 * A widget provider.  We have a string that we pull from a preference in order to show
 * the configuration settings and the current time when the widget was updated.  We also
 * register a BroadcastReceiver for time-changed and timezone-changed broadcasts, and
 * update then too.
 *
 * <p>See also the following files:
 * <ul>
 *   <li>ExampleAppWidgetConfigure.java</li>
 *   <li>ExampleBroadcastReceiver.java</li>
 *   <li>res/layout/appwidget_configure.xml</li>
 *   <li>res/layout/appwidget_provider.xml</li>
 *   <li>res/xml/appwidget_provider.xml</li>
 * </ul>
 */
public class WidgetProvider extends AppWidgetProvider {
    // log tag
    private static final String TAG = "SMSdroid.wdp";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        Log.d(TAG, "onUpdate");
	final Cursor cursor = context.getContentResolver().query(SmsReceiver.URI,
			MessageListAdapter.PROJECTION,
			MessageListAdapter.SELECTION_UNREAD, null, null);
	final int l = cursor.getCount();
	Log.d(TAG, "l: " + l);
        final int N = appWidgetIds.length;
        for (int i=0; i<N; i++) {
		RemoteViews views = getRemoteViews(context);
        	appWidgetManager.updateAppWidget(appWidgetId, views);
        }
    }

	/**
	 * Get {@link RemoteViews}.
	 * @package context {@link Context}
	 * @return {@link RemoteViews}
	 */
    static RemoteViews getRemoteViews(final Context context) {
        Intent intent = new Intent(context, SMSdroid.class);
	PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);
	int appWidgetId = appWidgetIds[i];
       	RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget);
	views.setTextViewText(R.id.text1, String.valueOf(l));
	views.setOnClickPendingIntent(R.id.button, pendingIntent);
	return views;
    }
}

