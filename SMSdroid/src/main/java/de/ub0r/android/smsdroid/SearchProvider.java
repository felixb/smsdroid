/*
 * Copyright (C) 2009-2015-2012 Felix Bechstein
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

import android.app.SearchManager;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MergeCursor;
import android.net.Uri;
import android.text.TextUtils;

import de.ub0r.android.lib.Utils;
import de.ub0r.android.logg0r.Log;

/**
 * Provide search results.
 *
 * @author flx
 */
public final class SearchProvider extends ContentProvider {

    /**
     * Tag for logging.
     */
    static final String TAG = "sp";

    /**
     * Authority.
     */
    private static final String AUTHORITY = "de.ub0r.android.smsdroid.SearchProvider";

    /**
     * {@link Uri} to messages.
     */
    private static final Uri SMS_URI = Uri.parse("content://sms/");

    /**
     * {@inheritDoc}
     */
    @Override
    public String getType(final Uri uri) {
        Log.d(TAG, "getType(", uri, ")");
        throw new UnsupportedOperationException("not implemented");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Cursor query(final Uri uri, final String[] projection, final String selection,
            final String[] selectionArgs, final String sortOrder) {
        Log.d(TAG, "query(", uri, ",..)");
        if (uri == null) {
            return null;
        }
        final String query = uri.getLastPathSegment();
        Log.d(TAG, "query: ", query);
        if (TextUtils.isEmpty(query) || query.equals(SearchManager.SUGGEST_URI_PATH_QUERY)) {
            return null;
        }
        final int limit = Utils.parseInt(uri.getQueryParameter("limit"), -1);
        Log.d(TAG, "limit: ", limit);
        final String[] proj = new String[]{"_id",
                "address as " + SearchManager.SUGGEST_COLUMN_TEXT_1,
                "body as " + SearchManager.SUGGEST_COLUMN_TEXT_2};
        final String where = "body like '%" + query + "%'";
        return new MergeCursor(new Cursor[]{getContext().getContentResolver()
                .query(SMS_URI, proj, where, null, null)});
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onCreate() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int delete(final Uri uri, final String selection, final String[] selectionArgs) {
        throw new UnsupportedOperationException("not implemented");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Uri insert(final Uri uri, final ContentValues values) {
        throw new UnsupportedOperationException("not implemented");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int update(final Uri uri, final ContentValues values, final String selection,
            final String[] selectionArgs) {
        throw new UnsupportedOperationException("not implemented");
    }
}
