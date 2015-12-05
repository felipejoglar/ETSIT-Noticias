package com.fjoglar.etsitnoticias;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import com.fjoglar.etsitnoticias.adapter.RssItemAdapter;
import com.fjoglar.etsitnoticias.data.RssContract;
import com.fjoglar.etsitnoticias.service.DownloadRssService;

import java.util.Calendar;


public class MainFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>,
        SwipeRefreshLayout.OnRefreshListener, SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String LOG_TAG = MainFragment.class.getSimpleName();

    private ListView mListView;
    private int mPosition = ListView.INVALID_POSITION;

    private static final String SELECTED_KEY = "selected_position";

    private static final int RSS_LOADER = 0;

    private RssItemAdapter mRssItemAdapter;
    private BroadcastReceiver mBroadcastReceiver;
    private SwipeRefreshLayout mSwipeRefreshLayout;

    /**
     * Una interfaz de Callback que todas las actividades que contienen este fragment deben
     * implementar. Este mecanismo permite a las actividades ser notificadas de las selecciones
     * de items.
     */
    public interface Callback {
        /**
         * DetailFragmentCallback para cuando un item ha sido seleccionado.
         */
        public void onItemSelected(Uri dateUri);
    }

    // Para la vista de lista sólo necesitamos una parte de los datos almacenados.
    // Especificamos las columnas que necesitamos.
    private static final String[] RSS_COLUMNS = {
            RssContract.RssEntry._ID,
            RssContract.RssEntry.COLUMN_TITLE,
            RssContract.RssEntry.COLUMN_DESCRIPTION,
            RssContract.RssEntry.COLUMN_LINK,
            RssContract.RssEntry.COLUMN_CATEGORY,
            RssContract.RssEntry.COLUMN_PUB_DATE
    };

    // Estos indices están ligados a RSS_COLUMNS.  Si RSS_COLUMNS cambia, estos
    // deben cambiar.
    public static final int COL_RSS_ID = 0;
    public static final int COL_RSS_TITLE = 1;
    public static final int COL_RSS_DESC = 2;
    public static final int COL_RSS_LINK = 3;
    public static final int COL_RSS_CATEGORY = 4;
    public static final int COL_RSS_PUB_DATE = 5;


    public MainFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        // Recibimos el resultado del servicio y si la sincronización se hizo
        // de manera manual o al iniciar la aplicación detenemos la animación
        // del SwipeToRefresh.
        mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String message = intent.getStringExtra(DownloadRssService.SERVICE_MESSAGE);
                if (message.equals(getContext().getString(R.string.service_result)) && mSwipeRefreshLayout != null) {
                    mSwipeRefreshLayout.setRefreshing(false);
                }
            }
        };

    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.mainfragment, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();
        if (id == R.id.action_settings) {
            startActivity(new Intent(getContext(), SettingsActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        mRssItemAdapter = new RssItemAdapter(getContext(), null, 0);

        View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        // Declaramos SwipeToRefresh.
        mSwipeRefreshLayout = (SwipeRefreshLayout) rootView.findViewById(R.id.swipe_to_refresh);
        mSwipeRefreshLayout.setColorSchemeResources(R.color.colorAccent);

        // Obtenemos una referencia al ListView y vinculamos el adaptador.
        mListView = (ListView) rootView.findViewById(R.id.listview_rss);
        mListView.setAdapter(mRssItemAdapter);

        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {

                Cursor cursor = (Cursor) adapterView.getItemAtPosition(position);
                if (cursor != null) {
                    ((Callback) getContext())
                            .onItemSelected(RssContract.RssEntry.buildRssWithId(
                                    cursor.getLong(COL_RSS_ID)
                            ));
                }
                mPosition = position;
            }
        });


        if (savedInstanceState != null && savedInstanceState.containsKey(SELECTED_KEY)) {
            mPosition = savedInstanceState.getInt(SELECTED_KEY);
        }

        mSwipeRefreshLayout.setOnRefreshListener(this);
        // Al iniciar la aplicación actualizamos los datos.
        mSwipeRefreshLayout.post(new Runnable() {
            @Override
            public void run() {
                updateData();
            }
        });

        return rootView;
    }

    @Override
    public void onRefresh() {
        updateData();
    }

    @Override
    public void onStart() {
        super.onStart();
        LocalBroadcastManager.getInstance(getContext()).registerReceiver((mBroadcastReceiver),
                new IntentFilter(DownloadRssService.SERVICE_RESULT)
        );
    }

    @Override
    public void onStop() {
        LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(mBroadcastReceiver);
        super.onStop();
    }

    @Override
    public void onResume() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getActivity());
        sp.registerOnSharedPreferenceChangeListener(this);
        super.onResume();
    }

    @Override
    public void onPause() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getActivity());
        sp.unregisterOnSharedPreferenceChangeListener(this);
        super.onPause();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        getLoaderManager().initLoader(RSS_LOADER, null, this);
        super.onActivityCreated(savedInstanceState);
    }

    private void updateData() {
        // Si la última actualización ha sido hace menos de 10 minutos no actualizamos directamente
        // así evitamos conexiones innecesarias cuando se rota la pantalla y conseguimos
        // un ahorro de batería, importente en dispositivos móviles.
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        if (System.currentTimeMillis() - prefs.getLong(getContext().getString(R.string.pref_last_updated_key), 0) > 10 * 60 * 1000) {
            mSwipeRefreshLayout.setRefreshing(true);
            if (Utility.isNetworkAvailable(getActivity())) {
                Intent intent = new Intent(getContext(), DownloadRssService.class);
                getContext().startService(intent);
            } else {
                mSwipeRefreshLayout.setRefreshing(false);
            }
        }

        AlarmManager alarmMgr = (AlarmManager) getContext().getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(getContext(), DownloadRssService.AlarmReceiver.class);
        PendingIntent alarmIntent = PendingIntent.getBroadcast(getContext(), 0, intent, 0);

        // Ponemos una alarma aproximadamente a las 00:00 horas.
        // Se trata de poder temporizar la sincronización de la aplicación para así
        // poder enviar notificaciones al usuario.
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.set(Calendar.HOUR_OF_DAY, 0);

        // Obtenemos el periodo de sincronización.
        int syncInterval = Integer.parseInt(prefs.getString(getContext().getString(R.string.pref_sync_frequency_key), "6"));
        alarmMgr.setInexactRepeating(AlarmManager.RTC_WAKEUP,
                calendar.getTimeInMillis(),
                AlarmManager.INTERVAL_HOUR * syncInterval,
                alarmIntent);

    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        // Guardamos el item seleccionado cuando la tableta rota.
        if (mPosition != ListView.INVALID_POSITION) {
            outState.putInt(SELECTED_KEY, mPosition);
        }
        super.onSaveInstanceState(outState);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {

        // Orden: por fecha, descendiente.
        String sortOrder = RssContract.RssEntry.COLUMN_PUB_DATE + " DESC";
        Uri rssUri = RssContract.RssEntry.CONTENT_URI;

        return new CursorLoader(getContext(),
                rssUri,
                RSS_COLUMNS,
                null,
                null,
                sortOrder);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        mRssItemAdapter.swapCursor(cursor);
        if (mPosition != ListView.INVALID_POSITION) {
            mListView.smoothScrollToPosition(mPosition);
        } else {
            mListView.setItemChecked(0, true);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        mRssItemAdapter.swapCursor(null);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if ( key.equals(getString(R.string.pref_item_id_key)) && MainActivity.mTwoPane ) {
            ((Callback) getContext())
                    .onItemSelected(RssContract.RssEntry.buildRssWithId(
                            sharedPreferences.getLong(getString(R.string.pref_item_id_key), 1)
                    ));
        }
    }

    public void reloadFragment() {
        getLoaderManager().restartLoader(RSS_LOADER, null, this);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        prefs.edit().putBoolean(getContext().getString(R.string.pref_modify_item_id_key),
                true).apply();
    }

}
