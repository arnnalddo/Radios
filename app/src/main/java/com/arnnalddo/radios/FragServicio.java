package com.arnnalddo.radios;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.koushikdutta.async.future.Future;
import com.koushikdutta.ion.Ion;

import java.util.ArrayList;


/**
 * Created by arnaldito100 on 18/8/17.
 * Copyright © 2017 Arnaldo Alfredo. All rights reserved.
 * http://www.arnnalddo.com
 * Fragmento Principal para ver los servicios
 * Esta clase abstracta es la «madre» de las clases de los servicios:
 * FragServicioClima, FragServicioCotizacion, FragServicioFutbol, FragServicioMedio, etc.
 * (!) Evitar modificar
 */

public abstract class FragServicio extends Fragment implements View.OnClickListener {
    //*****************************************************************
    // PROPIEDADES
    //*****************************************************************
    // Diseño
    protected View vistaPrincipal;// vista principal
    protected ProgressBar ruedaCentro;// indicador de carga de contenido (centro)
    protected TextView txtCentro;// contenedor donde se muestran mensajes (centro)
    protected Button btnCentro;// botón que aparece para reintentar cargar datos
    protected RecyclerView lista;// lista donde se muestran los ítems
    protected RecyclerView.Adapter<?> miLista;// adaptador para la lista
    protected final ArrayList<ItemLista> items = new ArrayList<>();// items para el adaptador de la lista
    //private AdView mAdView;
    // Banderas
    protected static boolean hayError = false;// para saber si hubo o no algún error
    protected static int codigoError = 0;// para mostrar cadena traducida aun después de onConfigurationChanged()
    protected static String extraError = null;// para mostrar mensaje de error extra (no traducible)
    protected static JsonElement campo;// campo del json
    protected static String valor;// valor del campo del json
    // Otros
    protected Context contexto;
    protected String idServicio;// identificador del servicio
    protected String nombreServicio;// nombre del servicio
    protected Future<JsonArray> json;// future para cargar los datos con ion
    //protected final Handler controladorIon = new Handler();// controlador para ion
    private String urlServicio;// url del json
    private JsonArray jsonTmp;// para guardar temporalmente el contenido del json (savedInstanceState) y listar después de onConfigurationChanged()
    
    //*****************************************************************
    // CONSTRUCTORES
    //*****************************************************************
    public FragServicio() {}
    
    //*****************************************************************
    //region MÉTODOS
    //*****************************************************************
    /**
     * Método para obtener los Datos con ion
     * (se asume que para todos los servicios, el json contiene
     * una matriz principal, y es esa matriz la que se obtiene)
     */
    private void cargarDatos(final String url) {
        // no intentar cargar si ya hay una carga en curso
        if (json != null && !json.isDone() && !json.isCancelled())
            return;
        // me aseguro de que la URL es válida
        if (!Util.verificarFormato(url, Util.FormatoTexto.url)) {
            codigoError = 18;
            extraError = null;
            verError(codigoError, null);
            return;// salgo de acá después de mostrar mensaje de error
        }
        // actualizo la iu
        lista.setVisibility(View.INVISIBLE);
        txtCentro.setVisibility(View.INVISIBLE);
        btnCentro.setVisibility(View.INVISIBLE);
        ruedaCentro.setVisibility(View.VISIBLE);
        // cargo el archivo como JsonArray...
        json = Ion.with(this)
                       // pongo la url...
                       .load(url)
                       // ...con un parámetro, indicando el idioma del dispositivo
                       .addQuery("idioma", Util.traerIdioma())
                       // 15 segundos de carga como máximo
                       .setTimeout(15000)
                       // no guardar en caché
                       .noCache()
                       //.setHandler(controladorIon)
                       .asJsonArray();
        // ...e invoco una «posllamada» al finalizar
        json.setCallback((error, resultado) -> {
            // oculto el indicador de carga (ruedita del centro)
            ruedaCentro.setVisibility(View.INVISIBLE);
            // si hubo error, informo al usuario...
            if (error != null) {
                error.printStackTrace();
                codigoError = 1;
                extraError = ": " + error.getMessage();
                verError(codigoError, extraError);
                return;// ...y "salgo" de acá
            }
            // si no hubo error, guardo el contenido y muestro la lista
            jsonTmp = resultado;
            lista.setVisibility(View.VISIBLE);
            // y monto la lista con los datos obtenidos
            montarLista(resultado);
        });
    }
    
