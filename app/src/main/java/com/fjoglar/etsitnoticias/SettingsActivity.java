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

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.support.v4.app.NavUtils;
import android.view.MenuItem;

import com.fjoglar.etsitnoticias.receiver.DeviceBootReceiver;
import com.fjoglar.etsitnoticias.service.DownloadRssService;

public class SettingsActivity extends AppCompatPreferenceActivity
        implements Preference.OnPreferenceChangeListener,
        SharedPreferences.OnSharedPreferenceChangeListener {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.pref_general);
        bindPreferenceSummaryToValue(findPreference(getString(R.string.pref_sync_frequency_key)));
    }

    private void bindPreferenceSummaryToValue(Preference preference) {
        preference.setOnPreferenceChangeListener(this);

        onPreferenceChange(preference,
                PreferenceManager
                        .getDefaultSharedPreferences(preference.getContext())
                        .getString(preference.getKey(), ""));
    }

    @Override
    public void onResume() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        // En la llamada a onResume la App está en primer plano así que se
        // pone el flag a true para que las notificaciones no se envíen.
        sp.edit().putBoolean(getString(R.string.pref_is_in_foreground_key), true).apply();
        // Se registra un Listener para que actúe cuando hay cambios en las SharedPrefereces.
        sp.registerOnSharedPreferenceChangeListener(this);
        super.onResume();
    }

    @Override
    public void onPause() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        // En la llamada a onPause la App deja de estar en primer plano así que se
        // pone el flag a false para que las notificaciones se envíen.
        sp.edit().putBoolean(getString(R.string.pref_is_in_foreground_key), false).apply();
        // Se desregistra el Listener.
        sp.unregisterOnSharedPreferenceChangeListener(this);
        super.onPause();
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object value) {
        String stringValue = String.valueOf(value);

        if (preference instanceof ListPreference) {
            ListPreference listPreference = (ListPreference) preference;
            int prefIndex = listPreference.findIndexOfValue(stringValue);
            if (prefIndex >= 0) {
                preference.setSummary(listPreference.getEntries()[prefIndex]);
            }
        } else {
            preference.setSummary(stringValue);
        }
        return true;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {

        AlarmManager alarmMgr = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);
        PendingIntent alarmIntent = PendingIntent.getBroadcast(
                this,
                0,
                new Intent(this, DownloadRssService.AlarmReceiver.class),
                0);

        // Si se cambia el ajuste de notificaciones:
        // Si se activa, se activa la alarma qe lanza las sincronizaciones y
        // se activa de nuevo el BroadcastReceiver de reinicio de dispositivo.
        // Si se desactiva, se cancela la alarma de sincronización y se desactiva
        // el BroadastReceiver de reinicio de dispositivo.
        if (key.equals(getString(R.string.pref_enable_notifications_key))) {
            if (sharedPreferences.getBoolean(getString(R.string.pref_enable_notifications_key), true)) {
                Utility.setAlarm(this, alarmMgr, alarmIntent);
                // Habilitamos el BroadastReceiver, una vez habilitado permanece habilitado
                // aunque reiniciemos.
                ComponentName receiver = new ComponentName(this, DeviceBootReceiver.class);
                PackageManager pm = this.getPackageManager();
                pm.setComponentEnabledSetting(receiver,
                        PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                        PackageManager.DONT_KILL_APP);
            } else {
                if (alarmMgr != null) {
                    alarmMgr.cancel(alarmIntent);
                }
                // Deshabilitamos el BroadastReceiver, una vez deshabilitado permanece
                // aunque reiniciemos.
                ComponentName receiver = new ComponentName(this, DeviceBootReceiver.class);
                PackageManager pm = this.getPackageManager();
                pm.setComponentEnabledSetting(receiver,
                        PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                        PackageManager.DONT_KILL_APP);
            }
        } else if (key.equals(getString(R.string.pref_sync_frequency_key))) {
            // Si se cambia la frecuencia de actualización:
            // Volvemos a activar la alarma con la nueva frecuencia.
            Utility.setAlarm(this, alarmMgr, alarmIntent);
        }
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            if (!super.onMenuItemSelected(featureId, item)) {
                NavUtils.navigateUpFromSameTask(this);
            }
            return true;
        }
        return super.onMenuItemSelected(featureId, item);
    }

}
