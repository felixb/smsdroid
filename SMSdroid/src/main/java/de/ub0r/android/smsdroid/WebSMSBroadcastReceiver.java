package de.ub0r.android.smsdroid;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;

import de.ub0r.android.lib.Log;

/**
 * Save messages sent by WebSMS to internal SMS database.
 */
public class WebSMSBroadcastReceiver extends BroadcastReceiver {

    private static final String TAG = "WebSMSBroadcastReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "onReceive(context, " + intent + ")");

        if ("de.ub0r.android.websms.SEND_SUCCESSFUL".equals(intent.getAction())) {
            try {
                Bundle extras = intent.getExtras();
                if (extras == null) {
                    Log.w(TAG, "empty extras");
                    return;
                }
                String[] recipients = extras.getStringArray("address");
                if (recipients == null || recipients.length == 0) {
                    Log.w(TAG, "empty recipients");
                    return;
                }

                for (int i = 0; i < recipients.length; ++i) {
                    recipients[i] = recipients[i].split(" ")[0];
                }

                String body = extras.getString("body");
                if (TextUtils.isEmpty(body)) {
                    Log.w(TAG, "empty body");
                    return;
                }

                ContentResolver cr = context.getContentResolver();
                ContentValues values = new ContentValues();

                // Insert all sms as sent
                for (int i = 0; i < recipients.length; ++i) {
                    values.clear();

                    values.put("receiver", recipients[i]);
                    values.put("body", body);
                    cr.insert(Uri.parse("content://sms/sent"), values);

                    Log.d(TAG, "Recipient " + i + " of " + recipients.length);
                    Log.d(TAG, "Insert sent SMS into database: " + recipients[i] + ", " + body);
                }
            } catch (Exception e) {
                Log.e(TAG, "unable to write messages to database", e);
            }
        }
    }
}
