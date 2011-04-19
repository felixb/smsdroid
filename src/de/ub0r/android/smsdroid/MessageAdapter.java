/*
 * Copyright (C) 2010-2011 Felix Bechstein
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
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.MergeCursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;
import de.ub0r.android.lib.Log;
import de.ub0r.android.lib.apis.Contact;

/**
 * Adapter for the list of {@link Conversation}s.
 * 
 * @author flx
 */
public class MessageAdapter extends ResourceCursorAdapter {
	/** Tag for logging. */
	static final String TAG = "msa";

	/** Used background drawable for messages. */
	private final int backgroundDrawableIn, backgroundDrawableOut;

	/** General WHERE clause. */
	private static final String WHERE = // .
	"(" + Message.PROJECTION_JOIN[Message.INDEX_TYPE] + " != "
			+ Message.SMS_DRAFT + " OR "
			+ Message.PROJECTION_JOIN[Message.INDEX_TYPE] + " IS NULL)";

	/** WHERE clause for drafts. */
	private static final String WHERE_DRAFT = // .
	"(" + Message.PROJECTION_SMS[Message.INDEX_THREADID] + " = ? AND "
			+ Message.PROJECTION_SMS[Message.INDEX_TYPE] + " = "
			+ Message.SMS_DRAFT + ")";
	// + " OR " + type + " = " + Message.SMS_PENDING;

	/** Thread id. */
	private int threadId = -1;
	/** Address. */
	private String address = null;
	/** Name. */
	private String name = null;
	/** Display Name (name if !=null, else address). */
	private String displayName = null;

	/** Used text size/color. */
	private final int textSize, textColor;

