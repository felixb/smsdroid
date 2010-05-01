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
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.CallLog.Calls;
import android.text.ClipboardManager;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;

import com.flurry.android.FlurryAgent;

import de.ub0r.android.smsdroid.cache.Persons;

/**
 * {@link ListActivity} showing a single conversation.
 * 
 * @author flx
 */
public class MessageList extends ListActivity implements OnItemClickListener,
		OnItemLongClickListener {
	/** Tag for output. */
	private static final String TAG = "SMSdroid.ml";

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
	/** Thread id. */
	private int threadId = -1;
	/** Address. */
	private String address = null;
	/** Name. */
	private String name = null;
	/** Display Name (name if !=null, else address). */
	private String displayName = null;

	/** ORIG_URI to resolve. */
	static final String URI = "content://mms-sms/conversations/";

	/** Dialog items shown if an item was long clicked. */
	private String[] longItemClickDialog = null;

	/** Sort list upside down. */
	private boolean sortUSD = true;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final void onStart() {
		super.onStart();
		FlurryAgent.onStartSession(this, SMSdroid.FLURRYKEY);
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
	 * {@inheritDoc}
	 */
	@Override
	public final void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setTheme(Preferences.getTheme(this));
		this.setContentView(R.layout.messagelist);
		Log.d(TAG, "onCreate()");

		final Intent i = this.getIntent();
		this.uri = i.getData();
		if (this.uri != null) {
			if (!this.uri.toString().startsWith(URI)) {
				this.uri = Uri.parse(URI + this.uri.getLastPathSegment());
			}
			this.parseIntent(i);
		}
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
		this.uri = intent.getData();
		if (this.uri != null) {
			this.parseIntent(intent);
		}
	}

	/**
	 * Parse data pushed by {@link Intent}.
	 * 
	 * @param intent
	 *            {@link Intent}
	 */
	private void parseIntent(final Intent intent) {
		Log.d(TAG, "got intent: " + this.uri.toString());

		this.threadId = Integer.parseInt(this.uri.getLastPathSegment());
		this.address = Conversation.getConversation(this.threadId).getAddress();
		this.name = Persons.getName(this, this.address, false);
		if (this.name == null) {
			this.displayName = this.address;
		} else {
			this.displayName = this.name;
		}
		Log.d(TAG, "address: " + this.address);
		Log.d(TAG, "name: " + this.name);
		Log.d(TAG, "displayName: " + this.displayName);

		final ListView lv = this.getListView();
		final View header = View.inflate(this, R.layout.newmessage_item, null);
		((TextView) header.findViewById(R.id.text1)).setText(R.string.answer);
		lv.addFooterView(header);
		lv.setStackFromBottom(true);

		MessagesAdapter adapter = new MessagesAdapter(this, this.uri);
		this.setListAdapter(adapter);

		this.setTitle(this.getString(R.string.app_name) + " > "
				+ this.displayName);
		String str;
		if (this.name == null) {
			str = this.address;
		} else {
			str = this.name + " <" + this.address + ">";
		}
		((TextView) header.findViewById(R.id.text2)).setText(str);
		this.setRead(this.threadId);
	}

	/**
	 * Set all messages in a given thread as read.
	 * 
	 * @param threadID
	 *            thread id
	 */
	private void setRead(final int threadID) {
		SMSdroid.markRead(this, Uri.parse(URI + threadID), 1);
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
			SMSdroid.deleteMessages(this, this.uri, R.string.delete_thread_);
			return true;
		case R.id.item_all_threads:
			this.startActivity(new Intent(this, SMSdroid.class));
			return true;
		case R.id.item_compose:
			this.startActivity(SMSdroid.getComposeIntent(null));
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
			this.startActivity(SMSdroid.getComposeIntent(this.address));
		} else {
			final Message m = new Message((Cursor) parent
					.getItemAtPosition(position));
			if (m.isMMS()) {
				final Uri target = Uri.parse(MessageList.URI + m.getThreadId());
				Intent i = new Intent(Intent.ACTION_VIEW, target);
				this.startActivity(Intent.createChooser(i, this
						.getString(R.string.view_mms)));
			}
			return;
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
			final Intent i = SMSdroid.getComposeIntent(this.address);
			this.startActivity(Intent.createChooser(i, this
					.getString(R.string.answer)));
			return true;
		} else {
			final Context context = this;
			final Message m = new Message((Cursor) parent
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
						SMSdroid.markRead(context, target, 1 - read);
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
						SMSdroid.deleteMessages(context, target,
								R.string.delete_message_);
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
