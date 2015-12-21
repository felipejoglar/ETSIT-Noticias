package com.fjoglar.etsitnoticias;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class SplashScreenActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Mostramos el logo de AITCYL durante un segundo y medio antes de iniciar
        // la aplicación.
        try {
            Thread.sleep(1500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Una vez finalizado el tiempo abrimos la actividad principal.
        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        // Estos Flags evitan que la App vuelva a mostrar esta pantalla cuando pulsamos
        // el botón de atrás del sistema.
        // Esta Activity sólo se muestra al iniciar la App.
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();

    }
}
