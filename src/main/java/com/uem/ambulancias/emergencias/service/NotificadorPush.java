package com.uem.ambulancias.emergencias.service;

import java.util.List;

/**
 * Notificaciones push para las apps cerradas (PB-03 R3). Se llama después del commit.
 */
public interface NotificadorPush {

	/** Avisa del incidente a esos teléfonos, ya ordenados del más cercano al más lejano. */
	void notificarNuevoIncidente(List<String> tokensPorCercania, IncidentePublicado incidente);

	/** Avisa a ese teléfono del traslado que le acaba de tocar. Va a un solo paramédico: el responsable. */
	void notificarTrasladoAsignado(String tokenPush, AvisoDeTraslado aviso);

}
