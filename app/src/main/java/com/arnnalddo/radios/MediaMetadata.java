package com.arnnalddo.radios;

import android.content.Context;
import android.os.Handler;

import androidx.annotation.NonNull;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.koushikdutta.ion.Ion;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;


public class MediaMetadata {
    //
    // INTERFACES
    //**********************************************************************************************
    interface Posllamada {
        void ponerMetadatos();
        void quitarMetadatos();
        void error();
    }
    //
    // PROPIEDADES
    //**********************************************************************************************
    // Lo que no será creado para subclases:
    private static final String separador = " - ";// esto es lo que separa a los metadatos iniciales
    private static String token = "";// para almacenar el token de Spotify obtenido
    private static Long validezToken = 0L;// para almacenar la validez del token de Spotify obtenido
    // Lo que será creado para subclases y no va a cambiar:
    private final Context contexto;// el contexto donde se mostrarán los metadatos
    private Posllamada posllamada;// para notificar al contexto cuando actualizar la IU
    private String url;// url de los metadatos principales
    private boolean desdeSpotify;// para saber si cargar metadatos desde Spotify también
    // Lo que va a cambiar y es privado
    private String raw, nombreCancion, nombreArtista, nombreAlbum, urlImagenAlbum;// principales
    protected final ScheduledExecutorService programadorDeTarea = Executors.newSingleThreadScheduledExecutor();// programador para actualizar los metadatos
    protected ScheduledFuture<?> tareaProgramada;// tarea que será ejecutada en un futuro
    //
    // CONSTRUCTORES
    //**********************************************************************************************
    protected MediaMetadata(@NonNull Context contexto) {
        this.contexto = contexto;
        cargarToken();
    }
    
    protected MediaMetadata(@NonNull Context contexto, @NonNull Posllamada posllamada, String urlMetadatosPrincipales, boolean desdeSpotify) {
        this.contexto = contexto;
        this.posllamada = posllamada;
        this.url = urlMetadatosPrincipales;
        this.desdeSpotify = desdeSpotify;
        // Cargar token apenas se instancia con desdeSpotify = true
        if (desdeSpotify) {
            cargarToken();
        }
    }
    //
    // MÉTODOS
    //**********************************************************************************************
    protected void programarActualizacion() {
        if (this.url == null) {
            System.out.println("No se ha proporcionado una URL de metadatos principales.");
            return;
        }
        cancelarActualizacion(false);
        if (!programadorDeTarea.isShutdown()) {
            System.out.println("Programando actualización automática de Metadatos...");
            Handler controladorMetadatos = new Handler();
            tareaProgramada = programadorDeTarea.scheduleAtFixedRate(() -> controladorMetadatos.post(() -> cargarMetadatos(this.url)),
                    100,
                    15000,
                    TimeUnit.MILLISECONDS);
        }
    }
    
    protected void cancelarActualizacion(boolean restablecerTodo) {
        if (tareaProgramada != null && !tareaProgramada.isCancelled()) {
            System.out.println("Cancelando actualización automática de Metadatos...");
            tareaProgramada.cancel(false);
            if (restablecerTodo)
                this.restablecer();
        }
    }
    
