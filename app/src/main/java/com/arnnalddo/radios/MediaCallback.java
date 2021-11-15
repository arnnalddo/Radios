package com.arnnalddo.radios;

/**
 * Created by arnaldito100 on 03/08/2013.
 * Copyright © 2013 Arnaldo Alfredo. All rights reserved.
 * http://www.arnnalddo.com
 * Media Callback
 */

interface MediaCallback {

    void inicializar(boolean ocultar);
    void conectando();
    void almacenando();
    void reproduciendo();
    void pausado();
    void detenido();
    void finalizado();
    void error();
    void cerrar();

}