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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class RssItemAdapter extends CursorAdapter {

    // Etiqueta para los logs de depuración.
    private final String LOG_TAG = RssItemAdapter.class.getSimpleName();

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
        date = formatTime(date);

        viewHolder.titleView.setText(cursor.getString(MainFragment.COL_RSS_TITLE));
        viewHolder.dateView.setText(date);
        viewHolder.descriptionView.setText(cursor.getString(MainFragment.COL_RSS_DESC));
        viewHolder.categoryView.setText(Utility.categoryToString(cursor.getString(MainFragment.COL_RSS_CATEGORY)));

    }

    /**
     * Coge la fecha de la noticia y la devuelve en un formato más adecuado para
     * la lectura en un ListView, pudiendo identificar de un vistazo, hace cuanto
     * salió la noticia.
     *
     * @param pDate Fecha de la noticia como String en formato "yyyy-MM-dd HH:mm:ss".
     * @return la fecha en el formato adecuado.
     */
    private String formatTime(String pDate) {
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
