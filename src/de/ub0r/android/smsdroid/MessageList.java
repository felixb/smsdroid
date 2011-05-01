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

import android.app.ListActivity;
import android.app.AlertDialog.Builder;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.CallLog.Calls;
import android.text.ClipboardManager;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.format.DateFormat;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import de.ub0r.android.lib.Log;
import de.ub0r.android.lib.Utils;
import de.ub0r.android.lib.apis.Contact;
import de.ub0r.android.lib.apis.ContactsWrapper;
import de.ub0r.android.lib.apis.TelephonyWrapper;

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

	/** {@link ContactsWrapper}. */
	private static final ContactsWrapper WRAPPER = ContactsWrapper
			.getInstance();

	/** Number of items. */
	private static final int WHICH_N = 8;
	/** Index in dialog: mark view/add contact. */
	private static final int WHICH_VIEW_CONTACT = 0;
	/** Index in dialog: mark call contact. */
	private static final int WHICH_CALL = 1;
	/** Index in dialog: mark read/unread. */
	private static final int WHICH_MARK_UNREAD = 2;
	/** Index in dialog: reply. */
	private static final int WHICH_REPLY = 3;
	/** Index in dialog: forward. */
	private static final int WHICH_FORWARD = 4;
	/** Index in dialog: copy text. */
	private static final int WHICH_COPY_TEXT = 5;
	/** Index in dialog: view details. */
	private static final int WHICH_VIEW_DETAILS = 6;
	/** Index in dialog: delete. */
	private static final int WHICH_DELETE = 7;

	/** Minimum length for showing sms length. */
	private static final int TEXT_LABLE_MIN_LEN = 50;

	/** Package name for System's chooser. */
	private static String chooserPackage = null;

	/** Used {@link Uri}. */
	private Uri uri;
	/** {@link Conversation} shown. */
	private Conversation conv = null;

	/** ORIG_URI to resolve. */
	static final String URI = "content://mms-sms/conversations/";

	/** Dialog items shown if an item was long clicked. */
	private final String[] longItemClickDialog = new String[WHICH_N];

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

	/** Enable autosend. */
	private boolean enableAutosend = true;
	/** Show textfield. */
	private boolean showTextField = true;
	/** Show {@link Contact}'s photo. */
	private boolean showPhoto = false;

	/** Default {@link Drawable} for {@link Contact}s. */
	private Drawable defaultContactAvatar = null;

	/** TextWatcher updating char count on writing. */
	private TextWatcher textWatcher = new TextWatcher() {
		/**
		 * {@inheritDoc}
		 */
		public void afterTextChanged(final Editable s) {
			final int len = s.length();
			if (len == 0) {
				if (MessageList.this.cbmgr.hasText()
						&& !PreferenceManager.getDefaultSharedPreferences(
								MessageList.this).getBoolean(
								Preferences.PREFS_HIDE_PASTE, false)) {
					MessageList.this.tvPaste.setVisibility(View.VISIBLE);
				} else {
					MessageList.this.tvPaste.setVisibility(View.GONE);
				}
				MessageList.this.etTextLabel.setVisibility(View.GONE);
			} else {
				MessageList.this.tvPaste.setVisibility(View.GONE);
				if (len > MessageList.TEXT_LABLE_MIN_LEN) {
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
		final SharedPreferences p = PreferenceManager
				.getDefaultSharedPreferences(this);
		final boolean showTitlebar = p.getBoolean(
				Preferences.PREFS_SHOWTITLEBAR, true);
		this.enableAutosend = p.getBoolean(Preferences.PREFS_ENABLE_AUTOSEND,
				true);
		this.showTextField = this.enableAutosend
				|| p.getBoolean(Preferences.PREFS_SHOWTEXTFIELD, true);
		this.showPhoto = p.getBoolean(Preferences.PREFS_CONTACT_PHOTO, false);
		final boolean hideSend = p.getBoolean(Preferences.PREFS_HIDE_SEND,
				false);
		this.setTheme(Preferences.getTheme(this));
		Utils.setLocale(this);
		this.setContentView(R.layout.messagelist);
		if (!showTitlebar) {
			this.findViewById(R.id.titlebar).setVisibility(View.GONE);
		}
		Log.d(TAG, "onCreate()");

		if (this.showPhoto) {
			this.defaultContactAvatar = this.getResources().getDrawable(
					R.drawable.ic_contact_picture);
		}
		if (hideSend) {
			this.findViewById(R.id.send_).setVisibility(View.GONE);
		}

		this.cbmgr = (ClipboardManager) this
				.getSystemService(CLIPBOARD_SERVICE);
		this.etText = (EditText) this.findViewById(R.id.text);
		this.etTextLabel = (TextView) this.findViewById(R.id.text_);
		this.tvPaste = (TextView) this.findViewById(R.id.text_paste);

		if (!this.showTextField) {
			this.findViewById(R.id.text_layout).setVisibility(View.GONE);
		}

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

		this.longItemClickDialog[WHICH_MARK_UNREAD] = this
				.getString(R.string.mark_unread_);
		this.longItemClickDialog[WHICH_REPLY] = this.getString(R.string.reply);
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
				try {
					this.startActivity(ConversationList.getComposeIntent(null));
				} catch (ActivityNotFoundException e) {
					Log.e(TAG, "activity not found", e);
					Toast.makeText(this, R.string.error_conv_null,
							Toast.LENGTH_LONG).show();
				}
				this.finish();
				return;
			}
		}

		final int threadId = Integer.parseInt(this.uri.getLastPathSegment());
		final Conversation c = Conversation.getConversation(this, threadId,
				true);
		this.conv = c;

		if (c == null) {
			Toast.makeText(this, R.string.error_conv_null, Toast.LENGTH_LONG)
					.show();
			this.finish();
			return;
		}

		final Contact contact = c.getContact();
		contact.update(this, false, true);

		Log.d(TAG, "address: " + contact.getNumber());
		Log.d(TAG, "name: " + contact.getName());
		Log.d(TAG, "displayName: " + contact.getDisplayName());

		final ListView lv = this.getListView();
		lv.setStackFromBottom(true);

		MessageAdapter adapter = new MessageAdapter(this, this.uri);
		this.setListAdapter(adapter);

		final String name = contact.getName();
		if (this.showPhoto && name != null) {
			final ImageView ivPhoto = (ImageView) this.findViewById(R.id.photo);
			ivPhoto.setImageDrawable(contact.getAvatar(this,
					this.defaultContactAvatar));
			ivPhoto.setOnClickListener(WRAPPER.getQuickContact(this, ivPhoto,
					contact.getLookUpUri(this.getContentResolver()), 2, null));
			ivPhoto.setVisibility(View.VISIBLE);
		}
		((TextView) this.findViewById(R.id.contact)).setText(contact
				.getNameAndNumber());

		// presence
		ImageView ivPresence = (ImageView) this.findViewById(R.id.presence);
		if (contact.getPresenceState() > 0) {
			ivPresence.setImageResource(Contact.getPresenceRes(contact
					.getPresenceState()));
			ivPresence.setVisibility(View.VISIBLE);
		} else {
			ivPresence.setVisibility(View.GONE);
		}

		final String body = intent.getStringExtra(Intent.EXTRA_TEXT);
		if (!TextUtils.isEmpty(body)) {
			this.etText.setText(body);
		}

		this.setRead();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected final void onResume() {
		super.onResume();
		final ListView lv = this.getListView();
		lv.setTranscriptMode(AbsListView.TRANSCRIPT_MODE_ALWAYS_SCROLL);
		lv.setAdapter(new MessageAdapter(this, this.uri));
		this.markedUnread = false;

		final Button btn = (Button) this.findViewById(R.id.send_);
		if (this.showTextField) {
			final Intent i = this.buildIntent(this.enableAutosend, false);
			final PackageManager pm = this.getPackageManager();
			ActivityInfo ai = null;
			if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean(
					Preferences.PREFS_SHOWTARGETAPP, true)) {
				ai = i.resolveActivityInfo(pm, 0);
			}
			if (ai == null) {
				btn.setText(R.string.send_);
				this.etText.setMinLines(1);
			} else {
				if (chooserPackage == null) {
					final ActivityInfo cai = this.buildIntent(
							this.enableAutosend, true).resolveActivityInfo(pm,
							0);
					if (cai != null) {
						chooserPackage = cai.packageName;
					}
				}
				if (ai.packageName.equals(chooserPackage)) {
					btn.setText(this.getString(R.string.send_) + "\n("
							+ this.getString(R.string.chooser_) + ")");
					this.etText.setMinLines(2);
				} else {
					Log.d(TAG, "ai.pn: " + ai.packageName);
					btn.setText(this.getString(R.string.send_) + "\n("
							+ ai.loadLabel(pm) + ")");
					this.etText.setMinLines(2);
				}
			}
		} else {
			btn.setText(R.string.reply);
		}
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
		final SharedPreferences p = PreferenceManager
				.getDefaultSharedPreferences(this);
		if (p.getBoolean(Preferences.PREFS_HIDE_RESTORE, false)) {
			menu.removeItem(R.id.item_restore);
		}
		return true;
	}

	/**
	 * {@inheritDoc}
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
			this.send(true, false);
			return true;
		case R.id.item_call:
			this.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("tel:"
					+ this.conv.getContact().getNumber())));
			return true;
		case R.id.item_restore:
			this.etText.setText(PreferenceManager.getDefaultSharedPreferences(
					this).getString(Preferences.PREFS_BACKUPLASTTEXT, null));
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
		final Message m = Message.getMessage(this, (Cursor) parent
				.getItemAtPosition(position));
		final Uri target = m.getUri();
		final int read = m.getRead();
		final int type = m.getType();
		Builder builder = new Builder(context);
		builder.setTitle(R.string.message_options_);

		final Contact contact = this.conv.getContact();
		final String a = contact.getNumber();
		Log.d(TAG, "p: " + a);
		final String n = contact.getName();

		String[] items = this.longItemClickDialog;
		if (TextUtils.isEmpty(n)) {
			items[WHICH_VIEW_CONTACT] = this.getString(R.string.add_contact_);
		} else {
			items[WHICH_VIEW_CONTACT] = this.getString(R.string.view_contact_);
		}
		items[WHICH_CALL] = this.getString(R.string.call) + " "
				+ contact.getDisplayName();
		if (read == 0) {
			items = items.clone();
			items[WHICH_MARK_UNREAD] = context.getString(R.string.mark_read_);
		}
		if (type == Message.SMS_DRAFT) {
			items = items.clone();
			items[WHICH_FORWARD] = context.getString(R.string.send_draft_);
		}
		builder.setItems(items, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(final DialogInterface dialog, final int which) {
				Intent i = null;
				switch (which) {
				case WHICH_VIEW_CONTACT:
					if (n == null) {
						i = ContactsWrapper.getInstance()
								.getInsertPickIntent(a);
						Conversation.flushCache();
					} else {
						final Uri u = MessageList.this.conv.getContact()
								.getUri();
						i = new Intent(Intent.ACTION_VIEW, u);
					}
					MessageList.this.startActivity(i);
					break;
				case WHICH_CALL:
					MessageList.this.startActivity(new Intent(
							Intent.ACTION_VIEW, Uri.parse("tel:" + a)));
					break;
				case WHICH_MARK_UNREAD:
					ConversationList.markRead(context, target, 1 - read);
					MessageList.this.markedUnread = true;
					break;
				case WHICH_REPLY:
					MessageList.this.startActivity(ConversationList
							.getComposeIntent(a));
					break;
				case WHICH_FORWARD:
					int resId;
					if (type == Message.SMS_DRAFT) {
						resId = R.string.send_draft_;
						i = ConversationList
								.getComposeIntent(MessageList.this.conv
										.getContact().getNumber());
					} else {
						resId = R.string.forward_;
						i = new Intent(Intent.ACTION_SEND);
						i.setType("text/plain");
						i.putExtra("forwarded_message", true);
					}
					CharSequence text = null;
					if (Preferences.decodeDecimalNCR(context)) {
						text = Converter.convertDecNCR2Char(m.getBody());
					} else {
						text = m.getBody();
					}
					i.putExtra(Intent.EXTRA_TEXT, text);
					i.putExtra("sms_body", text);
					context.startActivity(Intent.createChooser(i, context
							.getString(resId)));
					break;
				case WHICH_COPY_TEXT:
					final ClipboardManager cm = // .
					(ClipboardManager) context.getSystemService(// .
							Context.CLIPBOARD_SERVICE);
					if (Preferences.decodeDecimalNCR(context)) {
						cm.setText(Converter.convertDecNCR2Char(m.getBody()));
					} else {
						cm.setText(m.getBody());
					}
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
			this.send(true, false);
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
			this.send(false, true);
			return true;
		default:
			return true;
		}
	}

	/**
	 * Build an {@link Intent} for sending it.
	 * 
	 * @param autosend
	 *            autosend
	 * @param showChooser
	 *            show chooser
	 * @return {@link Intent}
	 */
	private Intent buildIntent(final boolean autosend, // .
			final boolean showChooser) {
		final String text = this.etText.getText().toString().trim();
		final Intent i = ConversationList.getComposeIntent(this.conv
				.getContact().getNumber());
		i.putExtra(Intent.EXTRA_TEXT, text);
		i.putExtra("sms_body", text);
		if (autosend && this.enableAutosend && text.length() > 0) {
			i.putExtra("AUTOSEND", "1");
		}
		if (showChooser) {
			return Intent.createChooser(i, this.getString(R.string.reply));
		} else {
			return i;
		}
	}

	/**
	 * Answer/send message.
	 * 
	 * @param autosend
	 *            enable autosend
	 * @param showChooser
	 *            show chooser
	 */
	private void send(final boolean autosend, final boolean showChooser) {
		final Intent i = this.buildIntent(autosend, showChooser);
		this.startActivity(i);
		PreferenceManager.getDefaultSharedPreferences(this).edit().putString(
				Preferences.PREFS_BACKUPLASTTEXT,
				this.etText.getText().toString()).commit();
		this.etText.setText("");
	}
}
