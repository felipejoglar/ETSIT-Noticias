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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Vector;

/**
 * Esta clase recoge un archivo de noticias XML de etsit.es.
 * Dado un InputStream como representación de las noticias, devuelve una List
 * de entradas, donde cada elemento de la List representa una sola noticia.
 */
public class RssXmlParser {

    // Etiqueta para los logs de depuración.
    private final String LOG_TAG = RssXmlParser.class.getSimpleName();

    private Context mContext;

    private final String TITLE_TAG = "title";
    private final String DESCRIPTION_TAG = "description";
    private final String LINK_TAG = "link";
    private final String CATEGORY_TAG = "category";
    private final String PUB_DATE_TAG = "pubDate";
    private String mTitle, mDescription, mLink, mCategory, mPubDate;
    private boolean mIsParsingTitle, mIsParsingDescription, mIsParsingLink, mIsParsingCategory, mIsParsingPubDate;
    private long mLastDownloadedNew = 0;
    private Vector<ContentValues> cVVector = new Vector<ContentValues>();

    public void parse(Context context, InputStream inputStream) throws XmlPullParserException, IOException {

        mContext = context;

        try {

            // Creamos el Pull Parser.
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            XmlPullParser xpp = factory.newPullParser();

            // Establecemos la entrada del Parser."ISO-8859-1"
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

            // Si ha salido correctamente, tenemos el vector de ContentValues con los nuevos datos,
            // así que borramos la tabla y la actualizamos de nuevo.
            if (cVVector.size() > 0) {
                // Antes de insertar los nuevos datos borramos los anteriores, de
                // manera que siempre tengamos la última información, tal y como
                // está en el tablón de www.etsit.es.
                // También nos ayuda a que en modo tablet, nos muestre la última
                // noticia cuando abrimos la aplicación.
                mContext.getContentResolver().delete(RssContract.RssEntry.CONTENT_URI, null, null);

                ContentValues[] cvArray = new ContentValues[cVVector.size()];
                cVVector.toArray(cvArray);
                mContext.getContentResolver().bulkInsert(RssContract.RssEntry.CONTENT_URI, cvArray);

                // Determinamos si hay que enviar notificación al usuario.
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
                if (mLastDownloadedNew > prefs.getLong(mContext.getString(R.string.pref_last_updated_key), 0))
                    prefs.edit().putBoolean(mContext.getString(R.string.pref_send_notification_key), true).apply();


                // Actualizamos la fecha de la última actualización.
                prefs.edit().putLong(mContext.getString(R.string.pref_last_updated_key), System.currentTimeMillis()).apply();
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
     * Guarda la noticia en la base de datos.
     */
    private void insertRss() {
        long dateTime = ParseDate(mPubDate).getTime();

        // No podemos tener una descripción nula.
        if (mDescription == null)
            mDescription = "";

        // Guardamos la fecha de la última noticia.
        // Esto nos ayuda a la hora de mandar las notificaciones al usuario.
        if (mLastDownloadedNew < dateTime)
            mLastDownloadedNew = dateTime;

        ContentValues rssValues = new ContentValues();

        rssValues.put(RssContract.RssEntry.COLUMN_TITLE, formatText(mTitle));
        rssValues.put(RssContract.RssEntry.COLUMN_DESCRIPTION, formatText(mDescription));
        rssValues.put(RssContract.RssEntry.COLUMN_LINK, mLink);
        rssValues.put(RssContract.RssEntry.COLUMN_CATEGORY, mCategory);
        rssValues.put(RssContract.RssEntry.COLUMN_PUB_DATE, dateTime);

        cVVector.add(rssValues);

    }

    /**
     * Permite convertir un String en fecha (Date).
     *
     * @param date Cadena de fecha "EEE, d MMM yyyy HH:mm:ss z"
     * @return Objeto   Date
     */
    private Date ParseDate(String date) {
        SimpleDateFormat formatter = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss z", Locale.US);
        Date result = null;
        try {
            result = formatter.parse(date);
        } catch (ParseException e) {
            Log.e(LOG_TAG, e.getMessage(), e);
            e.printStackTrace();
        }
        return result;
    }

    /**
     * Formatea el texto para que sólo tenga carácteres impimibles. Además también
     * corrige el problema de que en algunas descripciones hay muchos saltos de línea
     * seguidos. De esta manera el texto queda visualmente más agradable
     * y optimizado para su lectura.
     *
     * @param text Texto a formatear.
     * @return texto formateado.
     */
    private String formatText(String text) {
        text = text.replaceAll("[^\\s\\p{Print}]", "")
                .replace(" ", "AuxText")
                .replace("\r\nAuxText", "\r\n")
                .replaceAll("[\\r\\n]+", "\n\n")
                .replace("AuxText", " ");

        return text;
    }

}