    /**
     * Método para montar la Lista con los datos obtenidos.
     * Puesto que el json de casi todos los servicios tienen el mismo formato,
     * se usa este método para acceder a cada elemento y se invoca a
     * ponerItem(args) para poner los items
     * en la lista (que sí es diferente para cada servicio).
     * Anular en la subclase, en caso de que el formato
     * del json del servicio sea diferente.
     */
    protected void montarLista(final JsonArray matrizPrimaria) {
        // obtener el tamanio actual de items
        int tamanioActual = items.size();
        // limpiar los ítems actuales
        items.clear();
        // matriz secundaria del json
        JsonArray matrizSecundaria;
        // objeto final del json
        JsonObject objetoFinal;
        // intento montar la lista
        try {
            // Para cada elemento de la matriz primaria...
            for (int i = 0; i < matrizPrimaria.size(); i++) {
                // Verifico si se trata de un objeto json...
                if (matrizPrimaria.get(i) != null && matrizPrimaria.get(i).isJsonObject()) {
                    // ...y lo guardo en:
                    JsonObject objetoPrimario = matrizPrimaria.get(i).getAsJsonObject();
                    // Y por cada elemento del objeto primario...
                    for (String nombreObjeto: objetoPrimario.keySet()) {
                        // ...verifico si se trata de una matriz json (solo quiero matriz para mi disenio)
                        if (objetoPrimario.get(nombreObjeto) != null && objetoPrimario.get(nombreObjeto).isJsonArray()) {
                            // ...y la guardo en:
                            matrizSecundaria = objetoPrimario.get(nombreObjeto).getAsJsonArray();
                            // y si la matriz secundaria no está vacía...
                            //if (matrizSecundaria != null && matrizSecundaria.size() > 0) {
                                // por cada elemento de la matriz...
                                for (int posicion = 0; posicion < matrizSecundaria.size(); posicion++) {
                                    // ...verifico si se trata de un objeto json
                                    if (matrizSecundaria.get(posicion) != null && matrizSecundaria.get(posicion).isJsonObject()) {
                                        // para finalmente uardarlo en:
                                        objetoFinal = matrizSecundaria.get(posicion).getAsJsonObject();
                                        
                                        // y poner el ítem en la lista (:
                                        ponerItem(nombreObjeto, objetoFinal);
                                        
                                    }// fin if
                                }// fin for
                            //}// fin if
                        }// fin if
                    }// fin for
                }// fin if
            }// fin for
    
            // Si no se montó la lista, informo al usuario del problema y "salgo" de acá
            if (miLista == null || items.size() == 0) {
                codigoError = 1;
                extraError = ": " + ((miLista == null) ? "miLista = null" : Util.obtenerCadena(getActivity(), 19));
                verError(codigoError, extraError);
                return;
            }
            
            hayError = false;
            // informar a mi adaptador que los ítems anteriores ya se fueron para siempre
            miLista.notifyItemRangeRemoved(0, tamanioActual);
            // informar a mi adaptador la cantidad de nuevos ítems agregados
            miLista.notifyItemRangeInserted(0, items.size());
        
        } catch (Exception error) {
            error.printStackTrace();
            codigoError = 1;
            extraError = ": " + error.getMessage();
            verError(codigoError, extraError);
        }
    }
    
    /**
     * Método para crear los items.
     * Este es el método a modificar, según cada Fragmento "hijo" (:
     */
    protected abstract void ponerItem(String nombreObjeto, JsonObject objetoFinal);
    
    /**
     * Método para mostrar Mensajes de Error
     */
    protected void verError(int codigoError, String extra) {
        
        hayError = true;// actualizo el valor de la bandera
        txtCentro.setVisibility(View.VISIBLE);// muestro el mensaje de error
        btnCentro.setVisibility(View.VISIBLE);// muestro el botón para reintentar
        
        if (codigoError != 0 && contexto != null) {
            String m = Util.obtenerCadena(contexto, codigoError) + ((extra != null) ? extra : "");
            txtCentro.setText(m);
        }
        
    }
    //endregion
    
    //*****************************************************************
    // CICLO DE VIDA DEL FRAGMENTO
    //*****************************************************************
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // obtengo el contexto actual
        contexto = getContext();
        // Obtengo la URL del servicio a cargar, pasada por la actividad
        if (getArguments() != null) {
            urlServicio = getArguments().getString("urlServicio", null);
        }// fin if
    }
    
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        System.out.println(idServicio);
        System.out.println(nombreServicio);
        System.out.println(urlServicio);
        
        // Le doy "vida" al botoncito del centro (:
        btnCentro.setOnClickListener(this);
        // Cambio la fuente tipográfica de los elementos del centro,
        // para que se vean tan bonitos como yo :>
        Util.cambiarFuente(contexto, getString(R.string.fuente_regular), txtCentro);
        Util.cambiarFuente(contexto, getString(R.string.fuente_regular), btnCentro);
        
        // Si hubo error, pues muestro al usuario
        if (hayError)
            verError(codigoError, extraError);
    
        if (savedInstanceState == null || jsonTmp == null)
            cargarDatos(urlServicio);
        else
            montarLista(jsonTmp);
        
        // ¡Bum! Poner la interfaz (del "hijo")
        return vistaPrincipal;
    }
    
    @Override
    public void onStart() {
        super.onStart();
        cargarDatos(urlServicio);// TODO: atender (esto no vuelve a llamarse en onConfigChanged)
    }
    
    @Override
    public void onDestroy() {
        // Cancelo la carga de datos si está en curso
        if (json != null && !json.isDone() && !json.isCancelled()) {
            json.cancel();
            json = null;
        }
        super.onDestroy();// Destruir fragmento
    }
    
    @Override
    public void onClick(View v) {
        // Cuando se pulsa el botón del centro (reintentar cargar los datos)
        if (v.getId() == R.id.btnCentro)
            cargarDatos(urlServicio);
    }
}
