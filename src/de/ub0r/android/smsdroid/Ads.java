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

import java.util.List;
import java.util.Set;

import android.app.Activity;
import android.content.Context;
import android.location.Location;
import android.location.LocationManager;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.View;
import android.widget.ViewFlipper;

import com.google.ads.Ad;
import com.google.ads.AdListener;
import com.google.ads.AdRequest;
import com.google.ads.AdRequest.ErrorCode;
import com.google.ads.AdSize;
import com.google.ads.AdView;
import com.mobfox.sdk.BannerListener;
import com.mobfox.sdk.MobFoxView;
import com.mobfox.sdk.Mode;
import com.mobfox.sdk.RequestException;

import de.ub0r.android.lib.Log;

/**
 * Class managing ads.
 * 
 * @author flx
 */
public final class Ads implements AdListener, BannerListener {
	/** Tag for output. */
	private static final String TAG = "ads";

	/** Id for handler. */
	private static final int REFRESH_AD = 101;
	/** Refresh interval. */
	private static final long REFRESH_INTERVAL = 30000;

	/** AdMob's publisher id. */
	private final String mAdMobPubId;
	/** MobFox's publisher id. */
	private final String mMobFoxPubId;

	/** The {@link Activity}. */
	private final Activity mActivity;
	/** The {@link ViewFlipper}. */
	private final ViewFlipper mViewFlipper;
	/** AdMob's view. */
	private AdView mAdMobView;
	/** The {@link AdRequest}. */
	private AdRequest mAdMobRequest;
	/** MobFox's view. */
	private MobFoxView mMobFoxView;

	/** Keywords for AdMob. */
	private final Set<String> mKeywords;

	/** Hide me. */
	private boolean mHide = false;

	/** the {@link Looper}. */
	Looper refreshLooper;
	/** The {@link Handler}. */
	Handler refreshHandler;

	/**
	 * Default constructor.
	 * 
	 * @param pubIdAdMob
	 *            admob ad id
	 * @param pubIdMobFox
	 *            mobfox ad id
	 * @param activity
	 *            {@link Activity}
	 * @param resIdFlipper
	 *            resource id of the {@link ViewFlipper} holding the ads
	 * @param keywords
	 *            optional keywords for admob's ad requests
	 */
	public Ads(final String pubIdAdMob, final String pubIdMobFox, final Activity activity,
			final int resIdFlipper, final Set<String> keywords) {
		this.mAdMobPubId = pubIdAdMob;
		this.mMobFoxPubId = pubIdMobFox;
		this.mActivity = activity;
		this.mViewFlipper = (ViewFlipper) activity.findViewById(resIdFlipper);
		this.mKeywords = keywords;
	}

	/**
	 * Call this on {@link Activity}.onCreate().
	 */
	public void onCreate() {
		Log.d(TAG, "onCreate()");
		this.mHide = false;
		this.mAdMobView = new AdView(this.mActivity, AdSize.BANNER, this.mAdMobPubId);
		this.mAdMobView.setAdListener(this);
		this.mAdMobRequest = this.buildAdMobRequest();

		this.mMobFoxView = new MobFoxView(this.mActivity, this.mMobFoxPubId, Mode.LIVE, true, false);
		this.mMobFoxView.setBannerListener(this);

		this.mViewFlipper.addView(this.mMobFoxView);
		this.mViewFlipper.addView(this.mAdMobView);
		this.mViewFlipper.setAnimateFirstView(false);

		// Show MobFoxView
		this.mViewFlipper.setDisplayedChild(0);

		Thread refreshThread = new Thread() {
			@Override
			public void run() {

				Log.i(TAG, "Refresh Thread started");
				Looper.prepare();
				Ads.this.refreshLooper = Looper.myLooper();
				Ads.this.refreshHandler = new Handler(Ads.this.refreshLooper) {

					@Override
					public void handleMessage(final Message msg) {
						switch (msg.what) {
						case REFRESH_AD:
							Log.i(TAG, "Refresh Ad message received. Requesting ad from MobFox");
							Ads.this.mMobFoxView.loadNextAd();
							break;
						default:
							Log.w(TAG, "unknown msg.what: " + msg.what);
							break;
						}
					}
				};
				Looper.loop();
				Log.i(TAG, "Refresh Thread stopped");
			}
		};
		refreshThread.start();
	}

