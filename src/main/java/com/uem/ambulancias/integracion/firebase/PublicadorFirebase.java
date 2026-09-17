package com.uem.ambulancias.integracion.firebase;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.uem.ambulancias.emergencias.service.IncidentePublicado;
import com.uem.ambulancias.emergencias.service.PublicadorDeIncidentes;
import com.uem.ambulancias.flota.service.PosicionActualizada;
import com.uem.ambulancias.flota.service.PublicadorDePosiciones;

import lombok.RequiredArgsConstructor;

/**
 * Publica en Realtime Database. Solo el servidor escribe; las apps únicamente escuchan. Las fechas viajan en
 * ISO-8601, igual que en la API REST.
 */
@RequiredArgsConstructor
public class PublicadorFirebase implements PublicadorDeIncidentes, PublicadorDePosiciones {

	/** Un hijo por incidente abierto, con su id como clave. Lo escuchan las apps del paramédico. */
	static final String INCIDENTES_ABIERTOS = "incidentes-abiertos";

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
	public void publicarPosicion(PosicionActualizada posicion) {
		Map<String, Object> valores = Map.of(
				"latitud", posicion.latitud(),
				"longitud", posicion.longitud(),
				"en", posicion.momento().toString());
		EscriturasFirebase.registrarFallo(nodo(POSICIONES, posicion.ambulanciaId()).setValueAsync(valores),
				"publicar la posición de la ambulancia " + posicion.ambulanciaId());
	}

	private DatabaseReference nodo(String raiz, Long id) {
		return baseDatos.getReference(raiz).child(String.valueOf(id));
	}

}
