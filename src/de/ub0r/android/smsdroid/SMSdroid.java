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
import java.util.HashMap;
import java.util.List;

import android.app.Dialog;
import android.app.ListActivity;
import android.app.AlertDialog.Builder;
import android.content.ActivityNotFoundException;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;

import com.flurry.android.FlurryAgent;

/**
 * Main {@link ListActivity} showing conversations.
 * 
 * @author flx
 */
public class SMSdroid extends ListActivity implements OnItemClickListener,
		OnItemLongClickListener {
	/** Tag for output. */
	private static final String TAG = "SMSdroid";

	/** Flurry's API key. */
	public static final String FLURRYKEY = "Q63FP3PT9LWWR3GJXVG8";

	/** Prefs: name for last version run. */
	private static final String PREFS_LAST_RUN = "lastrun";

	/** URI to resolve. */
	static final Uri URI = Uri.parse("content://mms-sms/conversations/");

	/** Dialog: updates. */
	private static final int DIALOG_UPDATE = 1;
	/** Dialog: pre donate. */
	private static final int DIALOG_PREDONATE = 2;
	/** Dialog: post donate. */
	private static final int DIALOG_POSTDONATE = 3;

	/** Number of items. */
	private static final int WHICH_N = 4;
	/** Index in dialog: answer. */
	private static final int WHICH_ANSWER = 0;
	/** Index in dialog: view/add contact. */
	private static final int WHICH_VIEW_CONTACT = 1;
	/** Index in dialog: view. */
	private static final int WHICH_VIEW = 2;
	/** Index in dialog: delete. */
	private static final int WHICH_DELETE = 3;

	/** Preferences: hide ads. */
	private static boolean prefsNoAds = false;
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

	/** Minimum date. */
	public static final long MIN_DATE = 10000000000L;
	/** Miliseconds per seconds. */
	public static final long MILLIS = 1000L;

	/** Show contact's photo. */
	static boolean showContactPhoto = false;

	/** Dialog items shown if an item was long clicked. */
	private String[] longItemClickDialog = null;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final void onStart() {
		super.onStart();
		FlurryAgent.onStartSession(this, FLURRYKEY);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final void onStop() {
		super.onStop();
		FlurryAgent.onEndSession(this);
	}

	/**
	 * Show all rows of a particular {@link Uri}.
	 * 
	 * @param context
	 *            {@link Context}
	 * @param u
	 *            {@link Uri}
	 */
	static final void showRows(final Context context, final Uri u) {
		Log.d(TAG, "-----GET HEADERS-----");
		Log.d(TAG, "-- " + u.toString() + " --");
		Cursor c = context.getContentResolver()
				.query(u, null, null, null, null);
		if (c != null) {
			int l = c.getColumnCount();
			StringBuilder buf = new StringBuilder();
			for (int i = 0; i < l; i++) {
				buf.append(i + ":");
				buf.append(c.getColumnName(i));
				buf.append(" | ");
			}
			Log.d(TAG, buf.toString());
			if (c.moveToFirst()) {
				do {
					buf = new StringBuilder();
					for (int i = 0; i < l; i++) {
						buf.append(i + ":");
						buf.append(c.getString(i));
						buf.append(" | ");
					}
					Log.d(TAG, buf.toString());
				} while (c.moveToNext());
				c.close();
			}
		}
		Log.d(TAG, "-*---GET HEADERS---*-");

	}

	/**
	 * Show rows for debugging purposes.
	 * 
	 * @param context
	 *            {@link Context}
	 */
	static final void showRows(final Context context) {
		// this.showRows(ContactsWrapper.getInstance().getUriFilter());
		showRows(context, URI);
		showRows(context, Uri.parse("content://sms/"));
		showRows(context, Uri.parse("content://mms/"));
		// showRows(context, Uri.parse("content://mms-sms/threads"));
		// this.showRows(Uri.parse(MessageList.URI));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setTheme(Preferences.getTheme(this));
		this.setContentView(R.layout.conversationlist);

		final SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(this);
		// display changelog?
		String v0 = prefs.getString(PREFS_LAST_RUN, "");
		String v1 = this.getString(R.string.app_version);
		if (!v0.equals(v1)) {
			SharedPreferences.Editor editor = prefs.edit();
			editor.putString(PREFS_LAST_RUN, v1);
			editor.commit();
			this.showDialog(DIALOG_UPDATE);
		}

		showRows(this);

		showContactPhoto = prefs.getBoolean(Preferences.PREFS_CONTACT_PHOTO,
				false);
		final ListView list = this.getListView();
		final View header = View.inflate(this, R.layout.newmessage_item, null);
		list.addHeaderView(header);
		ConversationsAdapter adapter = new ConversationsAdapter(this);
		this.setListAdapter(adapter);
		list.setOnItemClickListener(this);
		list.setOnItemLongClickListener(this);
		this.longItemClickDialog = new String[WHICH_N];
		this.longItemClickDialog[WHICH_ANSWER] = this
				.getString(R.string.answer);
		this.longItemClickDialog[WHICH_VIEW_CONTACT] = this
				.getString(R.string.view_contact_);
		this.longItemClickDialog[WHICH_VIEW] = this
				.getString(R.string.view_thread_);
		this.longItemClickDialog[WHICH_DELETE] = this
				.getString(R.string.delete_thread_);
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
		Builder builder;
		switch (id) {
		case DIALOG_UPDATE:
			builder = new Builder(this);
			builder.setTitle(R.string.changelog_);
			final String[] changes = this.getResources().getStringArray(
					R.array.updates);
			final StringBuilder buf = new StringBuilder();
			final List<ResolveInfo> ri = this
					.getPackageManager()
					.queryBroadcastReceivers(
							new Intent("de.ub0r.android.websms.connector.INFO"),
							0);
			if (ri.size() == 0) {
				buf.append(changes[0]);
				builder.setNeutralButton("get WebSMS",
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(final DialogInterface d,
									final int which) {
								try {
									SMSdroid.this.startActivity(// .
											new Intent(
													Intent.ACTION_VIEW,
													Uri.parse(// .
															"market://search?q=pname:de.ub0r.android.websms")));
								} catch (ActivityNotFoundException e) {
									Log.e(TAG, "no market", e);
								}
							}
						});
			}
			for (int i = 1; i < changes.length; i++) {
				buf.append("\n\n");
				buf.append(changes[i]);
			}
			builder.setIcon(android.R.drawable.ic_menu_info_details);
			builder.setMessage(buf.toString().trim());
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
		inflater.inflate(R.menu.conversationlist, menu);
		if (prefsNoAds) {
			menu.removeItem(R.id.item_donate);
		}
		return true;
	}

	/**
	 * Mark all messages with a given {@link Uri} as read.
	 * 
	 * @param context
	 *            {@link Context}
	 * @param uri
	 *            {@link Uri}
	 * @param read
	 *            read status
	 */
	static final void markRead(final Context context, final Uri uri,
			final int read) {
		String select = Message.SELECTION_UNREAD.replace("0", String
				.valueOf(1 - read));
		final Cursor mCursor = context.getContentResolver().query(uri,
				Message.PROJECTION, select, null, null);
		if (mCursor.getCount() <= 0) {
			if (uri.toString().equals("content://sms/")) {
				SmsReceiver.updateNewMessageNotification(context, null);
			}
			return;
		}

		final ContentValues cv = new ContentValues();
		cv.put(Message.PROJECTION[Message.INDEX_READ], read);
		context.getContentResolver().update(uri, cv, select, null);
		SmsReceiver.updateNewMessageNotification(context, null);
	}

	/**
	 * Delete messages with a given {@link Uri}.
	 * 
	 * @param context
	 *            {@link Context}
	 * @param uri
	 *            {@link Uri}
	 * @param title
	 *            title of {@link Dialog}
	 */
	static final void deleteMessages(final Context context, final Uri uri,
			final int title) {
		final Cursor mCursor = context.getContentResolver().query(uri,
				Message.PROJECTION, null, null, null);
		if (mCursor.getCount() <= 0) {
			return;
		}

		Builder builder = new Builder(context);
		builder.setTitle(title);
		builder.setMessage(R.string.delete_messages_question);
		builder.setNegativeButton(android.R.string.no, null);
		builder.setPositiveButton(android.R.string.yes,
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(final DialogInterface dialog,
							final int which) {
						context.getContentResolver().delete(uri, null, null);
						SmsReceiver.updateNewMessageNotification(context, null);
					}
				});
		builder.create().show();
	}

	/**
	 *{@inheritDoc}
	 */
	@Override
	public final boolean onOptionsItemSelected(final MenuItem item) {
		switch (item.getItemId()) {
		case R.id.item_settings: // start settings activity
			this.startActivity(new Intent(this, Preferences.class));
			return true;
		case R.id.item_donate:
			this.showDialog(DIALOG_PREDONATE);
			return true;
		case R.id.item_delete_all_threads:
			deleteMessages(this, Uri.parse("content://sms/"),
					R.string.delete_threads_);
			return true;
		case R.id.item_mark_all_read:
			markRead(this, Uri.parse("content://sms/"), 1);
			return true;
		default:
			return false;
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
		final boolean ret = p.getBoolean(Preferences.PREFS_HIDEADS, false);
		if (ret != prefsNoAds) {
			final HashMap<String, String> params = new HashMap<String, String>();
			params.put("value", String.valueOf(ret));
			FlurryAgent.onEvent("switch prefsNoAds", params);
		}
		return ret;
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

	/**
	 * Get a {@link Intent} for sending a new message.
	 * 
	 * @param address
	 *            address
	 * @return {@link Intent}
	 */
	static final Intent getComposeIntent(final String address) {
		final Intent i = new Intent(Intent.ACTION_SENDTO);
		if (address == null) {
			i.setData(Uri.parse("sms:"));
		} else {
			i.setData(Uri.parse("smsto:" + address));
		}
		return i;
	}

	/**
	 * {@inheritDoc}
	 */
	public final void onItemClick(final AdapterView<?> parent, final View view,
			final int position, final long id) {
		if (position == 0) { // header
			this.startActivity(getComposeIntent(null));
		} else {
			final Conversation c = (Conversation) parent
					.getItemAtPosition(position);
			final Uri target = Uri.parse(MessageList.URI + c.getThreadId());
			final Intent i = new Intent(this, MessageList.class);
			i.setData(target);
			this.startActivity(i);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public final boolean onItemLongClick(final AdapterView<?> parent,
			final View view, final int position, final long id) {
		if (position == 0) { // header
			final Intent i = getComposeIntent(null);
			this.startActivity(Intent.createChooser(i, this
					.getString(R.string.new_message)));
			return true;
		} else {
			final Conversation c = (Conversation) parent
					.getItemAtPosition(position);
			final Uri target = Uri.parse(MessageList.URI + c.getThreadId());
			Builder builder = new Builder(this);
			String[] items = this.longItemClickDialog;
			final String a = c.getAddress(this);
			Log.d(TAG, "p: " + a);
			final String n = CachePersons.getName(this, a, null);
			if (n == null) {
				builder.setTitle(a);
				items = items.clone();
				items[WHICH_VIEW_CONTACT] = this
						.getString(R.string.add_contact_);
			} else {
				builder.setTitle(n);
			}
			builder.setItems(items, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(final DialogInterface dialog,
						final int which) {
					Intent i = null;
					switch (which) {
					case WHICH_ANSWER:
						SMSdroid.this.startActivity(getComposeIntent(a));
						break;
					case WHICH_VIEW_CONTACT:
						if (n == null) {
							i = ContactsWrapper.getInstance()
									.getInsertPickIntent(a);
						} else {
							final Uri uri = ContactsWrapper.getInstance()
									.getContactUri(
											CachePersons
													.getID(SMSdroid.this, a));
							i = new Intent(Intent.ACTION_VIEW, uri);
						}
						SMSdroid.this.startActivity(i);
						break;
					case WHICH_VIEW:
						i = new Intent(SMSdroid.this, MessageList.class);
						i.setData(target);
						SMSdroid.this.startActivity(i);
						break;
					case WHICH_DELETE:
						SMSdroid.deleteMessages(SMSdroid.this, target,
								R.string.delete_thread_);
						break;
					default:
						break;
					}
				}
			});
			builder.create().show();
		}
		return true;
	}

	/**
	 * Convert time into formated date.
	 * 
	 * @param format
	 *            format
	 * @param time
	 *            time
	 * @return formated date.
	 */
	static final String getDate(final String format, final long time) {
		long t = time;
		if (t < MIN_DATE) {
			t *= MILLIS;
		}
		return (String) DateFormat.format(format, t);
	}
}
