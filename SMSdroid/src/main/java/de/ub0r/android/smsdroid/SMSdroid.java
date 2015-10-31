/*
 * Copyright (C) 2009-2015 Felix Bechstein
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

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.os.Build;
import android.preference.PreferenceManager;
import android.provider.Telephony;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.telephony.SmsManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Toast;

import de.ub0r.android.logg0r.Log;

/**
 * @author flx
 */
public final class SMSdroid extends Application {

    /**
     * Tag for logging.
     */
    private static final String TAG = "app";

    /**
     * Projection for checking {@link Cursor}.
     */
    private static final String[] PROJECTION = new String[]{"_id"};

    @Override
    public void onCreate() {
        try {
            Class.forName("android.os.AsyncTask");
        } catch (Throwable ignore) {
        }

        super.onCreate();
        Log.i(TAG, "init SMSdroid v" + BuildConfig.VERSION_NAME + " (" + BuildConfig.VERSION_CODE
                + ")");

        // check for default app only when READ_SMS was granted
        // this may need a second launch on Android 6.0 though
        if (hasPermission(this, Manifest.permission.READ_SMS)) {
            final SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(this);
            int state = PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
            if (p.getBoolean(PreferencesActivity.PREFS_ACTIVATE_SENDER, true)) {
                try {
                    Cursor c = getContentResolver().query(SenderActivity.URI_SENT, PROJECTION,
                            null, null, "_id LIMIT 1");
                    if (c == null) {
                        Log.i(TAG, "disable .Sender: cursor=null");
                    } else if (SmsManager.getDefault() == null) {
                        Log.i(TAG, "disable .Sender: SmsManager=null");
                    } else {
                        state = PackageManager.COMPONENT_ENABLED_STATE_ENABLED;
                        Log.d(TAG, "enable .Sender");
                    }
                    if (c != null && !c.isClosed()) {
                        c.close();
                    }
                } catch (IllegalArgumentException | SQLiteException e) {
                    Log.e(TAG, "disable .Sender: ", e.getMessage(), e);
                }
            } else {
                Log.i(TAG, "disable .Sender");
            }
            getPackageManager().setComponentEnabledSetting(
                    new ComponentName(this, SenderActivity.class), state,
                    PackageManager.DONT_KILL_APP);
        } else {
            Log.w(TAG, "ignore .Sender state, READ_SMS permission is missing to check default app");
        }
    }

    /**
     * Get an {@link OnClickListener} for stating an Activity for given {@link Intent}.
     *
     * @param context {@link Context}
     * @param intent  {@link Intent}
     * @return {@link OnClickListener}
     */
    static OnClickListener getOnClickStartActivity(final Context context, final Intent intent) {
        if (intent == null) {
            return null;
        }
        return new OnClickListener() {
            @Override
            public void onClick(final View v) {
                try {
                    context.startActivity(intent);
                } catch (ActivityNotFoundException e) {
                    Log.w(TAG, "activity not found", e);
                    Toast.makeText(context, "no activity for data: " + intent.getType(),
                            Toast.LENGTH_LONG).show();
                }
            }
        };
    }

    static boolean isDefaultApp(final Context context) {
        // there is no default sms app before android 4.4
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            return true;
        }

        try {
            // check if this is the default sms app.
            // If the device doesn't support Telephony.Sms (i.e. tablet) getDefaultSmsPackage() will
            // be null.
            final String smsPackage = Telephony.Sms.getDefaultSmsPackage(context);
            return smsPackage == null || smsPackage.equals(BuildConfig.APPLICATION_ID);
        } catch (SecurityException e) {
            // some samsung devices/tablets want permission GET_TASKS o.O
            Log.e(TAG, "failed to query default SMS app", e);
            return true;
        }
    }

    static boolean hasPermission(final Context context, final String permission) {
        return ContextCompat.checkSelfPermission(context, permission)
                == PackageManager.PERMISSION_GRANTED;
    }

    static boolean requestPermission(final Activity activity, final String permission,
            final int requestCode, final int message,
            final DialogInterface.OnClickListener onCancelListener) {
        Log.i(TAG, "requesting permission: " + permission);
        if (!hasPermission(activity, permission)) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)) {
                new AlertDialog.Builder(activity)
                        .setTitle(R.string.permissions_)
                        .setMessage(message)
                        .setCancelable(false)
                        .setNegativeButton(android.R.string.cancel, onCancelListener)
                        .setPositiveButton(android.R.string.ok,
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(final DialogInterface dialogInterface,
                                            final int i) {
                                        ActivityCompat.requestPermissions(activity,
                                                new String[]{permission}, requestCode);
                                    }
                                })
                        .show();
            } else {
                ActivityCompat.requestPermissions(activity, new String[]{permission}, requestCode);
            }
            return false;
        } else {
            return true;
        }
    }
}
