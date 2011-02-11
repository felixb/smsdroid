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

import java.util.Calendar;
import java.util.List;

import android.app.Activity;
import android.app.Dialog;
import android.app.ListActivity;
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
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import de.ub0r.android.lib.Changelog;
import de.ub0r.android.lib.DonationHelper;
import de.ub0r.android.lib.Log;
import de.ub0r.android.lib.Utils;
import de.ub0r.android.lib.apis.Contact;
import de.ub0r.android.lib.apis.ContactsWrapper;

/**
 * Main {@link ListActivity} showing conversations.
 * 
 * @author flx
 */
public final class ConversationList extends ListActivity implements
		OnItemClickListener, OnItemLongClickListener, OnClickListener,
		OnLongClickListener {
	/** Tag for output. */
	public static final String TAG = "main";

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
	/** Show emoticons in {@link MessageList}. */
	public static boolean showEmoticons = false;

	/** Dialog items shown if an item was long clicked. */
	private String[] longItemClickDialog = null;

	/** Conversations. */
	private ConversationAdapter adapter = null;

	/** {@link ProgressBar} in title bar. */
	ProgressBar pbProgress = null;

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
		Log.d(TAG, "got intent: " + i.getAction());
		Log.d(TAG, "got uri: " + i.getData());
		final Bundle b = i.getExtras();
		Log.d(TAG, "user_query: " + b.get("user_query"));
		Log.d(TAG, "got extra: " + b);
		final String query = i.getStringExtra("user_query");
		Log.d(TAG, "user query: " + query);
		// TODO: do something with search query
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		final Intent i = this.getIntent();
		Log.d(TAG, "got intent: " + i.getAction());
		Log.d(TAG, "got uri: " + i.getData());
		Log.d(TAG, "got extra: " + i.getExtras());
		final SharedPreferences p = PreferenceManager
				.getDefaultSharedPreferences(this);
		final boolean showTitlebar = p.getBoolean(
				Preferences.PREFS_SHOWTITLEBAR, true);

		this.setTheme(Preferences.getTheme(this));
		Utils.setLocale(this);
		this.setContentView(R.layout.conversationlist);
		if (!showTitlebar) {
			this.findViewById(R.id.titlebar).setVisibility(View.GONE);
		}
		this.pbProgress = (ProgressBar) this.findViewById(R.id.progess);
		final ImageView iv = (ImageView) this.findViewById(R.id.compose);
		iv.setOnClickListener(this);
		iv.setOnLongClickListener(this);

		Changelog.showChangelog(this);
		final List<ResolveInfo> ri = this.getPackageManager()
				.queryBroadcastReceivers(
						new Intent("de.ub0r.android.websms.connector.INFO"), 0);
		if (ri.size() == 0) {
			final Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(// .
					"market://details?id=de.ub0r.android.websms"));
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
		this.longItemClickDialog[WHICH_ANSWER] = this
				.getString(R.string.answer);
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
			this.findViewById(R.id.ad).setVisibility(View.VISIBLE);
		}
		CAL_TODAY.setTimeInMillis(System.currentTimeMillis());
		CAL_TODAY.set(Calendar.HOUR_OF_DAY, 0);
		CAL_TODAY.set(Calendar.MINUTE, 0);
		CAL_TODAY.set(Calendar.SECOND, 0);
		CAL_TODAY.set(Calendar.MILLISECOND, 0);

		final SharedPreferences p = PreferenceManager
				.getDefaultSharedPreferences(this);
		showContactPhoto = p.getBoolean(Preferences.PREFS_CONTACT_PHOTO, false);
		showEmoticons = p.getBoolean(Preferences.PREFS_EMOTICONS, false);
		this.adapter.startMsgListQuery();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean onCreateOptionsMenu(final Menu menu) {
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
		cr.update(uri, cv, Message.SELECTION_READ_UNREAD, sel);
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
	 * @param message
	 *            message of the {@link Dialog}
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
			this.onClick(this.findViewById(R.id.compose));
			return true;
		case R.id.item_settings: // start settings activity
			this.startActivity(new Intent(this, Preferences.class));
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
			return false;
		}
	}

	/**
	 * Get a {@link Intent} for sending a new message.
	 * 
	 * @param address
	 *            address
	 * @return {@link Intent}
	 */
	static Intent getComposeIntent(final String address) {
		final Intent i = new Intent(Intent.ACTION_SENDTO);
		i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
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
	public void onItemClick(final AdapterView<?> parent, final View view,
			final int position, final long id) {
		final Conversation c = Conversation.getConversation(this,
				(Cursor) parent.getItemAtPosition(position), false);
		final Uri target = c.getUri();
		final Intent i = new Intent(this, MessageList.class);
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
					ConversationList.this.startActivity(getComposeIntent(a));
					break;
				case WHICH_CALL:
					i = new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + a));
					ConversationList.this.startActivity(i);
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
					ConversationList.this.startActivity(i);
					break;
				case WHICH_VIEW:
					i = new Intent(ConversationList.this, // .
							MessageList.class);
					i.setData(target);
					ConversationList.this.startActivity(i);
					break;
				case WHICH_DELETE:
					ConversationList.deleteMessages(ConversationList.this,
							target, R.string.delete_thread_,
							R.string.delete_thread_question, null);
					break;
				case WHICH_MARK_SPAM:
					ConversationList.addToOrRemoveFromSpamlist(
							ConversationList.this, c.getContact().getNumber());
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
				Preferences.PREFS_FULL_DATE, false)) {
			return DateFormat.getTimeFormat(context).format(t) + " "
					+ DateFormat.getDateFormat(context).format(t);
		} else if (t < CAL_TODAY.getTimeInMillis()) {
			return DateFormat.getDateFormat(context).format(t);
		} else {
			return DateFormat.getTimeFormat(context).format(t);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onClick(final View v) {
		switch (v.getId()) {
		case R.id.compose:
			final Intent i = getComposeIntent(null);
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
			break;
		default:
			break;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean onLongClick(final View v) {
		switch (v.getId()) {
		case R.id.compose:
			final Intent i = getComposeIntent(null);
			this.startActivity(Intent.createChooser(i, this
					.getString(R.string.new_message_)));
			return true;
		default:
			return false;
		}
	}
}
