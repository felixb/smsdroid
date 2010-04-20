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

import android.os.AsyncTask;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * @author flx
 */
public final class AsyncHelper extends AsyncTask<Void, Void, Void> {
	/**
	 * Fill Views by address.
	 * 
	 * @param address
	 *            address
	 * @param tvName
	 *            {@link TextView} for name
	 * @param ivPhoto
	 *            {@link ImageView} for photo
	 */
	private AsyncHelper(final String address, final TextView tvName,
			final ImageView ivPhoto) {
		// TODO Auto-generated constructor stub
	}

	/**
	 * Fill Views by threadId.
	 * 
	 * @param threadId
	 *            threadId
	 * @param tvAddress
	 *            {@link TextView} for address
	 * @param tvName
	 *            {@link TextView} for name
	 * @param tvCount
	 *            {@link TextView} for count
	 */
	private AsyncHelper(final long threadId, final TextView tvAddress,
			final TextView tvName, final TextView tvCount) {
		// TODO Auto-generated constructor stub
	}

	/**
	 * Fill Views by address.
	 * 
	 * @param address
	 *            address
	 * @param tvName
	 *            {@link TextView} for name
	 * @param ivPhoto
	 *            {@link ImageView} for photo
	 */
	public static void fillByAddress(final String address,
			final TextView tvName, final ImageView ivPhoto) {
		new AsyncHelper(address, tvName, ivPhoto).execute((Void[]) null);
	}

	/**
	 * Fill Views by threadId.
	 * 
	 * @param threadId
	 *            threadId
	 * @param tvAddress
	 *            {@link TextView} for address
	 * @param tvName
	 *            {@link TextView} for name
	 * @param tvCount
	 *            {@link TextView} for count
	 */
	public static void fillByThread(final long threadId,
			final TextView tvAddress, final TextView tvName,
			final TextView tvCount) {
		new AsyncHelper(threadId, tvAddress, tvName, tvCount)
				.execute((Void[]) null);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected Void doInBackground(final Void... arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	protected void onPostExecute(final Void... result) {
		// TODO Auto-generated method stub
	}
}
