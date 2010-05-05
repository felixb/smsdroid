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
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

/**
 * Preferences.
 * 
 * @author flx
 */
public class Preferences extends PreferenceActivity {
	/** Packagename of SendLog. */
	public static final String SENDLOG_PACKAGE_NAME = "org.l6n.sendlog";
	/** Classname of SendLog. */
	public static final String SENDLOG_CLASS_NAME = ".SendLog";

	/** Preference's name: vibrate on receive. */
	static final String PREFS_VIBRATE = "receive_vibrate";
	/** Preference's name: sound on receive. */
	static final String PREFS_SOUND = "receive_sound";
	/** Preference's name: led color. */
	private static final String PREFS_LED_COLOR = "receive_led_color";
	/** Preference's name: led flash. */
	private static final String PREFS_LED_FLASH = "receive_led_flash";
	/** Preference's name: vibrator pattern. */
	private static final String PREFS_VIBRATOR_PATTERN = "receive_vibrate_mode";
	/** Preference's name: hide ads. */
	static final String PREFS_HIDEADS = "hideads";
	/** Preference's name: enable notifications. */
	static final String PREFS_NOTIFICATION_ENABLE = "notification_enable";
	/** Prefernece's name: show contact's photo. */
	static final String PREFS_CONTACT_PHOTO = "show_contact_photo";
	/** Prefernece's name: show emoticons in messagelist. */
	static final String PREFS_EMOTICONS = "show_emoticons";
	/** Preference's name: theme. */
	private static final String PREFS_THEME = "theme";
	/** Theme: black. */
	private static final String THEME_BLACK = "black";
	/** Theme: light. */
	private static final String THEME_LIGHT = "light";
	/** Preference's name: textsize. */
	private static final String PREFS_TEXTSIZE = "textsize";
	/** Textsize: small. */
	private static final String TEXTSIZE_SMALL = "small";
	/** Textsize: medium. */
	private static final String TEXTSIZE_MEDIUM = "medium";
	/** Textsize: small. */
	private static final int TEXTSIZE_SMALL_SP = 13;
	/** Textsize: medium. */
	private static final int TEXTSIZE_MEDIUM_SP = 16;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// final int theme = Preferences.getTheme(this);
		// this.setTheme(theme);
		this.addPreferencesFromResource(R.xml.prefs);

		Preference p = this.findPreference("send_logs");
		if (p != null) {
			p.setOnPreferenceClickListener(// .
					new Preference.OnPreferenceClickListener() {
						public boolean onPreferenceClick(
								final Preference preference) {
							Preferences.this.collectAndSendLog();
							return true;
						}
					});
		}
		p = this.findPreference("clear_cache");
		if (p != null) {
			p.setOnPreferenceClickListener(// .
					new Preference.OnPreferenceClickListener() {
						public boolean onPreferenceClick(
								final Preference preference) {
							Preferences.this.getContentResolver().delete(
									ConversationProvider.CONTENT_URI, null,
									null);
							return true;
						}
					});
		}
	}

	/**
	 * Fire a given {@link Intent}.
	 * 
	 * @author flx
	 */
	private static class FireIntent implements DialogInterface.OnClickListener {
		/** {@link Activity}. */
		private final Activity a;
		/** {@link Intent}. */
		private final Intent i;

		/**
		 * Default Constructor.
		 * 
		 * @param activity
		 *            {@link Activity}
		 * @param intent
		 *            {@link Intent}
		 */
		public FireIntent(final Activity activity, final Intent intent) {
			this.a = activity;
			this.i = intent;
		}

		/**
		 * {@inheritDoc}
		 */
		public void onClick(final DialogInterface dialog, // .
				final int whichButton) {
			this.a.startActivity(this.i);
		}
	}

	/**
	 * Collect and send Log.
	 */
	final void collectAndSendLog() {
		final PackageManager packageManager = this.getPackageManager();
		Intent intent = packageManager
				.getLaunchIntentForPackage(SENDLOG_PACKAGE_NAME);
		String message;
		if (intent == null) {
			intent = new Intent(Intent.ACTION_VIEW, Uri
					.parse("market://search?q=pname:" + SENDLOG_PACKAGE_NAME));
			message = "Install the free SendLog application to "
					+ "collect the device log and send "
					+ "it to the developer.";
		} else {
			intent.setType("0||flx.yoo@gmail.com");
			message = "Run SendLog application.\nIt will collect the "
					+ "device log and send it to the developer." + "\n"
					+ "You will have an opportunity to review "
					+ "and modify the data being sent.";
		}
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		new AlertDialog.Builder(this).setTitle(
				this.getString(R.string.app_name)).setIcon(
				android.R.drawable.ic_dialog_info).setMessage(message)
				.setPositiveButton(android.R.string.ok,
						new FireIntent(this, intent)).setNegativeButton(
						android.R.string.cancel, null).show();
	}

	/**
	 * Get Theme from Preferences.
	 * 
	 * @param context
	 *            {@link Context}
	 * @return theme
	 */
	static final int getTheme(final Context context) {
		final SharedPreferences p = PreferenceManager
				.getDefaultSharedPreferences(context);
		final String s = p.getString(PREFS_THEME, THEME_BLACK);
		if (s != null && THEME_LIGHT.equals(s)) {
			return android.R.style.Theme_Light;
		}
		return android.R.style.Theme_Black;
	}

	/**
	 * Get Textsize from Preferences.
	 * 
	 * @param context
	 *            {@link Context}
	 * @return theme
	 */
	static final int getTextsize(final Context context) {
		final SharedPreferences p = PreferenceManager
				.getDefaultSharedPreferences(context);
		final String s = p.getString(PREFS_TEXTSIZE, TEXTSIZE_SMALL);
		if (s != null && TEXTSIZE_MEDIUM.equals(s)) {
			return TEXTSIZE_MEDIUM_SP;
		}
		return TEXTSIZE_SMALL_SP;
	}

	/**
	 * Get LED color pattern from Preferences.
	 * 
	 * @param context
	 *            {@link Context}
	 * @return pattern
	 */
	static final int getLEDcolor(final Context context) {
		final SharedPreferences p = PreferenceManager
				.getDefaultSharedPreferences(context);
		final String s = p.getString(PREFS_LED_COLOR, "65280");
		return Integer.parseInt(s);
	}

	/**
	 * Get LED flash pattern from Preferences.
	 * 
	 * @param context
	 *            {@link Context}
	 * @return pattern
	 */
	static final int[] getLEDflash(final Context context) {
		final SharedPreferences p = PreferenceManager
				.getDefaultSharedPreferences(context);
		final String s = p.getString(PREFS_LED_FLASH, "500_2000");
		final String[] ss = s.split("_");
		final int[] ret = new int[2];
		ret[0] = Integer.parseInt(ss[0]);
		ret[1] = Integer.parseInt(ss[1]);
		return ret;
	}

	/**
	 * Get vibrator pattern from Preferences.
	 * 
	 * @param context
	 *            {@link Context}
	 * @return pattern
	 */
	static final long[] getVibratorPattern(final Context context) {
		final SharedPreferences p = PreferenceManager
				.getDefaultSharedPreferences(context);
		final String s = p.getString(PREFS_VIBRATOR_PATTERN, "0");
		final String[] ss = s.split("_");
		final int l = ss.length;
		final long[] ret = new long[l];
		for (int i = 0; i < l; i++) {
			ret[i] = Long.parseLong(ss[i]);
		}
		return ret;
	}
}
