/*
 * Copyright (C) 2009-2011 Felix Bechstein
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
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.provider.CallLog.Calls;
import android.view.View;
import android.widget.ImageView;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;
import de.ub0r.android.lib.Log;
import de.ub0r.android.lib.apis.Contact;
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

	/** Used text size, color. */
	private final int textSize, textColor;

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

	/** Default {@link Drawable} for {@link Contact}s. */
	private Drawable defaultContactAvatar = null;

	/** Convert NCR. */
	private final boolean convertNCR;

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
						.setProgressBarIndeterminateVisibility(Boolean.FALSE);
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
		final ContentResolver cr = c.getContentResolver();
		this.queryHandler = new BackgroundQueryHandler(cr);
		SpamDB spam = new SpamDB(c);
		spam.open();
		this.blacklist = spam.getAllEntries();
		spam.close();
		spam = null;

		this.defaultContactAvatar = c.getResources().getDrawable(
				R.drawable.ic_contact_picture);

		this.convertNCR = Preferences.decodeDecimalNCR(c);
		this.textSize = Preferences.getTextsize(c);
		this.textColor = Preferences.getTextcolor(c);
		this.origCursor = cr.query(Conversation.URI_SIMPLE,
				Conversation.PROJECTION_SIMPLE, null, null, null);

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
			this.activity.setProgressBarIndeterminateVisibility(Boolean.TRUE);
			this.queryHandler.startQuery(MESSAGE_LIST_QUERY_TOKEN, null,
					Conversation.URI_SIMPLE, Conversation.PROJECTION_SIMPLE,
					null, null, SORT);
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
		final Contact contact = c.getContact();

		final TextView tvPerson = (TextView) view.findViewById(R.id.addr);
		final TextView tvCount = (TextView) view.findViewById(R.id.count);
		final TextView tvBody = (TextView) view.findViewById(R.id.body);
		final TextView tvDate = (TextView) view.findViewById(R.id.date);
		if (this.textSize > 0) {
			tvBody.setTextSize(this.textSize);
		}
		final int col = this.textColor;
		if (col != 0) {
			tvPerson.setTextColor(col);
			tvBody.setTextColor(col);
			tvCount.setTextColor(col);
			tvDate.setTextColor(col);
		}
		final ImageView ivPhoto = (ImageView) view.findViewById(R.id.photo);

		if (ConversationList.showContactPhoto) {
			ivPhoto.setImageDrawable(contact.getAvatar(this.activity,
					this.defaultContactAvatar));
			ivPhoto.setVisibility(View.VISIBLE);
			ivPhoto.setOnClickListener(WRAPPER
					.getQuickContact(context, ivPhoto, contact
							.getLookUpUri(context.getContentResolver()), 2,
							null));
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
		if (this.isBlocked(contact.getNumber())) {
			tvPerson.setText("[" + contact.getDisplayName() + "]");
		} else {
			tvPerson.setText(contact.getDisplayName());
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
		if (this.convertNCR) {
			tvBody.setText(Converter.convertDecNCR2Char(text));
		} else {
			tvBody.setText(text);
		}

		// date
		long time = c.getDate();
		tvDate.setText(ConversationList.getDate(context, time));

		// presence
		ImageView ivPresence = (ImageView) view.findViewById(R.id.presence);
		if (contact.getPresenceState() > 0) {
			ivPresence.setImageResource(Contact.getPresenceRes(contact
					.getPresenceState()));
			ivPresence.setVisibility(View.VISIBLE);
		} else {
			ivPresence.setVisibility(View.GONE);
		}
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
