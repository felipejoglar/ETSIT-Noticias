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

    // Clase que define los contenidos de tabla RSS.
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
