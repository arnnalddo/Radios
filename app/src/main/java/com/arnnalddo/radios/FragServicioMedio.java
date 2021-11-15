package com.arnnalddo.radios;

import static com.arnnalddo.radios.Util.ACTI_PR_RECIEN_CREADA;
import static com.arnnalddo.radios.Util.AUTOPLAY;
import static com.arnnalddo.radios.Util.EstadoMP;
import static com.arnnalddo.radios.Util.regionUsuario;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.HashSet;
import java.util.Set;


/**
 * Created by arnaldito100 on 20/04/2016.
 * Copyright © 2016 Arnaldo Alfredo. All rights reserved.
 * http://www.arnnalddo.com
 * Medios
 */
public class FragServicioMedio extends FragServicio implements View.OnClickListener, Lista.DetectorDeToque {
    
    private FloatingActionButton btnPlayStop;// botón flotante play/stop
    private Intent iMediaService;// intent para vincular con MediaService
    private Animation fab_open, fab_close, fab_rotar_ini, fab_rotar;// animaciones
    private Uri urlScheme;// URL Scheme (app://dato)
    private String[] ultMedioArray = {};// para guardar el valor del último medio seleccionado
    private static final String filtro = "modulacion";// para organizar el listado en secciones
    private String valorFiltro = "";// para guardar el valor del filtro
    private boolean nSeccion = false;// para saber si crear o no una nueva sección en la lista
    private static final String[] campos = {"id", "nombre", "modulacion", "ciudad", "region", "logo_chico_url", "logo_grande_url", "video", "url_metadatos", "grabaciones_url", "rtsp", "rtsp_tigo", "rtsp_copaco", "hls", "hls_tigo", "hls_copaco", "vip", "whatsapp"};// nombres de campos del json a usar
    public final MediaCallback mediaCallback = new ActualizarIU();// interfaz para actualizar mi IU, según MediaService
    
    public FragServicioMedio() {}
    
