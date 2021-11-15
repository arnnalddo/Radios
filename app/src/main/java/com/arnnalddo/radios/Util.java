package com.arnnalddo.radios;

import android.annotation.TargetApi;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.text.Spannable;
import android.text.SpannableString;
import android.util.Patterns;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.os.ConfigurationCompat;

import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;


/**
 * Created by arnaldito100 on 20/04/2016.
 * Copyright © 2016 Arnaldo Alfredo. All rights reserved.
 * http://www.arnnalddo.com
 * Métodos y otras cosas
 */
public class Util {
    
    public static final String PREF_ULTIMO_MEDIO = BuildConfig.APPLICATION_ID + ".ULTIMO_MEDIO";
    public static final String PREF_MOSTRAR_CONTROL_FLOTANTE_MEDIO = BuildConfig.APPLICATION_ID + ".MOSTRAR_FAB";
    
    enum EstadoMP {
        iniciando,
        conectando,
        almacenando,
        reproduciendo,
        pausado,
        detenido,
        finalizado,
        error
    }
    
    enum FormatoTexto {
        correo,
        tel,
        url
    }
    
    // Constantes publicas
    
    /**
     * A continuacion, algunos valores por defecto
     * */
    
    // REPRODUCCION AUTOMATICA DEL STREAM PRINCIPAL
    static final boolean AUTOPLAY = false;
    
    // DETECTAR AURICULARES
    static final boolean DETENER_AURICULAR = true;
    
    // MOSTRAR TIP SOBRE LISTA
    static final boolean MOSTRAR_TIP_LISTA = true;
    
    
    static boolean ACTI_PR_RECIEN_CREADA = false;
    
    static String operadora = "otra";
    static String regionUsuario = "italia";
    
    
    /**
     * Preferencias guardadas en el dispositivo
     */
    private static SharedPreferences CONFIGURACION_APP;
    
    // Obtener valor en formato de cadena (String)
    static String obtPreferencia(Context contexto, String nombre, String valorPorDefecto) {
        CONFIGURACION_APP = contexto.getSharedPreferences(BuildConfig.APPLICATION_ID + ".INFO", Context.MODE_PRIVATE);
        return CONFIGURACION_APP.getString(nombre, valorPorDefecto);
    }
    
    // Obtener valor en formato de cadena (Long)
    static Long obtPreferencia(Context contexto, String nombre, long valorPorDefecto) {
        CONFIGURACION_APP = contexto.getSharedPreferences(BuildConfig.APPLICATION_ID + ".INFO", Context.MODE_PRIVATE);
        return CONFIGURACION_APP.getLong(nombre, valorPorDefecto);
    }
    
    // Obtener valor en formato booleano (Boolean)
    @NonNull
    static Boolean obtPreferencia(Context contexto, String nombre, boolean valorPorDefecto) {
        CONFIGURACION_APP = contexto.getSharedPreferences(BuildConfig.APPLICATION_ID + ".INFO", Context.MODE_PRIVATE);
        return CONFIGURACION_APP.getBoolean(nombre, valorPorDefecto);
    }
    
