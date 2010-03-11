package de.ub0r.android.smsdroid;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;

public class SMSdroid extends ListActivity implements OnClickListener {
	/** Tag for output. */
	private static final String TAG = "SMSdroid";

	/** Prefs: name for last version run. */
	private static final String PREFS_LAST_RUN = "lastrun";

	/** Dialog: about. */
	private static final int DIALOG_ABOUT = 0;
	/** Dialog: update. */
	private static final int DIALOG_UPDATE = 1;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.conversationlist);

		SharedPreferences preferences = PreferenceManager
				.getDefaultSharedPreferences(this);
		// display changelog?
		String v0 = preferences.getString(PREFS_LAST_RUN, "");
		String v1 = this.getString(R.string.app_version);
		if (!v0.equals(v1)) {
			SharedPreferences.Editor editor = preferences.edit();
			editor.putString(PREFS_LAST_RUN, v1);
			editor.commit();
			this.showDialog(DIALOG_UPDATE);
		}

		Cursor mCursor = this.getContentResolver().query(
				Uri.parse("content://sms"), ConversationListAdapter.PROJECTION,
				null, null, ConversationListAdapter.SORT);
		this.startManagingCursor(mCursor);
		ConversationListAdapter adapter = new ConversationListAdapter(this,
				mCursor);
		this.setListAdapter(adapter);

		this.findViewById(R.id.new_message).setOnClickListener(this);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected final Dialog onCreateDialog(final int id) {
		Dialog d;
		switch (id) {
		case DIALOG_ABOUT:
			d = new Dialog(this);
			d.setContentView(R.layout.about);
			d.setTitle(this.getString(R.string.about_) + " v"
					+ this.getString(R.string.app_version));
			return d;
		case DIALOG_UPDATE:
			final AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle(R.string.changelog_);
			final String[] changes = this.getResources().getStringArray(
					R.array.updates);
			final StringBuilder buf = new StringBuilder(changes[0]);
			for (int i = 1; i < changes.length; i++) {
				buf.append("\n\n");
				buf.append(changes[i]);
			}
			builder.setIcon(android.R.drawable.ic_menu_info_details);
			builder.setMessage(buf.toString());
			builder.setCancelable(true);
			builder.setPositiveButton(android.R.string.ok,
					new DialogInterface.OnClickListener() {
						public void onClick(final DialogInterface dialog,
								final int id) {
							dialog.cancel();
						}
					});
			return builder.create();
		default:
			return null;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onClick(final View v) {
		switch (v.getId()) {
		case R.id.new_message:
			try {
				final Intent i = new Intent(Intent.ACTION_SENDTO);
				i.setData(Uri.parse("sms:"));
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