	/**
	 * Call this on {@link Activity}.onPause().
	 */
	public void onPause() {
		Log.d(TAG, "onPause()");
		if (this.refreshHandler != null) {
			this.refreshHandler.removeMessages(REFRESH_AD);
		}
		if (this.mAdMobView != null) {
			this.mAdMobView.stopLoading();
		}
		this.mMobFoxView.pause();
	}

	/**
	 * Call this on {@link Activity}.onResume().
	 */
	public void onResume() {
		Log.d(TAG, "onResume()");
		if (this.refreshHandler != null) {
			this.refreshHandler.removeMessages(REFRESH_AD);
			this.refreshHandler.sendEmptyMessage(REFRESH_AD);
		}
	}

	/**
	 * Call this on {@link Activity}.onDestroy().
	 */
	public void onDestroy() {
		Log.d(TAG, "onDestroy()");
		this.mHide = true;
		if (this.refreshLooper != null) {
			this.refreshLooper.quit();
			this.refreshLooper = null;
			this.refreshHandler = null;
		}
	}

	/**
	 * @return {@link AdRequest}
	 */
	private AdRequest buildAdMobRequest() {
		AdRequest adMobRequest = new AdRequest();
		try {
			LocationManager lm = (LocationManager) this.mActivity
					.getSystemService(Context.LOCATION_SERVICE);
			List<String> providers = lm.getProviders(true);
			if (providers != null && !providers.isEmpty()) {
				Location l = lm.getLastKnownLocation(providers.get(0));
				adMobRequest.setLocation(l);
			}
		} catch (Exception e) {
			Log.d(TAG, "error adding location: " + e.toString());
		}
		if (this.mKeywords != null) {
			adMobRequest.setKeywords(this.mKeywords);
		}
		// adMobRequest.setTesting(true);
		return adMobRequest;
	}

	@Override
	public void adClicked() {
		// TODO Auto-generated method stub

	}

	@Override
	public void bannerLoadFailed(final RequestException arg0) {
		try {
			this.mAdMobView.loadAd(this.mAdMobRequest);
		} catch (Exception ex) {
			if (this.refreshHandler != null) {
				this.refreshHandler.sendEmptyMessageDelayed(REFRESH_AD, REFRESH_INTERVAL);
			}
		}
	}

	@Override
	public void bannerLoadSucceeded() {
		this.mActivity.runOnUiThread(new Runnable() {
			public void run() {
				if (!Ads.this.mHide) {
					Ads.this.mViewFlipper.setVisibility(View.VISIBLE);
				}
				if (Ads.this.mViewFlipper.getCurrentView() != Ads.this.mMobFoxView) {
					Ads.this.mViewFlipper.setDisplayedChild(0);
				}
			}
		});
	}

	@Override
	public void noAdFound() {
		Log.d(TAG, "loading MobFox ad failed");
		try {
			this.mAdMobView.loadAd(this.mAdMobRequest);
		} catch (Exception ex) {
			if (this.refreshHandler != null) {
				this.refreshHandler.sendEmptyMessageDelayed(REFRESH_AD, REFRESH_INTERVAL);
			}
		}
	}

	@Override
	public void onDismissScreen(final Ad arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onFailedToReceiveAd(final Ad arg0, final ErrorCode arg1) {
		Log.d(TAG, "loading AdMob ad failed");
		if (this.refreshHandler != null) {
			this.refreshHandler.sendEmptyMessageDelayed(REFRESH_AD, REFRESH_INTERVAL);
		}
	}

	@Override
	public void onLeaveApplication(final Ad arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onPresentScreen(final Ad arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onReceiveAd(final Ad arg0) {
		this.mActivity.runOnUiThread(new Runnable() {
			public void run() {
				if (!Ads.this.mHide) {
					Ads.this.mViewFlipper.setVisibility(View.VISIBLE);
				}
				if (Ads.this.mViewFlipper.getCurrentView() != Ads.this.mAdMobView) {
					Ads.this.mViewFlipper.setDisplayedChild(1);
				}
			}
		});
		if (this.refreshHandler != null) {
			this.refreshHandler.sendEmptyMessageDelayed(REFRESH_AD, REFRESH_INTERVAL);
		}
	}
}
