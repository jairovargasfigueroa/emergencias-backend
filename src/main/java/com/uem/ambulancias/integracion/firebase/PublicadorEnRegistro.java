package com.uem.ambulancias.integracion.firebase;

import java.util.List;

import com.uem.ambulancias.emergencias.service.IncidentePublicado;
import com.uem.ambulancias.emergencias.service.NotificadorPush;
import com.uem.ambulancias.emergencias.service.PublicadorDeIncidentes;
import com.uem.ambulancias.emergencias.service.SeguimientoPublicado;
import com.uem.ambulancias.flota.service.PosicionActualizada;
import com.uem.ambulancias.flota.service.PublicadorDePosiciones;

import lombok.extern.slf4j.Slf4j;

/**
 * Reemplazo cuando Firebase está apagado ({@code sga.firebase.habilitado=false}): solo deja registro.
 */
@Slf4j
public class PublicadorEnRegistro implements PublicadorDeIncidentes, PublicadorDePosiciones, NotificadorPush {

	@Override
	public void publicarIncidenteAbierto(IncidentePublicado incidente) {
		log.info("Firebase apagado: no se publica el incidente {} ({}).", incidente.id(), incidente.estado());
	}

	@Override
	public void retirarIncidente(Long incidenteId) {
		log.info("Firebase apagado: no se retira el incidente {}.", incidenteId);
	}

	@Override
	public void publicarSeguimiento(SeguimientoPublicado seguimiento) {
		log.info("Firebase apagado: no se publica el seguimiento del incidente {} ({}, {} unidades).",
				seguimiento.incidenteId(), seguimiento.estado(), seguimiento.unidades().size());
	}

	@Override
	public void publicarPosicion(PosicionActualizada posicion) {
		log.debug("Firebase apagado: no se publica la posición de la ambulancia {}.", posicion.ambulanciaId());
	}

	@Override
	public void notificarNuevoIncidente(List<String> tokensPorCercania, IncidentePublicado incidente) {
		log.info("Firebase apagado: no se envía push del incidente {} a {} teléfonos.", incidente.id(),
				tokensPorCercania.size());
	}

}
