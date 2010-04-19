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
public class ConversationsAdapter extends ArrayAdapter<Conversation> {
	/** Tag for logging. */
	static final String TAG = "SMSdroid.coa";

	/** URI to resolve. */
	private static final Uri URI = Uri
			.parse("content://mms-sms/conversations/");

	/** Cursor's sort. */
	public static final String SORT = Calls.DATE + " DESC";

	/** {@link SMSdroid}. */
	private final SMSdroid context;

	/** Inner {@link Cursor}. */
	private Cursor cursor;

	/**
	 * Default Constructor.
	 * 
	 * @param c
	 *            {@link SMSdroid}
	 */
	public ConversationsAdapter(final SMSdroid c) {
		super(c, R.layout.conversationlist_item);
		this.context = c;

		Cursor cur;
		try {
			cur = c.getContentResolver().query(URI, Conversation.PROJECTION,
					null, null, SORT);
		} catch (SQLException e) {
			Log.w(TAG, "error while query", e);
			Conversation.PROJECTION[Conversation.INDEX_ADDRESS]// .
			= Conversation.ADDRESS_HERO;
			Conversation.PROJECTION[Conversation.INDEX_THREADID] // .
			= Conversation.THREADID_HERO;
			cur = c.getContentResolver().query(URI, Conversation.PROJECTION,
					null, null, SORT);
		}
		c.startManagingCursor(cur);
		cur.registerContentObserver(new ContentObserver(new Handler()) {
			@Override
			public void onChange(final boolean selfChange) {
				super.onChange(selfChange);
				Log.d(TAG, "changed cursor");
				ConversationsAdapter.this.buildArray();
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
			Conversation c = new Conversation(this.cursor);
			long d = c.getDate();
			int l = this.getCount();
			boolean added = false;
			for (int i = 0; i < l; i++) {
				if (d > this.getItem(i).getDate()) {
					this.insert(c, i);
					added = true;
					break;
				}
			}
			if (!added) {
				this.add(c);
			}
			Log.d(TAG, "added   " + c.getId() + " " + c.getThreadId());
			Log.d(TAG, "added.. " + c.getDate() + " " + c.getBody());
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
		Conversation c = this.getItem(position);
		View view = convertView;
		if (view == null) {
			view = View.inflate(this.context, R.layout.conversationlist_item,
					null);
		}

		final long threadID = c.getThreadId();
		int t = c.getType();
		if (t == Calls.INCOMING_TYPE) {
			((ImageView) view.findViewById(R.id.inout))
					.setImageResource(R.drawable.// .
					ic_call_log_list_incoming_call);
		} else if (t == Calls.OUTGOING_TYPE) {
			((ImageView) view.findViewById(R.id.inout))
					.setImageResource(R.drawable.// .
					ic_call_log_list_outgoing_call);
		}
		int read = c.getRead();
		if (read == 0) {
			view.findViewById(R.id.read).setVisibility(View.VISIBLE);
		} else {
			view.findViewById(R.id.read).setVisibility(View.INVISIBLE);
		}
		String address = c.getAddress(this.context);
		Log.d(TAG, "p: " + address);
		final TextView twPerson = (TextView) view.findViewById(R.id.text1);
		twPerson.setText(address);
		CachePersons.getName(this.context, address, twPerson);
		String text = c.getBody();
		if (text == null) {
			text = this.context.getString(R.string.mms_conversation);
		}
		((TextView) view.findViewById(R.id.text2)).setText(text);
		long time = c.getDate();
		((TextView) view.findViewById(R.id.text3)).setText(SMSdroid.getDate(
				Conversation.DATE_FORMAT, time));

		ImageView iv = (ImageView) view.findViewById(R.id.photo);
		if (SMSdroid.showContactPhoto) {
			CachePersons.getPicture(this.context, address, iv);
			iv.setVisibility(View.VISIBLE);
		} else {
			iv.setVisibility(View.GONE);
		}

		final Uri target = Uri.parse(MessageList.URI + threadID);
		final Cursor cur = this.context.getContentResolver().query(target,
				MessageListAdapter.PROJECTION, null, null, null);
		TextView tv = (TextView) view.findViewById(R.id.text4);
		tv.setText("(" + cur.getCount() + ")");
		cur.close();
		return view;
	}
}
