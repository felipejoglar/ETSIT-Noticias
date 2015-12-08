package com.fjoglar.etsitnoticias;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;
import android.util.Log;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class Utility {

    // Etiqueta para los logs de depuración.
    private final static String LOG_TAG = Utility.class.getSimpleName();

    public static String categoryToString(String category) {
        String result;

        if (category.equals("1") || category.equals("2")) {
            result = "General";
        } else if (category.equals("3") || category.equals("4")) {
            result = "Beca/Empleo";
        } else if (category.equals("5")) {
            result = "TFG/TFM";
        } else if (category.equals("11")) {
            result = "Conferencia/Taller";
        } else if (category.equals("12")) {
            result = "Destacado";
        } else if (category.equals("15")) {
            result = "Junta de Escuela";
        } else if (category.equals("16")) {
            result = "Investigación";
        } else {
            result = "Otros";
        }

        return result;
    }

    public static String capitalizeWord(String word) {
        return Character.toUpperCase(word.charAt(0)) + word.substring(1);
    }

    /**
     * Devuelve true si la red esta disponible.
     *
     * @param c Context usado para obtener el ConnectivityManager
     * @return true si la red esta disponible
     */
    public static boolean isNetworkAvailable(Context c) {
        ConnectivityManager cm =
                (ConnectivityManager) c.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting();
    }

    /**
     * Permite convertir un String en fecha (Date).
     *
     * @param date Cadena de fecha "EEE, d MMM yyyy HH:mm:ss z"
     * @return Objeto   Date
     */
    public static Date ParseDate(String date) {
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
     * Formatea el texto para que sólo tenga carácteres imprimibles. Además también
     * corrige el problema de que en algunas descripciones hay muchos saltos de línea
     * seguidos. De esta manera el texto queda visualmente más agradable
     * y optimizado para su lectura.
     *
     * @param text Texto a formatear.
     * @return texto formateado.
     */
    public static String formatText(String text) {
        text = text.replaceAll("[^\\s\\p{Print}]", "")
                .replace(" ", "AuxText")
                .replace("\r\nAuxText", "\r\n")
                .replaceAll("[\\r\\n]+", "\n\n")
                .replace("AuxText", " ");

        return text;
    }

    /**
     * Configura una alarma para poder realizar la sincronización en segundo plano.
     *
     * @param context Context para obtener el PreferenceManager
     */
    public static void setAlarm(Context context, AlarmManager alarmMgr, PendingIntent alarmIntent) {
        // Ponemos una alarma.
        // Se trata de poder temporizar la sincronización de la aplicación para así
        // poder enviar notificaciones al usuario.

        // Obtenemos el periodo de sincronización.
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        int syncInterval = 1000 * 60 * 60 *
                Integer.parseInt(prefs.getString(context.getString(R.string.pref_sync_frequency_key), "6"));
        alarmMgr.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                syncInterval,
                syncInterval,
                alarmIntent);
    }

    /**
     * Coge la fecha de la noticia y la devuelve en un formato más adecuado para
     * la lectura en un ListView, pudiendo identificar de un vistazo, hace cuanto
     * salió la noticia.
     *
     * @param pDate Fecha de la noticia como String en formato "yyyy-MM-dd HH:mm:ss".
     * @return la fecha en el formato adecuado.
     */
    public static String formatTime(String pDate) {
        int diffInDays = 0;
        SimpleDateFormat format = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss z");
        Calendar c = Calendar.getInstance();
        String formattedDate = format.format(c.getTime());

        Date d1 = null;
        Date d2 = null;
        try {

            d1 = format.parse(formattedDate);
            d2 = format.parse(pDate);
            long diff = d1.getTime() - d2.getTime();

            diffInDays = (int) (diff / (1000 * 60 * 60 * 24));
            if (diffInDays > 0) {
                if (diffInDays > 0 && diffInDays < 7) {
                    return diffInDays + "d";
                } else {
                    SimpleDateFormat formatter = new SimpleDateFormat("d");
                    Calendar calendar = Calendar.getInstance();
                    calendar.setTimeInMillis(d2.getTime());
                    String day = formatter.format(calendar.getTime());
                    formatter = new SimpleDateFormat("MMM");
                    String month = Utility.capitalizeWord(formatter.format(calendar.getTime()));
                    return day + " " + month;
                }
            } else {
                int diffHours = (int) (diff / (60 * 60 * 1000));
                if (diffHours > 0) {
                    return diffHours + "h";
                } else {
                    int diffMinutes = (int) ((diff / (60 * 1000) % 60));
                    return diffMinutes + "m";
                }
            }

        } catch (ParseException e) {
            e.printStackTrace();
        }

        return null;
    }

}
