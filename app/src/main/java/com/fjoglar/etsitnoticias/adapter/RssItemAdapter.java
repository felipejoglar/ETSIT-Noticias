package com.fjoglar.etsitnoticias.adapter;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.TextView;

import com.fjoglar.etsitnoticias.MainFragment;
import com.fjoglar.etsitnoticias.R;
import com.fjoglar.etsitnoticias.Utility;

import java.text.SimpleDateFormat;
import java.util.Calendar;

public class RssItemAdapter extends CursorAdapter {

    private SharedPreferences mPrefs;

    /**
     * Cache de las vistas de un objeto de la lista.
     */
    public static class ViewHolder {
        public final TextView titleView;
        public final TextView dateView;
        public final TextView descriptionView;
        public final TextView categoryView;

        public ViewHolder(View view) {
            titleView = (TextView) view.findViewById(R.id.list_item_title);
            dateView = (TextView) view.findViewById(R.id.list_item_date);
            descriptionView = (TextView) view.findViewById(R.id.list_item_description);
            categoryView = (TextView) view.findViewById(R.id.list_item_category);
        }
    }

    /**
     * Constructor. RssItemAdapter expone una lista de noticias RSS
     * desde un Cursor en una ListView.
     */
    public RssItemAdapter(Context context, Cursor c, int flags) {
        super(context, c, flags);
        mPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        mPrefs.edit().putBoolean(context.getString(R.string.pref_modify_item_id_key),
                true).apply();
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {

        View view = LayoutInflater.from(context).inflate(R.layout.rss_list_item, parent, false);

        ViewHolder viewHolder = new ViewHolder(view);
        view.setTag(viewHolder);

        return view;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {

        ViewHolder viewHolder = (ViewHolder) view.getTag();
        // Modificamos la noticia a mostrar en la vista de detalle para el modo de
        // dos paneles, de esta manera se cargará automaticamente la primera noticia
        // de la lista.
        if (mPrefs.getBoolean(context.getString(R.string.pref_modify_item_id_key), true)) {
            mPrefs.edit().putLong(context.getString(R.string.pref_item_id_key),
                    cursor.getLong(MainFragment.COL_RSS_ID)).apply();
            mPrefs.edit().putBoolean(context.getString(R.string.pref_modify_item_id_key),
                    false).apply();
        }

        // Formateamos la fecha.
        Long dateInMillis = cursor.getLong(MainFragment.COL_RSS_PUB_DATE);
        SimpleDateFormat formatter = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss z");
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(dateInMillis);
        String date = String.valueOf(formatter.format(calendar.getTime()));
        date = Utility.formatTime(date);

        // Actualizamos las vistas.
        viewHolder.titleView.setText(cursor.getString(MainFragment.COL_RSS_TITLE));
        viewHolder.dateView.setText(date);
        viewHolder.descriptionView.setText(cursor.getString(MainFragment.COL_RSS_DESC));
        viewHolder.categoryView.setText(Utility.categoryToString(cursor.getString(MainFragment.COL_RSS_CATEGORY)));

    }

}
