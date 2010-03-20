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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;

import android.app.Dialog;
import android.app.ListActivity;
import android.app.AlertDialog.Builder;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;

/**
 * Main {@link ListActivity} showing conversations.
 * 
 * @author flx
 */
public class SMSdroid extends ListActivity implements OnClickListener {
	/** Tag for output. */
	private static final String TAG = "SMSdroid";

	/** Prefs: name for last version run. */
	private static final String PREFS_LAST_RUN = "lastrun";

	/** URI to resolve. */
	static final Uri URI = Uri.parse("content://mms-sms/conversations/");

	/** Dialog: about. */
	private static final int DIALOG_ABOUT = 0;
	/** Dialog: updates. */
	private static final int DIALOG_UPDATE = 1;
	/** Dialog: pre donate. */
	private static final int DIALOG_PREDONATE = 2;
	/** Dialog: post donate. */
	private static final int DIALOG_POSTDONATE = 3;

	/** Preferences: hide ads. */
	static boolean prefsNoAds = false;
	/** Hased IMEI. */
	private static String imeiHash = null;
	/** Crypto algorithm for signing UID hashs. */
	private static final String ALGO = "RSA";
	/** Crypto hash algorithm for signing UID hashs. */
	private static final String SIGALGO = "SHA1with" + ALGO;
	/** My public key for verifying UID hashs. */
	private static final String KEY = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNAD"
			+ "CBiQKBgQCgnfT4bRMLOv3rV8tpjcEqsNmC1OJaaEYRaTHOCC"
			+ "F4sCIZ3pEfDcNmrZZQc9Y0im351ekKOzUzlLLoG09bsaOeMd"
			+ "Y89+o2O0mW9NnBch3l8K/uJ3FRn+8Li75SqoTqFj3yCrd9IT"
			+ "sOJC7PxcR5TvNpeXsogcyxxo3fMdJdjkafYwIDAQAB";
	/** Path to file containing signatures of UID Hash. */
	private static final String NOADS_SIGNATURES = "/sdcard/websms.noads";

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.conversationlist);

		SharedPreferences preferences = PreferenceManager
				.getDefaultSharedPreferences(this);
		// display changelog?
		String v0 = preferences.getString(PREFS_LAST_RUN, "");
		String v1 = this.getString(R.string.app_version);
		if (!v0.equals(v1)) {
			SharedPreferences.Editor editor = preferences.edit();
			editor.putString(PREFS_LAST_RUN, v1);
			editor.commit();
			this.showDialog(DIALOG_UPDATE);
		}

		Cursor mCursor = this.getContentResolver().query(URI,
				ConversationListAdapter.PROJECTION, null, null,
				ConversationListAdapter.SORT);
		this.startManagingCursor(mCursor);
		ConversationListAdapter adapter = new ConversationListAdapter(this,
				mCursor);
		this.setListAdapter(adapter);
		this.findViewById(R.id.new_message).setOnClickListener(this);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected final void onResume() {
		super.onResume();
		prefsNoAds = this.hideAds();
		if (!prefsNoAds) {
			this.findViewById(R.id.ad).setVisibility(View.VISIBLE);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected final Dialog onCreateDialog(final int id) {
		Dialog d;
		Builder builder;
		switch (id) {
		case DIALOG_ABOUT:
			d = new Dialog(this);
			d.setContentView(R.layout.about);
			d.setTitle(this.getString(R.string.about_) + " v"
					+ this.getString(R.string.app_version));
			return d;
		case DIALOG_UPDATE:
			builder = new Builder(this);
			builder.setTitle(R.string.changelog_);
			final String[] changes = this.getResources().getStringArray(
					R.array.updates);
			final StringBuilder buf = new StringBuilder(changes[0]);
			for (int i = 1; i < changes.length; i++) {
				buf.append("\n\n");
				buf.append(changes[i]);
			}
			builder.setIcon(android.R.drawable.ic_menu_info_details);
			builder.setMessage(buf.toString());
			builder.setCancelable(true);
			builder.setPositiveButton(android.R.string.ok, null);
			return builder.create();
		case DIALOG_PREDONATE:
			builder = new Builder(this);
			builder.setIcon(R.drawable.ic_menu_star);
			builder.setTitle(R.string.donate_);
			builder.setMessage(R.string.predonate);
			builder.setPositiveButton(R.string.donate_,
					new DialogInterface.OnClickListener() {
						public void onClick(final DialogInterface dialog,
								final int which) {
							try {
								SMSdroid.this.startActivity(new Intent(
										Intent.ACTION_VIEW, Uri.parse(// .
												SMSdroid.this.getString(// .
														R.string.donate_url))));
							} catch (ActivityNotFoundException e) {
								Log.e(TAG, "no browser", e);
							} finally {
								SMSdroid.this.showDialog(DIALOG_POSTDONATE);
							}
						}
					});
			builder.setNegativeButton(android.R.string.cancel, null);
			return builder.create();
		case DIALOG_POSTDONATE:
			builder = new Builder(this);
			builder.setIcon(R.drawable.ic_menu_star);
			builder.setTitle(R.string.remove_ads_);
			builder.setMessage(R.string.postdonate);
			builder.setPositiveButton(R.string.send_,
					new DialogInterface.OnClickListener() {
						public void onClick(final DialogInterface dialog,
								final int which) {
							final Intent in = new Intent(Intent.ACTION_SEND);
							in.putExtra(Intent.EXTRA_EMAIL, new String[] {
									SMSdroid.this.getString(// .
											R.string.donate_mail), "" });
							// FIXME: "" is a k9 hack. This is fixed in market
							// on 26.01.10. wait some more time..
							in.putExtra(Intent.EXTRA_TEXT, SMSdroid.this
									.getImeiHash());
							in.putExtra(Intent.EXTRA_SUBJECT, SMSdroid.this
									.getString(// .
									R.string.app_name)
									+ " " + SMSdroid.this.getString(// .
											R.string.donate_subject));
							in.setType("text/plain");
							SMSdroid.this.startActivity(in);
						}
					});
			builder.setNegativeButton(android.R.string.cancel, null);
			return builder.create();
		default:
			return null;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final boolean onCreateOptionsMenu(final Menu menu) {
		MenuInflater inflater = this.getMenuInflater();
		inflater.inflate(R.menu.menu, menu);
		if (prefsNoAds) {
			menu.removeItem(R.id.item_donate);
		}
		return true;
	}

	/**
	 *{@inheritDoc}
	 */
	@Override
	public final boolean onOptionsItemSelected(final MenuItem item) {
		switch (item.getItemId()) {
		case R.id.item_about: // start about dialog
			this.showDialog(DIALOG_ABOUT);
			return true;
		case R.id.item_settings: // start settings activity
			this.startActivity(new Intent(this, Preferences.class));
			return true;
		case R.id.item_donate:
			this.showDialog(DIALOG_PREDONATE);
			return true;
		case R.id.item_more:
			try {
				this.startActivity(new Intent(Intent.ACTION_VIEW, Uri
						.parse("market://search?q=pub:\"Felix Bechstein\"")));
			} catch (ActivityNotFoundException e) {
				Log.e(TAG, "no market", e);
			}
			return true;
		default:
			return false;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final void onClick(final View v) {
		switch (v.getId()) {
		case R.id.new_message:
			try {
				final Intent i = new Intent(Intent.ACTION_SENDTO);
				i.setData(Uri.parse("sms:"));
				this.startActivity(i);
			} catch (ActivityNotFoundException e) {
				Log.e(TAG, "could not find app to compose message", e);
			}
			break;
		default:
			break;
		}
	}

	/**
	 * Check for signature updates.
	 * 
	 * @return true if ads should be hidden
	 */
	private boolean hideAds() {
		final SharedPreferences p = PreferenceManager
				.getDefaultSharedPreferences(this);
		final File f = new File(NOADS_SIGNATURES);
		try {
			if (f.exists()) {
				final BufferedReader br = new BufferedReader(new FileReader(f));
				final byte[] publicKey = Base64Coder.decode(KEY);
				final KeyFactory keyFactory = KeyFactory.getInstance(ALGO);
				PublicKey pk = keyFactory
						.generatePublic(new X509EncodedKeySpec(publicKey));
				final String h = this.getImeiHash();
				boolean ret = false;
				while (true) {
					String l = br.readLine();
					if (l == null) {
						break;
					}
					try {
						byte[] signature = Base64Coder.decode(l);
						Signature sig = Signature.getInstance(SIGALGO);
						sig.initVerify(pk);
						sig.update(h.getBytes());
						ret = sig.verify(signature);
						if (ret) {
							break;
						}
					} catch (IllegalArgumentException e) {
						Log.w(TAG, "error reading line", e);
					}
				}
				br.close();
				f.delete();
				p.edit().putBoolean(Preferences.PREFS_HIDEADS, ret).commit();
			}
		} catch (Exception e) {
			Log.e(TAG, "error reading signatures", e);
		}
		return p.getBoolean(Preferences.PREFS_HIDEADS, false);
	}

	/**
	 * Get MD5 hash of the IMEI (device id).
	 * 
	 * @return MD5 hash of IMEI
	 */
	private String getImeiHash() {
		if (imeiHash == null) {
			// get imei
			TelephonyManager mTelephonyMgr = (TelephonyManager) this
					.getSystemService(TELEPHONY_SERVICE);
			final String did = mTelephonyMgr.getDeviceId();
			if (did != null) {
				imeiHash = md5(did);
			}
		}
		return imeiHash;
	}

	/**
	 * Calculate MD5 Hash from String.
	 * 
	 * @param s
	 *            input
	 * @return hash
	 */
	private static String md5(final String s) {
		try {
			// Create MD5 Hash
			MessageDigest digest = java.security.MessageDigest
					.getInstance("MD5");
			digest.update(s.getBytes());
			byte[] messageDigest = digest.digest();
			// Create Hex String
			StringBuilder hexString = new StringBuilder(32);
			int b;
			for (int i = 0; i < messageDigest.length; i++) {
				b = 0xFF & messageDigest[i];
				if (b < 0x10) {
					hexString.append('0' + Integer.toHexString(b));
				} else {
					hexString.append(Integer.toHexString(b));
				}
			}
			return hexString.toString();
		} catch (NoSuchAlgorithmException e) {
			Log.e(TAG, null, e);
		}
		return "";
	}
}
