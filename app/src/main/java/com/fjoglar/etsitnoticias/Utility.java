package com.fjoglar.etsitnoticias;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

public class Utility {

    public static String categoryToString(String category) {
        String result;

        if (category.equals("1") || category.equals("2")) {
            result = "General";
        } else if (category.equals("3") || category.equals("4")) {
            result = "Beca/Empleo";
        } else if (category.equals("5")) {
            result = "TFG/TFM";
        } else if (category.equals("11")) {
            result = "Conferencia/Taller";
        } else if (category.equals("12")) {
            result = "Destacado";
        } else if (category.equals("15")) {
            result = "Junta de Escuela";
        } else if (category.equals("16")) {
            result = "Investigación";
        } else {
            result = "Otros";
        }

        return result;
    }

    public static String capitalizeWord(String word) {
        return Character.toUpperCase(word.charAt(0)) + word.substring(1);
    }

    /**
     * Devuelve true si la red esta disponible.
     *
     * @param c Context usado para obtener el ConnectivityManager
     * @return true si la red esta disponible
     */
    static public boolean isNetworkAvailable(Context c) {
        ConnectivityManager cm =
                (ConnectivityManager) c.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting();
    }

}