	/**
	 * Default Constructor.
	 * 
	 * @param c
	 *            {@link MessageList}
	 * @param u
	 *            {@link Uri}
	 */
	public MessageAdapter(final MessageList c, final Uri u) {
		super(c, R.layout.messagelist_item,
				getCursor(c.getContentResolver(), u), true);
		this.backgroundDrawableIn = Preferences.getBubblesIn(c);
		this.backgroundDrawableOut = Preferences.getBubblesOut(c);
		this.textSize = Preferences.getTextsize(c);
		this.textColor = Preferences.getTextcolor(c);
		if (u == null || u.getLastPathSegment() == null) {
			this.threadId = -1;
		} else {
			this.threadId = Integer.parseInt(u.getLastPathSegment());
		}
		final Conversation conv = Conversation.getConversation(c,
				this.threadId, false);
		if (conv == null) {
			this.address = null;
			this.name = null;
			this.displayName = null;
		} else {
			final Contact contact = conv.getContact();
			this.address = contact.getNumber();
			this.name = contact.getName();
			this.displayName = contact.getDisplayName();
		}
		Log.d(TAG, "address: " + this.address);
		Log.d(TAG, "name: " + this.name);
		Log.d(TAG, "displayName: " + this.displayName);
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
		Log.d(TAG, "getCursor(" + u + ")");
		final Cursor[] c = new Cursor[] { null, null };

		int tid = -1;
		try {
			tid = Integer.parseInt(u.getLastPathSegment());
		} catch (Exception e) {
			Log.e(TAG, "error parsing uri: " + u, e);
		}

		try {
			Log.d(TAG, "where: " + WHERE);
			c[0] = cr.query(u, Message.PROJECTION_JOIN, WHERE, null, null);
		} catch (NullPointerException e) {
			Log.e(TAG, "error query: " + u + " / " + WHERE, e);
			c[0] = null;
		}

		final String[] sel = new String[] { String.valueOf(tid) };
		try {
			Log.d(TAG, "where: " + WHERE_DRAFT + " / sel: " + sel);
			c[1] = cr.query(Uri.parse("content://sms/"),
					Message.PROJECTION_SMS, WHERE_DRAFT, sel, Message.SORT_USD);
		} catch (NullPointerException e) {
			Log.e(TAG, "error query: " + u + " / " + WHERE_DRAFT + " sel: "
					+ sel, e);
			c[1] = null;
		}

		if (c[1] == null || c[1].getCount() == 0) {
			return c[0];
		}
		if (c[0] == null || c[0].getCount() == 0) {
			return c[1];
		}

		return new MergeCursor(c);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final void bindView(final View view, final Context context,
			final Cursor cursor) {
		final Message m = Message.getMessage(context, cursor);

		final TextView tvPerson = (TextView) view.findViewById(R.id.addr);
		final TextView tvBody = (TextView) view.findViewById(R.id.body);
		final TextView tvDate = (TextView) view.findViewById(R.id.date);
		if (this.textSize > 0) {
			tvBody.setTextSize(this.textSize);
		}
		final int col = this.textColor;
		if (col != 0) {
			tvPerson.setTextColor(col);
			tvBody.setTextColor(col);
			tvDate.setTextColor(col);
		}
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
			tvPerson.setText(context.getString(R.string.me) + subject);
			try {
				view.findViewById(R.id.layout).setBackgroundResource(
						this.backgroundDrawableOut);
			} catch (OutOfMemoryError e) {
				Log.e(TAG, "OOM while setting bg", e);
			}
			((ImageView) view.findViewById(R.id.inout))
					.setImageResource(R.drawable.// .
					ic_call_log_list_outgoing_call);
			break;
		case Message.SMS_IN:
		case Message.MMS_IN:
		default:
			tvPerson.setText(this.displayName + subject);
			try {
				view.findViewById(R.id.layout).setBackgroundResource(
						this.backgroundDrawableIn);
			} catch (OutOfMemoryError e) {
				Log.e(TAG, "OOM while setting bg", e);
			}
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

		final long time = m.getDate();
		tvDate.setText(ConversationList.getDate(context, time));

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
			ivPicture.setOnClickListener(SMSdroid.getOnClickStartActivity(
					context, i));
			ivPicture.setOnLongClickListener(m
					.getSaveAttachmentListener(context));
		} else {
			ivPicture.setVisibility(View.GONE);
			ivPicture.setOnClickListener(null);
		}

		Button btn = (Button) view.findViewById(R.id.btn_download_msg);
		CharSequence text = m.getBody();
		if (text == null && pic == null) {
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
					context.startActivity(Intent.createChooser(i,
							context.getString(R.string.view_mms)));
				}
			});

			btn.setVisibility(View.VISIBLE);
		} else {
			btn.setVisibility(View.GONE);
		}
		if (text == null) {
			tvBody.setVisibility(View.INVISIBLE);
			view.findViewById(R.id.btn_import_contact).setVisibility(View.GONE);
		} else {
			tvBody.setText(Preferences.decodeDecimalNCR(context) ? Converter
					.convertDecNCR2Char(text) : text);
			tvBody.setVisibility(View.VISIBLE);
			String stext = text.toString();
			if (stext.contains("BEGIN:VCARD") && stext.contains("END:VCARD")) {
				stext = stext.replaceAll(".*BEGIN:VCARD", "BEGIN:VCARD");
				stext = stext.replaceAll("END:VCARD.*", "END:VCARD");
				btn = (Button) view.findViewById(R.id.btn_import_contact);
				btn.setVisibility(View.VISIBLE);
				btn.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(final View v) {
						final Intent i = new Intent(Intent.ACTION_VIEW);
						Uri uri = ContentUris.withAppendedId(
								MessageProvider.CONTENT_URI, m.getId());
						i.setDataAndType(uri, "text/x-vcard");
						try {
							context.startActivity(i);
						} catch (ActivityNotFoundException e) {
							Log.e(TAG, "activity not found (text/x-vcard): "
									+ i.getAction(), e);
							Toast.makeText(context,
									"Activity not found: text/x-vcard",
									Toast.LENGTH_LONG).show();
						}
					}
				});
			} else {
				view.findViewById(R.id.btn_import_contact).setVisibility(
						View.GONE);
			}
		}
	}
}
