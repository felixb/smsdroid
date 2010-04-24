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
package de.ub0r.android.smsdroid.cache;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.widget.ImageView;
import android.widget.TextView;
import de.ub0r.android.smsdroid.Conversation;
import de.ub0r.android.smsdroid.R;

/**
 * @author flx
 */
public final class AsyncHelper extends AsyncTask<Void, Void, Void> {
	/** {@link Context}. */
	private final Context context;

	/** Address. */
	private String mAddress;
	/** Thread id. */
	private final long mThreadId;
	/** {@link Conversation}. */
	private final Conversation mConversation;
	/** Photo. */
	private Bitmap mPhoto = null;
	/** Name. */
	private String mName = null;
	/** Message count. */
	private int mCount = -1;

	/** {@link TextView} for name. */
	private final TextView targetTvName;
	/** {@link ImageView} for photo. */
	private final ImageView targetIvPhoto;
	/** {@link TextView} for address. */
	private final TextView targetTvAddress;
	/** {@link TextView} for count. */
	private final TextView targetTvCount;

	/**
	 * Fill Views by address.
	 * 
	 * @param c
	 *            {@link Context}
	 * @param address
	 *            address
	 * @param tvName
	 *            {@link TextView} for name
	 * @param ivPhoto
	 *            {@link ImageView} for photo
	 */
	private AsyncHelper(final Context c, final String address,
			final TextView tvName, final ImageView ivPhoto) {
		this.context = c;

		this.mThreadId = -1;
		this.mConversation = null;
		this.mAddress = address;

		this.targetTvName = tvName;
		this.targetIvPhoto = ivPhoto;

		this.targetTvAddress = null;
		this.targetTvCount = null;
	}

	/**
	 * Fill Views by threadId.
	 * 
	 * @param c
	 *            {@link Context}
	 * @param conversation
	 *            {@link Conversation}
	 * @param threadId
	 *            threadId
	 * @param tvAddress
	 *            {@link TextView} for address
	 * @param tvName
	 *            {@link TextView} for name
	 * @param tvCount
	 *            {@link TextView} for count
	 * @param ivPhoto
	 *            {@link ImageView} for photo
	 */
	private AsyncHelper(final Context c, final Conversation conversation,
			final long threadId, final TextView tvAddress,
			final TextView tvName, final TextView tvCount,
			final ImageView ivPhoto) {
		this.context = c;

		if (conversation != null) {
			this.mConversation = conversation;
			this.mThreadId = conversation.getThreadId();
			this.mAddress = conversation.getAddress();
		} else {
			this.mAddress = null;
			this.mConversation = null;
			this.mThreadId = threadId;
		}

		this.targetTvAddress = tvAddress;
		this.targetTvName = tvName;
		this.targetTvCount = tvCount;
		this.targetIvPhoto = ivPhoto;
	}

	/**
	 * Fill Views by address.
	 * 
	 * @param context
	 *            {@link Context}
	 * @param address
	 *            address
	 * @param tvName
	 *            {@link TextView} for name
	 * @param ivPhoto
	 *            {@link ImageView} for photo
	 */
	public static void fillByAddress(final Context context,
			final String address, final TextView tvName, // .
			final ImageView ivPhoto) {
		if (context == null || address == null) {
			return;
		}
		if (Persons.poke(address, ivPhoto != null) || false) {
			// load sync.
			if (tvName != null) {
				tvName.setText(Persons.getName(context, address, true));
			}
			if (ivPhoto != null) {
				ivPhoto.setImageBitmap(Persons.getPicture(context, address));
			}
		} else {
			// load async.
			new AsyncHelper(context, address, tvName, ivPhoto)
					.execute((Void[]) null);
		}

	}

