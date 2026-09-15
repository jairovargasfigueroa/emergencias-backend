package com.uem.ambulancias.integracion.firebase;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import com.google.api.core.ApiFuture;
import com.google.api.core.ApiFutureCallback;
import com.google.api.core.ApiFutures;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.uem.ambulancias.emergencias.service.IncidentePublicado;
import com.uem.ambulancias.emergencias.service.PublicadorTiempoReal;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Publica en Realtime Database. Las fechas viajan en ISO-8601, igual que en la API REST.
 */
@Slf4j
@RequiredArgsConstructor
public class PublicadorFirebase implements PublicadorTiempoReal {

	/** Nodo que escuchan las apps del paramédico: un hijo por incidente abierto, con su id como clave. */
	static final String INCIDENTES_ABIERTOS = "incidentes-abiertos";

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
		registrarFallo(nodoIncidente(incidente.id()).setValueAsync(valores), "publicar", incidente.id());
	}

	@Override
	public void retirarIncidente(Long incidenteId) {
		registrarFallo(nodoIncidente(incidenteId).removeValueAsync(), "retirar", incidenteId);
	}

	private DatabaseReference nodoIncidente(Long incidenteId) {
		return baseDatos.getReference(INCIDENTES_ABIERTOS).child(String.valueOf(incidenteId));
	}

	private static void registrarFallo(ApiFuture<Void> escritura, String accion, Long incidenteId) {
		ApiFutures.addCallback(escritura, new ApiFutureCallback<>() {

			@Override
			public void onFailure(Throwable error) {
				log.error("No se pudo {} el incidente {} en Firebase.", accion, incidenteId, error);
			}

			@Override
			public void onSuccess(Void resultado) {
				// Nada que hacer: las apps reciben el cambio por su listener.
			}

		}, MoreExecutors.directExecutor());
	}

}
