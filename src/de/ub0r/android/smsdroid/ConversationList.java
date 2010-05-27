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
import android.text.format.DateFormat;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import de.ub0r.android.lib.DonationHelper;
import de.ub0r.android.lib.Log;
import de.ub0r.android.lib.apis.ContactsWrapper;

/**
 * Main {@link ListActivity} showing conversations.
 * 
 * @author flx
 */
public class ConversationList extends ListActivity implements
		OnItemClickListener, OnItemLongClickListener {
	/** Tag for output. */
	public static final String TAG = "main";

	/** Prefs: name for last version run. */
	private static final String PREFS_LAST_RUN = "lastrun";

	/** ORIG_URI to resolve. */
	static final Uri URI = Uri.parse("content://mms-sms/conversations/");

	/** Dialog: updates. */
	private static final int DIALOG_UPDATE = 1;

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
	/** Path to file containing signatures of UID Hash. */
	private static final String NOADS_SIGNATURES = "/sdcard/websms.noads";

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
	private ConversationsAdapter adapter = null;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final void onStart() {
		super.onStart();
		AsyncHelper.setAdapter(this.adapter);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final void onStop() {
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
		}

	}

	/**
	 * Show rows for debugging purposes.
	 * 
	 * @param context
	 *            {@link Context}
	 */
	static final void showRows(final Context context) {
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
	public final void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
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
		showEmoticons = prefs.getBoolean(Preferences.PREFS_EMOTICONS, false);

		final ListView list = this.getListView();
		final View header = View.inflate(this, R.layout.newmessage_item, null);
		list.addHeaderView(header);
		this.adapter = new ConversationsAdapter(this);
		this.setListAdapter(this.adapter);
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
		prefsNoAds = DonationHelper.hideAds(this);
		if (!prefsNoAds) {
			this.findViewById(R.id.ad).setVisibility(View.VISIBLE);
		}
		this.adapter.startMsgListQuery();
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
									ConversationList.this.startActivity(// .
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
				Message.PROJECTION_READ, select, null, null);
		if (mCursor.getCount() <= 0) {
			if (uri.toString().equals("content://sms/")) {
				SmsReceiver.updateNewMessageNotification(context, null);
			} else if (uri.toString().equals("content://mms/")) {
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
	 * @param message
	 *            message of the {@link Dialog}
	 * @param activity
	 *            {@link Activity} to finish when deleting.
	 */
	static final void deleteMessages(final Context context, final Uri uri,
			final int title, final int message, final Activity activity) {
		final Cursor mCursor = context.getContentResolver().query(uri,
				Message.PROJECTION, null, null, null);
		if (mCursor.getCount() <= 0) {
			return;
		}

		Builder builder = new Builder(context);
		builder.setTitle(title);
		builder.setMessage(message);
		builder.setNegativeButton(android.R.string.no, null);
		builder.setPositiveButton(android.R.string.yes,
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(final DialogInterface dialog,
							final int which) {
						context.getContentResolver().delete(uri, null, null);
						if (activity != null) {
							activity.finish();
						}
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
			final Conversation c = Conversation.getConversation(this,
					(Cursor) parent.getItemAtPosition(position), false);
			final Uri target = c.getUri();
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
			final Conversation c = Conversation.getConversation(this,
					(Cursor) parent.getItemAtPosition(position), true);
			final Uri target = c.getUri();
			Builder builder = new Builder(this);
			String[] items = this.longItemClickDialog;
			final String a = c.getAddress();
			Log.d(TAG, "p: " + a);
			final String n = AsyncHelper.getContactName(this, a);
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
						ConversationList.this
								.startActivity(getComposeIntent(a));
						break;
					case WHICH_VIEW_CONTACT:
						if (n == null) {
							i = ContactsWrapper.getInstance()
									.getInsertPickIntent(a);
						} else {
							final Uri uri = ContactsWrapper.getInstance()
									.getContactUri(
											ConversationList.this
													.getContentResolver(),
											c.getPersonId());
							i = new Intent(Intent.ACTION_VIEW, uri);
						}
						ConversationList.this.startActivity(i);
						break;
					case WHICH_VIEW:
						i = new Intent(ConversationList.this, MessageList.class);
						i.setData(target);
						ConversationList.this.startActivity(i);
						break;
					case WHICH_DELETE:
						ConversationList.deleteMessages(ConversationList.this,
								target, R.string.delete_thread_,
								R.string.delete_thread_question, null);
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
	 * @param context
	 *            {@link Context}
	 * @param time
	 *            time
	 * @return formated date.
	 */
	static final String getDate(final Context context, final long time) {
		long t = time;
		if (t < MIN_DATE) {
			t *= MILLIS;
		}
		Calendar base = Calendar.getInstance();
		base.set(Calendar.HOUR_OF_DAY, 0);
		base.set(Calendar.MINUTE, 0);
		base.set(Calendar.SECOND, 0);
		if (t < base.getTimeInMillis()) {
			return DateFormat.getDateFormat(context).format(t);
		} else {
			return DateFormat.getTimeFormat(context).format(t);
		}
	}
}
