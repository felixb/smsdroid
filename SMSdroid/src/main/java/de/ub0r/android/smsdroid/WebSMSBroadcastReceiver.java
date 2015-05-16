package de.ub0r.android.smsdroid;

import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Telephony;
import android.text.TextUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.ub0r.android.logg0r.Log;

/**
 * Save messages sent by WebSMS to internal SMS database.
 */
public class WebSMSBroadcastReceiver extends BroadcastReceiver {

    private static final String TAG = "WebSMSBroadcastReceiver";

    /**
     * ACTION for publishing information about sent websms.
     */
    private static final String ACTION_CM_WEBSMS = "de.ub0r.android.callmeter.SAVE_WEBSMS";

    /**
     * Extra holding uri of sent sms.
     */
    private static final String EXTRA_WEBSMS_URI = "uri";

    /**
     * Extra holding name of connector.
     */
    private static final String EXTRA_WEBSMS_CONNECTOR = "connector";

    @TargetApi(Build.VERSION_CODES.KITKAT)
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "onReceive(context, ", intent, ")");
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            Log.e(TAG, TAG, " not available on API ", Build.VERSION.SDK_INT);
            return;
        }

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
                    // check whether we got a already known address with name
                    Log.d(TAG, "before recipients", recipients[i]);
                    if (recipients[i].contains("<")) {
                        Pattern smsPattern = Pattern.compile("<(.*?)>");
                        Matcher m = smsPattern.matcher(recipients[i]);
                        if (m.find()) {
                            recipients[i] = m.group(1);
                        } else {
                            Log.w(TAG, "Pattern failed.");
                            recipients[i] = recipients[i].split(" ")[0];
                        }
                    } else {
                        // pure numeric
                        recipients[i] = recipients[i].split(" ")[0];
                    }
                    Log.d(TAG, "after recipients", recipients[i]);
                }

                String body = extras.getString("body");
                if (TextUtils.isEmpty(body)) {
                    Log.w(TAG, "empty body");
                    return;
                }

                String connectorName = extras.getString("connector_name");

                ContentResolver cr = context.getContentResolver();
                ContentValues values = new ContentValues();
                values.put(Telephony.Sms.BODY, body);

                // Insert all sms as sent
                for (int i = 0; i < recipients.length; ++i) {
                    values.put(Telephony.Sms.ADDRESS, recipients[i]);
                    Uri u = cr.insert(Telephony.Sms.Sent.CONTENT_URI, values);
                    Log.d(TAG, "Recipient ", i, " of ", recipients.length);
                    Log.d(TAG, "Insert sent SMS into database: ", recipients[i], ", ", body);
                    sendSavedMessageToCallMeter(context, u, connectorName);
                }
            } catch (Exception e) {
                Log.e(TAG, "unable to write messages to database", e);
            }
        }
    }

    private void sendSavedMessageToCallMeter(final Context context, final Uri u,
            final String connectorName) {
        final Intent intent = new Intent(ACTION_CM_WEBSMS);
        intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
        intent.putExtra(EXTRA_WEBSMS_URI, u.toString());
        intent.putExtra(EXTRA_WEBSMS_CONNECTOR, connectorName.toLowerCase());
        context.sendBroadcast(intent);
    }
}
