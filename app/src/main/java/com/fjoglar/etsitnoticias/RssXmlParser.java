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
package com.fjoglar.etsitnoticias;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.fjoglar.etsitnoticias.data.RssContract;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Vector;

/**
 * Esta clase recoge un archivo de noticias XML de tel.uva.es.
 * Dado un InputStream como representación de las noticias, selecciona
 * y guarda las noticias en una base de datos SQLite que será accedida
 * posteriorente mediante un ContentProvider para mostrar las noticias.
 */
public class RssXmlParser {

    // Etiqueta para los logs de depuración.
    private final String LOG_TAG = RssXmlParser.class.getSimpleName();

    // Etiquetas XML a recoger.
    private final String TITLE_TAG = "title";
    private final String DESCRIPTION_TAG = "description";
    private final String LINK_TAG = "link";
    private final String CATEGORY_TAG = "category";
    private final String PUB_DATE_TAG = "pubDate";

    private String mTitle, mDescription, mLink, mCategory, mPubDate;
    private boolean mIsParsingTitle, mIsParsingDescription, mIsParsingLink, mIsParsingCategory, mIsParsingPubDate;
    private long mLastDownloadedNew = 0;
    private String mLastNewTitle;

    private Vector<ContentValues> cVVector = new Vector<ContentValues>();

    public void parse(Context context, InputStream inputStream) throws XmlPullParserException, IOException {

        try {

            // Creamos el Pull Parser.
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            XmlPullParser xpp = factory.newPullParser();

            // Establecemos la entrada del Parser.
            xpp.setInput(inputStream, null);

            // Cogemos el primer evento del Parser y empezamos a iterar sobre el documento XML.
            int eventType = xpp.getEventType();

            while (eventType != XmlPullParser.END_DOCUMENT) {

                if (eventType == XmlPullParser.START_TAG) {
                    startTag(xpp.getName());
                } else if (eventType == XmlPullParser.END_TAG) {
                    endTag(xpp.getName());
                } else if (eventType == XmlPullParser.TEXT) {
                    text(xpp.getText());
                }
                eventType = xpp.next();
            }

            // Si ha salido correctamente, se tiene un vector de ContentValues con los nuevos
            // datos, así que borramos la tabla y la actualizamos de nuevo.
            if (cVVector.size() > 0) {
                // Antes de insertar los nuevos datos se borran los anteriores, de
                // manera que siempre se tiene la última información, tal y como
                // está en el tablón de www.tel.uva.es.
                context.getContentResolver().delete(RssContract.RssEntry.CONTENT_URI, null, null);

                ContentValues[] cvArray = new ContentValues[cVVector.size()];
                cVVector.toArray(cvArray);
                context.getContentResolver().bulkInsert(RssContract.RssEntry.CONTENT_URI, cvArray);

                // Determinamos si hay que enviar notificación al usuario.
                // Si la fecha de la última noticia es más reciente que la última
                // actualización, se determina que hay que enviar notificación.
                // Después en DownloadRssService se comprueba si definitivamente hay
                // que notificar al usuario.
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
                if (mLastDownloadedNew > prefs.getLong(context.getString(R.string.pref_last_updated_key), 0)) {
                    prefs.edit().putBoolean(context.getString(R.string.pref_send_notification_key), true).apply();
                    prefs.edit().putString(context.getString(R.string.pref_last_new_title_key),mLastNewTitle).apply();
                }

                // Actualizamos la fecha de la última actualización.
                prefs.edit().putLong(context.getString(R.string.pref_last_updated_key), System.currentTimeMillis()).apply();
            }

        } catch (XmlPullParserException e) {
            Log.e(LOG_TAG, e.getMessage(), e);
            e.printStackTrace();
        }

    }

    public void startTag(String localName) {
        if (localName.equals(TITLE_TAG)) {
            mIsParsingTitle = true;
        } else if (localName.equals(DESCRIPTION_TAG)) {
            mIsParsingDescription = true;
        } else if (localName.equals(LINK_TAG)) {
            mIsParsingLink = true;
        } else if (localName.equals(CATEGORY_TAG)) {
            mIsParsingCategory = true;
        } else if (localName.equals(PUB_DATE_TAG)) {
            mIsParsingPubDate = true;
        }
    }

    public void text(String text) {
        if (mIsParsingTitle) {
            mTitle = text.trim();
        } else if (mIsParsingDescription) {
            mDescription = text.trim();
        } else if (mIsParsingLink) {
            mLink = text.trim();
        } else if (mIsParsingCategory) {
            mCategory = text.trim();
        } else if (mIsParsingPubDate) {
            mPubDate = text.trim();
        }
    }

    public void endTag(String localName) {
        if (localName.equals(TITLE_TAG)) {
            mIsParsingTitle = false;
        } else if (localName.equals(DESCRIPTION_TAG)) {
            mIsParsingDescription = false;
        } else if (localName.equals(LINK_TAG)) {
            mIsParsingLink = false;
        } else if (localName.equals(CATEGORY_TAG)) {
            mIsParsingCategory = false;
        } else if (localName.equals(PUB_DATE_TAG)) {
            mIsParsingPubDate = false;
        } else if (localName.equals("item")) {
            insertRss();
            mTitle = null;
            mDescription = null;
            mLink = null;
            mCategory = null;
            mPubDate = null;
        }
    }

    /**
     * Guarda la noticia en el ContentValues[] que luego se insertará en la base de datos.
     */
    private void insertRss() {
        long dateTime = Utility.ParseDate(mPubDate).getTime();

        // No podemos tener una descripción nula.
        if (mDescription == null)
            mDescription = "";

        // Guardamos la fecha de la última noticia.
        // Esto nos ayuda a la hora de mandar las notificaciones al usuario.
        // Guardamos el título de la última noticia para ponerla como descripción
        // de la notificación en caso de que haya que notificar.
        if (mLastDownloadedNew < dateTime) {
            mLastDownloadedNew = dateTime;
            mLastNewTitle = Utility.formatText(mTitle);
        }

        ContentValues rssValues = new ContentValues();

        rssValues.put(RssContract.RssEntry.COLUMN_TITLE, Utility.formatText(mTitle));
        rssValues.put(RssContract.RssEntry.COLUMN_DESCRIPTION, Utility.formatText(mDescription));
        rssValues.put(RssContract.RssEntry.COLUMN_LINK, mLink);
        rssValues.put(RssContract.RssEntry.COLUMN_CATEGORY, mCategory);
        rssValues.put(RssContract.RssEntry.COLUMN_PUB_DATE, dateTime);

        cVVector.add(rssValues);
    }

}
