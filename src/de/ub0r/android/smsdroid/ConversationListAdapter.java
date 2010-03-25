/*
 * Copyright (C) 2009 Felix Bechstein
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
import android.database.Cursor;
import android.net.Uri;
import android.provider.CallLog.Calls;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.ImageView;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;
import android.util.Log;

/**
 * CursorAdapter getting Name, Phone from DB.
 * 
 * @author flx
 */
public class ConversationListAdapter extends ResourceCursorAdapter {
	/** Tag for logging. */
	static final String TAG = "SMSdroid.cla";

	/** INDEX: id. */
	public static final int INDEX_ID = 0;
	/** INDEX: date. */
	public static final int INDEX_DATE = 1;
	/** INDEX: address. */
	public static final int INDEX_ADDRESS = 2;
	/** INDEX: thread_id. */
	public static final int INDEX_THREADID = 3;
	/** INDEX: body. */
	public static final int INDEX_BODY = 4;
	/** INDEX: type. */
	public static final int INDEX_TYPE = 5;
	/** INDEX: read. */
	public static final int INDEX_READ = 6;
	/** INDEX: person. */
	public static final int INDEX_PERSON = 7;

	/** Dateformat. //TODO: move me to xml */
	static final String DATE_FORMAT = "dd.MM. kk:mm";

	/** Cursor's projection. */
	public static final String[] PROJECTION = { //
	"_id", // 0
			Calls.DATE, // 1
			"address", // 2
			"thread_id", // 3
			"body", // 4
			Calls.TYPE, // 5
			"read", // 6
			"person", // 7
	};

	/** Cursor's sort. */
	public static final String SORT = Calls.DATE + " DESC";

	/**
	 * Default Constructor.
	 * 
	 * @param context
	 *            {@link Context}
	 * @param c
	 *            {@link Cursor}
	 */
	public ConversationListAdapter(final Context context, final Cursor c) {
		super(context, R.layout.conversationlist_item, c, true);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final void bindView(final View view, final Context context,
			final Cursor cursor) {
		final String threadID = cursor.getString(INDEX_THREADID);
		String s = "";
		int t = cursor.getInt(INDEX_TYPE);
		if (t == Calls.INCOMING_TYPE) {
			s = "<< ";
		} else if (t == Calls.OUTGOING_TYPE) {
			s = ">> ";
		}
		int read = cursor.getInt(INDEX_READ);
		if (read == 0) {
			view.findViewById(R.id.read).setVisibility(View.VISIBLE);
		} else {
			view.findViewById(R.id.read).setVisibility(View.INVISIBLE);
		}
		final String address = cursor.getString(INDEX_ADDRESS);
		final TextView twPerson = (TextView) view.findViewById(R.id.text1);
		twPerson.setText(address);
		CachePersons.getName(context, address, twPerson);
		((TextView) view.findViewById(R.id.text2)).setText(cursor
				.getString(INDEX_BODY));
		((TextView) view.findViewById(R.id.text3)).setText(s
				+ DateFormat.format(DATE_FORMAT, Long.parseLong(cursor
						.getString(INDEX_DATE))));

		ImageView iv = (ImageView) view.findViewById(R.id.photo);
		if (SMSdroid.showContactPhoto) {
			CachePersons.getPicture(context, address, iv);
			iv.setVisibility(View.VISIBLE);
		} else {
			iv.setVisibility(View.GONE);
		}

		Log.d(TAG, "person: " + cursor.getInt(INDEX_PERSON));

		final Uri target = Uri.parse(MessageList.URI + threadID);
		final Cursor c = context.getContentResolver().query(target,
				MessageListAdapter.PROJECTION, null, null, null);
		TextView tv = (TextView) view.findViewById(R.id.text4);
		tv.setText(" (" + c.getCount() + ")");
		c.close();
	}
}
