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

import android.app.Activity;
import android.app.Application;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.widget.Toast;
import de.ub0r.android.lib.Log;

/**
 * @author flx
 */
public final class SMSdroid extends Application {
	/** Tag for logging. */
	static final String TAG = "app";

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onCreate() {
		super.onCreate();
		Log.init("SMSdroid");
	}

	/**
	 * Get an {@link OnClickListener} for stating an {@link Activity} for given
	 * {@link Intent}.
	 * 
	 * @param context
	 *            {@link Context}
	 * @param intent
	 *            {@link Intent}
	 * @return {@link OnClickListener}
	 */
	static OnClickListener getOnClickStartActivity(final Context context,
			final Intent intent) {
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
					Toast.makeText(context,
							"no activity for data: " + intent.getType(),
							Toast.LENGTH_LONG).show();
				}
			}
		};
	}

	/**
	 * Get an {@link OnLongClickListener} for stating an {@link Activity} for
	 * given {@link Intent}.
	 * 
	 * @param context
	 *            {@link Context}
	 * @param intent
	 *            {@link Intent}
	 * @return {@link OnLongClickListener}
	 */
	static OnLongClickListener getOnLongClickStartActivity(
			final Context context, final Intent intent) {
		if (intent == null) {
			return null;
		}
		return new OnLongClickListener() {
			@Override
			public boolean onLongClick(final View v) {
				try {
					context.startActivity(intent);
					return true;
				} catch (ActivityNotFoundException e) {
					Log.w(TAG, "activity not found", e);
					Toast.makeText(context,
							"no activity for data: " + intent.getType(),
							Toast.LENGTH_LONG).show();
				}
				return false;
			}
		};
	}
}
