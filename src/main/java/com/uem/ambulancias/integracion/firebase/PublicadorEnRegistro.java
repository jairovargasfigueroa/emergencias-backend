package com.uem.ambulancias.integracion.firebase;

import java.util.List;

import com.uem.ambulancias.emergencias.domain.MotivoCancelacionAtencion;
import com.uem.ambulancias.emergencias.service.AvisoDeTraslado;
import com.uem.ambulancias.emergencias.service.AvisoParaCiudadano;
import com.uem.ambulancias.emergencias.service.IncidentePublicado;
import com.uem.ambulancias.emergencias.service.NotificadorPush;
import com.uem.ambulancias.emergencias.service.PublicadorDeIncidentes;
import com.uem.ambulancias.emergencias.service.PublicadorDeUnidades;
import com.uem.ambulancias.emergencias.service.SeguimientoPublicado;
import com.uem.ambulancias.emergencias.service.UnidadPublicada;
import com.uem.ambulancias.flota.service.PosicionActualizada;
import com.uem.ambulancias.flota.service.PublicadorDePosiciones;

import lombok.extern.slf4j.Slf4j;

/**
 * Reemplazo cuando Firebase está apagado ({@code sga.firebase.habilitado=false}): solo deja registro.
 */
@Slf4j
public class PublicadorEnRegistro
		implements PublicadorDeIncidentes, PublicadorDePosiciones, PublicadorDeUnidades, NotificadorPush {

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
	public void publicarPosicionEnSeguimiento(Long incidenteId, PosicionActualizada posicion) {
		log.debug("Firebase apagado: no se copia al seguimiento del incidente {} la posición de la ambulancia {}.",
				incidenteId, posicion.ambulanciaId());
	}

	@Override
	public void publicarPosicion(PosicionActualizada posicion) {
		log.debug("Firebase apagado: no se publica la posición de la ambulancia {}.", posicion.ambulanciaId());
	}

	@Override
	public void retirarPosicion(Long ambulanciaId) {
		log.info("Firebase apagado: no se retira la posición de la ambulancia {}.", ambulanciaId);
	}

	@Override
	public void publicarUnidad(UnidadPublicada unidad) {
		log.debug("Firebase apagado: no se publica el estado de la ambulancia {} ({}).", unidad.ambulanciaId(),
				unidad.estado());
	}

	@Override
	public void notificarNuevoIncidente(List<String> tokensPorCercania, IncidentePublicado incidente) {
		log.info("Firebase apagado: no se envía push del incidente {} a {} teléfonos.", incidente.id(),
				tokensPorCercania.size());
	}

	@Override
	public void notificarIncidenteAsignado(List<String> tokens, IncidentePublicado incidente) {
		log.info("Firebase apagado: no se avisa a {} teléfonos que los mandaron al incidente {}.", tokens.size(),
				incidente.id());
	}

	@Override
	public void notificarCiudadanos(List<AvisoParaCiudadano> avisos) {
		avisos.forEach(aviso -> log.info("Firebase apagado: no se le avisa a un ciudadano \"{}\" ({}).", aviso.titulo(),
				aviso.datos()));
	}

	@Override
	public void notificarTrasladoAsignado(List<String> tokens, AvisoDeTraslado aviso) {
		log.info("Firebase apagado: no se envía push del traslado {} a {} teléfonos de la tripulación.",
				aviso.trasladoId(), tokens.size());
	}

	@Override
	public void notificarTrasladoRetirado(List<String> tokens, AvisoDeTraslado aviso,
			MotivoCancelacionAtencion motivo) {
		log.info("Firebase apagado: no se avisa a {} teléfonos que el traslado {} ya no es suyo ({}).",
				tokens.size(), aviso.trasladoId(), motivo);
	}

}
