package com.uem.ambulancias.emergencias.service;

import java.util.List;

import com.uem.ambulancias.emergencias.domain.MotivoCancelacionAtencion;

/**
 * Notificaciones push para las apps cerradas (PB-03 R3). Se llama después del commit.
 */
public interface NotificadorPush {

	/** Avisa del incidente a esos teléfonos, ya ordenados del más cercano al más lejano. */
	void notificarNuevoIncidente(List<String> tokensPorCercania, IncidentePublicado incidente);

	/** Avisa a la tripulación de una unidad que la central la mandó a esa emergencia. */
	void notificarIncidenteAsignado(List<String> tokens, IncidentePublicado incidente);

	/** Avisa a la tripulación de una unidad del traslado que le acaba de tocar. */
	void notificarTrasladoAsignado(List<String> tokens, AvisoDeTraslado aviso);

	/** Avisos a ciudadanos, ya armados: cada uno va a su teléfono con su texto y lo que abre al tocarlo. */
	void notificarCiudadanos(List<AvisoParaCiudadano> avisos);

	/**
	 * Avisa a la tripulación que ese traslado ya no es suyo: {@code motivo} dice si lo canceló quien lo pidió o si
	 * el administrador se lo pasó a otra unidad.
	 */
	void notificarTrasladoRetirado(List<String> tokens, AvisoDeTraslado aviso, MotivoCancelacionAtencion motivo);

}
