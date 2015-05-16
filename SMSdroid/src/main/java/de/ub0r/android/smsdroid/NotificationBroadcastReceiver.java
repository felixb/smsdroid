package de.ub0r.android.smsdroid;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import de.ub0r.android.logg0r.Log;

/**
 * Receives intents from notifications.
 */
public class NotificationBroadcastReceiver extends BroadcastReceiver {

    private static final String TAG = "NotificationBroadcastReceiver";

    public static final String ACTION_MARK_READ = "de.ub0r.android.smsdroid.MARK_READ";

    public static final String EXTRA_MURI = "de.ub0r.android.smsdroid.MURI_KEY";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "onReceive(context, ", intent, ")");

        if (ACTION_MARK_READ.equals(intent.getAction())) {
            try {
                Bundle extras = intent.getExtras();
                if (extras == null) {
                    Log.w(TAG, "empty extras");
                    return;
                }

                // remember that we have to add the package here ..
                String muri = extras.getString(EXTRA_MURI);
                Log.d(TAG, "received uri: ", muri);
                ConversationListActivity.markRead(context, Uri.parse(muri), 1);

            } catch (Exception e) {
                Log.e(TAG, "unable to mark message read", e);
            }
        }
    }
}