	/**
	 * Fill Views by threadId.
	 * 
	 * @param context
	 *            {@link Context}
	 * @param conversation
	 *            {@link Conversation}
	 * @param threadId
	 *            threadId
	 * @param tvAddress
	 *            {@link TextView} for address
	 * @param tvName
	 *            {@link TextView} for name
	 * @param tvCount
	 *            {@link TextView} for count
	 * @param ivPhoto
	 *            {@link ImageView} for photo
	 */
	public static void fillByThread(final Context context,
			final Conversation conversation, final long threadId,
			final TextView tvAddress, final TextView tvName,
			final TextView tvCount, final ImageView ivPhoto) {
		long tId = threadId;
		if (tId < 0 && conversation != null) {
			tId = conversation.getId();
		}
		if (context == null || tId < 0) {
			return;
		}
		if (Threads.poke(tId) || true) {
			if (tvAddress != null || tvName != null || ivPhoto != null) {
				final String a = Threads.getAddress(context, tId);
				if (conversation != null && conversation.getAddress() == null
						&& a != null) {
					conversation.setAddress(a);
				}
				if (tvAddress != null) {
					tvAddress.setText(a);
				}
				if (tvName != null || ivPhoto != null) {
					fillByAddress(context, a, tvName, ivPhoto);
				}
			}
			if (tvCount != null) {
				tvCount.setText(// .
						"(" + Threads.getCount(context, tId) + ")");
			}
		} else {
			new AsyncHelper(context, conversation, tId, tvAddress, tvName,
					tvCount, ivPhoto).execute((Void[]) null);
		}
	}

	/**
	 * Fill Conversations data. If needed: spawn threads.
	 * 
	 * @param context
	 *            {@link Context}
	 * @param c
	 *            {@link Conversation}
	 */
	public static void fillConversation(final Context context,
			final Conversation c) {
		if (context == null || c == null || c.getThreadId() < 0) {
			return;
		}
		final long tId = c.getThreadId();
		String a = c.getAddress();
		if (Threads.poke(tId) || true) {
			if (a == null) {
				a = Threads.getAddress(context, tId);
				c.setAddress(a);
			}
			c.setCount(Threads.getCount(context, tId));
			if (c.getName() == null) {
				c.setName(Persons.getName(context, a, false));
			}
			if (c.getPhoto() == null) {
				c.setPhoto(Persons.getPicture(context, a));
			}
		}
		// TODO: fork thread to read address, name, photo, count
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected Void doInBackground(final Void... arg0) {
		if (this.mThreadId < 0) { // run Persons
			// in: mAddress
			// out: *Photo, *Name
			if (this.targetIvPhoto != null) {
				this.mPhoto = Persons.getPicture(this.context, this.mAddress);
			}
			if (this.targetTvName != null) {
				this.mName = Persons
						.getName(this.context, this.mAddress, false);
			}
		} else { // run Threads
			// in: mThreadId
			// out: *Address, *Count, *Name, *Photo
			if (this.targetTvAddress != null || this.targetTvName != null
					|| this.targetIvPhoto != null) {
				this.mAddress = Threads
						.getAddress(this.context, this.mThreadId);
			}
			if (this.targetTvCount != null) {
				this.mCount = Threads.getCount(this.context, this.mThreadId);
			}
			if (this.targetTvName != null) {
				this.mName = Persons
						.getName(this.context, this.mAddress, false);
			}
			if (this.targetIvPhoto != null) {
				this.mPhoto = Persons.getPicture(this.context, this.mAddress);
			}
		}
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onPreExecute() {
		if (this.targetIvPhoto != null) {
			this.targetIvPhoto.setImageResource(R.drawable.ic_contact_picture);
		}
		if (this.targetTvAddress != null) {
			this.targetTvAddress.setText("...");
		}
		if (this.targetTvCount != null) {
			this.targetTvCount.setText("");
		}
		if (this.targetTvName != null) {
			if (this.mAddress != null) {
				this.targetTvName.setText(this.mAddress);
			} else {
				this.targetTvName.setText("...");
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onPostExecute(final Void result) {
		if (this.targetIvPhoto != null && this.mPhoto != null) {
			this.targetIvPhoto.setImageBitmap(this.mPhoto);
		}
		if (this.targetTvAddress != null && this.mAddress != null) {
			this.targetTvAddress.setText(this.mAddress);
		}
		if (this.targetTvCount != null && this.mCount > 0) {
			this.targetTvCount.setText("(" + this.mCount + ")");
		}
		if (this.targetTvName != null) {
			if (this.mName != null) {
				this.targetTvName.setText(this.mName);
			} else if (this.mAddress != null) {
				this.targetTvName.setText(this.mAddress);
			}
		}
		if (this.mConversation != null
				&& this.mConversation.getAddress() == null
				&& this.mAddress != null) {
			this.mConversation.setAddress(this.mAddress);
		}
	}
}
