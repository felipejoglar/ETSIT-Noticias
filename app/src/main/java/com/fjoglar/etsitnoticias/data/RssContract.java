package com.fjoglar.etsitnoticias.data;

import android.content.ContentResolver;
import android.net.Uri;
import android.provider.BaseColumns;

/**
 * Define los nombres de tabla y columnas para la base de datos del RSS.
 */
public class RssContract {

    // El "Content authority" es el nombre para el content provider.
    public static final String CONTENT_AUTHORITY = "com.fjoglar.etsitnoticias";

    // Usamos CONTENT_AUTHORITY para crear la base de todas las URI's usadas para contactar
    // con el content provider.
    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);

    // Posibles paths (añadidos a la URI base)
    public static final String PATH_RSS = "rss";

    /* Clase que define los contenidos de tabla RSS. */
    public static final class RssEntry implements BaseColumns {

        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_RSS).build();

        public static final String CONTENT_TYPE =
                ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_RSS;
        public static final String CONTENT_ITEM_TYPE =
                ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_RSS;

        // Nombre de tabla.
        public static final String TABLE_NAME = "rss";

        // String con el título del item RSS.
        public static final String COLUMN_TITLE = "title";
        // Lo mismo con el resto de columnas.
        public static final String COLUMN_DESCRIPTION = "description";
        public static final String COLUMN_LINK = "link";
        public static final String COLUMN_CATEGORY = "category";
        // Fecha de publicación en milisegundos.
        public static final String COLUMN_PUB_DATE = "pub_date";

        public static Uri buildRssWithId(long id) {
            return CONTENT_URI.buildUpon().appendPath(Long.toString(id)).build();
        }

    }

}
