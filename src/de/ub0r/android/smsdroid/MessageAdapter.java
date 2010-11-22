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

import java.util.ArrayList;

import android.content.ActivityNotFoundException;
import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.graphics.Bitmap;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;
import de.ub0r.android.lib.Log;
import de.ub0r.android.smsdroid.ConversationProvider.Messages;
import de.ub0r.android.smsdroid.ConversationProvider.Threads;

/**
 * Adapter for the list of {@link Messages}s.
 * 
 * @author flx
 */
public class MessageAdapter extends ResourceCursorAdapter {
	/** Tag for logging. */
	static final String TAG = "msa";

	/** SQL WHERE: unread messages. */
	static final String SELECTION_UNREAD = "read = '0'";
	/** SQL WHERE: read messages. */
	static final String SELECTION_READ = "read = '1'";

	/** Used background drawable for messages. */
	private final int backgroundDrawableIn, backgroundDrawableOut;

	/** Thread id. */
	private long threadId = -1;
	/** Address. */
	private String address = null;
	/** Name. */
	private String name = null;
	/** Display Name (name if !=null, else address). */
	private String displayName = null;

	/** Used text size. */
	private final int textSize;

	/** {@link BackgroundQueryHandler}. */
	private final BackgroundQueryHandler queryHandler;

	/** Token for {@link BackgroundQueryHandler}: message list query. */
	private static final int MESSAGE_LIST_QUERY_TOKEN = 0;

	/** Reference to {@link ConversationList}. */
	private final MessageList activity;

	/** {@link Uri}. */
	private final Uri uri;

	/**
	 * Handle queries in background.
	 * 
	 * @author flx
	 */
	private final class BackgroundQueryHandler extends AsyncQueryHandler {

		/**
		 * A helper class to help make handling asynchronous
		 * {@link ContentResolver} queries easier.
		 * 
		 * @param contentResolver
		 *            {@link ContentResolver}
		 */
		public BackgroundQueryHandler(final ContentResolver contentResolver) {
			super(contentResolver);
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		protected void onQueryComplete(final int token, final Object cookie,
				final Cursor cursor) {
			switch (token) {
			case MESSAGE_LIST_QUERY_TOKEN:
				MessageAdapter.this.changeCursor(cursor);
				MessageAdapter.this.activity
						.setProgressBarIndeterminateVisibility(false);
				return;
			default:
				return;
			}
		}
	}

	/**
	 * Default Constructor.
	 * 
	 * @param c
	 *            {@link MessageList}
	 * @param u
	 *            {@link Uri}
	 * @param ccursor
	 *            conversation {@link Cursor}
	 */
	public MessageAdapter(final MessageList c, final Uri u, // .
			final Cursor ccursor) {
		super(c, R.layout.messagelist_item, null, true);
		this.uri = ContentUris.withAppendedId(Messages.THREAD_URI, ContentUris
				.parseId(u));
		this.activity = c;
		final ContentResolver cr = c.getContentResolver();
		this.queryHandler = new BackgroundQueryHandler(cr);
		final SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(c);
		boolean showBubbles = prefs
				.getBoolean(Preferences.PREFS_BUBBLES, false);
		if (showBubbles) {
			this.backgroundDrawableIn = R.drawable.bubble_in;
			this.backgroundDrawableOut = R.drawable.bubble_out;
		} else {
			if (Preferences.getTheme(c) == android.R.style.Theme_Black) {
				this.backgroundDrawableOut = R.drawable.grey_dark;
			} else {
				this.backgroundDrawableOut = R.drawable.grey_light;
			}
			this.backgroundDrawableIn = 0;
		}
		this.textSize = Preferences.getTextsize(c);
		this.threadId = ContentUris.parseId(u);
		if (ccursor != null && ccursor.moveToFirst()) {
			this.address = ccursor.getString(Threads.INDEX_ADDRESS);
			this.name = ccursor.getString(Threads.INDEX_NAME);
		}
		if (this.name == null) {
			// TODO: this.name = AsyncHelper.getContactName(c, this.address);
		}
		this.displayName = ConversationProvider.getDisplayName(this.address,
				this.name, false);
		Log.d(TAG, "address: " + this.address);
		Log.d(TAG, "name: " + this.name);
		Log.d(TAG, "displayName: " + this.displayName);

		cr.registerContentObserver(Messages.CONTENT_URI, false,
				new ContentObserver(this.queryHandler) {
					@Override
					public void onChange(final boolean selfChange) {
						super.onChange(selfChange);
						MessageAdapter.this.startMsgListQuery();
					}
				});
	}

