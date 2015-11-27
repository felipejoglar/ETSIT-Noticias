package com.fjoglar.etsitnoticias;

public class Utility {

    public static String CategoryToString(String category) {
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

}
