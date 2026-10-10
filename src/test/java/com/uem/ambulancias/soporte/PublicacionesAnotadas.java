package com.uem.ambulancias.soporte;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import com.uem.ambulancias.emergencias.service.AvisoParaCiudadano;
import com.uem.ambulancias.emergencias.domain.MotivoCancelacionAtencion;
import com.uem.ambulancias.emergencias.service.AvisoDeTraslado;
import com.uem.ambulancias.emergencias.service.AvisoParaParamedico;
import com.uem.ambulancias.emergencias.service.IncidentePublicado;
import com.uem.ambulancias.emergencias.service.SeguimientoPublicado;
import com.uem.ambulancias.emergencias.service.UnidadPublicada;
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
	private final List<AvisoDeAsignacion> avisosDeIncidenteAsignado = new CopyOnWriteArrayList<>();
	private final List<VersionDeResumen> versionesDeResumen = new CopyOnWriteArrayList<>();
	private final List<AvisoATripulacion> avisosDeTraslado = new CopyOnWriteArrayList<>();
	private final List<AvisoParaParamedico> avisosAParamedicos = new CopyOnWriteArrayList<>();
	private final List<UnidadPublicada> unidades = new CopyOnWriteArrayList<>();
	private final List<PosicionActualizada> posiciones = new CopyOnWriteArrayList<>();
	private final List<Long> posicionesRetiradas = new CopyOnWriteArrayList<>();

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

	@Override
	public void notificarTrasladoAsignado(List<String> tokens, AvisoDeTraslado aviso) {
		avisosDeTraslado.add(new AvisoATripulacion(aviso.trasladoId(), "ASIGNADO", List.copyOf(tokens)));
	}

	@Override
	public void notificarTrasladoRetirado(List<String> tokens, AvisoDeTraslado aviso,
			MotivoCancelacionAtencion motivo) {
		avisosDeTraslado.add(new AvisoATripulacion(aviso.trasladoId(), "RETIRADO_" + motivo, List.copyOf(tokens)));
	}

	/**
	 * Lo que se le avisó a la tripulación de ese traslado, en orden: {@code ASIGNADO} o {@code RETIRADO_<motivo>},
	 * con los teléfonos a los que llegó.
	 */
	public List<String> avisosDeTrasladoA(String tokenPush, Long trasladoId) {
		return avisosDeTraslado.stream()
				.filter(aviso -> aviso.trasladoId().equals(trasladoId) && aviso.tokens().contains(tokenPush))
				.map(AvisoATripulacion::tipo)
				.toList();
	}

	@Override
	public void publicarResumenNuevo(Long incidenteId, int version) {
		versionesDeResumen.add(new VersionDeResumen(incidenteId, version));
	}

	@Override
	public void notificarParamedicos(List<AvisoParaParamedico> avisos) {
		avisosAParamedicos.addAll(avisos);
	}

	/** Las versiones del resumen de ese incidente que se avisaron por el canal en vivo, en orden. */
	public List<Integer> versionesDeResumenPublicadas(Long incidenteId) {
		return versionesDeResumen.stream()
				.filter(publicada -> publicada.incidenteId().equals(incidenteId))
				.map(VersionDeResumen::version)
				.toList();
	}

	/** Los push que recibió ese teléfono de paramédico, en orden. */
	public List<AvisoParaParamedico> avisosAlParamedico(String tokenPush) {
		return avisosAParamedicos.stream().filter(aviso -> aviso.tokenPush().equals(tokenPush)).toList();
	}

	@Override
	public void notificarIncidenteAsignado(List<String> tokens, IncidentePublicado incidente) {
		avisosDeIncidenteAsignado.add(new AvisoDeAsignacion(incidente.id(), List.copyOf(tokens)));
	}

	/** Los teléfonos de la tripulación avisados de que la central les mandó ese incidente. */
	public List<String> avisadosDelDespacho(Long incidenteId) {
		return avisosDeIncidenteAsignado.stream()
				.filter(aviso -> aviso.incidenteId().equals(incidenteId))
				.flatMap(aviso -> aviso.tokens().stream())
				.toList();
	}

	@Override
	public void publicarUnidad(UnidadPublicada unidad) {
		unidades.add(unidad);
	}

	@Override
	public void publicarPosicion(PosicionActualizada posicion) {
		posiciones.add(posicion);
	}

	@Override
	public void retirarPosicion(Long ambulanciaId) {
		posicionesRetiradas.add(ambulanciaId);
	}

	/** Lo último que se publicó de esa unidad: lo que está viendo el centro de control y su tripulación. */
	public UnidadPublicada ultimaUnidadPublicada(Long ambulanciaId) {
		return unidades.stream()
				.filter(unidad -> unidad.ambulanciaId().equals(ambulanciaId))
				.reduce((anterior, siguiente) -> siguiente)
				.orElseThrow(() -> new AssertionError("No se publicó nada de la ambulancia " + ambulanciaId));
	}

	/** Las posiciones en vivo de esa unidad que se publicaron para el mapa, en orden. */
	public List<PosicionActualizada> posicionesPublicadas(Long ambulanciaId) {
		return posiciones.stream().filter(posicion -> posicion.ambulanciaId().equals(ambulanciaId)).toList();
	}

	/** Si la posición de esa unidad se sacó del mapa. */
	public boolean posicionRetirada(Long ambulanciaId) {
		return posicionesRetiradas.contains(ambulanciaId);
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
		avisosDeIncidenteAsignado.clear();
		versionesDeResumen.clear();
		avisosDeTraslado.clear();
		avisosAParamedicos.clear();
		unidades.clear();
		posiciones.clear();
		posicionesRetiradas.clear();
	}

	private record PosicionEnSeguimiento(Long incidenteId, PosicionActualizada posicion) {
	}

	private record AvisoDeAsignacion(Long incidenteId, List<String> tokens) {
	}

	private record VersionDeResumen(Long incidenteId, int version) {
	}

	private record AvisoATripulacion(Long trasladoId, String tipo, List<String> tokens) {
	}

}
