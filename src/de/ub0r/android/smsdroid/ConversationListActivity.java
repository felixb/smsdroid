/*
 * Copyright (C) 2010-2011 Felix Bechstein
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

import java.util.Calendar;
import java.util.HashSet;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog.Builder;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.Menu;
import android.support.v4.view.MenuItem;
import android.support.v4.view.Window;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import de.ub0r.android.lib.Changelog;
import de.ub0r.android.lib.DonationHelper;
import de.ub0r.android.lib.Log;
import de.ub0r.android.lib.Market;
import de.ub0r.android.lib.Utils;
import de.ub0r.android.lib.apis.Contact;
import de.ub0r.android.lib.apis.ContactsWrapper;

/**
 * Main {@link FragmentActivity} showing conversations.
 * 
 * @author flx
 */
public final class ConversationListActivity extends FragmentActivity implements
		OnItemClickListener, OnItemLongClickListener {
	/** Tag for output. */
	public static final String TAG = "main";

	/** Ad's unit id. */
	private static final String AD_UNITID = "a14b9f701ee348f";

	/** Ad's keywords. */
	public static final HashSet<String> AD_KEYWORDS = new HashSet<String>();
	static {
		AD_KEYWORDS.add("android");
		AD_KEYWORDS.add("mobile");
		AD_KEYWORDS.add("handy");
		AD_KEYWORDS.add("cellphone");
		AD_KEYWORDS.add("google");
		AD_KEYWORDS.add("htc");
		AD_KEYWORDS.add("samsung");
		AD_KEYWORDS.add("motorola");
		AD_KEYWORDS.add("market");
		AD_KEYWORDS.add("app");
		AD_KEYWORDS.add("message");
		AD_KEYWORDS.add("txt");
		AD_KEYWORDS.add("sms");
		AD_KEYWORDS.add("mms");
		AD_KEYWORDS.add("game");
		AD_KEYWORDS.add("websms");
		AD_KEYWORDS.add("amazon");
	}

	/** ORIG_URI to resolve. */
	static final Uri URI = Uri.parse("content://mms-sms/conversations/");

	/** Number of items. */
	private static final int WHICH_N = 6;
	/** Index in dialog: answer. */
	private static final int WHICH_ANSWER = 0;
	/** Index in dialog: answer. */
	private static final int WHICH_CALL = 1;
	/** Index in dialog: view/add contact. */
	private static final int WHICH_VIEW_CONTACT = 2;
	/** Index in dialog: view. */
	private static final int WHICH_VIEW = 3;
	/** Index in dialog: delete. */
	private static final int WHICH_DELETE = 4;
	/** Index in dialog: mark as spam. */
	private static final int WHICH_MARK_SPAM = 5;

	/** Preferences: hide ads. */
	private static boolean prefsNoAds = false;

	/** Minimum date. */
	public static final long MIN_DATE = 10000000000L;
	/** Miliseconds per seconds. */
	public static final long MILLIS = 1000L;

	/** Show contact's photo. */
	public static boolean showContactPhoto = false;
	/** Show emoticons in {@link MessageListActivity}. */
	public static boolean showEmoticons = false;

	/** Dialog items shown if an item was long clicked. */
	private String[] longItemClickDialog = null;

	/** Conversations. */
	private ConversationAdapter adapter = null;

	/** {@link Calendar} holding today 00:00. */
	private static final Calendar CAL_TODAY = Calendar.getInstance();
	static {
		CAL_TODAY.set(Calendar.HOUR_OF_DAY, 0);
		CAL_TODAY.set(Calendar.MINUTE, 0);
		CAL_TODAY.set(Calendar.SECOND, 0);
		CAL_TODAY.set(Calendar.MILLISECOND, 0);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onStart() {
		super.onStart();
		AsyncHelper.setAdapter(this.adapter);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onStop() {
		super.onStop();
		AsyncHelper.setAdapter(null);
	}

	/**
	 * Get {@link ListView}.
	 * 
	 * @return {@link ListView}
	 */
	private ListView getListView() {
		return (ListView) this.findViewById(android.R.id.list);
	}

	/**
	 * Set {@link ListAdapter} to {@link ListView}.
	 * 
	 * @param la
	 *            ListAdapter
	 */
	private void setListAdapter(final ListAdapter la) {
		this.getListView().setAdapter(la);
	}

	/**
	 * Show all rows of a particular {@link Uri}.
	 * 
	 * @param context
	 *            {@link Context}
	 * @param u
	 *            {@link Uri}
	 */
	static void showRows(final Context context, final Uri u) {
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
		}

	}

	/**
	 * Show rows for debugging purposes.
	 * 
	 * @param context
	 *            {@link Context}
	 */
	static void showRows(final Context context) {
		// this.showRows(ContactsWrapper.getInstance().getUriFilter());
		// showRows(context, URI);
		// showRows(context, Uri.parse("content://sms/"));
		// showRows(context, Uri.parse("content://mms/"));
		// showRows(context, Uri.parse("content://mms/part/"));
		// showRows(context, ConversationProvider.CONTENT_URI);
		// showRows(context, Uri.parse("content://mms-sms/threads"));
		// this.showRows(Uri.parse(MessageList.URI));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onNewIntent(final Intent intent) {
		final Intent i = intent;
		if (i != null) {
			Log.d(TAG, "got intent: " + i.getAction());
			Log.d(TAG, "got uri: " + i.getData());
			final Bundle b = i.getExtras();
			if (b != null) {
				Log.d(TAG, "user_query: " + b.get("user_query"));
				Log.d(TAG, "got extra: " + b);
			}
			final String query = i.getStringExtra("user_query");
			Log.d(TAG, "user query: " + query);
			// TODO: do something with search query
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		final Intent i = this.getIntent();
		Log.d(TAG, "got intent: " + i.getAction());
		Log.d(TAG, "got uri: " + i.getData());
		Log.d(TAG, "got extra: " + i.getExtras());

		this.setTheme(PreferencesActivity.getTheme(this));
		Utils.setLocale(this);
		this.setContentView(R.layout.conversationlist);

		Changelog.showChangelog(this);
		final List<ResolveInfo> ri = this.getPackageManager()
				.queryBroadcastReceivers(
						new Intent("de.ub0r.android.websms.connector.INFO"), 0);
		if (ri.size() == 0) {
			final Intent intent = Market.getInstallAppIntent(this,
					"de.ub0r.android.websms", Market.ALT_WEBSMS);
			Changelog.showNotes(this, "get WebSMS", null, intent);
		} else {
			Changelog.showNotes(this, null, null, null);
		}

		showRows(this);

		final ListView list = this.getListView();
		this.adapter = new ConversationAdapter(this);
		this.setListAdapter(this.adapter);
		list.setOnItemClickListener(this);
		list.setOnItemLongClickListener(this);
		this.longItemClickDialog = new String[WHICH_N];
		this.longItemClickDialog[WHICH_ANSWER] = this.getString(R.string.reply);
		this.longItemClickDialog[WHICH_CALL] = this.getString(R.string.call);
		this.longItemClickDialog[WHICH_VIEW_CONTACT] = this
				.getString(R.string.view_contact_);
		this.longItemClickDialog[WHICH_VIEW] = this
				.getString(R.string.view_thread_);
		this.longItemClickDialog[WHICH_DELETE] = this
				.getString(R.string.delete_thread_);
		this.longItemClickDialog[WHICH_MARK_SPAM] = this
				.getString(R.string.filter_spam_);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onResume() {
		super.onResume();
		prefsNoAds = DonationHelper.hideAds(this);
		if (!prefsNoAds) {
			Ads.loadAd(this, R.id.ad, AD_UNITID, AD_KEYWORDS);
		}
		CAL_TODAY.setTimeInMillis(System.currentTimeMillis());
		CAL_TODAY.set(Calendar.HOUR_OF_DAY, 0);
		CAL_TODAY.set(Calendar.MINUTE, 0);
		CAL_TODAY.set(Calendar.SECOND, 0);
		CAL_TODAY.set(Calendar.MILLISECOND, 0);

		final SharedPreferences p = PreferenceManager
				.getDefaultSharedPreferences(this);
		showContactPhoto = p.getBoolean(
				PreferencesActivity.PREFS_CONTACT_PHOTO, false);
		showEmoticons = p
				.getBoolean(PreferencesActivity.PREFS_EMOTICONS, false);
		this.adapter.startMsgListQuery();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean onCreateOptionsMenu(final Menu menu) {
		this.getMenuInflater().inflate(R.menu.conversationlist, menu);
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
	static void markRead(final Context context, final Uri uri, final int read) {
		Log.d(TAG, "markRead(" + uri + "," + read + ")");
		if (uri == null) {
			return;
		}
		String[] sel = Message.SELECTION_UNREAD;
		if (read == 0) {
			sel = Message.SELECTION_READ;
		}
		final ContentResolver cr = context.getContentResolver();
		final ContentValues cv = new ContentValues();
		cv.put(Message.PROJECTION[Message.INDEX_READ], read);
		try {
			cr.update(uri, cv, Message.SELECTION_READ_UNREAD, sel);
		} catch (IllegalArgumentException e) {
			Log.e(TAG, "failed update", e);
			Toast.makeText(context, e.getMessage(), Toast.LENGTH_LONG).show();
		}
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
	 *            title of Dialog
	 * @param message
	 *            message of the Dialog
	 * @param activity
	 *            {@link Activity} to finish when deleting.
	 */
	static void deleteMessages(final Context context, final Uri uri,
			final int title, final int message, final Activity activity) {
		Log.i(TAG, "deleteMessages(..," + uri + " ,..)");
		final Builder builder = new Builder(context);
		builder.setTitle(title);
		builder.setMessage(message);
		builder.setNegativeButton(android.R.string.no, null);
		builder.setPositiveButton(android.R.string.yes,
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(final DialogInterface dialog,
							final int which) {
						final int ret = context.getContentResolver().delete(
								uri, null, null);
						Log.d(TAG, "deleted: " + ret);
						if (activity != null && !activity.isFinishing()) {
							activity.finish();
						}
						if (ret > 0) {
							Conversation.flushCache();
							Message.flushCache();
							SmsReceiver.updateNewMessageNotification(context,
									null);
						}
					}
				});
		builder.show();
	}

	/**
	 * Add or remove an entry to/from blacklist.
	 * 
	 * @param context
	 *            {@link Context}
	 * @param addr
	 *            address
	 */
	private static void addToOrRemoveFromSpamlist(final Context context,
			final String addr) {
		final SpamDB db = new SpamDB(context);
		db.open();
		if (!db.isInDB(addr)) {
			db.insertNr(addr);
			Log.d(TAG, "Added " + addr + " to spam list");
		} else {
			db.removeNr(addr);
			Log.d(TAG, "Removed " + addr + " from spam list");
		}
		db.close();
	}

	/**
	 *{@inheritDoc}
	 */
	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		switch (item.getItemId()) {
		case R.id.item_compose:
			final Intent i = getComposeIntent(this, null);
			try {
				this.startActivity(i);
			} catch (ActivityNotFoundException e) {
				Log.e(TAG, "error launching intent: " + i.getAction() + ", "
						+ i.getData());
				Toast.makeText(
						this,
						"error launching messaging app!\n"
								+ "Please contact the developer.",
						Toast.LENGTH_LONG).show();
			}
			return true;
		case R.id.item_settings: // start settings activity
			if (Utils.isApi(Build.VERSION_CODES.HONEYCOMB)) {
				this
						.startActivity(new Intent(this,
								Preferences11Activity.class));
			} else {
				this.startActivity(new Intent(this, PreferencesActivity.class));
			}
			return true;
		case R.id.item_donate:
			this.startActivity(new Intent(this, DonationHelper.class));
			return true;
		case R.id.item_delete_all_threads:
			deleteMessages(this, Uri.parse("content://sms/"),
					R.string.delete_threads_, R.string.delete_threads_question,
					null);
			return true;
		case R.id.item_mark_all_read:
			markRead(this, Uri.parse("content://sms/"), 1);
			markRead(this, Uri.parse("content://mms/"), 1);
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	/**
	 * Get a {@link Intent} for sending a new message.
	 * 
	 * @param context
	 *            {@link Context}
	 * @param address
	 *            address
	 * @return {@link Intent}
	 */
	static Intent getComposeIntent(final Context context, // .
			final String address) {
		final Intent i = new Intent(Intent.ACTION_SENDTO);
		i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		if (address == null) {
			i.setData(Uri.parse("sms:"));
		} else {
			i.setData(Uri.parse("smsto:"
					+ PreferencesActivity.fixNumber(context, address)));
		}
		return i;
	}

	/**
	 * {@inheritDoc}
	 */
	public void onItemClick(final AdapterView<?> parent, final View view,
			final int position, final long id) {
		final Conversation c = Conversation.getConversation(this,
				(Cursor) parent.getItemAtPosition(position), false);
		final Uri target = c.getUri();
		final Intent i = new Intent(this, MessageListActivity.class);
		i.setData(target);
		try {
			this.startActivity(i);
		} catch (ActivityNotFoundException e) {
			Log.e(TAG, "error launching intent: " + i.getAction() + ", "
					+ i.getData());
			Toast.makeText(
					this,
					"error launching messaging app!\n"
							+ "Please contact the developer.",
					Toast.LENGTH_LONG).show();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean onItemLongClick(final AdapterView<?> parent,
			final View view, final int position, final long id) {
		final Conversation c = Conversation.getConversation(this,
				(Cursor) parent.getItemAtPosition(position), true);
		final Uri target = c.getUri();
		Builder builder = new Builder(this);
		String[] items = this.longItemClickDialog;
		final Contact contact = c.getContact();
		final String a = contact.getNumber();
		Log.d(TAG, "p: " + a);
		final String n = contact.getName();
		if (TextUtils.isEmpty(n)) {
			builder.setTitle(a);
			items = items.clone();
			items[WHICH_VIEW_CONTACT] = this.getString(R.string.add_contact_);
		} else {
			builder.setTitle(n);
		}
		final SpamDB db = new SpamDB(this.getApplicationContext());
		db.open();
		if (db.isInDB(a)) {
			items = items.clone();
			items[WHICH_MARK_SPAM] = this.getString(R.string.dont_filter_spam_);
		}
		db.close();
		builder.setItems(items, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(final DialogInterface dialog, final int which) {
				Intent i = null;
				switch (which) {
				case WHICH_ANSWER:
					ConversationListActivity.this
							.startActivity(getComposeIntent(
									ConversationListActivity.this, a));
					break;
				case WHICH_CALL:
					i = new Intent(Intent.ACTION_VIEW, Uri.parse("tel:" + a));
					ConversationListActivity.this.startActivity(i);
					break;
				case WHICH_VIEW_CONTACT:
					if (n == null) {
						i = ContactsWrapper.getInstance()
								.getInsertPickIntent(a);
						Conversation.flushCache();
					} else {
						final Uri uri = c.getContact().getUri();
						i = new Intent(Intent.ACTION_VIEW, uri);
					}
					ConversationListActivity.this.startActivity(i);
					break;
				case WHICH_VIEW:
					i = new Intent(ConversationListActivity.this, // .
							MessageListActivity.class);
					i.setData(target);
					ConversationListActivity.this.startActivity(i);
					break;
				case WHICH_DELETE:
					ConversationListActivity.deleteMessages(
							ConversationListActivity.this, target,
							R.string.delete_thread_,
							R.string.delete_thread_question, null);
					break;
				case WHICH_MARK_SPAM:
					ConversationListActivity.addToOrRemoveFromSpamlist(
							ConversationListActivity.this, c.getContact()
									.getNumber());
					break;
				default:
					break;
				}
			}
		});
		builder.create().show();
		return true;
	}

	/**
	 * Convert time into formated date.
	 * 
	 * @param context
	 *            {@link Context}
	 * @param time
	 *            time
	 * @return formated date.
	 */
	static String getDate(final Context context, final long time) {
		long t = time;
		if (t < MIN_DATE) {
			t *= MILLIS;
		}
		if (PreferenceManager.getDefaultSharedPreferences(context).getBoolean(
				PreferencesActivity.PREFS_FULL_DATE, false)) {
			return DateFormat.getTimeFormat(context).format(t) + " "
					+ DateFormat.getDateFormat(context).format(t);
		} else if (t < CAL_TODAY.getTimeInMillis()) {
			return DateFormat.getDateFormat(context).format(t);
		} else {
			return DateFormat.getTimeFormat(context).format(t);
		}
	}
}
