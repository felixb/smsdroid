package de.ub0r.android.smsdroid;

import java.util.List;

import android.app.ListActivity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;

public class MessageList extends ListActivity implements OnClickListener {
	/** Tag for output. */
	private static final String TAG = "SMSdroid.ml";

	/** Address. */
	private String address = null;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.messagelist);

		final Intent i = this.getIntent();
		final Uri uri = i.getData();
		Log.d(TAG, "launched with data: " + uri.toString());
		List<String> p = uri.getPathSegments();
		String threadID = p.get(p.size() - 1);

		Cursor mCursor = this.getContentResolver().query(
				Uri.parse("content://sms"), MessageListAdapter.PROJECTION,
				MessageListAdapter.SELECTION.replace("?", threadID), null,
				MessageListAdapter.SORT);
		this.startManagingCursor(mCursor);
		MessageListAdapter adapter = new MessageListAdapter(this, mCursor);
		this.setListAdapter(adapter);
		if (mCursor.moveToFirst()) {
			this.address = mCursor.getString(MessageListAdapter.INDEX_ADDRESS);
		}
		this.findViewById(R.id.answer).setOnClickListener(this);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onClick(final View v) {
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
}
