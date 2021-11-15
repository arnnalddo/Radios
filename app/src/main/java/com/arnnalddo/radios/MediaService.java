package com.arnnalddo.radios;

import com.arnnalddo.radios.Util.EstadoMP;

import android.annotation.TargetApi;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.view.View;
import android.widget.RemoteViews;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.media.app.NotificationCompat.MediaStyle;

import com.koushikdutta.async.future.Future;
import com.koushikdutta.ion.Ion;

import java.util.Timer;
import java.util.TimerTask;


/**
 * Created by arnaldito100 on 03/08/2013.
 * Copyright © 2013 Arnaldo Alfredo. All rights reserved.
 * http://www.arnnalddo.com
 * Servicio principal
 */
public class MediaService extends Service implements MediaMetadata.Posllamada, AudioManager.OnAudioFocusChangeListener, MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener, MediaPlayer.OnCompletionListener {
    //*****************************************************************
    // PROPIEDADES
    //*****************************************************************
    // (!) Cosas que no deben cambiar (para intents explícitas)
    public static final String ACCION_PREPARAR = BuildConfig.APPLICATION_ID + ".accionPreparar";
    public static final String ACCION_PLAY = BuildConfig.APPLICATION_ID + ".accionPlay";
    public static final String ACCION_PAUSE = BuildConfig.APPLICATION_ID + ".accionPause";
    public static final String ACCION_STOP = BuildConfig.APPLICATION_ID + ".accionStop";
    public static final String ACCION_PLAY_STOP = BuildConfig.APPLICATION_ID + ".accionPlayStop";
    public static final String ACCION_CERRAR = BuildConfig.APPLICATION_ID + ".accionCerrar";
    public static final String ACCION_AUTO_STOP = BuildConfig.APPLICATION_ID + ".accionAutoStop";
    public static final String ACCION_NOTIFICAR = BuildConfig.APPLICATION_ID + ".accionNotificar";
    public static final String MEDIO_URL = BuildConfig.APPLICATION_ID + ".streamURL";
    public static final String MEDIO_NOMBRE = BuildConfig.APPLICATION_ID + ".titulo";
    public static final String MEDIO_DETALLE = BuildConfig.APPLICATION_ID + ".subtitulo";
    public static final String MEDIO_URL_IMAGEN = BuildConfig.APPLICATION_ID + ".imagenURL";
    public static final String MEDIO_URL_METADATOS = BuildConfig.APPLICATION_ID + ".metadatosURL";
    public static final String EN_VIVO = BuildConfig.APPLICATION_ID + ".enVivo";
    public static final String TEMPORIZADOR = BuildConfig.APPLICATION_ID + ".temporizador";
    // Cosas que no van a cambiar (privadas)
    private static final String ETIQUETA = "MediaService";
    private static final int idNotifiRepro = 1;// identificador único para la notificación principal (el que muestra el reproductor y los metadatos)
    private static final int idNotifiModoAhorro = 2;// identificador único para la notificación en Modo Ahorro
    private static final long tiempoMaximoDeReproduccion = 7200000;// 2h para que el dispositivo siga «despierto» (después de empezar a escuchar)
    private static final long tiempoDeAutodestruccion = 120000; // 2min hasta destruir el servicio (después de pausar/detener)
    private static final boolean usarVistaPersonalizada = true;// (beta) para usar o no el disenio de notificación personalizado
    //private static final Object peticionesMetadatos = new Object();
    // Cosas que sí van cambiando
    private MediaMetadata mediaMetadata;
    private AudioManager audioManager;// administrador de audio (dispositivo)
    private AudioAttributes audioAttributes;// para poner atributos del audio
    private AudioFocusRequest audioFocusRequest;// para manejar el enfoque de audio
    private MediaCallback mediaCallback;// posllamada (para actualizar la UI en otras clases)
    private MediaSessionCompat mediaSession;// para permitir controlar la reproducción desde otros dispositivos (auriculares, autos, etc.)
    private MediaMetadataCompat.Builder mediaMetadataBuilder;// para actualizar los metadatos
    private PlaybackStateCompat.Builder playbackStateBuilder;
    private PowerManager.WakeLock wakeLock;// para impedir que el dispositivo «duerma» durante la reproducción Zzzz
    private WifiManager.WifiLock wifiLock;// para impedir que el dispositivo se desconecte de la red wifi durante la reproducción
    private NotificationManager notificationManager;// administrador de notificaciones (dispositivo)
    private NotificationCompat.Builder notificationBuilder;// para mostrar notificaciones al usuario en el área de notificaciones
    private PendingIntent piNotificacion, piPlayStopRepro, piCerrar;
    private RemoteViews vistaNotificacionPrincipal, vistaNotificacionPrincipalGrande;// vistas principales para la notificación personalizada
    // Otras
    public static EstadoMP estadoMP = EstadoMP.detenido;
    private static MediaPlayer mediaPlayer;
    private static String medioURL;
    private static String medioNombre;
    private static String medioDetalle;
    private static String medioURLMetadatos;
    private static String urlMedioAnterior;
    private static Bitmap medioLogo;
    private static int focoAudio;
    private static boolean notiVisible = false, enPrimerPlano = false;
    private static boolean esEnVivo = true;
    private Timer temporizadorNoti = null;
    private Timer temporizadorAS = null;
    private final IBinder mBinder = new LocalBinder();
    
