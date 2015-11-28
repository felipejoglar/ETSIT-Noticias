package com.fjoglar.etsitnoticias;

import android.content.Context;
import android.os.AsyncTask;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Clase que crea un hilo en segundo plano desde el que se descarga la información.
 * Esta AsyncTask coge el URL y crea una HttpUrlConnection. Una vez que la conexión
 * se ha establecido, la AsyncTask descarga la información como un InputStream.
 * Finalmente, el InputStream es convertida a un String.
 */
public class DownloadRssTask extends AsyncTask<Void, Void, Void> {

    // Etiqueta para los logs de depuración.
    private final String LOG_TAG = DownloadRssTask.class.getSimpleName();

    // La URL desde la que se obtienen las noticias.
    private final String DOWNLOAD_URL = "http://www.tel.uva.es/rss/tablon.xml";
    // todo: reestablecer el origen de datos real.
//    private final String DOWNLOAD_URL = "https://242269422d9d61e6af97c8c4814ad5985de2bed1.googledrive.com/host/0B8hd0RDbTmiRbmR4QWo2TkJDcEk/tablon.xml";

    // Variables de la clase
    private final Context mContext;
    private final SwipeRefreshLayout mSwipeRefreshLayout;

    public DownloadRssTask(Context context, SwipeRefreshLayout swipeRefreshLayout) {
        mContext = context;
        mSwipeRefreshLayout = swipeRefreshLayout;
    }

    @Override
    protected Void doInBackground(Void... params) {

        try {
            loadXmlFromNetwork(DOWNLOAD_URL);
        } catch (IOException | XmlPullParserException e) {
            Log.e(LOG_TAG, e.getMessage(), e);
            e.printStackTrace();
        }
        return null;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        if (mSwipeRefreshLayout != null){
            mSwipeRefreshLayout.setRefreshing(false);
        }
        super.onPostExecute(aVoid);
    }

    // Descarga el fichero XML etsit.es, lo parsea.
    private void loadXmlFromNetwork(String urlString) throws XmlPullParserException, IOException {
        InputStream stream = null;
        RssXmlParser rssXmlParser = new RssXmlParser();

        try {
            stream = downloadUrl(urlString);
            rssXmlParser.parse(mContext, stream);
            // Nos aseguramos de que el InputStream se cierra después de terminar de
            // usarlo.
        } finally {
            if (stream != null) {
                stream.close();
            }
        }
    }

    // Dado un URL, establece una HttpUrlConnection y obtiene
    // el contenido como un InputStream.
    private InputStream downloadUrl(String urlString) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setReadTimeout(10000 /* milliseconds */);
        conn.setConnectTimeout(15000 /* milliseconds */);
        conn.setRequestMethod("GET");
        conn.setDoInput(true);
        // Starts the query
        conn.connect();
        InputStream stream = conn.getInputStream();
        return stream;
    }

}
