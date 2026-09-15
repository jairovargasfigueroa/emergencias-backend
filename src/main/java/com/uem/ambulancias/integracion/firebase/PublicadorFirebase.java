package com.uem.ambulancias.integracion.firebase;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.uem.ambulancias.emergencias.service.IncidentePublicado;
import com.uem.ambulancias.emergencias.service.PublicadorDeIncidentes;
import com.uem.ambulancias.emergencias.service.SeguimientoPublicado;
import com.uem.ambulancias.flota.service.PosicionActualizada;
import com.uem.ambulancias.flota.service.PublicadorDePosiciones;

import lombok.RequiredArgsConstructor;

/**
 * Publica en Realtime Database. Solo el servidor escribe; las apps únicamente escuchan. Las fechas viajan en
 * ISO-8601, igual que en la API REST. Los hijos usan ids numéricos como clave: las apps deben recorrerlos con
 * {@code forEach} y no con {@code val()} del nodo padre, que puede convertirlos en arreglo.
 */
@RequiredArgsConstructor
public class PublicadorFirebase implements PublicadorDeIncidentes, PublicadorDePosiciones {

	/** Un hijo por incidente abierto, con su id como clave. Lo escuchan las apps del paramédico. */
	static final String INCIDENTES_ABIERTOS = "incidentes-abiertos";

	/** Un hijo por incidente, con su id como clave. Lo escucha la app del ciudadano que lo emitió. */
	static final String SEGUIMIENTO = "seguimiento";

	/** Posición en vivo de cada ambulancia en servicio, con su id como clave. */
	static final String POSICIONES = "posiciones";

	private final FirebaseDatabase baseDatos;

	@Override
	public void publicarIncidenteAbierto(IncidentePublicado incidente) {
		Map<String, Object> valores = new HashMap<>();
		valores.put("id", incidente.id());
		valores.put("latitud", incidente.latitud());
		valores.put("longitud", incidente.longitud());
		valores.put("estado", incidente.estado().name());
		valores.put("fechaHoraCreacion", incidente.fechaHoraCreacion().toString());
		if (incidente.cantidadAfectados() != null) {
			valores.put("cantidadAfectados", incidente.cantidadAfectados());
		}
		valores.put("descripciones", incidente.descripciones());
		valores.put("unidadesAcudiendo", incidente.unidadesAcudiendo());
		valores.put("actualizadoEn", Instant.now().toString());
		EscriturasFirebase.registrarFallo(nodo(INCIDENTES_ABIERTOS, incidente.id()).setValueAsync(valores),
				"publicar el incidente " + incidente.id());
	}

	@Override
	public void retirarIncidente(Long incidenteId) {
		EscriturasFirebase.registrarFallo(nodo(INCIDENTES_ABIERTOS, incidenteId).removeValueAsync(),
				"retirar el incidente " + incidenteId);
	}

	@Override
	public void publicarSeguimiento(SeguimientoPublicado seguimiento) {
		Map<String, Object> unidades = new HashMap<>();
		for (SeguimientoPublicado.Unidad unidad : seguimiento.unidades()) {
			Map<String, Object> valoresUnidad = new HashMap<>();
			valoresUnidad.put("placa", unidad.placa());
			valoresUnidad.put("estado", unidad.estado().name());
			if (unidad.latitud() != null && unidad.longitud() != null) {
				valoresUnidad.put("posicion", posicion(unidad.latitud(), unidad.longitud(), unidad.posicionEn()));
			}
			unidades.put(String.valueOf(unidad.ambulanciaId()), valoresUnidad);
		}

		Map<String, Object> valores = new HashMap<>();
		valores.put("incidenteId", seguimiento.incidenteId());
		valores.put("estado", seguimiento.estado().name());
		valores.put("actualizadoEn", Instant.now().toString());
		valores.put("unidades", unidades);
		EscriturasFirebase.registrarFallo(nodo(SEGUIMIENTO, seguimiento.incidenteId()).setValueAsync(valores),
				"publicar el seguimiento del incidente " + seguimiento.incidenteId());
	}

	@Override
	public void publicarPosicion(PosicionActualizada posicion) {
		EscriturasFirebase.registrarFallo(
				nodo(POSICIONES, posicion.ambulanciaId())
						.setValueAsync(posicion(posicion.latitud(), posicion.longitud(), posicion.momento())),
				"publicar la posición de la ambulancia " + posicion.ambulanciaId());
	}

	@Override
	public void publicarPosicionEnSeguimiento(Long incidenteId, PosicionActualizada posicion) {
		DatabaseReference nodoPosicion = nodo(SEGUIMIENTO, incidenteId)
				.child("unidades")
				.child(String.valueOf(posicion.ambulanciaId()))
				.child("posicion");
		EscriturasFirebase.registrarFallo(
				nodoPosicion.setValueAsync(posicion(posicion.latitud(), posicion.longitud(), posicion.momento())),
				"copiar al seguimiento del incidente " + incidenteId + " la posición de la ambulancia "
						+ posicion.ambulanciaId());
	}

	private DatabaseReference nodo(String raiz, Long id) {
		return baseDatos.getReference(raiz).child(String.valueOf(id));
	}

	private static Map<String, Object> posicion(double latitud, double longitud, Instant en) {
		Map<String, Object> valores = new HashMap<>();
		valores.put("latitud", latitud);
		valores.put("longitud", longitud);
		if (en != null) {
			valores.put("en", en.toString());
		}
		return valores;
	}

}
