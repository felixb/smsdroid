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

import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.MergeCursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Adapter for the list of {@link Conversation}s.
 * 
 * @author flx
 */
public class MessagesAdapter extends ResourceCursorAdapter {
	/** Tag for logging. */
	static final String TAG = "msa";

	/** SQL WHERE: unread messages. */
	static final String SELECTION_UNREAD = "read = '0'";
	/** SQL WHERE: read messages. */
	static final String SELECTION_READ = "read = '1'";

	/** Used background drawable for messages. */
	private final int backgroundDrawableIn, backgroundDrawableOut;

	/** Thread id. */
	private int threadId = -1;
	/** Address. */
	private String address = null;
	/** Name. */
	private String name = null;
	/** Display Name (name if !=null, else address). */
	private String displayName = null;

	/** Used text size. */
	private final int textSize;

	/**
	 * Default Constructor.
	 * 
	 * @param c
	 *            {@link MessageList}
	 * @param u
	 *            {@link Uri}
	 */
	public MessagesAdapter(final MessageList c, final Uri u) {
		super(c, R.layout.messagelist_item,
				getCursor(c.getContentResolver(), u), true);
		boolean showBubbles = PreferenceManager.getDefaultSharedPreferences(c)
				.getBoolean(Preferences.PREFS_BUBBLES, false);
		if (showBubbles) {
			this.backgroundDrawableIn = R.drawable.bubble_in;
			this.backgroundDrawableOut = R.drawable.bubble_out;
		} else {
			if (Preferences.getTheme(c) == android.R.style.Theme_Black) {
				this.backgroundDrawableOut = R.drawable.grey_dark;
			} else {
				this.backgroundDrawableOut = R.drawable.grey_light;
			}
			this.backgroundDrawableIn = 0;
		}
		this.textSize = Preferences.getTextsize(c);
		this.threadId = Integer.parseInt(u.getLastPathSegment());
		this.address = Conversation.getConversation(c, this.threadId, false)
				.getAddress();
		this.name = AsyncHelper.getContactName(c, this.address);
		if (this.name == null) {
			this.displayName = this.address;
		} else {
			this.displayName = this.name;
		}
		Log.d(TAG, "address: " + this.address);
		Log.d(TAG, "name: " + this.name);
		Log.d(TAG, "displayName: " + this.displayName);

		// this.notifyDataSetChanged();
	}

