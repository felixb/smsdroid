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

import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.os.Handler;
import android.provider.CallLog.Calls;
import android.view.View;
import android.widget.ImageView;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;

/**
 * Adapter for the list of {@link Conversation}s.
 * 
 * @author flx
 */
public class ConversationsAdapter extends ResourceCursorAdapter {
	/** Tag for logging. */
	static final String TAG = "SMSdroid.coa";

	/** Cursor's sort. */
	public static final String SORT = Calls.DATE + " DESC";

	/** Used text size. */
	private final int textSize;

	/** {@link Cursor} to the original Content to listen for changes. */
	private final Cursor origCursor;

	/**
	 * Default Constructor.
	 * 
	 * @param c
	 *            {@link SMSdroid}
	 */
	public ConversationsAdapter(final SMSdroid c) {
		super(c, R.layout.conversationlist_item, c.getContentResolver().query(
				ConversationProvider.CONTENT_URI, Conversation.PROJECTION,
				null, null, null), true);
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
						// Log.d(TAG, "call notifyDataSetChanged();");
						// ConversationsAdapter.this.getCursor().requery();
						// ConversationsAdapter.this.notifyDataSetChanged();
					}
				}
			});
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final void bindView(final View view, final Context context,
			final Cursor cursor) {
		final Conversation c = new Conversation(context, cursor);

		final TextView tvBody = (TextView) view.findViewById(R.id.body);
		tvBody.setTextSize(this.textSize);
		final TextView tvName = (TextView) view.findViewById(R.id.addr);
		final TextView tvCount = (TextView) view.findViewById(R.id.count);
		final ImageView ivPhoto = (ImageView) view.findViewById(R.id.photo);

		if (SMSdroid.showContactPhoto) {
			Bitmap b = c.getPhoto();
			if (b != null) {
				ivPhoto.setImageBitmap(b);
			} else {
				ivPhoto.setImageResource(R.drawable.ic_contact_picture);
			}
			ivPhoto.setVisibility(View.VISIBLE);
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
		tvName.setText(c.getDisplayName());

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
		((TextView) view.findViewById(R.id.date)).setText(SMSdroid.getDate(
				Conversation.DATE_FORMAT, time));
	}
}