    /**
     * Método para montar la Lista con los datos
     */
    @Override
    protected void montarLista(JsonArray matrizPrimaria) {
        // obtener el tamanio actual de items
        int tamanioActual = items.size();
        // limpiar los ítems actuales
        items.clear();
        // objeto final del json
        JsonObject objetoFinal;
        // campo del json
        if (contexto != null) {
            ultMedioArray = Util.obtPreferencia(contexto, Util.PREF_ULTIMO_MEDIO);
        }
        // intento montar la lista
        try {
            // 2. Intento obtener cada objeto del json
            // para luego manejar el valor de los campos
            for (int posicion = 0; posicion < matrizPrimaria.size(); posicion++) {
                // ...verifico si se trata de un objeto json
                if (matrizPrimaria.get(posicion) != null && matrizPrimaria.get(posicion).isJsonObject()) {
                    objetoFinal = matrizPrimaria.get(posicion).getAsJsonObject();
                    
                    ponerItem(null, objetoFinal);
                    
                }// fin if
                
            }// Fin for
            
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
    
    @Override
    protected void ponerItem(String nombreObjeto, JsonObject objetoFinal) {
        ItemListaMedio miItem = new ItemListaMedio(1);
        for (String nombreCampo : campos) {
            campo = objetoFinal.get(nombreCampo);
            if (campo != null) {
                valor = campo.getAsString().trim();
                if (filtro.equals(nombreCampo) && !valorFiltro.equals(valor)) {
                    nSeccion = true;
                    valorFiltro = valor;
                }
                switch (nombreCampo) {
                    case "id":
                        miItem.ponerID(valor);
                        break;
                    case "nombre":
                        miItem.ponerNombre(valor);
                        break;
                    case "region":
                        miItem.ponerRegion(valor);
                        break;
                    case "logo_chico_url":
                        miItem.ponerURLLogoChico(valor);
                        break;
                    case "logo_grande_url":
                        miItem.ponerURLLogoGrande(valor);
                        break;
                    case "url_metadatos":
                        miItem.ponerURLMetadatos(valor);
                    case "grabaciones_url":
                        miItem.ponerURLGrabaciones(valor);
                        break;
                    case "rtsp":
                    case "rtsp_tigo":
                    case "rtsp_copaco":
                    case "hls":
                    case "hls_tigo":
                    case "hls_copaco":
                        if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && nombreCampo.contains("hls")) || (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP && nombreCampo.contains("rtsp"))) {
                            if (nombreCampo.contains("tigo")) {
                                miItem.ponerURLTigo(valor);
                                break;
                            } else if (nombreCampo.contains("copaco")) {
                                miItem.ponerURLCopaco(valor);
                                break;
                            }
                            miItem.ponerURL(valor);
                            break;
                        }
                        break;
                    case "video":
                        boolean esVideo = valor.equalsIgnoreCase("true");
                        miItem.ponerEsVideo(esVideo);
                        break;
                    case "vip":
                        //medioEsVip = objeto.get(nombreCampo).getAsString().trim().equalsIgnoreCase("true");
                        break;
                    default:
                        break;
                }// Fin switch
                
            }// Fin if
            
        }// Fin for
        
        
        // Tengo un item (?), a ver...
        // Chequeo si hay ítem, si la región del usuario coincide con la del medio,
        // si existe al menos una URL de transmisión
        // y si el valor del id y nombre del medio no están vacíos
        // si no, para qué seguir ¯\_(ツ)_/¯
        if (regionUsuario != null && (miItem.region.equalsIgnoreCase(regionUsuario) || miItem.region.equalsIgnoreCase("global")) && (!miItem.url.isEmpty() || !miItem.urlTigo.isEmpty() || !miItem.urlCopaco.isEmpty()) && !miItem.id.isEmpty() && !miItem.nombre.isEmpty()) {
            
            // Chequear si el medio fue el ultimo seleccionado
            boolean fueUlt = (ultMedioArray.length > 0 && ultMedioArray[0].equals("00" + miItem.id));
            
            // Agregar título de sección
            //if (nSeccion) {
            //ItemListaMedio cabecera = new ItemListaMedio(0);
            //cabecera.ponerTituloSeccion(valorFiltro);
            //items.add(cabecera);
            //}
            
            // Agregar item
            miItem.ponerEsPrimerItem(nSeccion);
            items.add(miItem);
            //miLista.notifyItemInserted(posicion);
            
            nSeccion = false;// init/reset (debe estar acá)
            
            
            String urlTransmision = Util.obtenerURLMedio(miItem.url, miItem.urlTigo, miItem.urlCopaco);
            
            
            if (urlScheme != null && urlScheme.getHost() != null && urlScheme.getHost().equals(miItem.id)) {
                
                Log.d("URL SCHEME (:", urlScheme.getHost());
                
                if (!fueUlt || !MediaService.esEnVivo() || !MediaService.reproduciendo()) {
                    
                    if (!fueUlt) {
                        
                        Set<String> set = new HashSet<>();
                        set.add("00" + miItem.id);
                        set.add("01" + miItem.nombre);
                        set.add("02" + miItem.urlLogoChico);
                        set.add("03" + miItem.urlLogoGrande);
                        set.add("04" + miItem.url);
                        set.add("05" + miItem.urlTigo);
                        set.add("06" + miItem.urlCopaco);
                        set.add("07" + miItem.urlMetadatos);
                        
                        // Guardar en el dispositivo los datos del último medio seleccionado
                        if (contexto != null) {
                            Util.editarPreferencia(contexto, Util.PREF_ULTIMO_MEDIO, set);
                        }
                        
                    }
                    
                    if (contexto != null) {
                        Util.iniciarMediaService(contexto, urlTransmision, miItem.nombre, null, miItem.urlLogoGrande, miItem.urlMetadatos, true, true);
                    }
                    
                }
                
                // reset
                urlScheme = null;
                
            } else if (fueUlt && !miItem.esVideo && ACTI_PR_RECIEN_CREADA) {
                
                boolean autoPlay = (Util.obtPreferencia(contexto, "AUTOPLAY", AUTOPLAY));
                Util.iniciarMediaService(contexto, urlTransmision, miItem.nombre, null, miItem.urlLogoGrande, miItem.urlMetadatos, true, autoPlay);
                
            }// Fin else if
            
        }// Fin if
    }
    
