package de.ub0r.android.smsdroid;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import de.ub0r.android.lib.Log;

/**
 * Created by Michael on 03.01.14.
 */
public class WebSMSBroadcastReceiver extends BroadcastReceiver {

    private static final String TAG = "WebSMSBroadcastReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {

        Log.d(TAG, "onReceive called");
        if(intent.getAction().equals("de.ub0r.android.websms.SEND_SUCCESSFULL")) {
            try {
                    Bundle extras = intent.getExtras();
                    String[] addressees = extras.getStringArray("address");
                    String[] add_short = new String[addressees.length];

                    // Copy each number in the second array
                    for(int i = 0; i < add_short.length; ++i) {
                        String[] temp = addressees[i].split(" ");
                        add_short[i] = temp[0];
                    }

                    String body  = extras.getString("body");

                    ContentResolver resolver = context.getContentResolver();
                    ContentValues values = null;

                    // Insert all sms as sent
                    for(int i = 0; i < add_short.length; ++i) {
                        values = new ContentValues();

                        values.put("address", add_short[i]);
                        values.put("body", body);
                        resolver.insert(Uri.parse("content://sms/sent"), values);

                        Log.d(TAG, "Recipient " + String.valueOf(i) + " of " + String.valueOf(add_short.length));
                        Log.d(TAG, "Insert sent SMS into database: " + add_short[i] + ", " + body);
                }
            } catch (Exception e) {
                Log.d(TAG, "Exception during parsing the intent");
            }
        }
    }
}
