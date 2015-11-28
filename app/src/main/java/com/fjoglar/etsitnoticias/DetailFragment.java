package com.fjoglar.etsitnoticias;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.fjoglar.etsitnoticias.data.RssContract;

import java.text.SimpleDateFormat;
import java.util.Calendar;


public class DetailFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final String LOG_TAG = DetailFragment.class.getSimpleName();
    static final String DETAIL_URI = "URI";
    static final String SHARE_HASHTAG = "#NoticiasETSIT";

    private static final int DETAIL_LOADER = 0;

    private Uri mUri;
    private String mShareText;

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

    public DetailFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.detailfragment, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();
        if (id == R.id.action_share) {
            shareItem(mShareText);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        Bundle arguments = getArguments();
        if (arguments != null) {
            mUri = arguments.getParcelable(DetailFragment.DETAIL_URI);
        } else {
            mUri = RssContract.RssEntry.buildRssWithId(1);
        }

        return inflater.inflate(R.layout.fragment_detail, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        getLoaderManager().initLoader(DETAIL_LOADER, null, this);
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Intent intent = getActivity().getIntent();
        if (null != mUri) {

            return new CursorLoader(
                    getActivity(),
                    mUri,
                    RSS_COLUMNS,
                    null,
                    null,
                    null
            );
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, final Cursor data) {
        if (!data.moveToFirst()) {
            return;
        }

        mShareText = data.getString(COL_RSS_TITLE)
                + ". "
                + data.getString(COL_RSS_LINK)
                + " "
                + SHARE_HASHTAG;

        TextView detailTitle = (TextView) getView().findViewById(R.id.detail_title);
        TextView detailDate = (TextView) getView().findViewById(R.id.detail_date);
        TextView detailDescription = (TextView) getView().findViewById(R.id.detail_description);
        TextView detailCategory = (TextView) getView().findViewById(R.id.detail_category);
        Button detailLink = (Button) getView().findViewById(R.id.detail_link);

        // Formateamos la fecha.
        Long dateInMillis = data.getLong(COL_RSS_PUB_DATE);
        SimpleDateFormat formatter = new SimpleDateFormat("EEEE");
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(dateInMillis);
        String dayName = Utility.capitalizeWord(formatter.format(calendar.getTime()));
        formatter = new SimpleDateFormat("d");
        String dayNumber = String.valueOf(formatter.format(calendar.getTime()));
        formatter = new SimpleDateFormat("MMMM");
        String month = Utility.capitalizeWord(formatter.format(calendar.getTime()));
        formatter = new SimpleDateFormat("yyyy");
        String year = String.valueOf(formatter.format(calendar.getTime()));
        String date = dayName + ", " + dayNumber + " de " + month + " de " + year + ".";

        detailTitle.setText(data.getString(COL_RSS_TITLE));
        detailDate.setText(date);
        detailDescription.setText(data.getString(COL_RSS_DESC));
        detailCategory.setText(Utility.categoryToString(data.getString(COL_RSS_CATEGORY)));

        detailLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                moreInfo(data.getString(COL_RSS_LINK));
            }
        });

    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }

    private void moreInfo(String link) {
        Intent webIntent = new Intent(Intent.ACTION_VIEW);
        webIntent.setData(Uri.parse(link));

        if (webIntent.resolveActivity(getActivity().getPackageManager()) != null) {
            startActivity(webIntent);
        }

    }

    private void shareItem(String text) {
        Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
        sharingIntent.setType("text/plain");
        sharingIntent.putExtra(Intent.EXTRA_TEXT, text);
        sharingIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        sharingIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(Intent.createChooser(sharingIntent, getString(R.string.action_share)));
    }


}
