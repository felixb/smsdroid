/**
 * 
 */
package de.ub0r.android.smsdroid;

import java.util.ArrayList;

import android.app.Activity;
import android.app.PendingIntent;
import android.app.PendingIntent.CanceledException;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.text.TextUtils;
import android.widget.Toast;
import de.ub0r.android.lib.Log;
import de.ub0r.android.lib.apis.TelephonyWrapper;

/**
 * Class sending messages via standard Messaging interface.
 * 
 * @author flx
 */
public class Sender extends Activity {
	/** Tag for output. */
	private static final String TAG = "send";

	/** {@link TelephonyWrapper}. */
	private static final TelephonyWrapper TWRAPPER = TelephonyWrapper
			.getInstance();

	/** {@link Uri} for saving messages. */
	private static final Uri URI_SMS = Uri.parse("content://sms");
	/** {@link Uri} for saving sent messages. */
	private static final Uri URI_SENT = Uri.parse("content://sms/sent");
	/** Projection for getting the id. */
	private static final String[] PROJECTION_ID = // .
	new String[] { BaseColumns._ID };
	/** SMS DB: address. */
	private static final String ADDRESS = "address";
	/** SMS DB: read. */
	private static final String READ = "read";
	/** SMS DB: type. */
	public static final String TYPE = "type";
	/** SMS DB: body. */
	private static final String BODY = "body";
	/** SMS DB: date. */
	private static final String DATE = "date";

	/** Message set action. */
	public static final String MESSAGE_SENT_ACTION = // .
	"com.android.mms.transaction.MESSAGE_SENT";

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.parseIntent(this.getIntent());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected final void onNewIntent(final Intent intent) {
		super.onNewIntent(intent);
		this.parseIntent(intent);
	}

	/**
	 * Parse data pushed by {@link Intent}.
	 * 
	 * @param intent
	 *            {@link Intent}
	 */
	private void parseIntent(final Intent intent) {
		Log.d(TAG, "parseIntent(" + intent + ")");
		if (intent == null) {
			this.finish();
			return;
		}
		Log.d(TAG, "got action: " + intent.getAction());

		String address = null;
		String u = intent.getDataString();
		if (!TextUtils.isEmpty(u) && u.contains(":")) {
			address = u.split(":")[1];
		}
		u = null;

		CharSequence cstext = intent.getCharSequenceExtra(Intent.EXTRA_TEXT);
		String text = null;
		if (cstext != null) {
			text = cstext.toString();
			cstext = null;
		}
		if (TextUtils.isEmpty(text)) {
			Log.e(TAG, "text missing");
			Toast
					.makeText(this, R.string.error_missing_text,
							Toast.LENGTH_LONG).show();
			this.finish();
			return;
		}
		if (TextUtils.isEmpty(address)) {
			Log.e(TAG, "recipient missing");
			Toast.makeText(this, R.string.error_missing_reciepient,
					Toast.LENGTH_LONG).show();
			this.finish();
			return;
		}

		Log.d(TAG, "text: " + text);
		int[] l = TWRAPPER.calculateLength(text, false);
		Log.i(TAG, "text7: " + text.length() + ", " + l[0] + " " + l[1] + " "
				+ l[2] + " " + l[3]);
		l = TWRAPPER.calculateLength(text, true);
		Log.i(TAG, "text8: " + text.length() + ", " + l[0] + " " + l[1] + " "
				+ l[2] + " " + l[3]);

		// save draft
		final ContentResolver cr = this.getContentResolver();
		ContentValues values = new ContentValues();
		values.put(TYPE, Message.SMS_DRAFT);
		values.put(BODY, text);
		values.put(READ, 1);
		values.put(ADDRESS, address);
		Uri draft = null;
		// save sms to content://sms/sent
		Cursor cursor = cr.query(URI_SMS, PROJECTION_ID, TYPE + " = "
				+ Message.SMS_DRAFT + " AND " + ADDRESS + " = '" + address
				+ "' AND " + BODY + " like '" + text.replace("'", "_") + "'",
				null, DATE + " DESC");
		if (cursor != null && cursor.moveToFirst()) {
			draft = URI_SENT // .
					.buildUpon().appendPath(cursor.getString(0)).build();
			Log.d(TAG, "skip saving draft: " + draft);
		} else {
			draft = cr.insert(URI_SENT, values);
			Log.d(TAG, "draft saved: " + draft);
		}
		values = null;
		if (cursor != null && !cursor.isClosed()) {
			cursor.close();
		}
		cursor = null;

		final ArrayList<String> messages = TWRAPPER.divideMessage(text);
		final int c = messages.size();
		ArrayList<PendingIntent> sentIntents = new ArrayList<PendingIntent>(c);

		try {
			Log.d(TAG, "send messages to: " + address);

			for (int i = 0; i < c; i++) {
				final String m = messages.get(i);
				Log.d(TAG, "devided messages: " + m);

				final Intent sent = new Intent(MESSAGE_SENT_ACTION, draft,
						this, SmsReceiver.class);
				sentIntents.add(PendingIntent.getBroadcast(this, 0, sent, 0));
			}
			TWRAPPER.sendMultipartTextMessage(address, null, messages,
					sentIntents, null);
			Log.i(TAG, "message sent");
		} catch (Exception e) {
			Log.e(TAG, "unexpected error", e);
			for (PendingIntent pi : sentIntents) {
				if (pi != null) {
					try {
						pi.send();
					} catch (CanceledException e1) {
						Log.e(TAG, "unexpected error", e1);
					}
				}
			}
			this.finish();
			return;
		}

		this.finish();
	}
}
