package de.ub0r.android.smsdroid;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import de.ub0r.android.logg0r.Log;

/**
 * Receives intents from notifications.
 */
public class NotificationBroadcastReceiver extends BroadcastReceiver {

    private static final String TAG = "NotificationBroadcastReceiver";

    public static final String ACTION_MARK_READ = "de.ub0r.android.smsdroid.MARK_READ";

    @Override
    public void onReceive(final Context context, final Intent intent) {
        Log.d(TAG, "onReceive(context, ", intent, ")");

        final Uri uri = intent.getData();
        Log.d(TAG, "with uri: ", uri);
        if (ACTION_MARK_READ.equals(intent.getAction()) && uri != null) {
            try {
                ConversationListActivity.markRead(context, uri, 1);
            } catch (Exception e) {
                Log.e(TAG, "unable to mark message read", e);
            }
        } else {
            Log.e(TAG, "illegal intent: ", intent.getAction(), ", ", uri);
        }
    }
}
