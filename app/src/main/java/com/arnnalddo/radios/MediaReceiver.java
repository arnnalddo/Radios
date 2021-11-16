package com.arnnalddo.radios;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.view.KeyEvent;


/**
 * Created by arnaldito100 on 03/08/2013.
 * Copyright © 2013 Arnaldo Alfredo. All rights reserved.
 * http://www.arnnalddo.com
 * Receiver
 */
public class MediaReceiver extends BroadcastReceiver {
    
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction() != null && intent.getAction().equals(Intent.ACTION_MEDIA_BUTTON)) {
            if (intent.getExtras() != null) {
                final KeyEvent event = (KeyEvent) intent.getExtras().get(Intent.EXTRA_KEY_EVENT);
                if (event != null && event.getAction() == KeyEvent.ACTION_DOWN) {
                    
                    Intent i = new Intent(context, MediaService.class);
                    switch (event.getKeyCode()) {
                        case KeyEvent.KEYCODE_HEADSETHOOK:
                        case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                            i.setAction(MediaService.ACCION_PLAY_STOP);
                            break;
                        case KeyEvent.KEYCODE_MEDIA_PLAY:
                            i.setAction(MediaService.ACCION_PLAY);
                            break;
                        case KeyEvent.KEYCODE_MEDIA_PAUSE:
                            i.setAction(MediaService.ACCION_PAUSE);
                            break;
                        case KeyEvent.KEYCODE_MEDIA_STOP:
                            i.setAction(MediaService.ACCION_STOP);
                            break;
                        default:
                            break;
                    }//fin switch
                    
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                        context.startForegroundService(i);
                    else
                        context.startService(i);
                    
                }//fin if
            }//fin if
        }//fin if
    }
    
}