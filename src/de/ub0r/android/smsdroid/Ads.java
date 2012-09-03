/*
 * Copyright (C) 2011-2012 Felix Bechstein
 * 
 * This file is part of ub0rlib.
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

import java.util.Set;

import android.app.Activity;
import android.os.Build;
import android.view.View;
import android.webkit.WebViewDatabase;
import android.widget.LinearLayout;

import com.google.ads.Ad;
import com.google.ads.AdListener;
import com.google.ads.AdRequest;
import com.google.ads.AdRequest.ErrorCode;
import com.google.ads.AdSize;
import com.google.ads.AdView;

import de.ub0r.android.lib.Log;

/**
 * Class managing ads.
 * 
 * @author flx
 */
public final class Ads {
	/** Tag for output. */
	private static final String TAG = "ads";

	/**
	 * Hidden constructor.
	 */
	private Ads() {
		// nothing to do.
	}

	/**
	 * Load ads.
	 * 
	 * @param activity
	 *            activity to show ad in
	 * @param adBase
	 *            {@link LinearLayout} to ad the adView
	 * @param unitId
	 *            google's unit id
	 * @param keywords
	 *            keywords for the ads
	 */
	public static void loadAd(final Activity activity, final int adBase, final String unitId,
			final Set<String> keywords) {
		Log.d(TAG, "loadAd(" + unitId + ")");

		final LinearLayout adframe = (LinearLayout) activity.findViewById(adBase);
		if (adframe == null) {
			Log.e(TAG, "adframe=null");
			return;
		} else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.GINGERBREAD) {
			Log.d(TAG, "API " + Build.VERSION.SDK_INT + " <= GINGERBREAD");
			WebViewDatabase webViewDB = WebViewDatabase.getInstance(activity);
			if (webViewDB == null) {
				Log.e(TAG, "webViewDB == null");
				return;
			}
		}

		AdView adv;
		View v = adframe.getChildAt(0);
		if (v != null && v instanceof AdView) {
			adv = (AdView) v;
		} else {
			adv = new AdView(activity, AdSize.SMART_BANNER, unitId);
			adframe.addView(adv);
		}

		final AdRequest ar = new AdRequest();
		if (keywords != null) {
			ar.setKeywords(keywords);
		}

		adv.setAdListener(new AdListener() {
			@Override
			public void onReceiveAd(final Ad ad) {
				Log.d(TAG, "got ad: " + ad.toString());
				adframe.setVisibility(View.VISIBLE);
			}

			@Override
			public void onPresentScreen(final Ad ad) {
				// nothing todo
			}

			@Override
			public void onLeaveApplication(final Ad ad) {
				// nothing todo
			}

			@Override
			public void onFailedToReceiveAd(final Ad ad, final ErrorCode err) {
				Log.i(TAG, "failed to load ad: " + err);
			}

			@Override
			public void onDismissScreen(final Ad arg0) {
				// nothing todo
			}
		});
		Log.d(TAG, "send request");
		adv.loadAd(ar);
		Log.d(TAG, "loadAd() end");
	}
}
