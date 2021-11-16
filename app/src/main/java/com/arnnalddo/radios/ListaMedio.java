package com.arnnalddo.radios;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.os.Build;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.koushikdutta.ion.Ion;

import java.util.ArrayList;


/**
 * Created by arnaldito100 on 20/04/2016.
 * Copyright © 2016 Arnaldo Alfredo. All rights reserved.
 * http://www.arnnalddo.com
 * Adaptador para la lista (medios)
 */


class ListaMedio extends Lista {
    //
    // PROPIEDADES
    //**********************************************************************************************
    private final String fuenteNegrita, fuenteRegular;
    private final ColorStateList colorTextoItemPrimario;
    private int selectedPos = 0;
    TypedValue outValue = new TypedValue();
    Resources.Theme tema;
    //
    // CONSTRUCTORES
    //**********************************************************************************************
    ListaMedio(Context contexto, ArrayList<ItemLista> items, int totalTipoItem, DetectorDeToque itemClickListener) {
        super(contexto, items, totalTipoItem, itemClickListener);
        this.fuenteNegrita = contexto.getString(R.string.fuente_negrita);
        this.fuenteRegular = contexto.getString(R.string.fuente_regular);
        tema = contexto.getTheme();
        // Obtengo el color primario
        tema.resolveAttribute(android.R.attr.textColorPrimary, outValue, true);
        int colorPri = ContextCompat.getColor(contexto, outValue.resourceId);
        // Obtengo el color terciario
        tema.resolveAttribute(android.R.attr.textColorTertiary, outValue, true);
        int colorTer = ContextCompat.getColor(contexto, outValue.resourceId);
        
        int colorAccent = ContextCompat.getColor(contexto, R.color.secondary);
        int[][] estados = new int[][]{
                new int[]{android.R.attr.state_selected},// state_activated para listview
                new int[]{android.R.attr.state_pressed},
                new int[]{}
        };
        this.colorTextoItemPrimario = new ColorStateList(estados,
                new int[]{colorAccent, colorTer, colorPri}
        );
    }
    //
    // MÉTODOS
    //**********************************************************************************************
    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup madre, int tipoItem) {
        // elijo el id de la estructura (o disenio) correspondiente,
        // según el tipo de ítem
        int idDisenio;
        switch (tipoItem) {
            case 0:// cabecera
                idDisenio = R.layout.inc_item_cab_medio;
                break;
            case 1:// cuerpo
                idDisenio = R.layout.inc_item_medio;
                break;
            default:// datos no disponibles
                idDisenio = R.layout.inc_item_nodisponible;
                break;
        }
        View vista = inflater.inflate(idDisenio, madre, false);
        return new ViewHolder(vista, detectorDeToque);
    }
    
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, int posicion) {
        viewHolder.itemView.setSelected(selectedPos == posicion);
        int tipoItem = this.getItemViewType(posicion);
        ViewHolder holder = (ViewHolder) viewHolder;
        ItemListaMedio item = (ItemListaMedio) items.get(posicion);
        if (item != null) {
            switch (tipoItem) {
                case 0:// cabecera
                    holder.tituloSeccion.setText(item.tituloSeccion);
                    Util.cambiarFuente(contexto, fuenteNegrita, holder.tituloSeccion);
                    break;
                case 1:// cuerpo
                    // cargo el logotipo del medio o muestro una imagen alternativa
                    Ion.with(holder.imagen)
                            .placeholder(R.drawable.no_logo)
                            .error(R.drawable.no_logo)
                            .load(item.urlLogoChico);
                    // pongo el texto principal (nombre del medio) y estilos correspondientes
                    holder.nombre.setText(item.nombre);
                    Util.cambiarFuente(contexto, fuenteRegular, holder.nombre);
                    // verifico si fue el último medio seleccionado, para destacar el ítem
                    String[] ultMedioArray = Util.obtPreferencia(contexto, Util.PREF_ULTIMO_MEDIO);
                    boolean fueUlt = (ultMedioArray.length > 0 && ultMedioArray[0].equals("00" + item.id));
                    holder.seleccionarItem(fueUlt);
                    holder.divisor.setVisibility((item.esPrimerItem()) ? View.INVISIBLE : View.VISIBLE);
                    break;
                default:
                    break;
            }
        }
    }
    //
    // OTROS
    //**********************************************************************************************
    class ViewHolder extends Lista.ViewHolder implements View.OnClickListener {
        DetectorDeToque detectorDeToque;
        TextView tituloSeccion, nombre;
        ImageView imagen;
        View divisor;
        ViewHolder(@NonNull View vista, DetectorDeToque detectorDeToque) {
            super(vista);
            this.detectorDeToque = detectorDeToque;
            // este ítem es "clicable", entonces hago que sea "clicable"
            vista.setFocusable(true);
            vista.setClickable(true);
            vista.setOnClickListener(this);
            // ------------------------------------------
            tituloSeccion = vista.findViewById(R.id.tituloSeccion);
            // ------------------------------------------
            imagen = vista.findViewById(R.id.imagenMedio);
            // hago que la imagen tenga bordes redondeados para Android 5+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                imagen.setBackground(ContextCompat.getDrawable(contexto, R.drawable.fondo_logo_lista));
                imagen.setClipToOutline(true);
            }
            nombre = vista.findViewById(R.id.nombreMedio);
            nombre.setTextColor(colorTextoItemPrimario);
            divisor = vista.findViewById(R.id.divisor);
        }
        
        private void seleccionarItem(boolean seleccionar) {
            itemView.setSelected(seleccionar);
            if (seleccionar)
                selectedPos = getAdapterPosition();
        }
        
        @Override
        public void onClick(View view) {
            if (getAdapterPosition() == RecyclerView.NO_POSITION)
                return;
            notifyItemChanged(selectedPos);
            selectedPos = getAdapterPosition();
            notifyItemChanged(selectedPos);
            detectorDeToque.enToque(getAdapterPosition());
        }
    }
}
