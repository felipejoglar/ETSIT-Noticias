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

import com.fjoglar.etsitnoticias.R;
import com.fjoglar.etsitnoticias.RssXmlParser;
import com.fjoglar.etsitnoticias.SplashScreenActivity;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class DownloadRssService extends IntentService {

    // Etiqueta para los logs de depuración.
    private final String LOG_TAG = DownloadRssService.class.getSimpleName();

    // La URL desde la que se obtienen las noticias.
    private final String DOWNLOAD_URL = "http://www.tel.uva.es/rss/tablon.xml";
    private static final int RSS_NOTIFICATION_ID = 8008;

    // Constantes para notificar a MainFragment que el servicio ha finalizado.
    public static final String SERVICE_RESULT =
            "com.fjoglar.etsit.noticias.DownloadRssService.REQUEST_PROCESSED";
    public static final String SERVICE_MESSAGE =
            "com.fjoglar.etsit.noticias.DownloadRssService.SERVICE_MSG";
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
        boolean displayNotifications = prefs.getBoolean(
                this.getString(R.string.pref_enable_notifications_key),
                Boolean.parseBoolean(this.getString(R.string.pref_enable_notifications_default)));

        // Para enviar la notificación hay que cumplir 3 condiciones:
        // 1- Que las notificaciones estén habilitadas en los ajustes.
        // 2- Que haya una noticia nueva.
        // 3- Que la aplicación no esté activa.
        if (prefs.getBoolean(this.getString(R.string.pref_send_notification_key), false)
                && displayNotifications
                && !prefs.getBoolean(this.getString(R.string.pref_is_in_foreground_key), false)) {
            sendNotification();
        }

        sendResult(this.getString(R.string.service_result));
    }

    // Descarga el fichero XML de etsit.es y lo parsea.
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
        conn.setReadTimeout(10000 /* milisegundos */);
        conn.setConnectTimeout(15000 /* milisegundos */);
        conn.setRequestMethod("GET");
        conn.setDoInput(true);

        conn.connect();
        InputStream stream = conn.getInputStream();

        return stream;
    }

    /**
     * Crea y envía una notificación.
     */
    private void sendNotification() {
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
        Intent resultIntent = new Intent(this, SplashScreenActivity.class);

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
        // RSS_NOTIFICATION_ID permitiría actualizar la notificación mas tarde si fuese necesario.
        mNotificationManager.notify(RSS_NOTIFICATION_ID, mBuilder.build());

        // Determinamos que ya no hay que enviar notificación al usuario.
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefs.edit().putBoolean(this.getString(R.string.pref_send_notification_key), false).apply();
    }

    /**
     * Envía un mensaje que será recibido por el fragment que lanzó el servicio.
     *
     * @param message Mensaje a enviar.
     */
    public void sendResult(String message) {
        Intent intent = new Intent(SERVICE_RESULT);

        if (message != null)
            intent.putExtra(SERVICE_MESSAGE, message);

        mLocalBroadcastManager.sendBroadcast(intent);
    }

    /**
     * Receiver que controla la sincronización. Cuando salta la alarma de sincronización
     * este Receiver ejecuta el servicio.
     */
    public static class AlarmReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            Intent sendIntent = new Intent(context, DownloadRssService.class);
            context.startService(sendIntent);
        }
    }

}
