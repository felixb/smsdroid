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

import android.app.ListActivity;
import android.content.ActivityNotFoundException;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;

/**
 * {@link ListActivity} showing a single conversation.
 * 
 * @author flx
 */
public class MessageList extends ListActivity implements OnClickListener {
	/** Tag for output. */
	private static final String TAG = "SMSdroid.ml";

	/** Address. */
	private String address = null;
	/** URI to resolve. */
	static final String URI = "content://mms-sms/conversations/";

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.messagelist);

		// if (!SMSdroid.prefsNoAds) {
		// this.findViewById(R.id.ad).setVisibility(View.VISIBLE);
		// }

		this.findViewById(R.id.answer).setOnClickListener(this);

		final Intent i = this.getIntent();
		final Uri uri = i.getData();
		if (uri != null) {
			this.parseIntent(i);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected final void onNewIntent(final Intent intent) {
		super.onNewIntent(intent);
		final Uri uri = intent.getData();
		if (uri != null) {
			this.parseIntent(intent);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public final void onClick(final View v) {
		switch (v.getId()) {
		case R.id.answer:
			try {
				final Intent i = new Intent(Intent.ACTION_SENDTO);
				i.setData(Uri.parse("smsto:" + this.address));
				this.startActivity(i);
			} catch (ActivityNotFoundException e) {
				Log.e(TAG, "could not find app to compose message", e);
			}
			break;
		default:
			break;
		}
	}

	/**
	 * Parse data pushed by {@link Intent}.
	 * 
	 * @param intent
	 *            {@link Intent}
	 */
	private void parseIntent(final Intent intent) {
		final Uri uri = intent.getData();
		Log.d(TAG, "got intent: " + uri.toString());

		List<String> p = uri.getPathSegments();
		String threadID = p.get(p.size() - 1);

		Cursor mCursor = this.getContentResolver().query(uri,
				MessageListAdapter.PROJECTION, null, null,
				MessageListAdapter.SORT);
		this.startManagingCursor(mCursor);
		MessageListAdapter adapter = new MessageListAdapter(this, mCursor);
		this.setListAdapter(adapter);
		if (mCursor.moveToFirst()) {
			this.address = mCursor.getString(MessageListAdapter.INDEX_ADDRESS);
		}
		this.setTitle(this.getString(R.string.app_name) + " > " + this.address);
		this.setRead(threadID);
		SmsReceiver.updateNewMessageNotification(this, -1);

	}

	/**
	 * Set all messages in a given thread as read.
	 * 
	 * @param threadID
	 *            thread id
	 */
	private void setRead(final String threadID) {
		final Cursor mCursor = this.getContentResolver().query(
				Uri.parse(URI + threadID), MessageListAdapter.PROJECTION,
				MessageListAdapter.SELECTION_UNREAD, null, null);
		if (mCursor.getCount() <= 0) {
			return;
		}

		final ContentValues cv = new ContentValues();
		cv.put(MessageListAdapter.PROJECTION[MessageListAdapter.INDEX_READ], 1);
		this.getContentResolver().update(Uri.parse(URI + threadID), cv,
				MessageListAdapter.SELECTION_UNREAD, null);
	}
}
