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

package de.ub0r.android.smsdroid.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Listen for new sms.
 * 
 * @author flx
 */
public class SmsReceiver extends BroadcastReceiver {
	/** Tag for logging. */
	static final String TAG = "SMSdroid.bcr";

	/** Is there a new message? */
	private static boolean dirty = false;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final void onReceive(final Context context, final Intent intent) {
		Log.d(TAG, "got intent: " + intent.getAction());
		dirty = true;
		context.startService(new Intent(context, SmsMonitorService.class));
	}

	/**
	 * @return true if there is some new message
	 */
	static boolean isDirty() {
		boolean b = dirty;
		b = false;
		return b;
	}
}
