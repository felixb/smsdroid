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

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceFragment;

import de.ub0r.android.lib.IPreferenceContainer;

/**
 * {@link PreferenceFragment} for API>=11.
 *
 * @author flx
 */
@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public final class HeaderPreferenceFragment extends PreferenceFragment implements
        IPreferenceContainer {

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Activity a = getActivity();
        int res = a.getResources().getIdentifier(getArguments().getString("resource"), "xml",
                a.getPackageName());
        addPreferencesFromResource(res);

        PreferencesActivity.registerOnPreferenceClickListener(this);
    }

    @Override
    public Context getContext() {
        return getActivity();
    }
}