	/**
	 * Get the {@link Cursor}.
	 * 
	 * @param cr
	 *            {@link ContentResolver}
	 * @param u
	 *            {@link Uri}
	 * @return {@link Cursor}
	 */
	private static Cursor getCursor(final ContentResolver cr, final Uri u) {
		final Cursor[] c = new Cursor[] { null, null };
		final String type = Message.PROJECTION_JOIN[Message.INDEX_TYPE];
		final String mtype = Message.PROJECTION_JOIN[Message.INDEX_MTYPE];

		int tid = -1;
		try {
			tid = Integer.parseInt(u.getLastPathSegment());
		} catch (Exception e) {
			Log.e(TAG, "error parsing uri: " + u, e);
		}
		final String twhere = Message.PROJECTION_SMS[Message.INDEX_THREADID]
				+ " = " + tid + " AND (";

		String where = twhere + type + " = " + Message.SMS_IN // .
				+ " OR " + type + " = " + Message.SMS_OUT // .
				+ " OR " + mtype + " = " + Message.MMS_TOLOAD // .
				+ " OR " + mtype + " = " + Message.MMS_IN // .
				+ " OR " + mtype + " = " + Message.MMS_OUT + ")";

		c[0] = cr.query(u, Message.PROJECTION_JOIN, where, null, null);

		where = twhere + type + " = " + Message.SMS_DRAFT + ")";
		// + " OR " + type + " = " + Message.SMS_PENDING;

		c[1] = cr.query(Uri.parse("content://sms/"), Message.PROJECTION_SMS,
				where, null, Message.SORT_USD);

		// where = twhere + mtype + " = " + Message.MMS_DRAFT + ")";
		// c[2] = cr.query(Uri.parse("content://mms/"), Message.PROJECTION_MMS,
		// where, null, Message.SORT_USD);
		return new MergeCursor(c);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final void bindView(final View view, final Context context,
			final Cursor cursor) {
		final Message m = Message.getMessage(context, cursor);

		final TextView twPerson = (TextView) view.findViewById(R.id.addr);
		TextView twBody = (TextView) view.findViewById(R.id.body);
		twBody.setTextSize(this.textSize);
		int t = m.getType();

		String subject = m.getSubject();
		if (subject == null) {
			subject = "";
		} else {
			subject = ": " + subject;
		}
		// incoming / outgoing / pending
		final View pending = view.findViewById(R.id.pending);
		int pendingvisability = View.GONE;
		switch (t) {
		case Message.SMS_DRAFT:
			// TODO case Message.SMS_PENDING:
			// case Message.MMS_DRAFT:
			pendingvisability = View.VISIBLE;
		case Message.SMS_OUT: // handle drafts/pending here too
		case Message.MMS_OUT:
			twPerson.setText(context.getString(R.string.me) + subject);
			view.setBackgroundResource(this.backgroundDrawableOut);
			((ImageView) view.findViewById(R.id.inout))
					.setImageResource(R.drawable.// .
					ic_call_log_list_outgoing_call);
			break;
		case Message.SMS_IN:
		case Message.MMS_IN:
		default:
			twPerson.setText(this.displayName + subject);
			view.setBackgroundResource(this.backgroundDrawableIn);
			((ImageView) view.findViewById(R.id.inout))
					.setImageResource(R.drawable.// .
					ic_call_log_list_incoming_call);
			pending.setVisibility(View.GONE);
			break;
		}
		pending.setVisibility(pendingvisability);

		// unread / read
		if (m.getRead() == 0) {
			view.findViewById(R.id.read).setVisibility(View.VISIBLE);
		} else {
			view.findViewById(R.id.read).setVisibility(View.INVISIBLE);
		}

		final Button btn = (Button) view.findViewById(R.id.btn_download_msg);
		CharSequence text = m.getBody();
		if (text == null) {
			btn.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(final View v) {
					// view.findViewById(R.id.label_downloading).setVisibility(
					// View.VISIBLE);
					// btn.setVisibility(View.GONE);
					// Intent intent = new Intent();
					// intent.setClassName("com.android.mms",
					// ".transaction.TransactionService");
					// intent.putExtra("uri", m.getUri().toString());
					// intent.putExtra("type", 1);
					// context.startService(intent);

					final Uri target = Uri.parse(MessageList.URI
							+ m.getThreadId());
					Intent i = new Intent(Intent.ACTION_VIEW, target);
					context.startActivity(Intent.createChooser(i, context
							.getString(R.string.view_mms)));
				}
			});

			btn.setVisibility(View.VISIBLE);
		} else {
			btn.setVisibility(View.GONE);
		}
		twBody.setText(text);

		long time = m.getDate();
		((TextView) view.findViewById(R.id.date)).setText(SMSdroid.getDate(
				context, time));

		ImageView ivPicture = (ImageView) view.findViewById(R.id.picture);
		final Bitmap pic = m.getPicture();
		if (pic != null) {
			if (pic == Message.BITMAP_PLAY) {
				ivPicture.setImageResource(R.drawable.mms_play_btn);
			} else {
				ivPicture.setImageBitmap(pic);
			}
			ivPicture.setVisibility(View.VISIBLE);
			final Intent i = m.getContentIntent();
			if (i == null) {
				ivPicture.setOnClickListener(null);
			} else {
				ivPicture.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(final View v) {
						try {
							context.startActivity(i);
						} catch (ActivityNotFoundException e) {
							Log.w(TAG, "activity not found", e);
							Toast.makeText(context,
									"no activity for data: " + i.getType(),
									Toast.LENGTH_LONG).show();
						}
					}
				});
			}
		} else {
			ivPicture.setVisibility(View.GONE);
			ivPicture.setOnClickListener(null);
		}
	}
}
