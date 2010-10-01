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

import android.app.ListActivity;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.CallLog.Calls;
import android.text.ClipboardManager;
import android.text.format.DateFormat;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import de.ub0r.android.lib.Log;
import de.ub0r.android.lib.Utils;
import de.ub0r.android.lib.apis.ContactsWrapper;

/**
 * {@link ListActivity} showing a single conversation.
 * 
 * @author flx
 */
public class MessageList extends ListActivity implements OnItemClickListener,
		OnItemLongClickListener {
	/** Tag for output. */
	private static final String TAG = "ml";

	/** Number of items. */
	private static final int WHICH_N = 5;
	// private static final int WHICH_N = 6;
	/** Index in dialog: mark read/unread. */
	private static final int WHICH_MARK_UNREAD = 0;
	/** Index in dialog: forward. */
	private static final int WHICH_FORWARD = 1;
	/** Index in dialog: copy text. */
	private static final int WHICH_COPY_TEXT = 2;
	/** Index in dialog: view details. */
	private static final int WHICH_VIEW_DETAILS = 3;
	/** Index in dialog: delete. */
	private static final int WHICH_DELETE = 4;
	/** Index in dialog: speak. */
	private static final int WHICH_SPEAK = 5;

	/** Used {@link Uri}. */
	private Uri uri;
	/** {@link Conversation} shown. */
	private Conversation conv = null;

	/** ORIG_URI to resolve. */
	static final String URI = "content://mms-sms/conversations/";

	/** Dialog items shown if an item was long clicked. */
	private String[] longItemClickDialog = null;

	/** Sort list upside down. */
	private boolean sortUSD = true;

	/** Current FooterView. */
	private View currentHeader = null;

	/** Marked a message unread? */
	private boolean markedUnread = false;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		final SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(this);
		final boolean showTitlebar = prefs.getBoolean(
				Preferences.PREFS_SHOWTITLEBAR, true);
		if (!showTitlebar) {
			this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		}
		this.setTheme(Preferences.getTheme(this));
		Utils.setLocale(this);
		this.setContentView(R.layout.messagelist);
		Log.d(TAG, "onCreate()");

		this.parseIntent(this.getIntent());

		final ListView list = this.getListView();
		list.setOnItemLongClickListener(this);
		list.setOnItemClickListener(this);
		this.longItemClickDialog = new String[WHICH_N];
		this.longItemClickDialog[WHICH_MARK_UNREAD] = this
				.getString(R.string.mark_unread_);
		this.longItemClickDialog[WHICH_FORWARD] = this
				.getString(R.string.forward_);
		this.longItemClickDialog[WHICH_COPY_TEXT] = this
				.getString(R.string.copy_text_);
		this.longItemClickDialog[WHICH_VIEW_DETAILS] = this
				.getString(R.string.view_details_);
		this.longItemClickDialog[WHICH_DELETE] = this
				.getString(R.string.delete_message_);
		// this.longItemClickDialog[WHICH_SPEAK] =
		// this.getString(R.string.speak_);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected final void onNewIntent(final Intent intent) {
		super.onNewIntent(intent);
		this.parseIntent(intent);
	}

	/**
	 * Parse data pushed by {@link Intent}.
	 * 
	 * @param intent
	 *            {@link Intent}
	 */
	private void parseIntent(final Intent intent) {
		Log.d(TAG, "parseIntent(" + intent + ")");
		if (intent == null) {
			return;
		}
		Log.d(TAG, "got action: " + intent.getAction());
		Log.d(TAG, "got uri: " + intent.getData());

		this.uri = intent.getData();
		if (this.uri != null) {
			if (!this.uri.toString().startsWith(URI)) {
				this.uri = Uri.parse(URI + this.uri.getLastPathSegment());
			}
		} else {
			final long tid = intent.getLongExtra("thread_id", -1L);
			this.uri = Uri.parse(URI + tid);
			if (tid < 0L) {
				this.startActivity(ConversationList.getComposeIntent(null));
				this.finish();
				return;
			}
		}

		final int threadId = Integer.parseInt(this.uri.getLastPathSegment());
		this.conv = Conversation.getConversation(this, threadId, true);

		if (this.conv == null) {
			Toast.makeText(this, R.string.error_conv_null, Toast.LENGTH_LONG)
					.show();
			this.finish();
			return;
		}

		Log.d(TAG, "address: " + this.conv.getAddress());
		Log.d(TAG, "name: " + this.conv.getName());
		Log.d(TAG, "displayName: " + this.conv.getDisplayName());

		final ListView lv = this.getListView();
		final View header = View.inflate(this, R.layout.newmessage_item, null);
		((TextView) header.findViewById(R.id.text1)).setText(R.string.answer);
		final SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(this);
		final boolean showPhoto = prefs.getBoolean(
				Preferences.PREFS_CONTACT_PHOTO, false);
		if (showPhoto) {
			final ImageView iv = (ImageView) header.findViewById(R.id.photo);
			Bitmap photo = this.conv.getPhoto();
			if (photo == null) {
				AsyncHelper.fillConversation(this, this.conv, true);
				photo = this.conv.getPhoto();
			}
			if (photo != null && photo != Conversation.NO_PHOTO) {
				iv.setImageBitmap(photo);
				iv.setVisibility(View.VISIBLE);
			} else {
				iv.setVisibility(View.GONE);
			}
			iv
					.setOnClickListener(ContactsWrapper.getInstance()
							.getQuickContact(this, iv, this.conv.getAddress(),
									2, null));
		} else {
			header.findViewById(R.id.photo).setVisibility(View.GONE);
		}
		if (this.currentHeader != null) {
			lv.removeFooterView(this.currentHeader);
		}
		lv.addFooterView(header);
		this.currentHeader = header;
		lv.setStackFromBottom(true);

		MessageAdapter adapter = new MessageAdapter(this, this.uri);
		this.setListAdapter(adapter);

		this.setTitle(this.getString(R.string.app_name) + " > "
				+ this.conv.getDisplayName());
		String str;
		final String name = this.conv.getName();
		if (name == null) {
			str = this.conv.getAddress();
		} else {
			str = name + " <" + this.conv.getAddress() + ">";
		}
		((TextView) header.findViewById(R.id.text2)).setText(str);
		this.setRead();
	}

	/**
	 * Set selection to "answer" button.
	 */
	private void scrollToLastMessage() {
		final ListView lv = this.getListView();
		lv.setAdapter(new MessageAdapter(this, this.uri));
		lv.setSelection(lv.getCount() - 1);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final void onConfigurationChanged(final Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		this.scrollToLastMessage();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected final void onResume() {
		super.onResume();
		this.markedUnread = false;
		this.scrollToLastMessage();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected final void onPause() {
		super.onPause();
		if (!this.markedUnread) {
			this.setRead();
		}
	}

	/**
	 * Set all messages in a given thread as read.
	 */
	private void setRead() {
		if (this.conv != null) {
			ConversationList.markRead(this, this.conv.getUri(), 1);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final boolean onCreateOptionsMenu(final Menu menu) {
		MenuInflater inflater = this.getMenuInflater();
		inflater.inflate(R.menu.messagelist, menu);
		return true;
	}

	/**
	 *{@inheritDoc}
	 */
	@Override
	public final boolean onOptionsItemSelected(final MenuItem item) {
		switch (item.getItemId()) {
		case R.id.item_delete_thread:
			ConversationList.deleteMessages(this, this.uri,
					R.string.delete_thread_, R.string.delete_thread_question,
					this);
			return true;
		case R.id.item_all_threads:
			this.startActivity(new Intent(this, ConversationList.class));
			return true;
		case R.id.item_compose:
			this.startActivity(ConversationList.getComposeIntent(null));
			return true;
		default:
			return false;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public final void onItemClick(final AdapterView<?> parent, final View view,
			final int position, final long id) {
		int headerPos = 0;
		if (this.sortUSD) {
			headerPos = parent.getAdapter().getCount() - 1;
		}
		Log.d(TAG, "pos: " + position + " / header: " + headerPos);
		if (position == headerPos) { // header
			this.startActivity(ConversationList.getComposeIntent(this.conv
					.getAddress()));
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public final boolean onItemLongClick(final AdapterView<?> parent,
			final View view, final int position, final long id) {
		int headerPos = 0;
		if (this.sortUSD) {
			headerPos = parent.getAdapter().getCount() - 1;
		}
		Log.d(TAG, "pos: " + position + " / header: " + headerPos);
		if (position == headerPos) { // header
			final Intent i = ConversationList.getComposeIntent(this.conv
					.getAddress());
			this.startActivity(Intent.createChooser(i, this
					.getString(R.string.answer)));
			return true;
		} else {
			final Context context = this;
			final Message m = Message.getMessage(this, (Cursor) parent
					.getItemAtPosition(position));
			final Uri target = m.getUri();
			final int read = m.getRead();
			Builder builder = new Builder(context);
			builder.setTitle(R.string.message_options_);
			String[] items = this.longItemClickDialog;
			if (read == 0) {
				items = items.clone();
				items[WHICH_MARK_UNREAD] = context
						.getString(R.string.mark_read_);
			}
			builder.setItems(items, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(final DialogInterface dialog,
						final int which) {
					switch (which) {
					case WHICH_MARK_UNREAD:
						ConversationList.markRead(context, target, 1 - read);
						MessageList.this.markedUnread = true;
						break;
					case WHICH_FORWARD:
						final Intent i = new Intent(Intent.ACTION_SEND);
						i.setType("text/plain");
						i.putExtra(Intent.EXTRA_TEXT, m.getBody());
						context.startActivity(Intent.createChooser(i, context
								.getString(R.string.forward_)));
						break;
					case WHICH_COPY_TEXT:
						final ClipboardManager cm = // .
						(ClipboardManager) context.getSystemService(// .
								Context.CLIPBOARD_SERVICE);
						cm.setText(m.getBody());
						break;
					case WHICH_VIEW_DETAILS:
						final int t = m.getType();
						Builder b = new Builder(context);
						b.setTitle(R.string.view_details_);
						b.setCancelable(true);
						StringBuilder sb = new StringBuilder();
						final String a = m.getAddress(context);
						final long d = m.getDate();
						final String ds = DateFormat.format(// .
								context.getString(// .
										R.string.DATEFORMAT_details), d)
								.toString();
						String sentReceived;
						String fromTo;
						if (t == Calls.INCOMING_TYPE) {
							sentReceived = context
									.getString(R.string.received_);
							fromTo = context.getString(R.string.from_);
						} else if (t == Calls.OUTGOING_TYPE) {
							sentReceived = context.getString(R.string.sent_);
							fromTo = context.getString(R.string.to_);
						} else {
							sentReceived = "ukwn:";
							fromTo = "ukwn:";
						}
						sb.append(sentReceived + " ");
						sb.append(ds);
						sb.append("\n");
						sb.append(fromTo + " ");
						sb.append(a);
						sb.append("\n");
						sb.append(context.getString(R.string.type_));
						if (m.isMMS()) {
							sb.append(" MMS");
						} else {
							sb.append(" SMS");
						}
						b.setMessage(sb.toString());
						b.setPositiveButton(android.R.string.ok, null);
						b.show();
						break;
					case WHICH_DELETE:
						ConversationList.deleteMessages(context, target,
								R.string.delete_message_,
								R.string.delete_message_question, null);
						break;
					case WHICH_SPEAK:
						// TODO: implement me
						Toast.makeText(context, R.string.not_implemented,
								Toast.LENGTH_SHORT).show();
						break;
					default:
						break;
					}
				}
			});
			builder.show();
			return true;
		}
	}
}