    protected void actualizarFAB(Util.EstadoMP estado) {
        
        if (btnPlayStop == null)
            return;
        
        btnPlayStop.clearAnimation();// reset
        fab_rotar_ini.setAnimationListener(null);// reset
        
        boolean animar = btnPlayStop.getVisibility() == View.VISIBLE;
        
        switch (estado) {
            case iniciando:
                btnPlayStop.startAnimation(fab_open);
                btnPlayStop.setVisibility(View.VISIBLE);
                break;
            case conectando:
                btnPlayStop.setImageResource(R.drawable.ic_stop);
                btnPlayStop.setVisibility(View.VISIBLE);
                fab_rotar_ini.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {}
                    
                    @Override
                    public void onAnimationEnd(Animation animation) {
                        btnPlayStop.startAnimation(fab_rotar);
                        fab_rotar_ini.setAnimationListener(null);// reset
                    }
                    
                    @Override
                    public void onAnimationRepeat(Animation animation) {}
                });
                btnPlayStop.startAnimation(fab_rotar_ini);
                break;
            //case almacenando:
            case reproduciendo:
                btnPlayStop.setImageResource(R.drawable.ic_stop);
                break;
            case pausado:
            case detenido:
                btnPlayStop.setImageResource(R.drawable.ic_play);
                if (animar)
                    btnPlayStop.startAnimation(fab_open);
                break;
            default:
                break;
        }
    }
    
    /**
     * Método para reproducir o detener el Audio
     */
    private void playStop() {
        
        if (contexto != null) {
            
            String[] ultMedioArray = Util.obtPreferencia(contexto, Util.PREF_ULTIMO_MEDIO);
            
            if (MediaService.esEnVivo()) {
                
                iMediaService.setAction(MediaService.ACCION_PLAY_STOP);
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    contexto.startForegroundService(iMediaService);
                } else {
                    contexto.startService(iMediaService);
                }
                
            } else if (ultMedioArray.length > 0) {
                
                String nom = ultMedioArray[1].substring(2);
                String img = ultMedioArray[3].substring(2);
                String urlGlobal = ultMedioArray[4].substring(2);
                String urlTigo = ultMedioArray[5].substring(2);
                String urlCopaco = ultMedioArray[6].substring(2);
                String urlMetadatos = ultMedioArray[7].substring(2);
                
                String url = Util.obtenerURLMedio(urlGlobal, urlTigo, urlCopaco);
                
                Util.iniciarMediaService(contexto, url, nom, null, img, urlMetadatos, true, true);
                
            }
            
        }
        
    }
    
    /**
     * Implementación de la Interfaz MediaCallback (para actualizar mi interfaz de usuario, según MediaService)
     */
    private class ActualizarIU implements MediaCallback {
        
        @Override
        public void inicializar(boolean ocultar) {
            
            if (ocultar) {
                if (btnPlayStop == null)
                    return;
                btnPlayStop.clearAnimation();// reset
                fab_rotar_ini.setAnimationListener(null);// reset
                btnPlayStop.startAnimation(fab_close);
                btnPlayStop.setVisibility(View.INVISIBLE);
            } else {
                actualizarFAB(EstadoMP.iniciando);
            }
            
        }
        
        @Override
        public void conectando() {
            if (!MediaService.esEnVivo())
                return;
            actualizarFAB(EstadoMP.conectando);
        }
        
        @Override
        public void almacenando() {}
        
        @Override
        public void reproduciendo() {
            if (!MediaService.esEnVivo())
                return;
            actualizarFAB(EstadoMP.reproduciendo);
        }
        
        @Override
        public void pausado() {}
        
        @Override
        public void detenido() {
            if (!MediaService.esEnVivo())
                return;
            actualizarFAB(EstadoMP.detenido);
        }
        
        @Override
        public void error() {
            if (!MediaService.esEnVivo())
                return;
            Toast.makeText(contexto, getString(R.string.appNombre) + ": " + getString(R.string.error_conexion) + ".", Toast.LENGTH_SHORT).show();
        }
        
        @Override
        public void finalizado() {
            if (!MediaService.esEnVivo())
                return;
            Toast.makeText(contexto, getString(R.string.appNombre) + ": " + getString(R.string.final_archivo_msj) + ".", Toast.LENGTH_SHORT).show();
        }
        
        @Override
        public void cerrar() {
            if (!MediaService.esEnVivo())
                return;
            if (getActivity() != null)
                getActivity().finish();
        }
        
    }
    
    @Override
    public void onClick(View v) {
        int vId = v.getId();
        if (vId == R.id.btnPlayStop)
            playStop();
    }
    
    @Override
    public void enToque(int posicion) {
        
        if (items.get(posicion).tipoItem() != 0) {
            
            ItemListaMedio item = (ItemListaMedio) items.get(posicion);
            
            String urlTransmision = Util.obtenerURLMedio(item.url, item.urlTigo, item.urlCopaco);
            
            if (urlTransmision != null) {
                
                // Reproducir audio solo si video == "false"
                if (!item.esVideo) {
                    
                    String[] ultMedioArray = {};
                    
                    if (contexto != null) {
                        ultMedioArray = Util.obtPreferencia(contexto, Util.PREF_ULTIMO_MEDIO);
                    }
                    
                    if (ultMedioArray.length > 0 && ultMedioArray[0].equals("00" + item.id) && MediaService.esEnVivo() && MediaService.reproduciendo()) {
                        
                        System.out.print(item.nombre + " ya se está reproduciendo");
                        
                    } else {
                        
                        Set<String> set = new HashSet<>();
                        set.add("00" + item.id);
                        set.add("01" + item.nombre);
                        set.add("02" + item.urlLogoChico);
                        set.add("03" + item.urlLogoGrande);
                        set.add("04" + item.url);
                        set.add("05" + item.urlTigo);
                        set.add("06" + item.urlCopaco);
                        set.add("07" + item.urlMetadatos);
                        
                        if (contexto != null) {
                            
                            // Guardar en el dispositivo la info del último medio seleccionado
                            Util.editarPreferencia(contexto, Util.PREF_ULTIMO_MEDIO, set);
                            
                            // Reproducir
                            Util.iniciarMediaService(contexto, urlTransmision, item.nombre, null, item.urlLogoGrande, item.urlMetadatos, true, true);
                            
                        }
                        
                        //lista.setItemChecked(position, true);
                        
                    }
                    
                }
                
            } else {
                Toast.makeText(FragServicioMedio.this.contexto, getString(R.string.error) + ": stream_url=null", Toast.LENGTH_SHORT).show();
            }
            
        }
        
    }
    
    // -------------------------------------------------------
    // A PARTIR DE ACA INICIA EL "CICLO DE VIDA" DEL FRAGMENTO
    // -------------------------------------------------------
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        idServicio = "0";
        nombreServicio = "Medios";
        // Intent para trabajar con MediaService
        iMediaService = new Intent(contexto, MediaService.class);
        // Animaciones
        fab_open = AnimationUtils.loadAnimation(contexto, R.anim.fab_in);
        fab_close = AnimationUtils.loadAnimation(contexto, R.anim.fab_out);
        fab_rotar_ini = AnimationUtils.loadAnimation(contexto, R.anim.fab_rotar_ini);
        fab_rotar = AnimationUtils.loadAnimation(contexto, R.anim.fab_rotar);
        //setRetainInstance(true);// Retener fragmento en "onConfigChange()"// deprecado
    }
    
    @Override
    public View onCreateView(@NonNull LayoutInflater inflador, ViewGroup madre, Bundle savedInstanceState) {
        // (!) No cambiar el orden a continuación
        
        // Preparo la vista principal y sus objetos
        vistaPrincipal = inflador.inflate(R.layout.frag_servicio_medio, madre, false);
        ruedaCentro = vistaPrincipal.findViewById(R.id.ruedaCentro);
        txtCentro = vistaPrincipal.findViewById(R.id.txtCentro);
        btnCentro = vistaPrincipal.findViewById(R.id.btnCentro);
        //tip = vistaPrincipal.findViewById(R.id.tip_lista);
        TextView titulo = vistaPrincipal.findViewById(R.id.tituloPrincipal);
        Util.cambiarFuente(contexto, contexto.getString(R.string.fuente_regular), titulo);
        
        // (!) Debe estar aca (por si se cambia de tema)
        miLista = new ListaMedio(getActivity(), items, 2, this);
        lista = vistaPrincipal.findViewById(R.id.listaMedio);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            lista.setBackground(ContextCompat.getDrawable(contexto, R.drawable.fondo_lista));
            lista.setClipToOutline(true);
        }
        lista.setAdapter(miLista);
        
        // Botón Flotante Play/Stop
        btnPlayStop = vistaPrincipal.findViewById(R.id.btnPlayStop);
        btnPlayStop.setOnClickListener(this);
        boolean mostrarFab = (savedInstanceState != null && savedInstanceState.getBoolean(Util.PREF_MOSTRAR_CONTROL_FLOTANTE_MEDIO));
        btnPlayStop.setVisibility((mostrarFab) ? View.VISIBLE : View.INVISIBLE);
        
        // urlScheme
        if (getArguments() != null && getArguments().getParcelable("url_scheme") != null)
            urlScheme = getArguments().getParcelable("url_scheme");
        
        return super.onCreateView(inflador, madre, savedInstanceState);
    }
    
    @Override
    public void onSaveInstanceState(@NonNull final Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(Util.PREF_MOSTRAR_CONTROL_FLOTANTE_MEDIO, (btnPlayStop != null && btnPlayStop.getVisibility() == View.VISIBLE));
    }
}
