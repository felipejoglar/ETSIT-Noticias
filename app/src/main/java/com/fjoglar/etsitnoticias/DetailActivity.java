package com.fjoglar.etsitnoticias;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;

public class DetailActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);
        if (savedInstanceState == null) {
            // Creamos un Fragment de detalle y lo añadimos a la actividad
            // usando un FragmentTransaction.

            Bundle arguments = new Bundle();
            arguments.putParcelable(DetailFragment.DETAIL_URI, getIntent().getData());

            DetailFragment fragment = new DetailFragment();
            fragment.setArguments(arguments);
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.detail_container, fragment)
                    .commit();
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

}