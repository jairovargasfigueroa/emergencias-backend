package com.uem.ambulancias.emergencias.service;

import java.util.List;

/**
 * Notificaciones push para las apps cerradas (PB-03 R3). Se llama después del commit.
 */
public interface NotificadorPush {

	/** Avisa del incidente a esos teléfonos, ya ordenados del más cercano al más lejano. */
	void notificarNuevoIncidente(List<String> tokensPorCercania, IncidentePublicado incidente);

}
