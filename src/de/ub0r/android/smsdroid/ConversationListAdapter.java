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

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.provider.CallLog.Calls;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

/**
 * CursorAdapter getting Name, Phone from DB.
 * 
 * @author flx
 */
public class ConversationListAdapter extends SimpleCursorAdapter {
	/** Tag for logging. */
	final static String TAG = "SMSdroid.cla";

	/** INDEX: name. */
	public static final int NAME_INDEX = 1;
	/** INDEX: number. */
	public static final int NUMBER_INDEX = 2;
	/** INDEX: type. */
	public static final int NUMBER_TYPE = 3;

	private static final String DATE_FORMAT = "dd.MM. kk:mm";

	/** Global ContentResolver. */
	private ContentResolver mContentResolver;

	/** Cursor's projection. */
	public static final String[] PROJECTION = { //
	"_id",// 0
			Calls.DATE, // 1
			"address", // 2
			"thread_id", // 3
			"body", // 4
			Calls.TYPE, // 5
	};

	public static final String SORT = Calls.DATE + " DESC";

	/**
	 * {@inheritDoc}
	 */
	public ConversationListAdapter(final Context context, final Cursor c) {
		super(context, R.layout.conversationlist_item, c, new String[0],
				new int[0]);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final void bindView(final View view, final Context context,
			final Cursor cursor) {
		String s = "";
		int t = cursor.getInt(5);
		if (t == Calls.INCOMING_TYPE) {
			s = "<< ";
		} else if (t == Calls.OUTGOING_TYPE) {
			s = ">> ";
		}
		((TextView) view.findViewById(R.id.text1)).setText(s
				+ cursor.getString(2));
		((TextView) view.findViewById(R.id.text2)).setText(cursor.getString(4));
		((TextView) view.findViewById(R.id.text3)).setText(DateFormat.format(
				DATE_FORMAT, Long.parseLong(cursor.getString(1))));
	}
}