    protected void cargarMetadatos(@NonNull String urlMetadatosPrincipales) {
        // Cargo los metadatos desde la URL
        Ion.with(this.contexto)
                .load(urlMetadatosPrincipales)
                .noCache()
                .asString()
                .setCallback((error, metadatos) -> {
                    if (error != null) {
                        this.raw = "";
                        this.posllamada.error();
                        if (hayMetadatos())
                            this.restablecer();
                        error.printStackTrace();
                        return;
                    }
                    /* -----------------------------------------------------------------------------
                     * Si solo se necesita la información obtenida desde la URL:
                     * ---------------------------------------------------------------------------*/
                    if (!desdeSpotify) {
                        this.raw = metadatos;
                        return;
                    }
                    /* -----------------------------------------------------------------------------
                     * Para buscar todos los metadatos, se asume que se ha provehído la URL de un
                     * archivo que contiene (en formato texto simple) los metadatos principales con
                     * la siguiente estructura: nombre del artista - nombre de la canción.
                     *
                     * Puesto que puede tratarse de una URL de SHOUTcast, se hace lo que sigue:
                     * La información del archivo que se está reproduciendo, puede contener primero
                     * el nombre del artista y luego el nombre de la canción y viceversa. Así:
                     *      Charlie Cunningham - Minimun    o    Minimun - Charlie Cunningham
                     * (Según las pruebas hechas, puede cambiar después de iniciar la reproducción).
                     * Entonces, me aseguro de separar esos dos metadatos y luego verificar si es la
                     * misma canción la que se está reproduciendo, para no hacer una nueva búsqueda
                     * en la base de datos de Spotify (que es de donde obtengo los demás metadatos).
                     * ---------------------------------------------------------------------------*/
                    String[] cancionActual = metadatos.split(separador);// metadatos separados
                    if (cancionActual.length == 2
                                && !((cancionActual[0] + separador + cancionActual[1]).equals(this.raw))
                                && !((cancionActual[1] + separador + cancionActual[0]).equals(this.raw))) {
                        System.out.println("Nuevos metadatos: [" + metadatos + "]");
                        // Guardo la información para usarla después en la comparación de arriba
                        this.raw = metadatos;
                        // Y busco todos los demás metadatos en la base de datos de Spotify
                        this.cargarMetadatos(cancionActual);
                    }
                    if (cancionActual.length != 2 && hayMetadatos()) {
                        System.out.println("Sin metadatos nuevos, quito los metadatos anteriores...");
                        this.posllamada.quitarMetadatos();// debe estar antes de restablecer las variables
                        this.restablecer();
                    }
                });// fin ion cancion
    }
    
    protected void cargarMetadatos(@NonNull String[] metadatosPrincipales) {
        // Me aseguro de que la matriz metadatosPrincipales↗ tenga dos datos
        // (que deben ser: el nombre del artista y el de la canción)
        if (metadatosPrincipales.length != 2)
            return;
        System.out.println("Buscando en Spotify...");
        // Banderichis para los metadatos principales obtenidos
        String mdArtista, mdCancion;
        // Banderichis para los metadatos de Spotify
        JsonObject items, album;
        JsonArray artists;
        StringBuilder artistas = new StringBuilder();
        // Banderichi para saber si se obtuvieron o no los datos de Spotify
        boolean spotifyOK = false;
        // Por si se obtuvieron los datos en posiciones diferentes,
        // hago dos búsquedas (solo si la primera falla).
        for (int i = 0; (i < metadatosPrincipales.length) && (metadatosPrincipales[i] != null); i++) {
            mdArtista = metadatosPrincipales[i].trim();
            mdCancion = metadatosPrincipales[1 - i].trim();
            try {
                mdArtista = URLEncoder.encode(mdArtista, "utf-8");
                mdCancion = URLEncoder.encode(mdCancion, "utf-8");
                // Inicio la búsqueda --------------------------------------------------------------
                items = Ion.with(this.contexto)
                                .load(this.contexto.getString(R.string.appURISpotifySearch) + "?q=track:" + mdCancion + "+artist:" + mdArtista + "&type=track,artist&limit=1&market=ES")
                                .addHeader("Accept", "application/json")
                                .addHeader("Content-Type", "application/json")
                                .addHeader("Authorization", "Bearer " + token)
                                .noCache()
                                .asJsonObject().get()
                                .get("tracks").getAsJsonObject()
                                .get("items").getAsJsonArray().get(0)
                                .getAsJsonObject();
                // Nombre del Artista/Canción ------------------------------------------------------
                artists = items.get("artists").getAsJsonArray();
                for (int in = 0; (in < artists.size() && artists.get(in) != null); in++) {
                    
                    artistas.append(artists.get(in).getAsJsonObject().get("name").getAsString());
                    if ((artists.size() != 1) && (in + 1 != artists.size()))
                        artistas.append(", ");
                    
                }// fin for
                this.nombreArtista = artistas.toString();
                this.nombreCancion = items.get("name")
                                             .getAsString();
                // Álbum ---------------------------------------------------------------------------
                album = items.get("album").getAsJsonObject();
                this.nombreAlbum = album.get("name").getAsString();
                this.urlImagenAlbum = album.get("images").getAsJsonArray().get(0).getAsJsonObject()
                                              .get("url").getAsString();
                // Fin de la búsqueda --------------------------------------------------------------
                System.out.println("Spotify: ¡Metadatos encontrados!");
                System.out.println("----------------------------------------------------\nArtista: "
                                           + this.nombreArtista + "\nCanción: "
                                           + this.nombreCancion + "\nÁlbum: "
                                           + this.nombreAlbum + "\nImagen del álbum: "
                                           + this.urlImagenAlbum
                                           + "\n----------------------------------------------------");
                spotifyOK = true;
                break;
            } catch (Exception e) {
                if (e instanceof UnsupportedEncodingException) {
                    System.out.println("La codificación de los metadatos principales no es compatible.");
                } else {
                    System.out.println((i == 0) ? "Spotify: No se encontraron coincidencias en la primera búsqueda.\nSpotify: Buscando una vez más..." :
                                               (e instanceof IndexOutOfBoundsException) ? "Spotify: ¡Ay! No hay datos /:" : e.getLocalizedMessage());
                }
            }
        }
        
        if (spotifyOK)
            this.posllamada.ponerMetadatos();
        else {
            this.posllamada.error();
            if (hayMetadatos())
                this.restablecer();
        }
    }
    
