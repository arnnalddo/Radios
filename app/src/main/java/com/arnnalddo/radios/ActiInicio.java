package com.arnnalddo.radios;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import com.koushikdutta.async.future.Future;
import com.koushikdutta.ion.Ion;

import java.io.StringReader;


/**
 * Created by arnaldito100 on 23/09/2014.
 * Copyright © 2014 Arnaldo Alfredo. All rights reserved.
 * http://www.arnnalddo.com
 * Actividad de "arranque"
 */
public class ActiInicio extends AppCompatActivity implements View.OnClickListener {
    //*****************************************************************
    // PROPIEDADES
    //*****************************************************************
    private Future<String> json;// future para cargar la configuración de la app (json) con ion
    private Intent iActiPrincipal;// intent para iniciar ActiPrincipal
    private final Handler controladorIon = new Handler();// controlador para ion
    private int versionApp = 0;// bandera para informar al usuario si hay una nueva versión de la app
    private final String[] campos = {"version", "candy", "intersticial",
            "verGDPR", "urlFacebook", "urlTwitter", "urlInstagram", "urlTienda",
            "urlMedios", "urlNoticias", "urlCotizacion", "urlClima",
            "urlCine", "urlHoroscopo", "urlFutbol"};// nombres de campos del json a usar
    
    //*****************************************************************
    // MÉTODOS
    //*****************************************************************
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Primero lo primero... pongo mi disenio
        setContentView(R.layout.actividad_inicio);
        
        // Creo una intent para mostrar la actividad principal
        iActiPrincipal = new Intent(this, ActiPrincipal.class);
        
        // Verifico si se obtuvo una intent con datos,
        // para crear un vínculo directo al contenido de la aplicación (beta)
        if (getIntent() != null && getIntent().getData() != null) {
            Uri urlScheme = getIntent().getData();
            iActiPrincipal.setData(urlScheme);
        }
        
        // (!) Error conocido de ion (por si hay error en el cifrado de Google Play Services)
        Ion.getDefault(this).getConscryptMiddleware().enable(false);
        
