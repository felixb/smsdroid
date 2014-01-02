/*
 * Copyright (C) 2010-2012 Felix Bechstein
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

import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import de.ub0r.android.lib.IPreferenceContainer;
import de.ub0r.android.lib.Utils;

/**
 * {@link PreferenceActivity} for API>=11.
 * 
 * @author flx
 */
public final class Preferences11Activity extends PreferenceActivity implements IPreferenceContainer {
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setTitle(R.string.settings);
		Utils.setLocale(this);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onBuildHeaders(final List<Header> target) {
		this.loadHeadersFromResource(R.xml.preference_headers, target);
	}

    @Override
    protected boolean isValidFragment(String fragmentName) {
        return true;
    }

    /**
	 * {@inheritDoc}
	 */
	@Override
	public Activity getActivity() {
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Context getContext() {
		return this;
	}
}
