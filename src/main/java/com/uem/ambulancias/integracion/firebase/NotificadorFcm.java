package com.uem.ambulancias.integracion.firebase;

import java.util.List;

import com.google.firebase.messaging.AndroidConfig;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import com.uem.ambulancias.emergencias.service.IncidentePublicado;
import com.uem.ambulancias.emergencias.service.NotificadorPush;

import lombok.RequiredArgsConstructor;

/**
 * Envía el push con Firebase Cloud Messaging. El dato {@code incidenteId} permite a la app abrir el incidente.
 */
@RequiredArgsConstructor
public class NotificadorFcm implements NotificadorPush {

	private static final int LARGO_MAXIMO_DESCRIPCION = 100;

	private final FirebaseMessaging mensajeria;

	@Override
	public void notificarNuevoIncidente(List<String> tokensPorCercania, IncidentePublicado incidente) {
		if (tokensPorCercania.isEmpty()) {
			return;
		}
		Notification notificacion = Notification.builder()
				.setTitle("Nuevo incidente")
				.setBody(resumen(incidente))
				.build();
		List<Message> mensajes = tokensPorCercania.stream()
				.map(token -> Message.builder()
						.setToken(token)
						.setNotification(notificacion)
						.putData("incidenteId", String.valueOf(incidente.id()))
						.setAndroidConfig(AndroidConfig.builder().setPriority(AndroidConfig.Priority.HIGH).build())
						.build())
				.toList();
		EscriturasFirebase.registrarFallo(mensajeria.sendEachAsync(mensajes),
				"enviar el push del incidente " + incidente.id());
	}

	private static String resumen(IncidentePublicado incidente) {
		Integer cantidad = incidente.cantidadAfectados();
		String afectados = cantidad == null
				? "Afectados sin reportar"
				: cantidad + (cantidad == 1 ? " afectado reportado" : " afectados reportados");
		if (incidente.descripciones().isEmpty()) {
			return afectados;
		}
		String descripcion = incidente.descripciones().getFirst();
		if (descripcion.length() > LARGO_MAXIMO_DESCRIPCION) {
			descripcion = descripcion.substring(0, LARGO_MAXIMO_DESCRIPCION) + "…";
		}
		return afectados + " · " + descripcion;
	}

}
