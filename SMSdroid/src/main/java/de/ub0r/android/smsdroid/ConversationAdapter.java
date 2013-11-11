/*
 * Copyright (C) 2009-2012 Felix Bechstein
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

import android.app.Activity;
import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.provider.CallLog.Calls;
import android.view.View;
import android.view.ViewGroup;
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
	/** Reference to {@link ConversationListActivity}. */
	private final Activity activity;

	/** List of blocked numbers. */
	private final String[] blacklist;

	/** {@link ContactsWrapper}. */
	private static final ContactsWrapper WRAPPER = ContactsWrapper.getInstance();

	/** Default {@link Drawable} for {@link Contact}s. */
	private Drawable defaultContactAvatar = null;

	/** Convert NCR. */
	private final boolean convertNCR;

	/** Use grid instead of list. */
	private boolean useGridLayout;

	/** View holder. */
	private static class ViewHolder {
		TextView tvBody;
		TextView tvPerson;
		TextView tvCount;
		TextView tvDate;
		ImageView ivPhoto;
		View vRead;
	}

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
		protected void onQueryComplete(final int token, final Object cookie, final Cursor cursor) {
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
	 *            {@link ConversationListActivity}
	 */
	public ConversationAdapter(final Activity c) {
		super(c, R.layout.conversationlist_item, null, true);
		this.activity = c;

		SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(activity);
		this.useGridLayout = p.getBoolean("use_gridlayout", false);
		if (useGridLayout) {
			super.setViewResource(R.layout.conversation_square);
		}
		final ContentResolver cr = c.getContentResolver();
		this.queryHandler = new BackgroundQueryHandler(cr);
		SpamDB spam = new SpamDB(c);
		spam.open();
		this.blacklist = spam.getAllEntries();
		spam.close();
		spam = null;

		this.defaultContactAvatar = c.getResources().getDrawable(R.drawable.ic_contact_picture);

		this.convertNCR = PreferencesActivity.decodeDecimalNCR(c);
		this.textSize = PreferencesActivity.getTextsize(c);
		this.textColor = PreferencesActivity.getTextcolor(c);

		Cursor cursor = null;
		try {
			cursor = cr.query(Conversation.URI_SIMPLE, Conversation.PROJECTION_SIMPLE, null, null,
					null);
		} catch (SQLiteException e) {
			Log.e(TAG, "error getting conversations", e);
		}
		this.origCursor = cursor;

		if (this.origCursor != null) {
			this.origCursor.registerContentObserver(new ContentObserver(new Handler()) {
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
			this.queryHandler.startQuery(MESSAGE_LIST_QUERY_TOKEN, null, Conversation.URI_SIMPLE,
					Conversation.PROJECTION_SIMPLE, null, null, SORT);
		} catch (SQLiteException e) {
			Log.e(TAG, "error starting query", e);
		}
	}

	@Override
	public final View getView(final int pos, final View convertView, final ViewGroup parent) {
		return super.getView(pos, convertView, parent);
	}

	/*
	 * 
	 * /** {@inheritDoc}
	 */
	@Override
	public final void bindView(final View view, final Context context, final Cursor cursor) {
		final Conversation c = Conversation.getConversation(context, cursor, false);
		final Contact contact = c.getContact();

		ViewHolder holder = (ViewHolder) view.getTag();
		if (holder == null) {
			holder = new ViewHolder();
			holder.tvPerson = (TextView) view.findViewById(R.id.addr);
			holder.tvCount = (TextView) view.findViewById(R.id.count);
			holder.tvBody = (TextView) view.findViewById(R.id.body);
			holder.tvDate = (TextView) view.findViewById(R.id.date);
			holder.ivPhoto = (ImageView) view.findViewById(R.id.photo);
			holder.vRead = (View) view.findViewById(R.id.read);
			view.setTag(holder);
		}

		if (useGridLayout) {
			holder.tvCount.setVisibility(View.GONE);
		} else {
			final int count = c.getCount();
			if (count < 0) {
				holder.tvCount.setText("");
			} else {
				holder.tvCount.setText("(" + c.getCount() + ")");
			}
		}
		if (this.textSize > 0) {
			holder.tvBody.setTextSize(this.textSize);
		}

		final int col = this.textColor;
		if (col != 0) {
			holder.tvPerson.setTextColor(col);
			holder.tvBody.setTextColor(col);
			holder.tvCount.setTextColor(col);
			holder.tvDate.setTextColor(col);
		}

		if (useGridLayout || ConversationListActivity.showContactPhoto) {
			holder.ivPhoto.setImageDrawable(contact.getAvatar(this.activity,
					this.defaultContactAvatar));
			holder.ivPhoto.setVisibility(View.VISIBLE);
			if (!useGridLayout) {
				holder.ivPhoto.setOnClickListener(WRAPPER.getQuickContact(context, holder.ivPhoto,
						contact.getLookUpUri(context.getContentResolver()), 2, null));
			}
		} else {
			holder.ivPhoto.setVisibility(View.GONE);
		}

		if (this.isBlocked(contact.getNumber())) {
			holder.tvPerson.setText("[" + contact.getDisplayName() + "]");
		} else {
			holder.tvPerson.setText(contact.getDisplayName());
		}

		// read status
		if (c.getRead() == 0) {
			holder.vRead.setVisibility(View.VISIBLE);
		} else {
			holder.vRead.setVisibility(View.INVISIBLE);
		}

		// body
		String text = c.getBody();
		if (text == null) {
			text = context.getString(R.string.mms_conversation);
		}
		if (this.convertNCR) {
			holder.tvBody.setText(Converter.convertDecNCR2Char(text));
		} else {
			holder.tvBody.setText(text);
		}

		// date
		long time = c.getDate();
		holder.tvDate.setText(ConversationListActivity.getDate(context, time));

		// presence
		ImageView ivPresence = (ImageView) view.findViewById(R.id.presence);
		if (contact.getPresenceState() > 0) {
			ivPresence.setImageResource(Contact.getPresenceRes(contact.getPresenceState()));
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
