/*
 * Copyright (C) 2010-2012 Felix Bechstein
 * 
 * This file is part of WebSMS.
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
import android.net.Uri;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.text.TextUtils;
import android.view.View;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;

/**
 * CursorAdapter getting Name, Phone from DB. This class requires android API5+
 * to work.
 * 
 * @author flx
 */
public class MobilePhoneAdapter extends ResourceCursorAdapter {
	/** Preferences: show mobile numbers only. */
	private static boolean prefsMobilesOnly = false;

	/** Global ContentResolver. */
	private ContentResolver mContentResolver;

	/** Projection for content. */
	private static final String[] PROJECTION = new String[] { Phone._ID, // 0
			Phone.DISPLAY_NAME, // 1
			Phone.NUMBER, // 2
			Phone.TYPE // 3
	};

	/** Index of id/lookup key. */
	public static final int INDEX_ID = 0;
	/** Index of name. */
	public static final int INDEX_NAME = 1;
	/** Index of number. */
	public static final int INDEX_NUMBER = 2;
	/** Index of type. */
	public static final int INDEX_TYPE = 3;

	/** List of number types. */
	private final String[] types;

	/**
	 * Constructor.
	 * 
	 * @param context
	 *            context
	 */
	public MobilePhoneAdapter(final Context context) {
		super(context, R.layout.recipient_dropdown_item, null, true);
		mContentResolver = context.getContentResolver();
		types = context.getResources().getStringArray(android.R.array.phoneTypes);
	}

	/** View holder. */
	private static class ViewHolder {
		/** {@link TextView}s. */
		TextView tv1, tv2, tv3;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final void bindView(final View view, final Context context, final Cursor cursor) {
		ViewHolder holder = (ViewHolder) view.getTag();
		if (holder == null) {
			holder = new ViewHolder();
			holder.tv1 = (TextView) view.findViewById(R.id.text1);
			holder.tv2 = (TextView) view.findViewById(R.id.text2);
			holder.tv3 = (TextView) view.findViewById(R.id.text3);
			view.setTag(holder);
		}
		holder.tv1.setText(cursor.getString(INDEX_NAME));
		holder.tv2.setText(cursor.getString(INDEX_NUMBER));
		final int i = cursor.getInt(INDEX_TYPE) - 1;
		if (i >= 0 && i < types.length) {
			holder.tv3.setText(types[i]);
		} else {
			holder.tv3.setText("");
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final String convertToString(final Cursor cursor) {
		final String name = cursor.getString(INDEX_NAME);
		final String number = cursor.getString(INDEX_NUMBER);
		if (TextUtils.isEmpty(name)) {
			return cleanRecipient(number);
		}
		return name + " <" + cleanRecipient(number) + '>';
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final Cursor runQueryOnBackgroundThread(final CharSequence constraint) {
		String s = constraint == null ? null : constraint.toString();
		String where = prefsMobilesOnly ? Phone.TYPE + " = " + Phone.TYPE_MOBILE + " OR "
				+ Phone.TYPE + " = " + Phone.TYPE_WORK_MOBILE : null;
		Uri u = Uri.withAppendedPath(Phone.CONTENT_FILTER_URI, Uri.encode(s));
		Cursor cursor = mContentResolver.query(u, PROJECTION, where, null, Phone.DISPLAY_NAME);
		return cursor;
	}

	/**
	 * @param b
	 *            set to true, if only mobile numbers should be displayed.
	 */
	static final void setMobileNumbersOnly(final boolean b) {
		prefsMobilesOnly = b;
	}

	/**
	 * Clean recipient's phone number from [ -.()<>].
	 * 
	 * @param recipient
	 *            recipient's mobile number
	 * @return clean number
	 */
	public static String cleanRecipient(final String recipient) {
		if (TextUtils.isEmpty(recipient)) {
			return "";
		}
		String n;
		int i = recipient.indexOf("<");
		int j = recipient.indexOf(">");
		if (i != -1 && i < j) {
			n = recipient.substring(recipient.indexOf("<"), recipient.indexOf(">"));
		} else {
			n = recipient;
		}
		return n.replaceAll("[^*#+0-9]", "").replaceAll("^[*#][0-9]*#", "");
	}
}