	/**
	 * Start ConversationList query.
	 */
	public final void startMsgListQuery() {
		// Cancel any pending queries
		this.queryHandler.cancelOperation(MESSAGE_LIST_QUERY_TOKEN);
		try {
			// Kick off the new query
			this.activity.setProgressBarIndeterminateVisibility(true);
			this.queryHandler.startQuery(MESSAGE_LIST_QUERY_TOKEN, null,
					this.uri, Messages.PROJECTION, null, null, null);
		} catch (SQLiteException e) {
			Log.e(TAG, "error starting query", e);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final void bindView(final View view, final Context context,
			final Cursor cursor) {
		final TextView twPerson = (TextView) view.findViewById(R.id.addr);
		TextView twBody = (TextView) view.findViewById(R.id.body);
		if (this.textSize > 0) {
			twBody.setTextSize(this.textSize);
		}
		int t = cursor.getInt(Messages.INDEX_TYPE);

		String subject = cursor.getString(Messages.INDEX_SUBJECT);
		if (subject == null) {
			subject = "";
		} else {
			subject = ": " + subject;
		}
		// incoming / outgoing / pending
		final View pending = view.findViewById(R.id.pending);
		int pendingvisability = View.GONE;
		switch (t) {
		case Messages.TYPE_SMS_DRAFT:
			// case Message.MMS_DRAFT:
			pendingvisability = View.VISIBLE;
		case Messages.TYPE_SMS_OUT: // handle drafts/pending here too
		case Messages.TYPE_MMS_OUT:
			twPerson.setText(context.getString(R.string.me) + subject);
			try {
				view.setBackgroundResource(this.backgroundDrawableOut);
			} catch (OutOfMemoryError e) {
				Log.e(TAG, "OOM while setting bg", e);
			}
			((ImageView) view.findViewById(R.id.inout))
					.setImageResource(R.drawable.// .
					ic_call_log_list_outgoing_call);
			break;
		case Messages.TYPE_SMS_IN:
		case Messages.TYPE_MMS_IN:
		default:
			twPerson.setText(this.displayName + subject);
			try {
				view.setBackgroundResource(this.backgroundDrawableIn);
			} catch (OutOfMemoryError e) {
				Log.e(TAG, "OOM while setting bg", e);
			}
			((ImageView) view.findViewById(R.id.inout))
					.setImageResource(R.drawable.// .
					ic_call_log_list_incoming_call);
			pending.setVisibility(View.GONE);
			break;
		}
		pending.setVisibility(pendingvisability);

		// unread / read
		if (cursor.getInt(Messages.INDEX_READ) == 0) {
			view.findViewById(R.id.read).setVisibility(View.VISIBLE);
		} else {
			view.findViewById(R.id.read).setVisibility(View.INVISIBLE);
		}

		long time = cursor.getLong(Messages.INDEX_DATE);
		((TextView) view.findViewById(R.id.date)).setText(ConversationList
				.getDate(context, time));

		CharSequence text = cursor.getString(Messages.INDEX_BODY);
		ImageView ivPicture = (ImageView) view.findViewById(R.id.picture);
		Bitmap pic = null;
		Intent contentIntent = null;
		if (t > 100) {
			final ArrayList<Object> mms = Messages.getParts(context
					.getContentResolver(), cursor.getLong(Messages.INDEX_ID));
			if (mms != null) {
				final int l = mms.size();
				for (int i = 0; i < l; i++) {
					final Object o = mms.get(i);
					if (o instanceof CharSequence) {
						if (text == null) {
							text = (CharSequence) o;
						}
					} else if (o instanceof Bitmap) {
						pic = (Bitmap) o;
					} else if (o instanceof Intent) {
						contentIntent = (Intent) o;
					}
				}
			}
			if (pic != null) {
				if (pic == Messages.BITMAP_PLAY) {
					ivPicture.setImageResource(R.drawable.mms_play_btn);
				} else {
					ivPicture.setImageBitmap(pic);
				}
				final Intent i = contentIntent;
				ivPicture.setVisibility(View.VISIBLE);
				if (i == null) {
					ivPicture.setOnClickListener(null);
				} else {
					ivPicture.setOnClickListener(new OnClickListener() {
						@Override
						public void onClick(final View v) {
							try {
								context.startActivity(i);
							} catch (ActivityNotFoundException e) {
								Log.w(TAG, "activity not found", e);
								Toast.makeText(context,
										"no activity for data: " + i.getType(),
										Toast.LENGTH_LONG).show();
							}
						}
					});
				}
			}
		}
		if (pic == null) {
			ivPicture.setVisibility(View.GONE);
			ivPicture.setOnClickListener(null);
		}
		final Button btn = (Button) view.findViewById(R.id.btn_download_msg);
		if (text == null && pic == null) {
			btn.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(final View v) {
					// view.findViewById(R.id.label_downloading).setVisibility(
					// View.VISIBLE);
					// btn.setVisibility(View.GONE);
					// Intent intent = new Intent();
					// intent.setClassName("com.android.mms",
					// ".transaction.TransactionService");
					// intent.putExtra("uri", m.getUri().toString());
					// intent.putExtra("type", 1);
					// context.startService(intent);

					final Uri target = ContentUris.withAppendedId(
							Threads.ORIG_URI, cursor
									.getLong(Messages.INDEX_THREADID));
					Intent i = new Intent(Intent.ACTION_VIEW, target);
					context.startActivity(Intent.createChooser(i, context
							.getString(R.string.view_mms)));
				}
			});

			btn.setVisibility(View.VISIBLE);
		} else {
			btn.setVisibility(View.GONE);
		}
		if (text == null) {
			twBody.setVisibility(View.INVISIBLE);
		} else {
			if (ConversationList.showEmoticons) {
				text = SmileyParser.getInstance(context).addSmileySpans(text);
			}
			twBody.setText(text);
			twBody.setVisibility(View.VISIBLE);
		}
	}

	/**
	 * Close {@link Cursor}.
	 */
	public final void close() {
		final Cursor c = this.getCursor();
		if (c != null && !c.isClosed()) {
			c.close();
		}
	}
}
