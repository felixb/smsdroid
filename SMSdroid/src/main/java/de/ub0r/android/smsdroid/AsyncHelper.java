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

import android.content.Context;
import android.os.AsyncTask;

import java.util.concurrent.RejectedExecutionException;

import de.ub0r.android.logg0r.Log;

/**
 * @author flx
 */
public final class AsyncHelper extends AsyncTask<Void, Void, Void> {

    /**
     * Tag for logging.
     */
    static final String TAG = "ash";

    /**
     * {@link ConversationAdapter} to invalidate on new data.
     */
    private static ConversationAdapter adapter = null;

    /**
     * {@link Context}.
     */
    private final Context context;

    /**
     * {@link Conversation}.
     */
    private final Conversation conv;

    /**
     * Changed anything?
     */
    private boolean changed = false;

    /**
     * Fill {@link Conversation}.
     *
     * @param c   {@link Context}
     * @param con {@link Conversation}
     */

    private AsyncHelper(final Context c, final Conversation con) {
        context = c;
        conv = con;
    }

    /**
     * Fill Conversations data. If needed: spawn threads.
     *
     * @param context {@link Context}
     * @param c       {@link Conversation}
     * @param sync    fetch of information
     */
    public static void fillConversation(final Context context, final Conversation c,
            final boolean sync) {
        Log.d(TAG, "fillConversation(ctx, conv, ", sync, ")");
        if (context == null || c == null || c.getThreadId() < 0) {
            return;
        }
        AsyncHelper helper = new AsyncHelper(context, c);
        if (sync) {
            helper.doInBackground((Void) null);
        } else {
            try {
                helper.execute((Void) null);
            } catch (RejectedExecutionException e) {
                Log.e(TAG, "rejected execution", e);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Void doInBackground(final Void... arg0) {
        if (conv == null) {
            return null;
        }
        Log.d(TAG, "doInBackground()");
        try {
            changed = conv.getContact().update(context, true,
                    ConversationListActivity.showContactPhoto);
        } catch (NullPointerException e) {
            Log.e(TAG, "error updating contact", e);
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onPostExecute(final Void result) {
        if (changed && adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    /**
     * Set {@link ConversationAdapter} to invalidate data after refreshing.
     *
     * @param a {@link ConversationAdapter}
     */
    public static void setAdapter(final ConversationAdapter a) {
        adapter = a;
    }
}
