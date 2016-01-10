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
