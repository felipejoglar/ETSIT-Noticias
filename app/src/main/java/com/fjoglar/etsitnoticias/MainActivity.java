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

import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.LinearLayout;

import com.fjoglar.etsitnoticias.receiver.DeviceBootReceiver;

public class MainActivity extends AppCompatActivity implements MainFragment.Callback {

    private static final String DETAILFRAGMENT_TAG = "DFTAG";

    public static boolean mTwoPane;
    private DrawerLayout mDrawerLayout;
    private SharedPreferences mPrefs;

    private LinearLayout mFilter1;
    private LinearLayout mFilter2;
    private LinearLayout mFilter3;
    private LinearLayout mFilter4;
    private LinearLayout mFilter5;
    private LinearLayout mFilter6;
    private LinearLayout mFilter7;
    private LinearLayout mFilter8;

    private CheckBox mCheckBoxFilter1;
    private CheckBox mCheckBoxFilter2;
    private CheckBox mCheckBoxFilter3;
    private CheckBox mCheckBoxFilter4;
    private CheckBox mCheckBoxFilter5;
    private CheckBox mCheckBoxFilter6;
    private CheckBox mCheckBoxFilter7;
    private CheckBox mCheckBoxFilter8;

    // Se crea un OnClickListener personalizado para el filtro.
    private final View.OnClickListener mDrawerItemCheckBoxClickListener =
            new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    if (view instanceof LinearLayout) {
                        // Si se pulsa en el texto se actualiza también el CheckBox.
                        CheckBox checkBox = findCheckBoxByTag(view.getTag().toString());
                        assert checkBox != null;
                        if (checkBox.isChecked()) {
                            checkBox.setChecked(false);
                        } else {
                            checkBox.setChecked(true);
                        }
                        // Se actualiza el filtro y el listado de noticias.
                        updateFilter();
                        mFragment.reloadFragment();

                    } else if (view instanceof CheckBox) {
                        // Se actualiza el filtro y el listado de noticias.
                        updateFilter();
                        mFragment.reloadFragment();
                    }
                }
            };

    private MainFragment mFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        mFragment = (MainFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_main);

        // Si es la primera ejecución de la App habilitamos el BroadastReceiver, una
        // vez habilitado permanece habilitado aunque reiniciemos.
        // Se hace sólo una vez ya que luego se activa o desactiva junto con las
        // notificaciones en SettingsActivity.
        if (mPrefs.getBoolean(this.getString(R.string.pref_first_boot_key), true)){
            ComponentName receiver = new ComponentName(this, DeviceBootReceiver.class);
            PackageManager pm = this.getPackageManager();
            pm.setComponentEnabledSetting(receiver,
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    PackageManager.DONT_KILL_APP);
        }

        // Declaramos la barra de navegación lateral.
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow_flipped, GravityCompat.END);

        // Inicializamos las vistas del filtro.
        initializeFilterViews();

        // Comprobamos los filtros que están activados.
        checkActivatedFilters();

        // Vinculamos los Listeners a las vistas del filtro.
        attachListeners();

        if (findViewById(R.id.detail_container) != null) {
            // La vista detail_container se presentará sólo en grandes pantallas.
            // Si esta vista está presente, entonces la actividad debe estar
            // en modo de dos paneles.
            mTwoPane = true;

            if (savedInstanceState == null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.detail_container, new DetailFragment(), DETAILFRAGMENT_TAG)
                        .commit();
            }
        } else {
            mTwoPane = false;
        }

    }

    @Override
    public void onResume() {
        // En la llamada a onResume la App está en primer plano así que se
        // pone el flag a true para que las notificaciones no se envíen.
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefs.edit().putBoolean(getString(R.string.pref_is_in_foreground_key), true).apply();
        super.onResume();
    }

    @Override
    public void onPause() {
        // En la llamada a onPause la App deja de estar en primer plano así que se
        // pone el flag a false para que las notificaciones se envíen.
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefs.edit().putBoolean(getString(R.string.pref_is_in_foreground_key), false).apply();
        super.onPause();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.mainactivity, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        if (id == R.id.action_filter) {
            if (mDrawerLayout != null && mDrawerLayout.isDrawerOpen(GravityCompat.END)) {
                mDrawerLayout.closeDrawer(GravityCompat.END);
            } else {
                assert mDrawerLayout != null;
                mDrawerLayout.openDrawer(GravityCompat.END);
            }
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (mDrawerLayout != null && mDrawerLayout.isDrawerOpen(GravityCompat.END)) {
            mDrawerLayout.closeDrawer(GravityCompat.END);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onItemSelected(Uri contentUri) {
        // Al seleccionar una noticia se mostrará en la zona de vista detalle si
        // estamos en modo de dos paneles. Si no se lanzará una nueva Activity con
        // la vista detalle.
        if (mTwoPane) {
            Bundle args = new Bundle();
            args.putParcelable(DetailFragment.DETAIL_URI, contentUri);

            DetailFragment fragment = new DetailFragment();
            fragment.setArguments(args);

            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.detail_container, fragment, DETAILFRAGMENT_TAG)
                    .commit();
        } else {
            Intent intent = new Intent(this, DetailActivity.class)
                    .setData(contentUri);
            startActivity(intent);
        }
    }

    private void initializeFilterViews() {
        mFilter1 = (LinearLayout) findViewById(R.id.filter_1);
        mFilter2 = (LinearLayout) findViewById(R.id.filter_2);
        mFilter3 = (LinearLayout) findViewById(R.id.filter_3);
        mFilter4 = (LinearLayout) findViewById(R.id.filter_4);
        mFilter5 = (LinearLayout) findViewById(R.id.filter_5);
        mFilter6 = (LinearLayout) findViewById(R.id.filter_6);
        mFilter7 = (LinearLayout) findViewById(R.id.filter_7);
        mFilter8 = (LinearLayout) findViewById(R.id.filter_8);

        mCheckBoxFilter1 = (CheckBox) findViewById(R.id.filter_checkbox_1);
        mCheckBoxFilter2 = (CheckBox) findViewById(R.id.filter_checkbox_2);
        mCheckBoxFilter3 = (CheckBox) findViewById(R.id.filter_checkbox_3);
        mCheckBoxFilter4 = (CheckBox) findViewById(R.id.filter_checkbox_4);
        mCheckBoxFilter5 = (CheckBox) findViewById(R.id.filter_checkbox_5);
        mCheckBoxFilter6 = (CheckBox) findViewById(R.id.filter_checkbox_6);
        mCheckBoxFilter7 = (CheckBox) findViewById(R.id.filter_checkbox_7);
        mCheckBoxFilter8 = (CheckBox) findViewById(R.id.filter_checkbox_8);
    }

    private void checkActivatedFilters() {
        mCheckBoxFilter1.setChecked(mPrefs.getBoolean(getString(R.string.pref_filter_1_key), true));
        mCheckBoxFilter2.setChecked(mPrefs.getBoolean(getString(R.string.pref_filter_2_key), true));
        mCheckBoxFilter3.setChecked(mPrefs.getBoolean(getString(R.string.pref_filter_3_key), true));
        mCheckBoxFilter4.setChecked(mPrefs.getBoolean(getString(R.string.pref_filter_4_key), true));
        mCheckBoxFilter5.setChecked(mPrefs.getBoolean(getString(R.string.pref_filter_5_key), true));
        mCheckBoxFilter6.setChecked(mPrefs.getBoolean(getString(R.string.pref_filter_6_key), true));
        mCheckBoxFilter7.setChecked(mPrefs.getBoolean(getString(R.string.pref_filter_7_key), true));
        mCheckBoxFilter8.setChecked(mPrefs.getBoolean(getString(R.string.pref_filter_8_key), true));
    }

    private void attachListeners() {
        mFilter1.setOnClickListener(mDrawerItemCheckBoxClickListener);
        mFilter2.setOnClickListener(mDrawerItemCheckBoxClickListener);
        mFilter3.setOnClickListener(mDrawerItemCheckBoxClickListener);
        mFilter4.setOnClickListener(mDrawerItemCheckBoxClickListener);
        mFilter5.setOnClickListener(mDrawerItemCheckBoxClickListener);
        mFilter6.setOnClickListener(mDrawerItemCheckBoxClickListener);
        mFilter7.setOnClickListener(mDrawerItemCheckBoxClickListener);
        mFilter8.setOnClickListener(mDrawerItemCheckBoxClickListener);

        mCheckBoxFilter1.setOnClickListener(mDrawerItemCheckBoxClickListener);
        mCheckBoxFilter2.setOnClickListener(mDrawerItemCheckBoxClickListener);
        mCheckBoxFilter3.setOnClickListener(mDrawerItemCheckBoxClickListener);
        mCheckBoxFilter4.setOnClickListener(mDrawerItemCheckBoxClickListener);
        mCheckBoxFilter5.setOnClickListener(mDrawerItemCheckBoxClickListener);
        mCheckBoxFilter6.setOnClickListener(mDrawerItemCheckBoxClickListener);
        mCheckBoxFilter7.setOnClickListener(mDrawerItemCheckBoxClickListener);
        mCheckBoxFilter8.setOnClickListener(mDrawerItemCheckBoxClickListener);
    }

    private CheckBox findCheckBoxByTag(String tag) {
        if (tag.equals(getString(R.string.filter_1))) {
            return mCheckBoxFilter1;
        } else if (tag.equals(getString(R.string.filter_2))) {
            return mCheckBoxFilter2;
        } else if (tag.equals(getString(R.string.filter_3))) {
            return mCheckBoxFilter3;
        } else if (tag.equals(getString(R.string.filter_4))) {
            return mCheckBoxFilter4;
        } else if (tag.equals(getString(R.string.filter_5))) {
            return mCheckBoxFilter5;
        } else if (tag.equals(getString(R.string.filter_6))) {
            return mCheckBoxFilter6;
        } else if (tag.equals(getString(R.string.filter_7))) {
            return mCheckBoxFilter7;
        } else if (tag.equals(getString(R.string.filter_8))) {
            return mCheckBoxFilter8;
        } else {
            return null;
        }
    }

    /**
     * Guarda los filtros seleccionados para mantenerlos aunque se cierre la aplicación.
     */
    private void updateFilter() {
        // Se comprueban las categorías que se quieren mostrar.
        if (mCheckBoxFilter1.isChecked()) {
            mPrefs.edit().putBoolean(getString(R.string.pref_filter_1_key), true).apply();
        } else {
            mPrefs.edit().putBoolean(getString(R.string.pref_filter_1_key), false).apply();
        }

        if (mCheckBoxFilter2.isChecked()) {
            mPrefs.edit().putBoolean(getString(R.string.pref_filter_2_key), true).apply();
        } else {
            mPrefs.edit().putBoolean(getString(R.string.pref_filter_2_key), false).apply();
        }

        if (mCheckBoxFilter3.isChecked()) {
            mPrefs.edit().putBoolean(getString(R.string.pref_filter_3_key), true).apply();
        } else {
            mPrefs.edit().putBoolean(getString(R.string.pref_filter_3_key), false).apply();
        }

        if (mCheckBoxFilter4.isChecked()) {
            mPrefs.edit().putBoolean(getString(R.string.pref_filter_4_key), true).apply();
        } else {
            mPrefs.edit().putBoolean(getString(R.string.pref_filter_4_key), false).apply();
        }

        if (mCheckBoxFilter5.isChecked()) {
            mPrefs.edit().putBoolean(getString(R.string.pref_filter_5_key), true).apply();
        } else {
            mPrefs.edit().putBoolean(getString(R.string.pref_filter_5_key), false).apply();
        }

        if (mCheckBoxFilter6.isChecked()) {
            mPrefs.edit().putBoolean(getString(R.string.pref_filter_6_key), true).apply();
        } else {
            mPrefs.edit().putBoolean(getString(R.string.pref_filter_6_key), false).apply();
        }

        if (mCheckBoxFilter7.isChecked()) {
            mPrefs.edit().putBoolean(getString(R.string.pref_filter_7_key), true).apply();
        } else {
            mPrefs.edit().putBoolean(getString(R.string.pref_filter_7_key), false).apply();
        }

        if (mCheckBoxFilter8.isChecked()) {
            mPrefs.edit().putBoolean(getString(R.string.pref_filter_8_key), true).apply();
        } else {
            mPrefs.edit().putBoolean(getString(R.string.pref_filter_8_key), false).apply();
        }
    }

}
