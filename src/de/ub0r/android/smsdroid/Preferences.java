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
	/** Preference's name: hide ads. */
	static final String PREFS_HIDEADS = "hideads";
	/** Preference's name: enable notifications. */
	static final String PREFS_NOTIFICATION_ENABLE = "notification_enable";
	/** Preference's name: theme. */
	private static final String PREFS_THEME = "theme";
	/** Theme: default. */
	private static final String THEME_DEFAULT = "default";
	/** Theme: black. */
	private static final String THEME_BLACK = "black";
	/** Theme: light. */
	private static final String THEME_LIGHT = "light";

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setTheme(Preferences.getTheme(this));
		this.addPreferencesFromResource(R.xml.prefs);
	}

	static final int getTheme(final Context context) {
		final SharedPreferences p = PreferenceManager
				.getDefaultSharedPreferences(context);
		final String s = p.getString(PREFS_THEME, THEME_DEFAULT);
		if (s == null || THEME_DEFAULT.equals(s)) {
			return android.R.style.Theme;
		} else if (THEME_BLACK.equals(s)) {
			return android.R.style.Theme_Black;
		} else if (THEME_LIGHT.equals(s)) {
			return android.R.style.Theme_Light;
		}
		return android.R.style.Theme;
	}
}
