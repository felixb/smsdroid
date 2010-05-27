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
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.Toast;
import de.ub0r.android.lib.Log;

/**
 * Display send IMEI hash, read signature..
 * 
 * @author flx
 */
public class DonationHelper extends Activity implements OnClickListener {
	/** Tag for output. */
	private static final String TAG = "dh";

	/** Standard buffer size. */
	public static final int BUFSIZE = 512;

	/** Preference: paypal id. */
	private static final String PREFS_DONATEMAIL = "donate_mail";

	/** URL for checking hash. */
	private static final String URL = "http://nossl.ub0r.de/donation/";

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

	/** {@link EditText} for paypal id. */
	private EditText etPaypalId;

	/** Hashed IMEI. */
	private static String imeiHash = null;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.donation);

		this.findViewById(R.id.donate).setOnClickListener(this);
		this.findViewById(R.id.send).setOnClickListener(this);
		this.etPaypalId = (EditText) this.findViewById(R.id.paypalid);
		this.etPaypalId.setText(PreferenceManager.getDefaultSharedPreferences(
				this).getString(PREFS_DONATEMAIL, ""));
	}

	/**
	 * {@inheritDoc}
	 */
	public final void onClick(final View v) {
		switch (v.getId()) {
		case R.id.donate:
			this.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(this
					.getString(R.string.donate_url))));
			return;
		case R.id.send:
			if (this.etPaypalId.getText().toString().length() == 0) {
				Toast.makeText(this, R.string.paypal_id_, Toast.LENGTH_LONG)
						.show();
				return;
			}
			final SharedPreferences p = PreferenceManager
					.getDefaultSharedPreferences(this);
			p.edit().putString(PREFS_DONATEMAIL,
					this.etPaypalId.getText().toString()).commit();
			loadImeiHash(this, this.etPaypalId.getText().toString());
			return;
		default:
			return;
		}
	}

	/**
	 * Send a mail with user's IMEI hash.
	 * 
	 * @param activity
	 *            {@link Activity}
	 * @param paypalId
	 *            Paypal Id
	 */
	public static void loadImeiHash(final Activity activity, // .
			final String paypalId) {
		final HttpGet request = new HttpGet(URL
				+ "?mail="
				+ Uri.encode(paypalId)
				+ "&hash="
				+ getImeiHash(activity)
				+ "&lang="
				+ activity.getString(R.string.lang)
				+ "&app="
				+ activity.getString(R.string.app_name).replaceAll(" ", "")
						.toLowerCase());
		try {
			final HttpResponse response = new DefaultHttpClient()
					.execute(request);
			int resp = response.getStatusLine().getStatusCode();
			if (resp != 200) {
				Toast.makeText(activity,
						"Service is down. Retry later. Returncode: " + resp,
						Toast.LENGTH_LONG).show();
				return;
			}
			final BufferedReader bufferedReader = new BufferedReader(
					new InputStreamReader(response.getEntity().getContent()),
					BUFSIZE);
			final String line = bufferedReader.readLine();
			final boolean ret = checkSig(activity, line);
			final SharedPreferences prefs = PreferenceManager
					.getDefaultSharedPreferences(activity);
			prefs.edit().putBoolean(Preferences.PREFS_HIDEADS, ret).commit();

			int text = R.string.sig_loaded;
			if (!ret) {
				text = R.string.sig_failed;
			}
			Toast.makeText(activity, text, Toast.LENGTH_LONG).show();
			if (ret) {
				activity.finish();
			} else {
				Toast.makeText(activity, line, Toast.LENGTH_LONG).show();
			}
		} catch (ClientProtocolException e) {
			Log.e(TAG, "error loading sig", e);
			Toast.makeText(activity, e.getMessage(), Toast.LENGTH_LONG).show();
		} catch (IOException e) {
			Log.e(TAG, "error loading sig", e);
			Toast.makeText(activity, e.getMessage(), Toast.LENGTH_LONG).show();
		}
	}

	/**
	 * Get MD5 hash of the IMEI (device id).
	 * 
	 * @param context
	 *            {@link Context}
	 * @return MD5 hash of IMEI
	 */
	public static String getImeiHash(final Context context) {
		if (imeiHash == null) {
			// get imei
			TelephonyManager mTelephonyMgr = (TelephonyManager) context
					.getSystemService(TELEPHONY_SERVICE);
			final String did = mTelephonyMgr.getDeviceId();
			if (did != null) {
				imeiHash = md5(did);
			}
		}
		return imeiHash;
	}

	/**
	 * Check for signature updates.
	 * 
	 * @param context
	 *            {@link Context}
	 * @param s
	 *            signature
	 * @return true if ads should be hidden
	 */
	public static boolean checkSig(final Context context, final String s) {
		Log.d(TAG, "checkSig(ctx, " + s + ")");
		boolean ret = false;
		try {
			final byte[] publicKey = Base64Coder.decode(KEY);
			final KeyFactory keyFactory = KeyFactory.getInstance(ALGO);
			PublicKey pk = keyFactory.generatePublic(new X509EncodedKeySpec(
					publicKey));
			final String h = getImeiHash(context);
			Log.d(TAG, "hash: " + h);
			final String cs = s.replaceAll(" |\n|\t", "");
			Log.d(TAG, "read sig: " + cs);
			try {
				byte[] signature = Base64Coder.decode(cs);
				Signature sig = Signature.getInstance(SIGALGO);
				sig.initVerify(pk);
				sig.update(h.getBytes());
				ret = sig.verify(signature);
				Log.d(TAG, "ret: " + ret);
			} catch (IllegalArgumentException e) {
				Log.w(TAG, "error reading signature", e);
			}
		} catch (Exception e) {
			Log.e(TAG, "error reading signatures", e);
		}
		return ret;
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
