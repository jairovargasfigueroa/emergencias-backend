package com.uem.ambulancias.integracion.firebase;

import com.uem.ambulancias.emergencias.service.IncidentePublicado;
import com.uem.ambulancias.emergencias.service.PublicadorDeIncidentes;
import com.uem.ambulancias.flota.service.PosicionActualizada;
import com.uem.ambulancias.flota.service.PublicadorDePosiciones;

import lombok.extern.slf4j.Slf4j;

/**
 * Reemplazo cuando Firebase está apagado ({@code sga.firebase.habilitado=false}): solo deja registro.
 */
@Slf4j
public class PublicadorEnRegistro implements PublicadorDeIncidentes, PublicadorDePosiciones {

	@Override
	public void publicarIncidenteAbierto(IncidentePublicado incidente) {
		log.info("Firebase apagado: no se publica el incidente {} ({}).", incidente.id(), incidente.estado());
	}

	@Override
	public void retirarIncidente(Long incidenteId) {
		log.info("Firebase apagado: no se retira el incidente {}.", incidenteId);
	}

	@Override
	public void publicarPosicion(PosicionActualizada posicion) {
		log.debug("Firebase apagado: no se publica la posición de la ambulancia {}.", posicion.ambulanciaId());
	}

}
