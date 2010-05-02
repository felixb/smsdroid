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

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

/**
 * Preferences.
 * 
 * @author flx
 */
public class Preferences extends PreferenceActivity {
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
