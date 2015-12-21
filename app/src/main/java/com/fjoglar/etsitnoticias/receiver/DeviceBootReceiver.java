package com.fjoglar.etsitnoticias.receiver;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.fjoglar.etsitnoticias.Utility;
import com.fjoglar.etsitnoticias.service.DownloadRssService;

/**
 * Este BroadcastReceiver establece la alarma de sincronización
 * cuando el dispositivo se ha reiniciado. Esto es importante ya
 * que las alarmas se cancelan cuando se apaga el dispositivo.
 *
 * Más información:
 * http://developer.android.com/intl/es/training/scheduling/alarms.html#boot
 * http://developer.android.com/intl/es/reference/android/content/BroadcastReceiver.html
 */
public class DeviceBootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals("android.intent.action.BOOT_COMPLETED")) {
            AlarmManager alarmMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            PendingIntent alarmIntent = PendingIntent.getBroadcast(
                    context,
                    0,
                    new Intent(context, DownloadRssService.AlarmReceiver.class),
                    0);

            Utility.setAlarm(context, alarmMgr, alarmIntent);
        }
    }

}
