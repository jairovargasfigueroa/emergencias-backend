package com.uem.ambulancias.soporte;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import com.uem.ambulancias.emergencias.service.AvisoParaCiudadano;
import com.uem.ambulancias.emergencias.service.IncidentePublicado;
import com.uem.ambulancias.emergencias.service.SeguimientoPublicado;
import com.uem.ambulancias.flota.service.PosicionActualizada;
import com.uem.ambulancias.integracion.firebase.PublicadorEnRegistro;

/**
 * Reemplaza a Firebase en las pruebas: en vez de publicar, anota lo que se habría publicado para poder revisarlo.
 * Lo que no se anota sigue yendo al registro, como con Firebase apagado.
 */
public class PublicacionesAnotadas extends PublicadorEnRegistro {

	private final List<IncidentePublicado> incidentesAbiertos = new CopyOnWriteArrayList<>();
	private final List<Long> incidentesRetirados = new CopyOnWriteArrayList<>();
	private final List<SeguimientoPublicado> seguimientos = new CopyOnWriteArrayList<>();
	private final List<List<String>> avisosDeIncidenteNuevo = new CopyOnWriteArrayList<>();
	private final List<PosicionEnSeguimiento> posicionesEnSeguimiento = new CopyOnWriteArrayList<>();
	private final List<AvisoParaCiudadano> avisosACiudadanos = new CopyOnWriteArrayList<>();

	@Override
	public void publicarIncidenteAbierto(IncidentePublicado incidente) {
		incidentesAbiertos.add(incidente);
	}

	@Override
	public void retirarIncidente(Long incidenteId) {
		incidentesRetirados.add(incidenteId);
	}

	@Override
	public void publicarSeguimiento(SeguimientoPublicado seguimiento) {
		seguimientos.add(seguimiento);
	}

	@Override
	public void notificarNuevoIncidente(List<String> tokensPorCercania, IncidentePublicado incidente) {
		avisosDeIncidenteNuevo.add(List.copyOf(tokensPorCercania));
	}

	@Override
	public void publicarPosicionEnSeguimiento(Long incidenteId, PosicionActualizada posicion) {
		posicionesEnSeguimiento.add(new PosicionEnSeguimiento(incidenteId, posicion));
	}

	@Override
	public void notificarCiudadanos(List<AvisoParaCiudadano> avisos) {
		avisosACiudadanos.addAll(avisos);
	}

	/** Las posiciones en vivo que se copiaron al seguimiento de ese incidente, en orden. */
	public List<PosicionActualizada> posicionesEnSeguimiento(Long incidenteId) {
		return posicionesEnSeguimiento.stream()
				.filter(copia -> copia.incidenteId().equals(incidenteId))
				.map(PosicionEnSeguimiento::posicion)
				.toList();
	}

	/** Las posiciones en vivo copiadas a cualquier seguimiento. */
	public int cantidadDePosicionesEnSeguimiento() {
		return posicionesEnSeguimiento.size();
	}

	/** Los push que recibió ese teléfono de ciudadano, en orden. */
	public List<AvisoParaCiudadano> avisosAlCiudadano(String tokenPush) {
		return avisosACiudadanos.stream().filter(aviso -> aviso.tokenPush().equals(tokenPush)).toList();
	}

	public List<IncidentePublicado> incidentesAbiertos(Long incidenteId) {
		return incidentesAbiertos.stream().filter(publicado -> publicado.id().equals(incidenteId)).toList();
	}

	public boolean fueRetirado(Long incidenteId) {
		return incidentesRetirados.contains(incidenteId);
	}

	/** El último seguimiento publicado de ese incidente: lo que está viendo el ciudadano. */
	public SeguimientoPublicado ultimoSeguimiento(Long incidenteId) {
		return seguimientos.stream()
				.filter(seguimiento -> seguimiento.incidenteId().equals(incidenteId))
				.reduce((anterior, siguiente) -> siguiente)
				.orElseThrow(() -> new AssertionError("No se publicó ningún seguimiento del incidente " + incidenteId));
	}

	/** Los teléfonos avisados del último incidente nuevo, en el orden en que se avisaron. */
	public List<String> ultimoAvisoDeIncidenteNuevo() {
		if (avisosDeIncidenteNuevo.isEmpty()) {
			throw new AssertionError("No se avisó ningún incidente nuevo");
		}
		return avisosDeIncidenteNuevo.getLast();
	}

	/** Cuántas veces se avisó por push de un incidente nuevo, desde que empezó la prueba. */
	public int cantidadDeAvisosDeIncidenteNuevo() {
		return avisosDeIncidenteNuevo.size();
	}

	/** Cada prueba empieza sin nada anotado. */
	public void olvidar() {
		incidentesAbiertos.clear();
		incidentesRetirados.clear();
		seguimientos.clear();
		avisosDeIncidenteNuevo.clear();
		posicionesEnSeguimiento.clear();
		avisosACiudadanos.clear();
	}

	private record PosicionEnSeguimiento(Long incidenteId, PosicionActualizada posicion) {
	}

}
