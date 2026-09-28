package com.uem.ambulancias.emergencias.dto;

import java.time.Instant;

import com.uem.ambulancias.emergencias.domain.Atencion;
import com.uem.ambulancias.emergencias.domain.EstadoAtencion;
import com.uem.ambulancias.emergencias.domain.ProblemaDeTraslado;
import com.uem.ambulancias.emergencias.domain.Traslado;

/**
 * Una fila de la tabla del panel: el pedido más la unidad que lo está haciendo, si ya tiene una. Sin unidad
 * asignada, los datos de la atención vienen nulos.
 */
public record TrasladoDelPanelResponse(

		TrasladoResponse traslado,
		/** Por qué necesita que el administrador haga algo ahora. Nulo si no necesita nada. */
		ProblemaDeTraslado problema,
		/** Cuándo se le avisó a la familia que no hubo unidad. Solo en los no cubiertos. */
		Instant horaFamiliaAvisada,
		Long atencionId,
		String placa,
		EstadoAtencion estadoAtencion,
		String paramedico) {

	public static TrasladoDelPanelResponse de(Traslado traslado, Atencion atencion) {
		ProblemaDeTraslado problema = ProblemaDeTraslado.de(traslado, atencion, Instant.now());
		if (atencion == null) {
			return new TrasladoDelPanelResponse(TrasladoResponse.de(traslado), problema,
					traslado.getHoraFamiliaAvisada(), null, null, null, null);
		}
		return new TrasladoDelPanelResponse(
				TrasladoResponse.de(traslado, atencion),
				problema,
				traslado.getHoraFamiliaAvisada(),
				atencion.getId(),
				atencion.getAmbulancia().getPlaca(),
				atencion.getEstado(),
				atencion.getParamedicoResponsable() == null ? null
						: atencion.getParamedicoResponsable().getNombreCompleto());
	}

}
