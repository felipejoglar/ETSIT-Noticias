package com.fjoglar.etsitnoticias.service;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.fjoglar.etsitnoticias.MainActivity;
import com.fjoglar.etsitnoticias.R;
import com.fjoglar.etsitnoticias.RssXmlParser;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class DownloadRssService extends IntentService {

    // Etiqueta para los logs de depuración.
    private final String LOG_TAG = DownloadRssService.class.getSimpleName();

    // La URL desde la que se obtienen las noticias.
//    private final String DOWNLOAD_URL = "http://www.tel.uva.es/rss/tablon.xml";
    // todo: reestablecer el origen de datos real.
    private final String DOWNLOAD_URL = "https://242269422d9d61e6af97c8c4814ad5985de2bed1.googledrive.com/host/0B8hd0RDbTmiRbmR4QWo2TkJDcEk/tablon.xml";
    private static final int RSS_NOTIFICATION_ID = 8008;

    public static final String SERVICE_RESULT = "com.fjoglar.etsit.noticias.DownloadRssService.REQUEST_PROCESSED";
    public static final String SERVICE_MESSAGE = "com.fjoglar.etsit.noticias.DownloadRssService.SERVICE_MSG";
    private LocalBroadcastManager mLocalBroadcastManager;

    public DownloadRssService() {
        super("DownloadRssService");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mLocalBroadcastManager = LocalBroadcastManager.getInstance(this);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        try {
            loadXmlFromNetwork(DOWNLOAD_URL);
        } catch (IOException | XmlPullParserException e) {
            Log.e(LOG_TAG, e.getMessage(), e);
            e.printStackTrace();
        }

        // Comprobamos si hay que enviar notificación al usuario.
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        boolean displayNotifications = prefs.getBoolean(this.getString(R.string.pref_enable_notifications_key),
                Boolean.parseBoolean(this.getString(R.string.pref_enable_notifications_default)));

        if (prefs.getBoolean(this.getString(R.string.pref_send_notification_key), false) && displayNotifications) {

            int iconId = R.drawable.ic_notification;
            String title = this.getString(R.string.app_name);
            String contentText = this.getString(R.string.notification_text);

            // NotificationCompatBuilder es una buena manera de crear notificaciones
            // compatibles con versiones anteriores de Android.
            NotificationCompat.Builder mBuilder =
                    new NotificationCompat.Builder(this)
                            .setSmallIcon(iconId)
                            .setContentTitle(title)
                            .setContentText(contentText)
                            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                            .setAutoCancel(true);

            // Cuando el usuario pulsa la notificación se abre la aplicación.
            Intent resultIntent = new Intent(this, MainActivity.class);

            // Hacemos que yendo hacia atrás volvamos al escritorio.
            TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
            stackBuilder.addNextIntent(resultIntent);
            PendingIntent resultPendingIntent =
                    stackBuilder.getPendingIntent(
                            0,
                            PendingIntent.FLAG_UPDATE_CURRENT
                    );
            mBuilder.setContentIntent(resultPendingIntent);

            NotificationManager mNotificationManager =
                    (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
            // RSS_NOTIFICATION_ID permite actualizar la notificación mas tarde si fuese necesario.
            mNotificationManager.notify(RSS_NOTIFICATION_ID, mBuilder.build());

            // Determinamos que ya no hay que enviar notificación al usuario.
            prefs.edit().putBoolean(this.getString(R.string.pref_send_notification_key), false).apply();
        }

        sendResult(this.getString(R.string.service_result));

    }

    // Descarga el fichero XML etsit.es, lo parsea.
    private void loadXmlFromNetwork(String urlString) throws XmlPullParserException, IOException {
        InputStream stream = null;
        RssXmlParser rssXmlParser = new RssXmlParser();

        try {
            stream = downloadUrl(urlString);
            rssXmlParser.parse(this, stream);
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

    public void sendResult(String message) {
        Intent intent = new Intent(SERVICE_RESULT);
        if(message != null)
            intent.putExtra(SERVICE_MESSAGE, message);
        mLocalBroadcastManager.sendBroadcast(intent);
    }

    public static class AlarmReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            Intent sendIntent = new Intent(context, DownloadRssService.class);
            context.startService(sendIntent);

        }
    }

}