    // Obtener valor en formato StringSet
    static String[] obtPreferencia(Context contexto, String nombre) {
        
        CONFIGURACION_APP = contexto.getSharedPreferences(BuildConfig.APPLICATION_ID + ".INFO", Context.MODE_PRIVATE);
        
        Set<String> set = CONFIGURACION_APP.getStringSet(nombre, new HashSet<>());
        String[] array = (set.size() > 0) ? set.toArray(new String[set.size()]) : null;
        
        if (array == null) {
            return new String[0];
        }
        
        java.util.Arrays.sort(array);
        
        return array;
        
    }
    
    
    // Esditar preferencia en formato de cadena (String)
    static void editarPreferencia(Context contexto, String nombre, String valor) {
        CONFIGURACION_APP = contexto.getSharedPreferences(BuildConfig.APPLICATION_ID + ".INFO", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = CONFIGURACION_APP.edit();
        editor.putString(nombre, valor);
        editor.apply();
    }
    
    // Esditar preferencia en formato booleano (Boolean)
    static void editarPreferencia(Context contexto, String nombre, long valor) {
        CONFIGURACION_APP = contexto.getSharedPreferences(BuildConfig.APPLICATION_ID + ".INFO", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = CONFIGURACION_APP.edit();
        editor.putLong(nombre, valor);
        editor.apply();
    }
    
    // Esditar preferencia en formato booleano (Boolean)
    static void editarPreferencia(Context contexto, String nombre, boolean valor) {
        CONFIGURACION_APP = contexto.getSharedPreferences(BuildConfig.APPLICATION_ID + ".INFO", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = CONFIGURACION_APP.edit();
        editor.putBoolean(nombre, valor);
        editor.apply();
    }
    
    // Esditar preferencia en formato booleano (StringSet)
    static void editarPreferencia(Context contexto, String nombre, Set<String> valor) {
        CONFIGURACION_APP = contexto.getSharedPreferences(BuildConfig.APPLICATION_ID + ".INFO", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = CONFIGURACION_APP.edit();
        editor.putStringSet(nombre, valor);
        editor.apply();
    }
    
    
    /**
     * Intentar cambiar la fuente de la aplicacion
     */
    static void cambiarFuente(Context contexto, String fuente, TextView textView) {
        
        final Typeface miFuente = getTF(contexto, fuente);
        
        if (miFuente != null) {
            try {
                textView.setTypeface(miFuente);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        
    }
    
    static void cambiarFuente(Context contexto, String fuente, EditText editText) {
        
        final Typeface miFuente = getTF(contexto, fuente);
        
        if (miFuente != null) {
            try {
                editText.setTypeface(miFuente);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        
    }
    
    
    static void cambiarFuente(Context contexto, String fuente, Button boton) {
        
        final Typeface miFuente = getTF(contexto, fuente);
        
        if (miFuente != null) {
            try {
                boton.setTypeface(miFuente);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        
    }
    
    
    static void cambiarFuente(Context contexto, String fuente, Menu menu) {
        
        final Typeface miFuente = getTF(contexto, fuente);
        
        for (int i = 0; i < menu.size(); i++) {
            
            MenuItem mi = menu.getItem(i);
            SubMenu subMenu = mi.getSubMenu();
            
            if (subMenu != null && subMenu.size() > 0) {
                
                for (int j = 0; j < subMenu.size(); j++) {
                    
                    MenuItem subMenuItem = subMenu.getItem(j);
                    cambiarFuente(miFuente, subMenuItem);
                    
                }
                
            }
            
            cambiarFuente(miFuente, mi);
            
        }
        
    }
    
    
    static void cambiarFuente(Typeface fuente, MenuItem mi) {
        
        SpannableString mNewTitle = new SpannableString(mi.getTitle());
        mNewTitle.setSpan(new CustomTypefaceSpan("", fuente), 0, mNewTitle.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);
        mi.setTitle(mNewTitle);
        
    }
    
    
    static CharSequence cambiarFuente(@NonNull Context contexto, @NonNull String fuente, @NonNull String texto) {
        // Obtengo la fuente
        final Typeface miFuente = getTF(contexto, fuente);
        // y cambio la fuente del texto
        SpannableString mNewTitle = new SpannableString(texto);
        mNewTitle.setSpan(new CustomTypefaceSpan("", miFuente), 0, mNewTitle.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);
        return mNewTitle;
    }
    
    private static final Hashtable<String, Typeface> cache = new Hashtable<>();
    
    @Nullable
    private static Typeface getTF(Context c, String assetPath) {
        synchronized (cache) {
            if (!cache.containsKey(assetPath)) {
                try {
                    Typeface t = Typeface.createFromAsset(c.getAssets(), assetPath);
                    cache.put(assetPath, t);
                } catch (Exception e) {
                    return null;
                }
            }
            return cache.get(assetPath);
        }
    }
    
    
    /**
     * Verificar Formato de cadena
     */
    static boolean verificarFormato(String cadena, FormatoTexto tipo) {
        
        if (cadena == null || cadena.isEmpty()) {
            return false;
        }
        
        switch (tipo) {
            case correo:
                return Patterns.EMAIL_ADDRESS.matcher(cadena).matches();
            case tel:
                return Patterns.PHONE.matcher(cadena).matches();
            case url:
            default:
                return Patterns.WEB_URL.matcher(cadena).matches();
        }
        
    }
    
    
    /**
     * (Beta) Obtener cadena o concatenacion de cadenas (principalmente para mi alerta)
     */
    @Nullable
    static String obtenerCadena(Context contexto, int codigo) {
        switch (codigo) {
            case 1:
                return contexto.getString(R.string.error_carga_contenido);
            case 2:
                return contexto.getString(R.string.error_conexion);
            case 3:
                return contexto.getString(R.string.error_red_no_disponible);
            case 4:
                return contexto.getString(R.string.error_verificar_conexion);
            case 5:
                return contexto.getString(R.string.error_json_txt);
            case 6:
                return contexto.getString(R.string.error_m1);
            case 7:
                return contexto.getString(R.string.error_m2);
            case 8:
                return contexto.getString(R.string.error_m3);
            case 9:
                return contexto.getString(R.string.error_m4);
            case 10:
                return contexto.getString(R.string.error_carga_contenido) + ". " + contexto.getString(R.string.error_verificar_conexion);
            case 11:
                return contexto.getString(R.string.error_red_no_disponible) + " " + contexto.getString(R.string.error_verificar_conexion);
            case 12:
                return contexto.getString(R.string.mensaje_noenviado) + " " + contexto.getString(R.string.error_verificar_conexion);
            case 13:
                return contexto.getString(R.string.enviando_mensaje);
            case 14:
                return contexto.getString(R.string.mensaje_enviado);
            case 15:
                return contexto.getString(R.string.mensaje_noenviado);
            case 16:
                return contexto.getString(R.string.licencia_txt);
            case 17:
                return "ERROR_VALOR_MEDIO";
            case 18:
                return "ERROR_URL";
            case 19:
                return "ERROR_FORMATO_JSON";
            case 20:
                return contexto.getString(R.string.error);
            case 21:
                return contexto.getString(R.string.politica_txt);
            default:
                return null;
        }
    }
    
    
    static boolean iniActividad(Context contexto, Intent intent, Boolean mostrarMensajes) {
        try {
            contexto.startActivity(intent);
            return true;
        } catch (ActivityNotFoundException e) {
            if (mostrarMensajes) {
                Toast.makeText(contexto, contexto.getString(R.string.error), Toast.LENGTH_SHORT).show();
            }
            return false;
        }
    }
    
    
    static void irAGPlay(Context contexto) {
        
        // Intento abrir en la app de la tienda
        Intent verGPlay = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + BuildConfig.APPLICATION_ID));
        
        if (!iniActividad(contexto, verGPlay, false)) {
            
            // Si la app de la tienda no esta instalada, entonces intento abrir en el navegador
            verGPlay = new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + BuildConfig.APPLICATION_ID));
            iniActividad(contexto, verGPlay, true);
            
        }
        
    }
    
    @TargetApi(value = Build.VERSION_CODES.LOLLIPOP)
    static boolean enModoAhorro(@NonNull Context contexto) {
        PowerManager pm = (PowerManager) contexto.getSystemService(Context.POWER_SERVICE);
        return (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && pm != null && pm.isPowerSaveMode());
    }
    
    public static void iniciarMediaService(@NonNull Context contexto, @NonNull String urlMedio, @NonNull String nombreMedio, String detalleMedio, String urlImagenMedio, String urlMetadatos, Boolean enVivo, Boolean reproducir) {
        // Creo una nueva Intent para iniciar MediaService
        Intent iPlay = new Intent(contexto, MediaService.class);
        // Pongo los dos datos obligatorios en la intent:
        iPlay.putExtra(MediaService.MEDIO_URL, urlMedio);
        iPlay.putExtra(MediaService.MEDIO_NOMBRE, nombreMedio);
        // Pongo el detalle en la intent (si existe)
        if (detalleMedio != null)
            iPlay.putExtra(MediaService.MEDIO_DETALLE, detalleMedio);
        // Pongo la url de la imagen en la intent (si existe)
        if (urlImagenMedio != null)
            iPlay.putExtra(MediaService.MEDIO_URL_IMAGEN, urlImagenMedio);
        // Pongo la url de donde se obtienen los metadatos de la canción que se escucha en la intent (si existe)
        if (urlMetadatos != null)
            iPlay.putExtra(MediaService.MEDIO_URL_METADATOS, urlMetadatos);
        // Pongo un booleano en la intent, para saber si es en vivo o no
        iPlay.putExtra(MediaService.EN_VIVO, enVivo);
        // Pongo un booleano en la intent, para saber si se va a reproducir automáticamente o no
        iPlay.setAction((reproducir) ? MediaService.ACCION_PLAY : MediaService.ACCION_PREPARAR);
        // Y finalmente, inicio el servicio
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            contexto.startForegroundService(iPlay);
        else
            contexto.startService(iPlay);
    }
    
    
    /**
     * Método para obtener la URL de transmisión, según la operadora
     * Debe llamarse cada vez que se intenta reproducir algo,
     * ya que el ISP puede cambiar en cualquier momento
     */
    static String obtenerURLMedio(String medioURLGlobal, String medioURLTigo, String medioURLCopaco) {
        return (operadora.equals("tigo")) ? medioURLTigo : (operadora.equals("copaco")) ? medioURLCopaco : medioURLGlobal;
    }
    
    
    static String traerIdioma() {
        return ConfigurationCompat.getLocales(Resources.getSystem().getConfiguration()).get(0).toString();
    }
    
}
