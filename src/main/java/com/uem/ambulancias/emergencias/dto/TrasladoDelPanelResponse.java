package com.uem.ambulancias.emergencias.dto;

import java.time.Instant;
import java.util.List;

import com.uem.ambulancias.emergencias.domain.Atencion;
import com.uem.ambulancias.emergencias.domain.EstadoAtencion;
import com.uem.ambulancias.emergencias.domain.MotivoSinTraslado;
import com.uem.ambulancias.emergencias.domain.ProblemaDeTraslado;
import com.uem.ambulancias.emergencias.domain.Traslado;
import com.uem.ambulancias.usuarios.domain.RolUsuario;
import com.uem.ambulancias.usuarios.domain.Usuario;

/**
 * Una fila de la tabla del panel: el pedido más la unidad que lo está haciendo, si ya tiene una. Sin unidad
 * asignada, los datos de la atención vienen nulos y los hitos, vacíos.
 */
public record TrasladoDelPanelResponse(

		TrasladoResponse traslado,
		/** Por qué necesita que el administrador haga algo ahora. Nulo si no necesita nada. */
		ProblemaDeTraslado problema,
		/** Cuándo se le avisó a la familia que no hubo unidad. Solo en los no cubiertos. */
		Instant horaFamiliaAvisada,
		/** La última vez que una unidad lo devolvió: por eso va primero en la fila. */
		Instant horaDevolucion,
		Long atencionId,
		String placa,
		EstadoAtencion estadoAtencion,
		String paramedico,
		/** Lo que hizo la unidad que lo tiene o lo terminó, del hito más viejo al más nuevo. */
		List<AtencionEnCursoResponse.Hito> hitos,
		/** Por qué no viajó nadie, cuando la unidad lo cerró sin traslado. */
		MotivoSinTraslado motivoSinTraslado,
		/** Quién de la central lo cargó, si se pidió por teléfono. Nulo si se pidió por la app. */
		String registradoPorLaCentral,
		/** El teléfono de quien pidió el traslado, para devolverle la llamada. */
		String telefonoSolicitante) {

	public static TrasladoDelPanelResponse de(Traslado traslado, Atencion atencion) {
		ProblemaDeTraslado problema = ProblemaDeTraslado.de(traslado, atencion, Instant.now());
		if (atencion == null) {
			return new TrasladoDelPanelResponse(TrasladoResponse.de(traslado), problema,
					traslado.getHoraFamiliaAvisada(), traslado.getHoraDevolucion(), null, null, null, null, List.of(),
					null, registradoPorLaCentral(traslado), telefonoSolicitante(traslado));
		}
		return new TrasladoDelPanelResponse(
				TrasladoResponse.de(traslado, atencion),
				problema,
				traslado.getHoraFamiliaAvisada(),
				traslado.getHoraDevolucion(),
				atencion.getId(),
				atencion.getAmbulancia().getPlaca(),
				atencion.getEstado(),
				atencion.getParamedicoResponsable() == null ? null
						: atencion.getParamedicoResponsable().getNombreCompleto(),
				AtencionEnCursoResponse.hitosDe(atencion),
				atencion.getMotivoSinTraslado(),
				registradoPorLaCentral(traslado),
				telefonoSolicitante(traslado));
	}

	private static String registradoPorLaCentral(Traslado traslado) {
		Usuario solicitante = traslado.getSolicitante();
		return solicitante.getRol() == RolUsuario.ADMIN ? solicitante.getNombreCompleto() : null;
	}

	/** Pedido por teléfono, el número es el de quien viaja o su familia; por la app, el de la cuenta. */
	private static String telefonoSolicitante(Traslado traslado) {
		Usuario solicitante = traslado.getSolicitante();
		return solicitante.getRol() == RolUsuario.ADMIN ? traslado.getPasajero().getTelefono()
				: solicitante.getTelefono();
	}

}
