package com.arnnalddo.radios;


/**
 * Created by arnaldito100 on 17/08/17.
 * Copyright © 2017 Arnaldo Alfredo. All rights reserved.
 * http://www.arnnalddo.com
 * Ítem para la lista (medios)
 */

class ItemListaMedio implements ItemLista {
    //*****************************************************************
    // PROPIEDADES
    //*****************************************************************
    private final int tipoItem;
    String tituloSeccion, id, region, nombre, url, urlTigo, urlCopaco, urlLogoChico, urlLogoGrande, urlMetadatos, urlGrabaciones;
    boolean esVideo;
    private boolean esPrimerItem;
    
    //*****************************************************************
    // CONSTRUCTORES
    //*****************************************************************
    ItemListaMedio(int tipoItem) {
        this.tipoItem = tipoItem;
    }
    
    //*****************************************************************
    // MÉTODOS
    //*****************************************************************
    protected void ponerTituloSeccion(String titulo) {
        this.tituloSeccion = titulo;
    }
    
    protected void ponerID(String id) {
        this.id = id;
    }
    
    protected void ponerRegion(String region) {
        this.region = region;
    }
    
    protected void ponerNombre(String nombre) {
        this.nombre = nombre;
    }
    
    protected void ponerURL(String url) {
        this.url = url;
    }
    
    protected void ponerURLTigo(String urlTigo) {
        this.urlTigo = urlTigo;
    }
    
    protected void ponerURLCopaco(String urlCopaco) {
        this.urlCopaco = urlCopaco;
    }
    
    protected void ponerURLLogoChico(String url) {
        this.urlLogoChico = url;
    }
    
    protected void ponerURLLogoGrande(String url) {
        this.urlLogoGrande = url;
    }
    
    protected void ponerURLMetadatos(String url) {
        this.urlMetadatos = url;
    }
    
    protected void ponerURLGrabaciones(String url) {
        this.urlGrabaciones = url;
    }
    
    protected void ponerEsVideo(boolean trueOfalse) {
        this.esVideo = trueOfalse;
    }
    
    protected void ponerEsPrimerItem(boolean trueOfalse) {
        this.esPrimerItem = trueOfalse;
    }
    
    protected boolean esPrimerItem() {
        return this.esPrimerItem;
    }
    
    @Override
    public int tipoItem() {
        return this.tipoItem;
    }
}
