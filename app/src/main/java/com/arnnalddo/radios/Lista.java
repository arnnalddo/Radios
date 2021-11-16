package com.arnnalddo.radios;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;


/**
 * Created by arnaldito100 on 18/8/17.
 * Copyright © 2017 Arnaldo Alfredo. All rights reserved.
 * http://www.arnnalddo.com
 * Adaptador para las listas principales
 * Esta clase abstracta es la «madre» de los adaptadores de las listas:
 * ListaClima, ListaCotizacion, ListaFutbol, ListaMedio, etc.
 * (!) Evitar modificar
 */


public abstract class Lista extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    //
    // INTERFACES
    //**********************************************************************************************
    // Para detectar el toque en el ítem
    protected interface DetectorDeToque {
        void enToque(int posicion);
    }
    // Para detectar el toque prolongado en el ítem
    protected interface DetectorDeToqueLargo {
        boolean enToqueLargo(int posicion);
    }
    //
    // PROPIEDADES
    //**********************************************************************************************
    // Contexto donde se muestra la lista
    protected final Context contexto;
    // «Inflador» del disenio
    protected final LayoutInflater inflater;
    // Lista de ítems
    protected final ArrayList<ItemLista> items;
    // Cantidad de tipos de items
    protected final int totalTipoItem;
    // Interfaz para detectar el toque en el ítem
    protected DetectorDeToque detectorDeToque;
    // Interfaz para detectar el toque prolongado en el ítem
    protected DetectorDeToqueLargo detectorDeToqueLargo;
    //
    // CONSTRUCTORES
    //**********************************************************************************************
    Lista(Context contexto, ArrayList<ItemLista> items, int totalTipoItem, DetectorDeToque itemClickListener, DetectorDeToqueLargo itemLongClickListener) {
        super();
        this.contexto = contexto;
        this.items = items;
        this.totalTipoItem = totalTipoItem;
        this.inflater = LayoutInflater.from(contexto);
        this.detectorDeToque = itemClickListener;
        this.detectorDeToqueLargo = itemLongClickListener;
    }
    
    Lista(Context contexto, ArrayList<ItemLista> items, int totalTipoItem, DetectorDeToque itemClickListener) {
        super();
        this.contexto = contexto;
        this.items = items;
        this.totalTipoItem = totalTipoItem;
        this.inflater = LayoutInflater.from(contexto);
        this.detectorDeToque = itemClickListener;
    }
    
    public Lista(Context contexto, ArrayList<ItemLista> items, int totalTipoItem) {
        super();
        this.contexto = contexto;
        this.items = items;
        this.totalTipoItem = totalTipoItem;
        this.inflater = LayoutInflater.from(contexto);
    }
    //
    // MÉTODOS
    //**********************************************************************************************
    // Para obtener el tipo de ítem
    @Override
    public int getItemViewType(int position) {
        return items.get(position).tipoItem();
    }
    // Para obtener la cantidad de ítems
    @Override
    public int getItemCount() {
        return items.size();
    }
    // Para reciclar/rehusar los componentes del disenio (objetivo principal)
    protected abstract static class ViewHolder extends RecyclerView.ViewHolder {
        public ViewHolder(@NonNull View vista) {
            super(vista);
        }
    }
}

