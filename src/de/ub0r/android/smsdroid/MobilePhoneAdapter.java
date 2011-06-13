/*
 * Copyright (C) 2010 Felix Bechstein
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
import android.view.View;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;
import de.ub0r.android.lib.DbUtils;
import de.ub0r.android.lib.Utils;
import de.ub0r.android.lib.apis.ContactsWrapper;

/**
 * CursorAdapter getting Name, Phone from DB.
 * 
 * @author flx
 */
public class MobilePhoneAdapter extends ResourceCursorAdapter {
	/** Preferences: show mobile numbers only. */
	private static boolean prefsMobilesOnly;

	/** Global ContentResolver. */
	private ContentResolver mContentResolver;

	/** {@link ContactsWrapper} to use. */
	private static final ContactsWrapper WRAPPER = ContactsWrapper
			.getInstance();

	/** {@link Uri} to content. */
	private static final Uri URI = WRAPPER.getContentUri();
	/** Projection for content. */
	private static final String[] PROJECTION = WRAPPER.getContentProjection();
	/** Order for content. */
	private static final String SORT = WRAPPER.getContentSort();

	/** List of number types. */
	final String[] types;

	/**
	 * Constructor.
	 * 
	 * @param context
	 *            context
	 */
	public MobilePhoneAdapter(final Context context) {
		super(context, R.layout.recipient_dropdown_item, null);
		this.mContentResolver = context.getContentResolver();
		this.types = context.getResources().getStringArray(
				android.R.array.phoneTypes);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final void bindView(final View view, final Context context,
			final Cursor cursor) {
		((TextView) view.findViewById(R.id.text1)).setText(cursor
				.getString(ContactsWrapper.CONTENT_INDEX_NAME));
		((TextView) view.findViewById(R.id.text2)).setText(cursor
				.getString(ContactsWrapper.CONTENT_INDEX_NUMBER));
		final int i = cursor.getInt(ContactsWrapper.CONTENT_INDEX_TYPE) - 1;
		if (i >= 0 && i < this.types.length) {
			((TextView) view.findViewById(R.id.text3)).setText(this.types[i]);
		} else {
			((TextView) view.findViewById(R.id.text3)).setText("");
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final String convertToString(final Cursor cursor) {
		final String name = cursor
				.getString(ContactsWrapper.CONTENT_INDEX_NAME);
		final String number = cursor
				.getString(ContactsWrapper.CONTENT_INDEX_NUMBER);
		if (name == null || name.length() == 0) {
			return Utils.cleanRecipient(number);
		}
		return name + " <" + Utils.cleanRecipient(number) + '>';
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final Cursor runQueryOnBackgroundThread(// .
			final CharSequence constraint) {
		String where = null;
		if (constraint != null) {
			where = WRAPPER.getContentWhere(constraint.toString());
			if (prefsMobilesOnly) {
				where = DbUtils.sqlAnd(where, WRAPPER.getMobilesOnlyString());
			}
		}

		final Cursor cursor = this.mContentResolver.query(URI, PROJECTION,
				where, null, SORT);
		return cursor;
	}

	/**
	 * @param b
	 *            set to true, if only mobile numbers should be displayed.
	 */
	static final void setMoileNubersObly(final boolean b) {
		prefsMobilesOnly = b;
	}
}
