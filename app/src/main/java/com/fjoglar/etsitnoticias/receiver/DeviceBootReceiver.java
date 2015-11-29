package com.fjoglar.etsitnoticias.receiver;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.fjoglar.etsitnoticias.R;
import com.fjoglar.etsitnoticias.service.DownloadRssService;

import java.util.Calendar;

/**
 * Este BroadcastReceiver establece la alarma de sincronización
 * cuando el dispositivo se ha reiniciado. Esto es importante ya
 * que las alarmas se cancelan cuando se apaga el dispositivo.
 */
public class DeviceBootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals("android.intent.action.BOOT_COMPLETED")) {
            AlarmManager alarmMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            Intent alarmIntent = new Intent(context, DownloadRssService.AlarmReceiver.class);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, 0);

            // Ponemos una alarma aproximadamente a las 00:00 horas.
            // Se trata de poder temporizar la sincronización de la aplicación para así
            // poder enviar notificaciones al usuario.
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(System.currentTimeMillis());
            calendar.set(Calendar.HOUR_OF_DAY, 0);

            // Obtenemos el periodo de sincronización.
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            int syncInterval = Integer.parseInt(prefs.getString(context.getString(R.string.pref_sync_frequency_key), "6"));
            alarmMgr.setInexactRepeating(AlarmManager.RTC_WAKEUP,
                    calendar.getTimeInMillis(),
                    AlarmManager.INTERVAL_HOUR * syncInterval,
                    pendingIntent);
        }
    }

}
