package com.uem.ambulancias.emergencias.dto;

import com.uem.ambulancias.emergencias.domain.Atencion;
import com.uem.ambulancias.emergencias.domain.EstadoAtencion;
import com.uem.ambulancias.emergencias.domain.Traslado;

/**
 * Una fila de la tabla del panel: el pedido más la unidad que lo está haciendo, si ya tiene una. Sin unidad
 * asignada, los tres últimos campos vienen nulos y esa fila es de las que necesitan una decisión.
 */
public record TrasladoDelPanelResponse(

		TrasladoResponse traslado,
		Long atencionId,
		String placa,
		EstadoAtencion estadoAtencion,
		String paramedico) {

	public static TrasladoDelPanelResponse de(Traslado traslado, Atencion atencion) {
		if (atencion == null) {
			return new TrasladoDelPanelResponse(TrasladoResponse.de(traslado), null, null, null, null);
		}
		return new TrasladoDelPanelResponse(
				TrasladoResponse.de(traslado),
				atencion.getId(),
				atencion.getAmbulancia().getPlaca(),
				atencion.getEstado(),
				atencion.getParamedicoResponsable() == null ? null
						: atencion.getParamedicoResponsable().getNombreCompleto());
	}

}
