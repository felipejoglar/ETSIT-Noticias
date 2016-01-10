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

/**
 * Servicio que ejecuta la tarea de sincronización en segundo plano.
 * De esta manera evitamos los problemas de una sincronización con elementos vinculados
 * a la Interfaz de Usuario como AsyncTask o Threads implementados desde la Activity.
 * Más información:
 * http://developer.android.com/intl/es/training/run-background-service/create-service.html
 */
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
        // Se usa este LocalBroadcastManager para notificar al Fragment principal
        // cuando el servicio ha finalizado.
        mLocalBroadcastManager = LocalBroadcastManager.getInstance(this);
    }

    @Override
    protected void onHandleIntent(Intent intent) {

        // Lo primero es cargar los datos en formato XML desde la web de la ETSIT.
        try {
            loadXmlFromNetwork(DOWNLOAD_URL);
        } catch (IOException | XmlPullParserException e) {
            Log.e(LOG_TAG, e.getMessage(), e);
            e.printStackTrace();
        }

        // Comprobamos si hay que enviar notificación al usuario.
        // Para enviar la notificación hay que cumplir 3 condiciones:
        // 1- Que las notificaciones estén habilitadas en los ajustes.
        // 2- Que haya una noticia nueva.
        // 3- Que la aplicación no esté activa.
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        boolean sendNotification = prefs.getBoolean(
                this.getString(R.string.pref_send_notification_key),
                false);
        boolean displayNotifications = prefs.getBoolean(
                this.getString(R.string.pref_enable_notifications_key),
                Boolean.parseBoolean(this.getString(R.string.pref_enable_notifications_default)));
        boolean isInForeground = prefs.getBoolean(
                this.getString(R.string.pref_is_in_foreground_key),
                false);
        // Se obtiene el título de la última noticia para mostarlo en la notificación.
        String notificationText = prefs.getString(
                this.getString(R.string.pref_last_new_title_key),
                this.getString(R.string.pref_last_new_title_default));

        if (sendNotification && displayNotifications && !isInForeground) {
            sendNotification(notificationText);
        }

        // La sincronización ha finalizado, se envía la notificación al Fragment principal.
        sendResult(this.getString(R.string.service_result));
    }

    /**
     * Descarga el fichero XML de etsit.es y lo parsea.
     *
     * @param urlString URL del fichero XML a descargar.
     * @throws XmlPullParserException
     * @throws IOException
     */
    private void loadXmlFromNetwork(String urlString) throws XmlPullParserException, IOException {
        InputStream stream = null;
        RssXmlParser rssXmlParser = new RssXmlParser();

        try {
            // Primero se descarga el fichero.
            stream = downloadUrl(urlString);
            // Posteriormente se parsea y se guardan las noticias en la base de datos.
            rssXmlParser.parse(this, stream);
        } finally {
            // Nos aseguramos de que el InputStream se cierra después de terminar de
            // usarlo.
            if (stream != null) {
                stream.close();
            }
        }
    }

    /**
     * Dado un URL, establece una HttpUrlConnection y obtiene el contenido
     * como un InputStream.
     * <p/>
     * Más información:
     * http://developer.android.com/intl/es/training/basics/network-ops/connecting.html
     *
     * @param urlString URL del fichero XML a descargar.
     * @return InputStream con los datos descargados.
     * @throws IOException
     */
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
    private void sendNotification(String notificationText) {
        int iconId = R.drawable.ic_notification;
        String title = this.getString(R.string.notification_text);

        // NotificationCompatBuilder es una buena manera de crear notificaciones
        // compatibles con versiones anteriores de Android.
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(iconId)
                        .setContentTitle(title)
                        .setContentText(notificationText)
                        .setColor(getResources().getColor(R.color.colorPrimary))
                        .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                        .setAutoCancel(true)
                        .setStyle(new NotificationCompat.BigTextStyle()
                                .bigText(notificationText));

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
     * Envía un mensaje que será recibido por el Fragment que lanzó el servicio.
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
