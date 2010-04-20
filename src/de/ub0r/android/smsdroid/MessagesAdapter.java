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

import de.ub0r.android.smsdroid.cache.Persons;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.SQLException;
import android.net.Uri;
import android.os.Handler;
import android.provider.CallLog.Calls;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * Adapter for the list of {@link Conversation}s.
 * 
 * @author flx
 */
public class MessagesAdapter extends ArrayAdapter<Message> {
	/** Tag for logging. */
	static final String TAG = "SMSdroid.msa";

	/** SQL WHERE: unread messages. */
	static final String SELECTION_UNREAD = "read = '0'";
	/** SQL WHERE: read messages. */
	static final String SELECTION_READ = "read = '1'";

	/** Cursor's sort, upside down. */
	public static final String SORT_USD = Calls.DATE + " ASC";
	/** Cursor's sort, normal. */
	public static final String SORT_NORM = Calls.DATE + " DESC";

	/** Dateformat. //TODO: move me to xml */
	private static final String DATE_FORMAT = Conversation.DATE_FORMAT;

	/** Used background drawable for outgoing messages. */
	private final int backgroundDrawableOut;

	/** {@link MessageList}. */
	private final MessageList context;

	/** Inner {@link Cursor}. */
	private Cursor cursor;

	/** Sorting order. */
	private final String sort;

	/** Uri to represent. */
	private final Uri uri;

	/** Used text size. */
	private final int textSize;

	/**
	 * Default Constructor.
	 * 
	 * @param c
	 *            {@link MessageList}
	 * @param u
	 *            {@link Uri}
	 * @param s
	 *            sorting order.
	 */
	public MessagesAdapter(final MessageList c, final Uri u, final String s) {
		super(c, R.layout.messagelist_item);
		this.context = c;
		if (Preferences.getTheme(this.context) == android.R.style.Theme_Black) {
			this.backgroundDrawableOut = R.drawable.grey_dark;
		} else {
			this.backgroundDrawableOut = R.drawable.grey_light;
		}
		this.textSize = Preferences.getTextsize(c);
		this.uri = u;
		this.sort = s;

		Cursor cur;
		try {
			cur = c.getContentResolver().query(this.uri,
					Conversation.PROJECTION, null, null, this.sort);
		} catch (SQLException e) {
			Log.w(TAG, "error while query", e);
			Conversation.PROJECTION[Conversation.INDEX_ADDRESS]// .
			= Conversation.ADDRESS_HERO;
			Conversation.PROJECTION[Conversation.INDEX_THREADID] // .
			= Conversation.THREADID_HERO;
			cur = c.getContentResolver().query(this.uri,
					Conversation.PROJECTION, null, null, this.sort);
		}
		c.startManagingCursor(cur);
		cur.registerContentObserver(new ContentObserver(new Handler()) {
			@Override
			public void onChange(final boolean selfChange) {
				super.onChange(selfChange);
				Log.d(TAG, "changed cursor");
				MessagesAdapter.this.buildArray();
			}
		});
		this.cursor = cur;
		this.buildArray();
	}

	/**
	 * Build the inner array.
	 */
	final void buildArray() {
		if (this.cursor == null) {
			return;
		}
		if (!this.cursor.requery()) {
			return;
		}
		this.cursor.moveToFirst();
		this.clear();
		do {
			final Message m = new Message(this.cursor);
			final long d = m.getDate();
			final int l = this.getCount();
			boolean added = false;
			for (int i = 0; i < l; i++) {
				if ((this.sort == SORT_NORM && d > this.getItem(i).getDate())
						|| (this.sort == SORT_USD && d < this.getItem(i)
								.getDate())) {
					this.insert(m, i);
					added = true;
					break;
				}
			}
			if (!added) {
				this.add(m);
			}
			Log.d(TAG, "added   " + m.getId() + " " + m.getThreadId());
			Log.d(TAG, "added.. " + m.getDate() + " " + m.getBody());
		} while (this.cursor.moveToNext());
		Log.d(TAG, "notifyDataSetChanged()");
		this.notifyDataSetChanged();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final View getView(final int position, final View convertView,
			final ViewGroup parent) {
		final Message m = this.getItem(position);
		View view = convertView;
		TextView twBody;
		if (view == null) {
			view = View.inflate(this.context, R.layout.messagelist_item, null);
			twBody = (TextView) view.findViewById(R.id.body);
			twBody.setTextSize(this.textSize);
		} else {
			twBody = (TextView) view.findViewById(R.id.body);
		}

		int t = m.getType();
		final TextView twPerson = (TextView) view.findViewById(R.id.addr);

		if (t == Calls.INCOMING_TYPE) {
			final String address = m.getAddress(this.context);
			Log.d(TAG, "p: " + address);
			twPerson.setText(address);
			Persons.getName(this.context, address, twPerson);
			view.setBackgroundResource(0);
			((ImageView) view.findViewById(R.id.inout))
					.setImageResource(R.drawable.// .
					ic_call_log_list_incoming_call);
		} else if (t == Calls.OUTGOING_TYPE) {
			twPerson.setText(R.string.me);
			view.setBackgroundResource(this.backgroundDrawableOut);
			((ImageView) view.findViewById(R.id.inout))
					.setImageResource(R.drawable.// .
					ic_call_log_list_outgoing_call);
		}
		int read = m.getRead();
		if (read == 0) {
			view.findViewById(R.id.read).setVisibility(View.VISIBLE);
		} else {
			view.findViewById(R.id.read).setVisibility(View.INVISIBLE);
		}
		String text = m.getBody();
		if (text == null) {
			text = this.context.getString(R.string.mms_not_supported);
		}
		twBody.setText(text);
		long time = m.getDate();
		((TextView) view.findViewById(R.id.date)).setText(SMSdroid.getDate(
				DATE_FORMAT, time));

		return view;
	}
}
