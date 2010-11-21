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
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.CallLog.Calls;
import android.text.ClipboardManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.format.DateFormat;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import de.ub0r.android.lib.Log;
import de.ub0r.android.lib.Utils;
import de.ub0r.android.lib.apis.TelephonyWrapper;
import de.ub0r.android.smsdroid.ConversationProvider.Messages;
import de.ub0r.android.smsdroid.ConversationProvider.Threads;

/**
 * {@link ListActivity} showing a single conversation.
 * 
 * @author flx
 */
public class MessageList extends ListActivity implements OnItemClickListener,
		OnItemLongClickListener, OnClickListener, OnLongClickListener {
	/** Tag for output. */
	private static final String TAG = "ml";
	/** {@link TelephonyWrapper}. */
	public static final TelephonyWrapper TWRAPPER = TelephonyWrapper
			.getInstance();

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

	/** Minimum length for showing sms length. */
	private final int TEXT_LABLE_MIN_LEN = 50;

	/** Used {@link Uri}. */
	private Uri uri;

	/** ORIG_URI to resolve. */
	static final String URI = "content://mms-sms/conversations/";

	/** Dialog items shown if an item was long clicked. */
	private String[] longItemClickDialog = null;

	/** Sort list upside down. */
	private boolean sortUSD = true;

	/** Marked a message unread? */
	private boolean markedUnread = false;

	/** {@link EditText} holding text. */
	private EditText etText;
	/** {@link TextView} for pasting text. */
	private TextView tvPaste;
	/** Text's label. */
	private TextView etTextLabel;
	/** {@link ClipboardManager}. */
	private ClipboardManager cbmgr;
	/** Address. */
	private String address;
	/** Name. */
	private String name;
	/** Thread's id. */
	private long threadId;

	/** TextWatcher updating char count on writing. */
	private TextWatcher textWatcher = new TextWatcher() {
		/**
		 * {@inheritDoc}
		 */
		public void afterTextChanged(final Editable s) {
			final int len = s.length();
			if (len == 0) {
				if (MessageList.this.cbmgr.hasText()) {
					MessageList.this.tvPaste.setVisibility(View.VISIBLE);
				} else {
					MessageList.this.tvPaste.setVisibility(View.GONE);
				}
				MessageList.this.etTextLabel.setVisibility(View.GONE);
			} else {
				MessageList.this.tvPaste.setVisibility(View.GONE);
				if (len > MessageList.this.TEXT_LABLE_MIN_LEN) {
					MessageList.this.etTextLabel.setVisibility(View.VISIBLE);
					int[] l = TWRAPPER.calculateLength(s.toString(), false);
					MessageList.this.etTextLabel.setText(l[0] + "/" + l[2]);
				} else {
					MessageList.this.etTextLabel.setVisibility(View.GONE);
				}
			}
		}

		/** Needed dummy. */
		public void beforeTextChanged(final CharSequence s, final int start,
				final int count, final int after) {
		}

		/** Needed dummy. */
		public void onTextChanged(final CharSequence s, final int start,
				final int before, final int count) {
		}
	};

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

		this.cbmgr = (ClipboardManager) this
				.getSystemService(CLIPBOARD_SERVICE);
		this.etText = (EditText) this.findViewById(R.id.text);
		this.etTextLabel = (TextView) this.findViewById(R.id.text_);
		this.tvPaste = (TextView) this.findViewById(R.id.text_paste);

		this.parseIntent(this.getIntent());

		final ListView list = this.getListView();
		list.setOnItemLongClickListener(this);
		list.setOnItemClickListener(this);
		View v = this.findViewById(R.id.send_);
		v.setOnClickListener(this);
		v.setOnLongClickListener(this);
		this.tvPaste.setOnClickListener(this);
		this.etText.addTextChangedListener(this.textWatcher);
		this.textWatcher.afterTextChanged(this.etText.getEditableText());

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
		this.threadId = intent.getLongExtra("thread_id", -1L);
		if (this.threadId < 0L && this.uri != null) {
			this.threadId = ContentUris.parseId(this.uri);
		}
		if (this.threadId < 0L) {
			this.startActivity(ConversationList.getComposeIntent(null));
			this.finish();
			return;
		}

		this.uri = ContentUris.withAppendedId(Messages.THREAD_URI,
				this.threadId);
		final Cursor ccursor = this.getContentResolver().query(
				ContentUris.withAppendedId(Threads.CONTENT_URI, this.threadId),
				Threads.PROJECTION, null, null, null);

		if (ccursor == null || !ccursor.moveToFirst()) {
			Toast.makeText(this, R.string.error_conv_null, Toast.LENGTH_LONG)
					.show();
			this.finish();
			return;
		}

		this.address = ccursor.getString(Threads.INDEX_ADDRESS);
		this.name = ccursor.getString(Threads.INDEX_NAME);
		final String displayName = ConversationProvider.getDisplayName(
				this.address, this.name, false);
		final String fullDisplayName = ConversationProvider.getDisplayName(
				this.address, this.name, true);

		Log.d(TAG, "address: " + this.address);
		Log.d(TAG, "name: " + this.name);
		Log.d(TAG, "displayName: " + displayName);
		Log.d(TAG, "fullDisplayName: " + fullDisplayName);

		final ListView lv = this.getListView();
		lv.setStackFromBottom(true);

		MessageAdapter adapter = new MessageAdapter(this, this.uri, ccursor);
		if (!ccursor.isClosed()) {
			ccursor.close();
		}
		this.setListAdapter(adapter);

		this.setTitle(this.getString(R.string.app_name) + " > "
				+ fullDisplayName);
		this.setRead();
	}

	/**
	 * Set selection to "answer" button.
	 */
	private void scrollToLastMessage() {
		final ListView lv = this.getListView();
		final Cursor ccursor = this.getContentResolver().query(
				ContentUris.withAppendedId(Threads.CONTENT_URI, this.threadId),
				Threads.PROJECTION, null, null, null);
		if (ccursor == null || !ccursor.moveToFirst()) {
			return;
		}
		lv.setAdapter(new MessageAdapter(this, this.uri, ccursor));
		if (!ccursor.isClosed()) {
			ccursor.close();
		}
		lv.setSelection(this.getListAdapter().getCount() - 1);
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
		ConversationList.markRead(this, ContentUris.withAppendedId(
				Messages.THREAD_URI, this.threadId), 1);
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
		case R.id.item_answer:
			this.startActivity(ConversationList.getComposeIntent(this.address));
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
		this.onItemLongClick(parent, view, position, id);
	}

	/**
	 * {@inheritDoc}
	 */
	public final boolean onItemLongClick(final AdapterView<?> parent,
			final View view, final int position, final long id) {
		final Context context = this;
		final Cursor currentCursor = (Cursor) parent
				.getItemAtPosition(position);
		final Uri target = ContentUris.withAppendedId(Messages.CONTENT_URI, id);
		final int read = currentCursor.getInt(Messages.INDEX_READ);
		final int type = currentCursor.getInt(Messages.INDEX_TYPE);
		Builder builder = new Builder(context);
		builder.setTitle(R.string.message_options_);
		String[] items = this.longItemClickDialog;
		if (read == 0) {
			items = items.clone();
			items[WHICH_MARK_UNREAD] = context.getString(R.string.mark_read_);
		}
		if (type == Messages.TYPE_SMS_DRAFT) {
			items = items.clone();
			items[WHICH_FORWARD] = context.getString(R.string.send_draft_);
		}
		builder.setItems(items, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(final DialogInterface dialog, final int which) {
				switch (which) {
				case WHICH_MARK_UNREAD:
					ConversationList.markRead(context, target, 1 - read);
					MessageList.this.markedUnread = true;
					break;
				case WHICH_FORWARD:
					Intent i;
					int resId;
					if (type == Messages.TYPE_SMS_DRAFT) {
						resId = R.string.send_draft_;
						i = ConversationList
								.getComposeIntent(MessageList.this.address);
					} else {
						resId = R.string.forward_;
						i = new Intent(Intent.ACTION_SEND);
						i.setType("text/plain");
					}
					final String text = currentCursor
							.getString(Messages.INDEX_BODY);
					i.putExtra(Intent.EXTRA_TEXT, text);
					i.putExtra("sms_body", text);
					context.startActivity(Intent.createChooser(i, context
							.getString(resId)));
					break;
				case WHICH_COPY_TEXT:
					final ClipboardManager cm = // .
					(ClipboardManager) context.getSystemService(// .
							Context.CLIPBOARD_SERVICE);
					cm.setText(currentCursor.getString(Messages.INDEX_BODY));
					break;
				case WHICH_VIEW_DETAILS:
					final int t = currentCursor.getInt(Messages.INDEX_TYPE);
					Builder b = new Builder(context);
					b.setTitle(R.string.view_details_);
					b.setCancelable(true);
					StringBuilder sb = new StringBuilder();
					final String a = currentCursor
							.getString(Messages.INDEX_ADDRESS);
					final long d = currentCursor.getLong(Messages.INDEX_DATE);
					final String ds = DateFormat.format(// .
							context.getString(// .
									R.string.DATEFORMAT_details), d).toString();
					String sentReceived;
					String fromTo;
					if (t == Calls.INCOMING_TYPE) {
						sentReceived = context.getString(R.string.received_);
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
					if (t > 100) {
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

	/**
	 * {@inheritDoc}
	 */
	public final void onClick(final View v) {
		switch (v.getId()) {
		case R.id.send_:
			final String text = this.etText.getText().toString().trim();
			if (text.length() == 0) {
				return;
			}
			final Intent i = ConversationList.getComposeIntent(this.address);
			i.putExtra(Intent.EXTRA_TEXT, text);
			i.putExtra("sms_body", text);
			i.putExtra("AUTOSEND", "1");
			this.startActivity(i);
			this.etText.setText("");
			return;
		case R.id.text_paste:
			final CharSequence s = this.cbmgr.getText();
			this.etText.setText(s);
			return;
		default:
			return;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public final boolean onLongClick(final View v) {
		switch (v.getId()) {
		case R.id.send_:
			final String text = this.etText.getText().toString().trim();
			if (text.length() == 0) {
				return true;
			}
			final Intent i = ConversationList.getComposeIntent(this.address);
			i.putExtra(Intent.EXTRA_TEXT, text);
			i.putExtra("sms_body", text);
			this.startActivity(Intent.createChooser(i, this
					.getString(R.string.answer)));
			this.etText.setText("");
			return true;
		default:
			return true;
		}
	}
}
