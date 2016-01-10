/*
 * Copyright (C) 2016 Felipe Joglar Santos
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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

/**
 * Define el ContentProvider que se va a usar para almacenar y extraer
 * los datos de la base de dato SQLite que hay por debajo.
 *
 * Más información:
 * http://developer.android.com/intl/es/guide/topics/providers/content-provider-creating.html
 */
public class RssProvider extends ContentProvider {

    // El URI Matcher usado por este content provider.
    private static final UriMatcher sUriMatcher = buildUriMatcher();
    private RssDbHelper mRssDbHelper;

    private static final int RSS = 100;
    private static final int RSS_ITEM = 101;

    static UriMatcher buildUriMatcher() {
        final UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
        final String authority = RssContract.CONTENT_AUTHORITY;

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

        // Se usa el Uri Matcher para determinar el tipo de URI.
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
            // Consulta general con filtrado por categoría.
            // Para la vista de lista de noticias.
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
            // Consulta de una sola noticia filtrada por ID.
            // Para la vista detalle.
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
        // Esto hace que se borren todas las filas y devuelve el número de las que se
        // han borrado.
        if (null == selection) selection = "1";
        switch (match) {
            case RSS:
                rowsDeleted = db.delete(
                        RssContract.RssEntry.TABLE_NAME, selection, selectionArgs);
                break;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
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
        // Con este método se inserta una lista de varias noticias mediante una Transaction
        // de manera consistente y eficiente.
        // De esta manera no necesitamos agregar las noticias una a una.
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