    private void manejarIntent(Intent intent) {
        // Si no hay intent o acción, salgo de acá
        if (intent == null || intent.getAction() == null)
            return;
        // Obtengo la acción y la realizo según cada tipo
        switch (intent.getAction()) {
            case ACCION_PREPARAR:
                if (esEnVivo && mediaCallback != null)
                    mediaCallback.inicializar((intent.getBooleanExtra("ocultar", false)));
                
                // Desde Android O, acá hay que llamar a startForeground()
                // [que está en mostrarNotificacion()]
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    notificar(estadoMP);
                }
                
                break;
            case ACCION_PLAY:
                reproducir(true);
                break;
            case ACCION_PAUSE:
            case ACCION_STOP:
                detener();
                break;
            case ACCION_PLAY_STOP:
                if (reproduciendo())
                    detener();
                else// inicializar reproductor solo si no está en estado pausado
                    reproducir(estadoMP != EstadoMP.pausado);
                break;
            case ACCION_AUTO_STOP:// para programar detención automática
                long tiempo = intent.getLongExtra(TEMPORIZADOR, 0);
                detenerDespues(tiempo);
                break;
            case ACCION_CERRAR:// para cerrar la aplicación
                if (mediaCallback != null)
                    mediaCallback.cerrar();
                break;
            case ACCION_NOTIFICAR:
                if (notiVisible)
                    notificar(estadoMP);
            case Intent.ACTION_HEADSET_PLUG:// para cuando se conectan los auriculares
                int estado = intent.getIntExtra("state", -1);
                if (Util.obtPreferencia(MediaService.this, "DETENER_AURICULAR", Util.DETENER_AURICULAR) && estado == 0 && reproduciendo())
                    detener();
                break;
            case "android.os.action.POWER_SAVE_MODE_CHANGED":// para cuando el usuario cambia el modo ahorro
                notificarSobreModoAhorro(!Util.enModoAhorro(MediaService.this));
            default:
                break;
        }
    }
    
    private void manejarEnfoque(boolean abandonarEnfoque) {
        if (abandonarEnfoque) {
            if (focoAudio == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                System.out.println("Abandonando enfoque de audio...");
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    audioManager.abandonAudioFocusRequest(audioFocusRequest);
                else
                    audioManager.abandonAudioFocus(this);
                focoAudio = AudioManager.AUDIOFOCUS_LOSS;
            }
        } else if (focoAudio != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (audioFocusRequest == null) {
                    audioFocusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN).setAudioAttributes(audioAttributes)
                                                //.setAcceptsDelayedFocusGain(true)
                                                .setOnAudioFocusChangeListener(this).build();
                }
                focoAudio = audioManager.requestAudioFocus(audioFocusRequest);
            } else {
                focoAudio = audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
            }
            
            if (focoAudio == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                System.out.println("Enfoque de audio obtenido (:");
            } else {
                System.out.println("No se pudo obtener el enfoque de audio /:");
            }
        }
    }
    
    private void reproducir(boolean inicializar) {
        
        if (inicializar) {
            // Cancelo el temporizador de la notificación
            cancelarTempNoti();
            // Instancio MediaPlayer solo si es nulo
            if (mediaPlayer == null) {
                mediaPlayer = new MediaPlayer();
                mediaPlayer.setOnPreparedListener(this);
                mediaPlayer.setOnErrorListener(this);
                mediaPlayer.setOnCompletionListener(this);
                // Poner los atributos del audio (indicar al sistema que se trata de música)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {// para Android 5 o posterior
                    if (audioAttributes == null) {
                        audioAttributes = new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build();
                        mediaPlayer.setAudioAttributes(audioAttributes);
                    }
                } else// para versiones anteriores
                    mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            } else// Caso contrario, lo restablezco
                mediaPlayer.reset();
            // TODO: de acá se eliminó lo del candy
            try {
                System.out.println("Inicializando con: " + medioURL);
                // Se inicializa MediaPlayer al usar 'setDataSource'
                mediaPlayer.setDataSource(medioURL);// (!) debe estar en try, con catch IllegalArgumentException e IOException
            } catch (Exception e) {
                if (mediaCallback != null)
                    mediaCallback.error();
                e.printStackTrace();
                detener();
                return;
            }
            
            mediaPlayer.prepareAsync();// llamar después de otras configuraciones de MediaPlayer
            manejarEnfoque(false);
            notificar(EstadoMP.conectando);
            
            // Detener tarea para actualizar los metadatos y restablecer las variables
            if (urlMedioAnterior != null && !urlMedioAnterior.equals(medioURL)) {
                if (this.mediaMetadata != null)
                    this.mediaMetadata.cancelarActualizacion(true);// debe estar acá
            }
    
            // Instanciar MediaMetadata y cargar un nuevo token si necesario
            this.mediaMetadata = new MediaMetadata(this, this, medioURLMetadatos, true);
            
        } else if (mediaPlayer != null) {
            
            if (wifiLock != null && !wifiLock.isHeld()) {
                System.out.println("WifiLock.acquire()");
                wifiLock.acquire();
            }
            if (wakeLock != null && !wakeLock.isHeld()) {
                System.out.println("WakeLock.acquire()");
                wakeLock.acquire(tiempoMaximoDeReproduccion);
            }
            
            notificar(EstadoMP.reproduciendo);
            
            // Programar la tarea para actualizar los metadatos
            // de forma automática (debe estar después de mostrarNotificacion())
            if (this.mediaMetadata != null)
                this.mediaMetadata.programarActualizacion();
            
            // Debe estar después del resto
            urlMedioAnterior = medioURL;
    
            System.out.println("Reproduciendo");
            mediaPlayer.start();
        }
        // Mostrar notificacion al usuario si activó el modo ahorro de batería
        if (Util.enModoAhorro(this))
            notificarSobreModoAhorro(false);
    }
    
    private void detener() {
        if (esEnVivo || estadoMP == EstadoMP.error || estadoMP == EstadoMP.conectando || estadoMP == EstadoMP.almacenando/*(mediaPlayer != null && !mediaPlayer.isPlaying())*/) {
            System.out.println("Deteniendo...");
            if (mediaPlayer != null)
                mediaPlayer.reset();
            notificar(EstadoMP.detenido);
        } else {
            System.out.println("Pausando...");
            if (mediaPlayer != null)
                mediaPlayer.pause();
            notificar(EstadoMP.pausado);
        }
        // detener la tarea que que actualiza los metadatos
        if (this.mediaMetadata != null)
            this.mediaMetadata.cancelarActualizacion(false);
        
        // Beta: si perdí el enfoque de audio por largo rato, durante la transmision en vivo, debo llamar a mediaPlayer.release()
        liberarCosas(focoAudio == AudioManager.AUDIOFOCUS_LOSS && esEnVivo);
        
        // Programo la cancelación o eliminación de la notificación principal
        borrarNotificacion(tiempoDeAutodestruccion);
        
        // Se habilita al usuario cerrar la app deslizando la notificacion
        stopForeground(false);// Debe estar después de borrarNotificacion()
        enPrimerPlano = false;
    }
    
    private void detenerDespues(final long tiempo) {
        // Si el tiempo es 0, me aseguro de
        // cancelar cualquier temporizador y salgo
        if (tiempo == 0) {
            cancelarDetencion();
            return;
        }
        // Si ya existe un temporizador activo,
        // lo cancelo para iniciar uno nuevo
        if (temporizadorAS != null)
            temporizadorAS.cancel();
        temporizadorAS = new Timer();
        // Intento programar uno nuevo
        try {
            temporizadorAS.schedule(new TemporizadorAS(), tiempo);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void cancelarDetencion() {
        // cancelar si ya existe
        if (temporizadorAS != null) {
            temporizadorAS.cancel();
            temporizadorAS = null;
            System.out.println("Temporizador cancelado.");
        }
        if (Util.obtPreferencia(MediaService.this, "AUTOSTOP", 0L) > 0)
            Util.editarPreferencia(MediaService.this, "AUTOSTOP", 0L);
    }
    
    Future<Bitmap> ionImagen;
    private void cargarImagen(@NonNull String url, boolean esLogo) {
        System.out.println("Cargando " + ((esLogo) ? "logo" : "imagen del álbum") + "...");
        ionImagen = Ion.with(this)
                            .load(url)
                            .asBitmap();
        ionImagen.setCallback((e, nuevaImagen) -> {
            if (e != null) {
                System.out.println("No se pudo cargar la imagen" + ((esLogo) ? ", pongo la predeterminada." : ". " + e.getLocalizedMessage()));
                nuevaImagen = esLogo || medioLogo == null ? BitmapFactory.decodeResource(getResources(), R.drawable.no_logo) : medioLogo;
            }
            // Actualizo la bandera del logo del medio si se trata de logo
            if (esLogo)
                medioLogo = nuevaImagen;
            // Actualizo la imagen de mediaSession
            if (mediaSession != null && mediaMetadataBuilder != null) {
                mediaMetadataBuilder.putBitmap(android.support.v4.media.MediaMetadataCompat.METADATA_KEY_ALBUM_ART, nuevaImagen);
                mediaSession.setMetadata(mediaMetadataBuilder.build());
            }
            // Actualizo la imagen de la notificación
            if (notificationBuilder != null) {
                if (usarVistaPersonalizada && vistaNotificacionPrincipal != null && vistaNotificacionPrincipalGrande != null) {
                    vistaNotificacionPrincipal.setImageViewBitmap(R.id.imagenNotificacion, nuevaImagen);
                    vistaNotificacionPrincipalGrande.setImageViewBitmap(R.id.imagenNotificacion, nuevaImagen);
                } else {
                    notificationBuilder.setLargeIcon(nuevaImagen);
                }
                notificationManager.notify(idNotifiRepro, notificationBuilder.build());
            }
            System.out.println("Listo (:");
        });// fin ion
    }
    
    private static RemoteViews[] vistas;
    private static String[] metodos;
    private static int[] idVistas;
    private MediaStyle mediaStyle;
    private void notificar(EstadoMP estado) {
        // Actualizo mi bandera
        estadoMP = estado;
        /* -----------------------------------------------------------------------------------------
         * Inicializo mediaSession si es necesario:
         * ---------------------------------------------------------------------------------------*/
        if (mediaSession == null) {
            ComponentName mediaButtonReceiver = new ComponentName(this, MediaReceiver.class);
            mediaSession = new MediaSessionCompat(this, ETIQUETA, mediaButtonReceiver, null);
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS | MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);
            }
            mediaSession.setCallback(mMediaSessionCallback);
            try {
                mediaSession.setActive(true);
            } catch (NullPointerException e) {
                // Algunas versiones de KitKat no soportan AudioManager.registerMediaButtonIntent
                // con un PendingIntent. Arrojaran "NullPointerException", en algunos casos
                // activaran "MediaSessionCompat" con solo "transport controls".
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                    mediaSession.setActive(false);
                    mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);
                    mediaSession.setActive(true);
                }
            }
        }
        /* -----------------------------------------------------------------------------------------
         * Construyo una nueva notificación si aún no fue creada:
         * ---------------------------------------------------------------------------------------*/
        if (notificationBuilder == null) {
            String idNotiMedio = BuildConfig.APPLICATION_ID + ".NotiMedio";
            notificationBuilder = new NotificationCompat.Builder(this, idNotiMedio);
            notificationBuilder.setSmallIcon(R.drawable.outline_radio_white_24);// ícono para la barra de estado (obligat orio)
            notificationBuilder.setShowWhen(false);
            notificationBuilder.setContentIntent(this.piNotificacion);
            notificationBuilder.setDeleteIntent(this.piCerrar);
            notificationBuilder.setPriority(NotificationCompat.PRIORITY_HIGH);
            // Para dispositivos con Android 5.0 (Lollipop) o posteriores:
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                notificationBuilder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
                // Para dispositivos con Android 7.0 (Nougat) o posteriores:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    notificationBuilder.setColor(ContextCompat.getColor(this, R.color.secondary));
                    
                    // Para dispositivos con Android 8.0 (Oreo) o posteriores:
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        CharSequence nombreCanal = getString(R.string.reproduccion);
                        NotificationChannel canalNoti = new NotificationChannel(idNotiMedio, nombreCanal, NotificationManager.IMPORTANCE_LOW);
                        canalNoti.setDescription(getString(R.string.des_noti_reproduccion));
                        notificationManager.createNotificationChannel(canalNoti);
                    }
                    
                } else // Para dispositivos con Android 5.0, 5.1 (Lollipop) y 6.0 (Marshmallow)
                    notificationBuilder.setColor(ContextCompat.getColor(this, R.color.secondaryVariant));
            }
        }
        /* -----------------------------------------------------------------------------------------
         * Preparo las vistas personalizadas si aún no fueron creadas y si se decidió mostrarlas,
         * que cambian según el tema de la aplicación. En caso de que se decida no usar el estilo
         * personalizado, entonces se configura el estilo predeterminado del SO con MediaStyle().
         * ---------------------------------------------------------------------------------------*/
        if (usarVistaPersonalizada) {
            
            if (vistaNotificacionPrincipal == null) {
                vistaNotificacionPrincipal = new RemoteViews(BuildConfig.APPLICATION_ID, R.layout.notificacion_principal);
                vistaNotificacionPrincipalGrande = new RemoteViews(BuildConfig.APPLICATION_ID, R.layout.notificacion_principal_grande);
                vistas = new RemoteViews[]{vistaNotificacionPrincipal, vistaNotificacionPrincipalGrande};
                metodos = new String[]{"setTextColor", "setTextColor", "setColorFilter", "setColorFilter", "setBackgroundColor", "setBackgroundColor"};
                idVistas = new int[]{R.id.tituloNotificacion, R.id.subtituloNotificacion, R.id.botonPlayNotificacion, R.id.botonCerrarNotificacion, R.id.fondoNotificacion, R.id.divisorNotificacion};
            }
            
            // Defino los colores que voy a usar, según el tema
            // Inicializar acá siempre, puesto que el tema puede cambiar en cualquier momento
            boolean modoOscuro = Util.obtPreferencia(this, "TEMA", false);
            int colorTitulo = modoOscuro ? 0xFFFFFFFF : 0xFF222222;
            int colorSubtitulo = modoOscuro ? 0xFFCCCCCC : 0xFF666666;
            int colorBotonPlay = modoOscuro ? 0xFFFFFFFF : 0xFF666666;
            int colorBotonCerrar = modoOscuro ? 0xFFCCCCCC : 0xFF666666;
            int colorFondo = modoOscuro ? 0xFF222222 : 0xFFFBFBFB;
            int colorFondoDivisor = modoOscuro ? 0xFF444444 : 0xFFCCCCCC;
            int[] colores = new int[]{colorTitulo, colorSubtitulo, colorBotonPlay, colorBotonCerrar, colorFondo, colorFondoDivisor};
            
            for (RemoteViews vista : vistas) {
                for (int m = 0; m < metodos.length; m++) {
                    for (int id = 0; id < idVistas.length; id++) {
                        if (idVistas[id] == R.id.divisorNotificacion && vista == vistaNotificacionPrincipal)
                            break;
                        vista.setInt(idVistas[id], metodos[id], colores[id]);
                    }
                }
                vista.setOnClickPendingIntent(R.id.botonPlayNotificacion, this.piPlayStopRepro);
                vista.setOnClickPendingIntent(R.id.botonCerrarNotificacion, this.piCerrar);
            }
            notificationBuilder.setCustomContentView(vistaNotificacionPrincipal);// pongo la vista
            if (medioURL != null)// pongo la vista expandida solo si voy a mostrar el reproductor con los botones
                notificationBuilder.setCustomBigContentView(vistaNotificacionPrincipalGrande);
            notificationBuilder.setCategory(NotificationCompat.CATEGORY_TRANSPORT);// esto indica al sistema que se trata de un reproductor multimedia
            
        } else if (mediaStyle == null) {
            mediaStyle = new MediaStyle();
            mediaStyle.setMediaSession(mediaSession.getSessionToken());
            mediaStyle.setShowCancelButton(true);
            mediaStyle.setCancelButtonIntent(this.piCerrar);
            mediaStyle.setShowActionsInCompactView(0);
            notificationBuilder.setStyle(mediaStyle);
        }
        /* -----------------------------------------------------------------------------------------
         * A partir de acá, agrego otras configuraciones a la notificación, dependiendo del estado
         * de la reproducción. Así tambien para playbackStateBuilder //TODO: hay que probar de nuevo
         * ---------------------------------------------------------------------------------------*/
        int btnAccion;
        String textoBotonControl;
        // TODO: hay que actualizar el título y el subtítulo desde metadatos (?)
        String subtitulo;
        switch (estado) {
            case conectando:
            case almacenando:
            case reproduciendo:
                if (estado == EstadoMP.conectando) {
                    if (mediaCallback != null)
                        mediaCallback.conectando();
                    notificationBuilder.setTicker(medioNombre);
                    subtitulo = (esEnVivo)
                                        ? getString(R.string.conectando_msj)
                                        : medioDetalle;
                } else if (estado == EstadoMP.almacenando) {
                    if (mediaCallback != null)
                        mediaCallback.almacenando();
                    subtitulo = (esEnVivo)
                                        ? getString(R.string.almacenando_msj)
                                        : medioDetalle;
                } else {
                    if (mediaCallback != null)
                        mediaCallback.reproduciendo();
                    subtitulo = (esEnVivo)
                                        ? getString(R.string.envivo_msj)
                                        : medioDetalle;
                }
                // conectando, almacenando y reproduciendo
                textoBotonControl = getString((esEnVivo) ? R.string.detener : R.string.pausar);
                btnAccion = (usarVistaPersonalizada)
                                    ? (esEnVivo) ? R.drawable.ic_stop : R.drawable.ic_pause
                                    : (esEnVivo) ? R.drawable.ic_media_stop : android.R.drawable.ic_media_pause;
                this.playbackStateBuilder.setState(PlaybackStateCompat.STATE_PLAYING, 0, 0);
                notificationBuilder.setOngoing(true);
                break;
            case detenido:
            case pausado:
            default:
                if (estado == EstadoMP.pausado) {
                    if (mediaCallback != null)
                        mediaCallback.pausado();
                } else if (estado == EstadoMP.detenido) {
                    if (mediaCallback != null) {
                        if (esEnVivo)
                            mediaCallback.detenido();
                        else
                            mediaCallback.pausado();
                    }
                }
                notificationBuilder.setOngoing(false);// esto impide al usuario ocultar la notificación deslizando horizontalmente
                subtitulo = (esEnVivo) ? (medioURL != null) ? getString(R.string.desconectado_msj) : getString(R.string.des_noti_seleccionar) : medioDetalle;
                textoBotonControl = getString(R.string.reproducir);
                btnAccion = (usarVistaPersonalizada) ? R.drawable.ic_play : android.R.drawable.ic_media_play;
                // Con "STATE_STOPPED", la notificacion baja (pierde prioridad) si se abrió antes de
                // otra app que obtuvo el enfoque de audio. Con "STATE_PAUSED", permanece arriba
                // si no se usa otra app que requiera el enfoque de audio.
                // STATED_STOPED no actualiza el botón en KitKat
                this.playbackStateBuilder.setState(PlaybackStateCompat.STATE_PAUSED, 0, 0);
                break;
        }
        /* -----------------------------------------------------------------------------------------
         * Actualizo el estado de mediaSession y agrego el botón correspondiente a la notificación:
         * ---------------------------------------------------------------------------------------*/
        mediaSession.setPlaybackState(this.playbackStateBuilder.build());
        if (medioURL != null) {
            if (usarVistaPersonalizada) {
                if (estado == EstadoMP.conectando) {
                    vistaNotificacionPrincipal.setInt(R.id.botonPlayNotificacion, "setVisibility", View.VISIBLE);
                    vistaNotificacionPrincipalGrande.setInt(R.id.botonPlayNotificacion, "setVisibility", View.VISIBLE);
                    //vistaNotificacionPrincipalGrande.setInt(R.id.divisorNotificacion, "setVisibility", View.VISIBLE);
                }
                vistaNotificacionPrincipal.setImageViewResource(R.id.botonPlayNotificacion, btnAccion);
                vistaNotificacionPrincipalGrande.setImageViewResource(R.id.botonPlayNotificacion, btnAccion);
            } else {
                notificationBuilder.clearActions();
                notificationBuilder.addAction(btnAccion, textoBotonControl, this.piPlayStopRepro);
                // (!) OJO: Sin addAction(), no se muestra la notificacion al iniciar el servicio
            }
        } else if (usarVistaPersonalizada) {
            vistaNotificacionPrincipal.setInt(R.id.botonPlayNotificacion, "setVisibility", View.INVISIBLE);
            vistaNotificacionPrincipalGrande.setInt(R.id.botonPlayNotificacion, "setVisibility", View.INVISIBLE);
            //vistaNotificacionPrincipalGrande.setInt(R.id.divisorNotificacion, "setVisibility", View.INVISIBLE);
        }
        /* -------------------------------------------------------------------------------------
         * A partir de acá se ponen los datos principales y obligatorios en la vista de la
         * notificación predeterminada del SO (título y subtítulo). Además, los metadatos.
         *
         * El estado reproduciendo es especial. En caso de que hayan metadatos anteriores,
         * los muestro y salgo de acá.
         * ---------------------------------------------------------------------------------------*/
        if ((esEnVivo && urlMedioAnterior != null && urlMedioAnterior.equals(medioURL) && (estado == EstadoMP.reproduciendo || estado == EstadoMP.detenido)) && this.mediaMetadata != null && this.mediaMetadata.hayMetadatos()) {
            if (estado == EstadoMP.reproduciendo) {
                System.out.println("Restaurando metadatos...");
                ponerMetadatos();
            } else {
                quitarMetadatos();
            }
            return;
        }
        notificationBuilder.setContentTitle(medioNombre);// título (obligatorio)
        notificationBuilder.setContentText(subtitulo);// subtítulo (obligatorio)
        
        if (usarVistaPersonalizada) {
            vistaNotificacionPrincipal.setTextViewText(R.id.tituloNotificacion, medioNombre);
            vistaNotificacionPrincipal.setTextViewText(R.id.subtituloNotificacion, subtitulo);
            vistaNotificacionPrincipalGrande.setTextViewText(R.id.tituloNotificacion, medioNombre);
            vistaNotificacionPrincipalGrande.setTextViewText(R.id.subtituloNotificacion, subtitulo);
        }
        
        if (mediaMetadataBuilder != null) {
            mediaMetadataBuilder.putString(android.support.v4.media.MediaMetadataCompat.METADATA_KEY_TITLE, medioNombre);
            mediaMetadataBuilder.putString(android.support.v4.media.MediaMetadataCompat.METADATA_KEY_ALBUM, subtitulo);
            mediaSession.setMetadata(mediaMetadataBuilder.build());
        }
        /* -----------------------------------------------------------------------------------------
         * Acá finalmente se muestra la nueva notificación o bien, se actualiza:
         * ---------------------------------------------------------------------------------------*/
        if (!enPrimerPlano) {
            System.out.println("Poniendo MediaService en primer plano...");
            startForeground(idNotifiRepro, notificationBuilder.build());
            enPrimerPlano = true;
        } else {
            System.out.println("Actualizando notificación...");
            notificationManager.notify(idNotifiRepro, notificationBuilder.build());
        }
        notiVisible = true;
        
    }
    
    private void borrarNotificacion(final long demora) {
        // Borrar de forma inmediata
        if (demora == 0) {
            if (notificationManager != null) {
                System.out.println("Borro todas las notificaciones.");
                notificationManager.cancelAll();
            }
            if (enPrimerPlano) {
                System.out.println("Quitando MediaService de primer plano...");
                stopForeground(true);
                enPrimerPlano = false;
                notiVisible = false;
            }
        } else {
            if (reproduciendo())
                return;
            // Si existe temporizador, lo cancelo y...
            if (temporizadorNoti != null)
                temporizadorNoti.cancel();
            // creo uno nuevo
            temporizadorNoti = new Timer();
            try {
                temporizadorNoti.schedule(new TemporizadorNoti(), demora);
                System.out.println("Temporizador iniciado.");
            } catch (Exception e) {
                e.printStackTrace();
                temporizadorNoti = null;
            }
        }
    }
    
    @TargetApi(value = Build.VERSION_CODES.LOLLIPOP)
    private void notificarSobreModoAhorro(boolean cancelar) {
        // Salgo de acá si no hay administrador de notificaciones
        if (notificationManager == null)
            return;
        // Cancelar u ocultar la notificación y salir de acá
        if (cancelar) {
            notificationManager.cancel(idNotifiModoAhorro);
            return;
        }
        // (!) IMPORTANTE: No cambiar el orden
        // Si lo anterior no se cumple, entonces paso a crear la notificación
        // Creo un nuevo identificador (requerido por Android Oreo)...
        String idNotiModoAhorro = BuildConfig.APPLICATION_ID + ".NotiModoAhorro";
        // ...y también un canal, requerido a partir de Android Oreo (API 26)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence nombreCanal = getString(R.string.modo_ahorro_tit);
            int importanciaCanal = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel canal = new NotificationChannel(idNotiModoAhorro, nombreCanal, importanciaCanal);
            canal.setDescription(getString(R.string.des_noti_modo_ahorro));
            canal.setShowBadge(false);
            notificationManager.createNotificationChannel(canal);
        }
        
        // Obtengo el subtítulo
        CharSequence subtitulo = getString(R.string.modo_ahorro_txt);
        // Creo la nueva notificación
        NotificationCompat.Builder notificacion = new NotificationCompat.Builder(MediaService.this, idNotiModoAhorro);
        notificacion.setSmallIcon(R.drawable.outline_radio_white_24);// ícono para la barra de estado (obligatorio)
        notificacion.setContentTitle(getString(R.string.modo_ahorro_tit));// título (obligatorio)
        notificacion.setContentText(subtitulo);// subtítulo (obligatorio)
        notificacion.setTicker(getString(R.string.modo_ahorro_tit));
        notificacion.setOngoing(true);
        // Le pongo estilo
        notificacion.setStyle(new NotificationCompat.BigTextStyle().bigText(subtitulo));// subtítulo (obligatorio)
        notificacion.setColor(ContextCompat.getColor(this, R.color.secondary));
        notificacion.setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
        notificacion.setPriority(NotificationCompat.PRIORITY_DEFAULT);
        
        // ¡Bum! Muestro la notificación (:
        notificationManager.notify(idNotifiModoAhorro, notificacion.build());
    }
    
    private void cancelarTempNoti() {
        if (temporizadorNoti != null) {
            temporizadorNoti.cancel();
            temporizadorNoti = null;
            System.out.println("Temporizador cancelado.");
        }
    }
    
    private void liberarCosas(boolean tambienMediaPlayer) {
        
        if (wifiLock != null && wifiLock.isHeld()) {
            System.out.println("wifiLock.release()");
            wifiLock.release();
        }
        
        if (wakeLock != null && wakeLock.isHeld()) {
            System.out.println("wakeLock.release()");
            wakeLock.release();
        }
        
        if (tambienMediaPlayer) {
            
            if (mediaSession != null) {
                System.out.println("mediaSession.release()");
                mediaSession.setActive(false);
                mediaSession.release();
                mediaSession = null;
            }
            
            manejarEnfoque(true);
            
            if (mediaPlayer != null) {
                System.out.println("mediaPlayer.release()");
                //mediaPlayer.reset();
                mediaPlayer.release();
                mediaPlayer = null;
            }
            
            System.out.println("Cancelando peticiones de ion...");
            Ion.getDefault(this).cancelAll();
            
        }
        
        
    }
    
    public static boolean esEnVivo() {
        return esEnVivo;
    }
    
    public static boolean reproduciendo() {
        return (estadoMP == EstadoMP.reproduciendo || estadoMP == EstadoMP.almacenando || estadoMP == EstadoMP.conectando);
    }
    
    public void ponerPosllamada(MediaCallback posllamada) {
        this.mediaCallback = posllamada;
    }
    
    @Override
    public void ponerMetadatos() {
        if (mediaSession != null && mediaMetadataBuilder != null) {
            mediaMetadataBuilder.putString(android.support.v4.media.MediaMetadataCompat.METADATA_KEY_TITLE, this.mediaMetadata.traerCancion());
            mediaMetadataBuilder.putString(android.support.v4.media.MediaMetadataCompat.METADATA_KEY_ALBUM, this.mediaMetadata.traerArtista());
            mediaSession.setMetadata(mediaMetadataBuilder.build());
        }
        // Actualizo los datos de la notificación
        if (notificationBuilder != null) {
            // Obligatorios (aun sin vistas personalizadas):
            notificationBuilder.setContentTitle(this.mediaMetadata.traerCancion());
            notificationBuilder.setContentText(this.mediaMetadata.traerArtista());
            // Con vistas personalizadas:
            if (usarVistaPersonalizada && vistaNotificacionPrincipal != null && vistaNotificacionPrincipalGrande != null) {
                vistaNotificacionPrincipal.setTextViewText(R.id.tituloNotificacion, this.mediaMetadata.traerCancion());
                vistaNotificacionPrincipal.setTextViewText(R.id.subtituloNotificacion, this.mediaMetadata.traerArtista());
                vistaNotificacionPrincipalGrande.setTextViewText(R.id.tituloNotificacion, this.mediaMetadata.traerCancion());
                vistaNotificacionPrincipalGrande.setTextViewText(R.id.subtituloNotificacion, this.mediaMetadata.traerArtista());
            }
            notificationManager.notify(idNotifiRepro, notificationBuilder.build());
        }
        cargarImagen(this.mediaMetadata.traerURLImagen(), false);
    }
    
    @Override
    public void quitarMetadatos() {
        System.out.println("Quitando metadatos...");
        String subtitulo;
        Bitmap imagen = (medioLogo != null) ? medioLogo : BitmapFactory.decodeResource(getResources(), R.drawable.no_logo);
        switch (estadoMP) {
            case conectando:
                subtitulo = (esEnVivo) ? getString(R.string.conectando_msj) : medioDetalle;
                break;
            case almacenando:
                subtitulo = (esEnVivo) ? getString(R.string.almacenando_msj) : medioDetalle;
                break;
            case reproduciendo:
                subtitulo = (esEnVivo) ? getString(R.string.envivo_msj) : medioDetalle;
                break;
            default:
                subtitulo = (esEnVivo) ? getString(R.string.desconectado_msj) : medioDetalle;
                break;
        }
        if (mediaSession != null && mediaMetadataBuilder != null) {
            mediaMetadataBuilder.putString(android.support.v4.media.MediaMetadataCompat.METADATA_KEY_TITLE, medioNombre);
            mediaMetadataBuilder.putString(android.support.v4.media.MediaMetadataCompat.METADATA_KEY_ALBUM, subtitulo);
            mediaMetadataBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, imagen);
            mediaSession.setMetadata(mediaMetadataBuilder.build());
        }
        // Actualizo los datos de la notificación
        if (notificationBuilder != null) {
            // Obligatorios (aun sin vistas personalizadas):
            notificationBuilder.setContentTitle(medioNombre);
            notificationBuilder.setContentText(subtitulo);
            if (usarVistaPersonalizada && vistaNotificacionPrincipal != null && vistaNotificacionPrincipalGrande != null) {
                vistaNotificacionPrincipal.setTextViewText(R.id.tituloNotificacion, medioNombre);
                vistaNotificacionPrincipal.setTextViewText(R.id.subtituloNotificacion, subtitulo);
                vistaNotificacionPrincipal.setImageViewBitmap(R.id.imagenNotificacion, imagen);
                vistaNotificacionPrincipalGrande.setTextViewText(R.id.tituloNotificacion, medioNombre);
                vistaNotificacionPrincipalGrande.setTextViewText(R.id.subtituloNotificacion, subtitulo);
                vistaNotificacionPrincipalGrande.setImageViewBitmap(R.id.imagenNotificacion, imagen);
            } else {
                notificationBuilder.setLargeIcon(imagen);
            }
            notificationManager.notify(idNotifiRepro, notificationBuilder.build());
        }
        
    }
    
    @Override
    public void error() {
        if (this.mediaMetadata.hayMetadatos())
            quitarMetadatos();
    }
    
    private class TemporizadorNoti extends TimerTask {
        Handler h = new Handler();
        @Override
        public void run() {
            h.post(() -> {
                if (!reproduciendo()) {
                    cancelarTempNoti();
                    borrarNotificacion(0);
                }
            });
        }
    }
    
    private class TemporizadorAS extends TimerTask {
        Handler h = new Handler();
        @Override
        public void run() {
            h.post(() -> {
                if (reproduciendo()) {
                    cancelarDetencion();
                    detener();
                }
            });
        }
    }
    
    private final MediaSessionCompat.Callback mMediaSessionCallback = new MediaSessionCompat.Callback() {
        @Override
        public void onPlay() {
            reproducir(estadoMP != EstadoMP.pausado);
            super.onPlay();
        }
        
        @Override
        public void onPause() {
            detener();
            super.onPause();
        }
        
        @Override
        public void onStop() {
            detener();
            super.onStop();
        }
        
    };
    
    private final BroadcastReceiver receptor = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            manejarIntent(intent);
        }
    };
    
    public class LocalBinder extends Binder {
        MediaService traerMediaService() {
            return MediaService.this;
        }
    }
    
    public void onAudioFocusChange(int enfoque) {
        switch (enfoque) {
            case AudioManager.AUDIOFOCUS_GAIN:
                System.out.println("Gané el enfoque de audio.");
                // Si se está reproduciendo
                if (reproduciendo() && mediaPlayer != null)
                    mediaPlayer.setVolume(1.0f, 1.0f);// aumento el volumen
                else if (estadoMP == EstadoMP.pausado)// si se pausó la reproducción...
                    reproducir(false);// ...vuelvo a reproducir
                break;
            case AudioManager.AUDIOFOCUS_LOSS:
                System.out.println("¡Uy! Perdí el enfoque de audio por largo tiempo.");
                //if (reproduciendo()) {
                //if (esEnVivo)
                //manejarEnfoque(true);
                detener();// debe estar después de manejarFocoAudio()
                //}
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                // Esto también se cumple si se recibe llamada en algunos dispositivos
                System.out.println("Perdí el enfoque de audio por tiempo limitado.");
                if (reproduciendo())
                    detener();// pausa si no es en vivo
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                System.out.println("Perdí el enfoque de audio solo un rato, reduzco el volumen.");
                if (reproduciendo() && mediaPlayer != null)
                    mediaPlayer.setVolume(0.1f, 0.1f);
                break;
        }
    }
    
    @Override
    public void onPrepared(MediaPlayer mp) {
        // mediaPlayer preparadísimo, reproducir
        reproducir(false);
    }
    
    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        System.out.println("Error de reproducción.");
        if (mediaCallback != null)
            mediaCallback.error();
        // Debe estar antes de detener()
        estadoMP = EstadoMP.error;
        detener();
        return true;// Si hay error, no es necesario llamar a onCompletionListener, por eso: true (?)
    }
    
    @Override
    public void onCompletion(MediaPlayer mp) {
        System.out.println("Reproducción finalizada.");
        // Debe estar antes de detener()
        estadoMP = EstadoMP.finalizado;
        if (mediaCallback != null)
            mediaCallback.finalizado();
        detener();
        
    }
    
    //*****************************************************************
    // CICLO DE VIDA DEL SERVICIO
    //*****************************************************************
    @Override
    public void onCreate() {
        System.out.println("MediaService creado.");
        // Acciones principales
        IntentFilter accion = new IntentFilter();
        accion.addAction(ACCION_PREPARAR);
        accion.addAction(ACCION_PLAY);
        accion.addAction(ACCION_PAUSE);
        accion.addAction(ACCION_STOP);
        accion.addAction(ACCION_PLAY_STOP);
        // Acción para programar detención automática de reproducción
        accion.addAction(ACCION_AUTO_STOP);
        // Acción para notificar al usuario
        accion.addAction(ACCION_NOTIFICAR);
        // Acción para cerrar la aplicación desde la notificación
        accion.addAction(ACCION_CERRAR);
        // Acción para controlar el audio con los auriculares
        accion.addAction(Intent.ACTION_HEADSET_PLUG);
        // Acción para detectar el enfoque de audio
        accion.addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
        // Acción para detectar la activación del modo ahorro de batería
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            accion.addAction("android.os.action.POWER_SAVE_MODE_CHANGED");
        // Le doy prioridad máxima...
        accion.setPriority(Integer.MAX_VALUE);
        // y registro mi receptor de acciones
        registerReceiver(receptor, accion);
        
        // Valor predeterminado para el título (notificacion y otras cosas)
        medioNombre = getString(R.string.appNombre);
        
        // Inicializo lo básico para la reproducción de audio en segundo plano
        Context contexto = getApplicationContext();
        audioManager = (AudioManager) contexto.getSystemService(Context.AUDIO_SERVICE);
        PowerManager powerManager = (PowerManager) contexto.getSystemService(POWER_SERVICE);
        WifiManager wifiManager = (WifiManager) contexto.getSystemService(Context.WIFI_SERVICE);
        notificationManager = (NotificationManager) contexto.getSystemService(NOTIFICATION_SERVICE);
        mediaMetadataBuilder = new MediaMetadataCompat.Builder();
        
        // Creo una intent o acción para cuando el usuario toca la notificacion:
        Intent toqueNotifi = new Intent(this, (esEnVivo) ? ActiPrincipal.class : ActiPrincipal.class);
        toqueNotifi.putExtra("desdeServicio", true);
        toqueNotifi.setAction(Intent.ACTION_MAIN);
        toqueNotifi.addCategory(Intent.CATEGORY_LAUNCHER);
        this.piNotificacion = PendingIntent.getActivity(this, 0, toqueNotifi, 0);
        // Creo una intent o acción para cuando el usuario toca el Botón Play/Stop o Play/Pause
        Intent iPlayStop = new Intent(ACCION_PLAY_STOP);
        this.piPlayStopRepro = PendingIntent.getBroadcast(this, 0, iPlayStop, PendingIntent.FLAG_UPDATE_CURRENT);
        // Creo una intent o acción para cuando el usuario toca el botón Cerrar
        Intent iCerrar = new Intent(ACCION_CERRAR);
        this.piCerrar = PendingIntent.getBroadcast(this, 0, iCerrar, PendingIntent.FLAG_ONE_SHOT);
        
        this.playbackStateBuilder = new PlaybackStateCompat.Builder();
        this.playbackStateBuilder.setActions(PlaybackStateCompat.ACTION_PLAY_PAUSE | PlaybackStateCompat.ACTION_PLAY | PlaybackStateCompat.ACTION_PAUSE | PlaybackStateCompat.ACTION_STOP);
        
        if (powerManager != null)
            wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, ETIQUETA + ":wakelock");
        if (wifiManager != null)
            wifiLock = wifiManager.createWifiLock(WifiManager.WIFI_MODE_FULL, ETIQUETA + ":wifilock");
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Si no hay intents, inicio el servicio y "salgo" de acá, pero
        // no quiero reiniciarlo después de destruirlo (START_NOT_STICKY)
        if (intent == null)
            return START_NOT_STICKY;
        
        // Obtengo la URL de transmisión
        if (intent.getStringExtra(MEDIO_URL) != null)
            medioURL = intent.getStringExtra(MEDIO_URL);
        
        // Obtengo el nombre del medio
        if (intent.getStringExtra(MEDIO_NOMBRE) != null)
            medioNombre = intent.getStringExtra(MEDIO_NOMBRE);
        
        // Obtengo el subtítulo o detalle del medio
        if (intent.getStringExtra(MEDIO_DETALLE) != null)
            medioDetalle = intent.getStringExtra(MEDIO_DETALLE);
        else if (medioDetalle == null)
            medioDetalle = getString(R.string.nodisponible_txt);
        
        // Obtengo la imagen del medio si se obtuvo una URL
        if (intent.getStringExtra(MEDIO_URL_IMAGEN) != null) {
            String medioURLLogo = intent.getStringExtra(MEDIO_URL_IMAGEN);
            cargarImagen(medioURLLogo, true);
        }
        
        // Obtengo la URL de donde se obtienen los metadatos de las canciones (beta)
        if (intent.getStringExtra(MEDIO_URL_METADATOS) != null) {
            medioURLMetadatos = intent.getStringExtra(MEDIO_URL_METADATOS);
        }
        
        // Obtengo el valor de la variable enVivo para saber si es un medio en vivo o no
        esEnVivo = intent.getBooleanExtra(EN_VIVO, esEnVivo);
        
        // Manejo las intents de acciones recibidas para saber qué hacer
        manejarIntent(intent);
        return START_NOT_STICKY;
    }
    
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }
    
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (notiVisible)
            notificar(estadoMP);
    }
    
    @Override
    public void onDestroy() {
        System.out.println("Destruyendo MediaService...");
        cancelarDetencion();
        borrarNotificacion(0);
        cancelarTempNoti();
        liberarCosas(true);
        unregisterReceiver(receptor);
        if (this.mediaMetadata != null && this.mediaMetadata.programadorDeTarea != null)
            this.mediaMetadata.programadorDeTarea.shutdown();
        super.onDestroy();
    }
    
}