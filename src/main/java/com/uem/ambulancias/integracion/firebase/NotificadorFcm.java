package com.uem.ambulancias.integracion.firebase;

import java.time.format.DateTimeFormatter;
import java.util.List;

import com.google.firebase.messaging.AndroidConfig;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import com.uem.ambulancias.emergencias.service.AvisoDeTraslado;
import com.uem.ambulancias.emergencias.service.IncidentePublicado;
import com.uem.ambulancias.emergencias.service.NotificadorPush;

import lombok.RequiredArgsConstructor;

/**
 * Envía el push con Firebase Cloud Messaging. Los datos {@code incidenteId} y {@code trasladoId} permiten a la
 * app abrir directo lo que se avisa.
 */
@RequiredArgsConstructor
public class NotificadorFcm implements NotificadorPush {

	private static final int LARGO_MAXIMO_TEXTO = 100;

	private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

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

	@Override
	public void notificarTrasladoAsignado(String tokenPush, AvisoDeTraslado aviso) {
		Message mensaje = Message.builder()
				.setToken(tokenPush)
				.setNotification(Notification.builder()
						.setTitle("Traslado asignado")
						.setBody(resumen(aviso))
						.build())
				.putData("trasladoId", String.valueOf(aviso.trasladoId()))
				.setAndroidConfig(AndroidConfig.builder().setPriority(AndroidConfig.Priority.HIGH).build())
				.build();
		EscriturasFirebase.registrarFallo(mensajeria.sendAsync(mensaje),
				"enviar el push del traslado " + aviso.trasladoId());
	}

	private static String resumen(IncidentePublicado incidente) {
		Integer cantidad = incidente.cantidadAfectados();
		String afectados = cantidad == null
				? "Afectados sin reportar"
				: cantidad + (cantidad == 1 ? " afectado reportado" : " afectados reportados");
		if (incidente.descripciones().isEmpty()) {
			return afectados;
		}
		return afectados + " · " + recortar(incidente.descripciones().getFirst());
	}

	/**
	 * Esto se lee de un vistazo y muchas veces al volante, así que va lo que decide el viaje y nada más: a quién
	 * se lleva, para cuándo, a dónde y por dónde se entra. Lo que falta no deja hueco: se omite.
	 */
	private static String resumen(AvisoDeTraslado aviso) {
		StringBuilder resumen = new StringBuilder(aviso.pasajero());
		if (aviso.horaCita() != null) {
			resumen.append(" · cita ").append(HORA.format(aviso.horaCita()));
		}
		if (aviso.destino() != null) {
			resumen.append(" · ").append(recortar(aviso.destino()));
		}
		if (aviso.referenciaOrigen() != null) {
			resumen.append(" · ").append(recortar(aviso.referenciaOrigen()));
		}
		return resumen.toString();
	}

	/** Un texto largo empuja al resto fuera de la notificación, que es una sola línea en la pantalla bloqueada. */
	private static String recortar(String texto) {
		return texto.length() > LARGO_MAXIMO_TEXTO ? texto.substring(0, LARGO_MAXIMO_TEXTO) + "…" : texto;
	}

}