    protected boolean hayMetadatos() {
        return this.nombreArtista != null && this.nombreCancion != null;
    }
    
    protected String traerRaw() {
        return this.raw;
    }
    
    protected String traerArtista() {
        return this.nombreArtista;
    }
    
    protected String traerCancion() {
        return this.nombreCancion;
    }
    
    protected String traerAlbum() {
        return this.nombreAlbum;
    }
    
    protected String traerURLImagen() {
        return this.urlImagenAlbum;
    }
    
    private void restablecer() {
        this.nombreArtista = null;
        this.nombreCancion = null;
        this.nombreAlbum = null;
        this.urlImagenAlbum = null;
    }
    
    private Timer programadorToken = null;
    private void cargarToken() {
        // No cargar si ya se obtuvo y no caducó
        if (validezToken > System.currentTimeMillis())
            return;
        
        System.out.println("Spotify: Intento obtener el token...");
        Ion.with(this.contexto)
                .load(this.contexto.getString(R.string.appURISpotifyToken))
                .noCache()
                .asJsonObject()
                .setCallback((errorDeToken, tokenInfo) -> {
                    if (errorDeToken != null) {
                        System.out.println("Spotify: no se pudo obtener el token.");
                        errorDeToken.printStackTrace();
                        return;
                    }
                    token = tokenInfo.get("token").getAsString();
                    validezToken = tokenInfo.get("validez").getAsLong() * 1000;// de segundos a ms
                    long tkn = (validezToken - System.currentTimeMillis()) / 60000;// temporal
                    System.out.println("Spotify: Token obtenido:");
                    System.out.println("----------------------------------------------------\n"
                                               + token
                                               + "\nExpira en "
                                               + tkn
                                               + " min." +
                                               "\n----------------------------------------------------");
                    // TODO: si no se obtuvo, nunca se va a cargar los metadatos
                    //  (mirar el return de arriba o la condición de abajo). Hay que reintentar
                    //  o notificar al usuario
                    // Programo una tarea para actualizar el token de forma automática,
                    // según su tiempo de validez
                    if (validezToken != null) {
                        // Si ya existe un programador activo, lo cancelo para iniciar uno nuevo
                        if (programadorToken != null)
                            programadorToken.cancel();
                        programadorToken = new Timer();
                        try {
                            programadorToken.schedule(new ProgramadorToken(), validezToken);
                            System.out.println("Tarea programada: "
                                                       + "se intentará obtener un nuevo token en "
                                                       + tkn
                                                       + " min.");
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    
                });
    }
    
    private class ProgramadorToken extends TimerTask {
        Handler handler = new Handler();
        @Override
        public void run() {
            handler.post(MediaMetadata.this::cargarToken);
        }
    }
}
