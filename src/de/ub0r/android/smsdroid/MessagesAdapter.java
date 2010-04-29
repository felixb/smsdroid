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

import java.util.List;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CallLog.Calls;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;
import de.ub0r.android.smsdroid.cache.Persons;
import de.ub0r.android.smsdroid.cache.Threads;

/**
 * Adapter for the list of {@link Conversation}s.
 * 
 * @author flx
 */
public class MessagesAdapter extends ResourceCursorAdapter {
	/** Tag for logging. */
	static final String TAG = "SMSdroid.msa";

	/** SQL WHERE: unread messages. */
	static final String SELECTION_UNREAD = "read = '0'";
	/** SQL WHERE: read messages. */
	static final String SELECTION_READ = "read = '1'";

	/** Dateformat. //TODO: move me to xml */
	private static final String DATE_FORMAT = Conversation.DATE_FORMAT;

	/** Used background drawable for outgoing messages. */
	private final int backgroundDrawableOut;

	/** Used {@link Uri}. */
	private Uri uri;
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

	/**
	 * Default Constructor.
	 * 
	 * @param c
	 *            {@link MessageList}
	 * @param u
	 *            {@link Uri}
	 */
	public MessagesAdapter(final MessageList c, final Uri u) {
		super(c, R.layout.messagelist_item, c.getContentResolver().query(u,
				Conversation.PROJECTION, null, null, null), true);
		if (Preferences.getTheme(c) == android.R.style.Theme_Black) {
			this.backgroundDrawableOut = R.drawable.grey_dark;
		} else {
			this.backgroundDrawableOut = R.drawable.grey_light;
		}
		this.textSize = Preferences.getTextsize(c);
		this.uri = u;
		List<String> p = u.getPathSegments();
		this.threadId = Long.parseLong(p.get(p.size() - 1));
		this.address = Threads.getAddress(c, this.threadId);
		this.name = Persons.getName(c, this.address, false);
		if (this.name == null) {
			this.displayName = this.address;
		} else {
			this.displayName = this.name;
		}
		Log.d(TAG, "address: " + this.address);
		Log.d(TAG, "name: " + this.name);
		Log.d(TAG, "displayName: " + this.displayName);

		// this.notifyDataSetChanged();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final void bindView(final View view, final Context context,
			final Cursor cursor) {
		final Message m = new Message(cursor);

		final TextView twPerson = (TextView) view.findViewById(R.id.addr);
		TextView twBody = (TextView) view.findViewById(R.id.body);
		twBody.setTextSize(this.textSize);
		int t = m.getType();

		// incoming / outgoing
		if (t == Calls.INCOMING_TYPE) {
			twPerson.setText(this.displayName);
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

		// unread / read
		if (m.getRead() == 0) {
			view.findViewById(R.id.read).setVisibility(View.VISIBLE);
		} else {
			view.findViewById(R.id.read).setVisibility(View.INVISIBLE);
		}

		String text = m.getBody();
		if (text == null) {
			text = context.getString(R.string.mms_not_supported);
		}
		twBody.setText(text);
		long time = m.getDate();
		((TextView) view.findViewById(R.id.date)).setText(SMSdroid.getDate(
				DATE_FORMAT, time));
	}
}
