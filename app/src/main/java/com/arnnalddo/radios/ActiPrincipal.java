package com.arnnalddo.radios;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;


/**
 * Created by arnaldito100 on 03/08/2013.
 * Copyright © 2013 Arnaldo Alfredo. All rights reserved.
 * http://www.arnnalddo.com
 * Actividad principal
 */
public class ActiPrincipal extends AppCompatActivity {
    
    // Otros
    private Intent iServicio;
    private boolean mBound = false;
    private String urlMedios;
    
    /**
     * Manejar la intent
     */
    private void manejarIntent(Intent i) {
        if (i != null) {
            if (i.getExtras() != null) {
                Bundle extra = i.getExtras();
                if (extra.get("urlMedios") != null)
                    urlMedios = extra.getString("urlMedios");
            }
        }
    }
    
    
    /**
     * Defines callbacks for service binding, passed to bindService()
     */
    private final ServiceConnection mConnection = new ServiceConnection() {
        
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            MediaService.LocalBinder binder = (MediaService.LocalBinder) service;
            MediaService mService = binder.traerMediaService();
            mBound = true;
            
            FragServicioMedio fragServicioMedio = (FragServicioMedio) getSupportFragmentManager().findFragmentById(CONTENT_VIEW_ID);
            if (fragServicioMedio != null) {
                
                mService.ponerPosllamada(fragServicioMedio.mediaCallback);
                
                if (MediaService.esEnVivo()) {
                    fragServicioMedio.actualizarFAB(MediaService.estadoMP);
                } else {
                    // TODO esto se está llamando siempre
                    fragServicioMedio.actualizarFAB(Util.EstadoMP.detenido);
                }
                
            }
            
        }
        
        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
        
    };
    
    
    /**
     * Desde aca el ciclo de vida de la Actividad
     */
    private static final int CONTENT_VIEW_ID = R.id.contenedor_fragmento;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.actividad_principal);
        
        manejarIntent(getIntent());
        iServicio = new Intent(this, MediaService.class);
        
        Bundle textoExtra = new Bundle();
        textoExtra.putString("urlServicio", urlMedios);
        
        if (savedInstanceState == null) {
            Util.ACTI_PR_RECIEN_CREADA = true;// Actividad recien creada
            Fragment fragMedios = new FragServicioMedio();
            fragMedios.setArguments(textoExtra);
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
            transaction.add(R.id.contenedor_fragmento, fragMedios, "fragServicioMedio");
            transaction.addToBackStack(null);
            transaction.commit();
        }
    
        ActionBar ab = getSupportActionBar();
        if (ab != null)
            ab.setElevation(0);
    }
    
    
    @Override
    protected void onStart() {
        if (Util.enModoAhorro(this))
            Toast.makeText(this, getString(R.string.powerSaver), Toast.LENGTH_LONG).show();
        
        if (iServicio != null && !mBound)
            bindService(iServicio, mConnection, Context.BIND_AUTO_CREATE);
        
        super.onStart();
    }
    
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        manejarIntent(intent);
    }
    
    
    @Override
    public void onBackPressed() {
        if (MediaService.reproduciendo()) {
            try {
                moveTaskToBack(true);
            } catch (IllegalStateException e) {
                e.printStackTrace();
            }
        } else {
            finish();
        }
        //else super.onBackPressed();
    }
    
    @Override
    protected void onStop() {
        if (Util.ACTI_PR_RECIEN_CREADA)
            Util.ACTI_PR_RECIEN_CREADA = false;
        
        if (mBound) {
            unbindService(mConnection);
            mBound = false;
        }
        super.onStop();
    }
    
    
    // Detener servicio al salir de la app
    @Override
    public void onDestroy() {
        if (!isChangingConfigurations())
            stopService(iServicio);
        super.onDestroy();
    }
    
}