        // Cargar datos de configuraciones principales
        cargarDatos(getString(R.string.appURIConfig));
    }
    
    @Override
    public void onClick(View vista) {
        // Obtengo el identificador de la vista
        int id = vista.getId();
        // Si se trata del botón para actualizar la aplicación,
        if (id == R.id.botonActualizarAhora) {
            // guardo en el dispositivo el código de versión obtenido del json,
            Util.editarPreferencia(ActiInicio.this, "VERSION", versionApp);
            // llevo al usuario a la tienda...
            Util.irAGPlay(ActiInicio.this);
            // ...y finalizo la aplicación
            finish();
        } else if (id == R.id.botonActualizarDespues) {// Si se trata del botón para ignorar la actualización,
            // guardo en el dispositivo el código de versión obtenido del json
            Util.editarPreferencia(ActiInicio.this, "VERSION", versionApp);
            // y muestro la actividad principal
            verActividadPrincipal();
        }
    }
    
    private void cargarDatos(final String url) {
        // No intentar cargar si ya hay una carga en curso
        if (json != null && !json.isDone() && !json.isCancelled())
            return;
        // Me aseguro de que la URL es válida
        if (!Util.verificarFormato(url, Util.FormatoTexto.url)) {
            verError(Util.obtenerCadena(ActiInicio.this, 18));
            return;
        }
        // Cargo el archivo como String...
        json = Ion.with(ActiInicio.this)
                       .load(url)
                       .noCache()// no guardar en caché
                       .setHandler(controladorIon)
                       .asString();
        // ...e invoco una «posllamada» al finalizar
        json.setCallback((error, configuraciones) -> {
            // Si hubo error, informo al usuario
            if (error != null) {
                verError(getString(R.string.error_json_txt) + ((error.getMessage() == null) ? " " + getString(R.string.error_desconocido) : " " + error.getMessage()));
                return;
            }
            // Si no hubo error, verifico si se trata o no de un jsonp
            if (configuraciones.endsWith(")") || configuraciones.endsWith(");")) {
                // y trato de convertir a json
                try {
                    configuraciones = configuraciones.substring(configuraciones.indexOf("(") + 1, configuraciones.lastIndexOf(")"));
                } catch (Exception errorJsonp) {
                    errorJsonp.printStackTrace();
                }
            }
            // luego, trato de obtener los elementos
            try {
                // para "parsear" (analizar)  el json
                JsonParser analizador = new JsonParser();
                // para leer o interpretar el json como cadena
                JsonReader lector = new JsonReader(new StringReader(configuraciones));
                // ignoro errores comunes de sintaxis (de json)
                lector.setLenient(true);
                // Obtengo el contenido como objeto,
                Object objeto = analizador.parse(lector);
                // lo interpreto como matriz
                JsonArray matriz = (JsonArray) objeto;
                // finalizo mi lectura del json
                lector.close();
                // y, finalmente, obtengo cada objeto de la matriz
                for (int i = 0; i < matriz.size(); i++) {
                    // Obtengo el objeto del json
                    JsonObject obj = matriz.get(i).getAsJsonObject();
                    // Trato de proseguir solo si el json contiene el campo
                    // "urlMedios" y su valor es una URL valida
                    if (obj.has("urlMedios") && !obj.get("urlMedios").isJsonNull() && Util.verificarFormato(obj.get("urlMedios").getAsString().trim(), Util.FormatoTexto.url)) {
                        // Obtengo el valor de cada campo del objeto
                        for (String campo : campos) {
                            // Y continúo solo si el valor no es nulo
                            if (obj.has(campo) && !obj.get(campo).isJsonNull()) {
                                if ("version".equals(campo)) {// si se trara del código de versión de la aplicación,
                                    // obtengo el valor como entero (solo voy a usar este dato en esta actividad)
                                    versionApp = obj.get(campo).getAsInt();
                                } else {// para el resto de objetos, sus valores los
                                    // pongo en la intent de la actividad principal
                                    iActiPrincipal.putExtra(campo, obj.get(campo).getAsString().trim());
                                }// fin switch
                            }// fin if
                        }// fin for
                        // Casos especiales
                        iActiPrincipal.putExtra("verMenu", (!obj.has("verMenu") || obj.get("verMenu").getAsString().trim().equalsIgnoreCase("true")));
                        // Mostrar un mensaje para actualizar la aplicación, si
                        // hay una nueva versión disponible (según el dato del json)
                        if (Util.obtPreferencia(ActiInicio.this, "VERSION", BuildConfig.VERSION_CODE) < versionApp && versionApp > BuildConfig.VERSION_CODE) {
                            // "inflo" (agrego) mi disenio del mensaje de actualización al
                            // contenedor principal del disenio de esta actividad,
                            LayoutInflater inflador = LayoutInflater.from(ActiInicio.this);
                            inflador.inflate(R.layout.inc_actualizacion, findViewById(R.id.contenedorPrincipal), true);
                            // obtengo cada vista u objeto del disenio,
                            TextView titulo = findViewById(R.id.tituloActualizar);// vista del título
                            TextView descripcion = findViewById(R.id.descripcionActualizar);// vista de la descripción
                            Button botonActualizar = findViewById(R.id.botonActualizarAhora);// botón para actualizar
                            Button botonIgnorar = findViewById(R.id.botonActualizarDespues);// botón para ignorar actualización
                            // le pongo acción a los botones y,
                            botonActualizar.setOnClickListener(ActiInicio.this);
                            botonIgnorar.setOnClickListener(ActiInicio.this);
                            // cambio la fuente tipográfica de cada objeto, para que luzcan preciosos como yo
                            Util.cambiarFuente(ActiInicio.this, getString(R.string.fuente_negrita), titulo);
                            Util.cambiarFuente(ActiInicio.this, getString(R.string.fuente_regular), descripcion);
                            Util.cambiarFuente(ActiInicio.this, getString(R.string.fuente_regular), botonActualizar);
                            Util.cambiarFuente(ActiInicio.this, getString(R.string.fuente_regular), botonIgnorar);
                        } else// Si no hay nueva versión disponible, muestro la actividad principal
                            verActividadPrincipal();
                    } else// Si el json no tiene el elemento "urlMedios", muestro un mensaje de error
                        verError(Util.obtenerCadena(ActiInicio.this, 19));
                }// fin for (matriz)
            } catch (Exception errorJson) {
                errorJson.printStackTrace();
                verError(errorJson.getMessage());
            }// fin try/catch
        });// fin ion
    }// fin cargarDatos
    
    private void verActividadPrincipal() {
        // (!) No cambiar el orden
        startActivity(iActiPrincipal);// inicio ActiPrincipal
        finish();// y finalizo esta actividad...
        // ...sin animación
        overridePendingTransition(0, 0);
    }
    
    private void verError(String mensaje) {
        // cambio la fuente tipográfica del título
        String titAD = Util.cambiarFuente(this, getString(R.string.fuente_negrita), getString(R.string.error)).toString().toUpperCase();
        // Creo una "ventanita" para mostrar el mensaje de error
        // y dos botones (para reintentar carar los datos o abandonar la aplicación)
        AlertDialog.Builder arqui = new AlertDialog.Builder(ActiInicio.this)
                                            .setCancelable(false)
                                            .setTitle(titAD)
                                            .setMessage(mensaje)
                                            .setPositiveButton(getString(R.string.reintentar), (dialog, which) -> {
                                                dialog.dismiss();
                                                cargarDatos(getString(R.string.appURIConfigAlt));
                                            })
                                            .setNegativeButton(getString(R.string.menu_salir), (dialog, which) -> {
                                                dialog.dismiss();
                                                finish();
                                            });
        try {
            final AlertDialog ventanita = arqui.create();
            // Pongo animación a la ventanita
            if (ventanita.getWindow() != null)
                ventanita.getWindow().getAttributes().windowAnimations = R.style.AnimAlerta;
            // Cuando se muestra la ventanita, esto es lo que va a pasar...
            ventanita.setOnShowListener(dialog -> {
                // voy a obtener cada elemento...
                TextView txtAD = ventanita.findViewById(android.R.id.message);
                Button btn1 = ventanita.getButton(AlertDialog.BUTTON_NEGATIVE);
                Button btn2 = ventanita.getButton(AlertDialog.BUTTON_POSITIVE);
                Button btn3 = ventanita.getButton(AlertDialog.BUTTON_NEUTRAL);
                // ...y cambiar la fuente tipográfica, para que se vean tan bonitos como xó
                if (txtAD != null)
                    Util.cambiarFuente(ActiInicio.this, getString(R.string.fuente_regular), txtAD);
                if (btn1 != null)
                    Util.cambiarFuente(ActiInicio.this, getString(R.string.fuente_regular), btn1);
                if (btn2 != null)
                    Util.cambiarFuente(ActiInicio.this, getString(R.string.fuente_regular), btn2);
                if (btn3 != null)
                    Util.cambiarFuente(ActiInicio.this, getString(R.string.fuente_regular), btn3);
            });
            // ¡Bum! El usuario ve la ventanita (:
            ventanita.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
