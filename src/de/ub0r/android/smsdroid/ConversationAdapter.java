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

import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.graphics.Bitmap;
import android.os.Handler;
import android.provider.CallLog.Calls;
import android.view.View;
import android.widget.ImageView;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;
import de.ub0r.android.lib.Log;
import de.ub0r.android.lib.apis.ContactsWrapper;

/**
 * Adapter for the list of {@link Conversation}s.
 * 
 * @author flx
 */
public class ConversationAdapter extends ResourceCursorAdapter {
	/** Tag for logging. */
	static final String TAG = "coa";

	/** Cursor's sort. */
	public static final String SORT = Calls.DATE + " DESC";

	/** Used text size. */
	private final int textSize;

	/** {@link Cursor} to the original Content to listen for changes. */
	private final Cursor origCursor;

	/** {@link BackgroundQueryHandler}. */
	private final BackgroundQueryHandler queryHandler;
	/** Token for {@link BackgroundQueryHandler}: message list query. */
	private static final int MESSAGE_LIST_QUERY_TOKEN = 0;
	/** Reference to {@link ConversationList}. */
	private final ConversationList activity;

	/** List of blocked numbers. */
	private final String[] blacklist;

	/** {@link ContactsWrapper}. */
	private static final ContactsWrapper WRAPPER = ContactsWrapper
			.getInstance();

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
				ConversationAdapter.this.changeCursor(cursor);
				ConversationAdapter.this.activity
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
	 *            {@link ConversationList}
	 */
	public ConversationAdapter(final ConversationList c) {
		super(c, R.layout.conversationlist_item, null, true);
		this.activity = c;
		this.queryHandler = new BackgroundQueryHandler(c.getContentResolver());
		SpamDB spam = new SpamDB(c);
		spam.open();
		this.blacklist = spam.getAllEntries();
		spam.close();
		spam = null;

		this.textSize = Preferences.getTextsize(c);
		this.origCursor = c.getContentResolver().query(
				ConversationProvider.ORIG_URI,
				new String[] {
						ConversationProvider.PROJECTION[// .
						ConversationProvider.INDEX_ID],
						ConversationProvider.PROJECTION[// .
						ConversationProvider.INDEX_DATE] }, null, null, null);

		// does not work.
		if (this.origCursor != null) {
			this.origCursor.registerContentObserver(new ContentObserver(
					new Handler()) {
				@Override
				public void onChange(final boolean selfChange) {
					super.onChange(selfChange);
					if (!selfChange) {
						Log.d(TAG, "call startMsgListQuery();");
						ConversationAdapter.this.startMsgListQuery();
						Log.d(TAG, "invalidate cache");
						Conversation.invalidate();
					}
				}
			});
		}
		// this.startMsgListQuery();
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
					ConversationProvider.CONTENT_URI,
					ConversationProvider.PROJECTION, null, null, null);
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
		final Conversation c = Conversation.getConversation(context, cursor,
				false);

		final TextView tvBody = (TextView) view.findViewById(R.id.body);
		tvBody.setTextSize(this.textSize);
		final TextView tvName = (TextView) view.findViewById(R.id.addr);
		final TextView tvCount = (TextView) view.findViewById(R.id.count);
		final ImageView ivPhoto = (ImageView) view.findViewById(R.id.photo);

		if (ConversationList.showContactPhoto) {
			Bitmap b = c.getPhoto();
			if (b != null && b != Conversation.NO_PHOTO) {
				ivPhoto.setImageBitmap(b);
			} else {
				ivPhoto.setImageResource(R.drawable.ic_contact_picture);
			}
			ivPhoto.setVisibility(View.VISIBLE);
			ivPhoto.setOnClickListener(WRAPPER.getQuickContact(context,
					ivPhoto, c.getAddress(), 2, null));
		} else {
			ivPhoto.setVisibility(View.GONE);
		}

		// count
		final int count = c.getCount();
		if (count < 0) {
			tvCount.setText("");
		} else {
			tvCount.setText("(" + c.getCount() + ")");
		}
		if (this.isBlocked(c.getAddress())) {
			tvName.setText("[" + c.getDisplayName() + "]");
		} else {
			tvName.setText(c.getDisplayName());
		}

		int t = c.getType();
		if (t == Calls.INCOMING_TYPE) {
			((ImageView) view.findViewById(R.id.inout))
					.setImageResource(R.drawable.// .
					ic_call_log_list_incoming_call);
		} else {
			((ImageView) view.findViewById(R.id.inout))
					.setImageResource(R.drawable.// .
					ic_call_log_list_outgoing_call);
		}

		// read status
		if (c.getRead() == 0) {
			view.findViewById(R.id.read).setVisibility(View.VISIBLE);
		} else {
			view.findViewById(R.id.read).setVisibility(View.INVISIBLE);
		}

		// body
		String text = c.getBody();
		if (text == null) {
			text = context.getString(R.string.mms_conversation);
		}
		tvBody.setText(text);

		// date
		long time = c.getDate();
		((TextView) view.findViewById(R.id.date)).setText(ConversationList
				.getDate(context, time));
	}

	/**
	 * Check if address is blacklisted.
	 * 
	 * @param addr
	 *            address
	 * @return true if address is blocked
	 */
	private boolean isBlocked(final String addr) {
		if (addr == null) {
			return false;
		}
		final int l = this.blacklist.length;
		for (int i = 0; i < l; i++) {
			if (addr.equals(this.blacklist[i])) {
				return true;
			}
		}
		return false;
	}
}
