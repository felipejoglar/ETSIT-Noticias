package com.fjoglar.etsitnoticias.data;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.SharedPreferences;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.preference.PreferenceManager;

import com.fjoglar.etsitnoticias.R;

public class RssProvider extends ContentProvider {

    // El URI Matcher usado por este content provider.
    private static final UriMatcher sUriMatcher = buildUriMatcher();
    private RssDbHelper mRssDbHelper;

    static final int RSS = 100;
    static final int RSS_ITEM = 101;

    static UriMatcher buildUriMatcher() {
        // All paths added to the UriMatcher have a corresponding code to return when a match is
        // found.  The code passed into the constructor represents the code to return for the root
        // URI.  It's common to use NO_MATCH as the code for this case.
        final UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
        final String authority = RssContract.CONTENT_AUTHORITY;

        // For each type of URI you want to add, create a corresponding code.
        matcher.addURI(authority, RssContract.PATH_RSS, RSS);
        matcher.addURI(authority, RssContract.PATH_RSS + "/#", RSS_ITEM);

        return matcher;
    }

    @Override
    public boolean onCreate() {
        mRssDbHelper = new RssDbHelper(getContext());
        return true;
    }

    @Override
    public String getType(Uri uri) {

        // Use the Uri Matcher to determine what kind of URI this is.
        final int match = sUriMatcher.match(uri);

        switch (match) {
            case RSS:
                return RssContract.RssEntry.CONTENT_TYPE;
            case RSS_ITEM:
                return RssContract.RssEntry.CONTENT_ITEM_TYPE;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {
        Cursor retCursor;
        switch (sUriMatcher.match(uri)) {
            case RSS: {
                // Comprobamos las categorías que debemos mostrar.
                String where = "";
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
                if (prefs.getBoolean(getContext().getString(R.string.pref_filter_1_key), true)) {
                    where += "category = '12' OR ";
                }
                if (prefs.getBoolean(getContext().getString(R.string.pref_filter_2_key), true)) {
                    where += "category = '1' OR ";
                    where += "category = '2' OR ";
                }
                if (prefs.getBoolean(getContext().getString(R.string.pref_filter_3_key), true)) {
                    where += "category = '11' OR ";
                }
                if (prefs.getBoolean(getContext().getString(R.string.pref_filter_4_key), true)) {
                    where += "category = '3' OR ";
                    where += "category = '4' OR ";
                }
                if (prefs.getBoolean(getContext().getString(R.string.pref_filter_5_key), true)) {
                    where += "category = '5' OR ";
                }
                if (prefs.getBoolean(getContext().getString(R.string.pref_filter_6_key), true)) {
                    where += "category = '15' OR ";
                }
                if (prefs.getBoolean(getContext().getString(R.string.pref_filter_7_key), true)) {
                    where += "category = '16' OR ";
                }
                if (prefs.getBoolean(getContext().getString(R.string.pref_filter_8_key), true)) {
                    where += "category = '6' OR ";
                    where += "category = '7' OR ";
                    where += "category = '8' OR ";
                    where += "category = '9' OR ";
                    where += "category = '10' OR ";
                    where += "category = '13' OR ";
                    where += "category = '14' OR ";
                }
                if (where.length() > 4)
                    where = where.substring(0, where.length() - 4);
                if (!prefs.getBoolean(getContext().getString(R.string.pref_filter_1_key), true) && where.equals(""))
                    // En este caso no tenemos seleccionada ninguna categoría así que no se debería mostrar nada.
                    where = "category = '99'";
                retCursor = mRssDbHelper.getReadableDatabase().query(
                        RssContract.RssEntry.TABLE_NAME,
                        projection,
                        where,
                        selectionArgs,
                        null,
                        null,
                        sortOrder
                );
                break;
            }
            // "rss/*"
            case RSS_ITEM: {
                String where = "_id = " + uri.getPathSegments().get(1);
                retCursor = mRssDbHelper.getReadableDatabase().query(
                        RssContract.RssEntry.TABLE_NAME,
                        projection,
                        where,
                        selectionArgs,
                        null,
                        null,
                        sortOrder
                );
                break;

            }
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        retCursor.setNotificationUri(getContext().getContentResolver(), uri);
        return retCursor;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        final SQLiteDatabase db = mRssDbHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        Uri returnUri;

        switch (match) {
            case RSS: {
                long _id = db.insert(RssContract.RssEntry.TABLE_NAME, null, values);
                if (_id > 0)
                    returnUri = RssContract.RssEntry.CONTENT_URI;
                else
                    throw new android.database.SQLException("Failed to insert row into " + uri);
                break;
            }
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return returnUri;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        final SQLiteDatabase db = mRssDbHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        int rowsDeleted;
        // this makes delete all rows return the number of rows deleted
        if (null == selection) selection = "1";
        switch (match) {
            case RSS:
                rowsDeleted = db.delete(
                        RssContract.RssEntry.TABLE_NAME, selection, selectionArgs);
                break;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        // Because a null deletes all rows
        if (rowsDeleted != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return rowsDeleted;
    }

    @Override
    public int update(
            Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        final SQLiteDatabase db = mRssDbHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        int rowsUpdated;

        switch (match) {
            case RSS:
                rowsUpdated = db.update(RssContract.RssEntry.TABLE_NAME, values, selection,
                        selectionArgs);
                break;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        if (rowsUpdated != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return rowsUpdated;
    }

    @Override
    public int bulkInsert(Uri uri, ContentValues[] values) {
        final SQLiteDatabase db = mRssDbHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case RSS:
                db.beginTransaction();
                int returnCount = 0;
                try {
                    for (ContentValues value : values) {
                        long _id = db.insert(RssContract.RssEntry.TABLE_NAME, null, value);
                        if (_id != -1) {
                            returnCount++;
                        }
                    }
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
                getContext().getContentResolver().notifyChange(uri, null);
                return returnCount;
            default:
                return super.bulkInsert(uri, values);
        }
    }

}
