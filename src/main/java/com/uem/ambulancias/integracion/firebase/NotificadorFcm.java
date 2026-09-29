package com.uem.ambulancias.integracion.firebase;

import java.time.format.DateTimeFormatter;
import java.util.List;

import com.google.firebase.messaging.AndroidConfig;
import com.google.firebase.messaging.AndroidNotification;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import com.uem.ambulancias.emergencias.domain.MotivoCancelacionAtencion;
import com.uem.ambulancias.emergencias.service.AvisoDeTraslado;
import com.uem.ambulancias.emergencias.service.AvisoParaCiudadano;
import com.uem.ambulancias.emergencias.service.AvisoParaParamedico;
import com.uem.ambulancias.emergencias.service.IncidentePublicado;
import com.uem.ambulancias.emergencias.service.NotificadorPush;

import lombok.RequiredArgsConstructor;

/**
 * Envía el push con Firebase Cloud Messaging. Los datos {@code incidenteId} y {@code trasladoId} permiten a la
 * app abrir directo lo que se avisa; en los traslados, {@code tipo} le dice además si se lo dieron o se lo sacaron.
 */
@RequiredArgsConstructor
public class NotificadorFcm implements NotificadorPush {

	private static final int LARGO_MAXIMO_TEXTO = 100;

	private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

	/** El canal de Android de la app del ciudadano. */
	private static final String CANAL_CIUDADANO = "avisos";

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
	public void notificarIncidenteAsignado(List<String> tokens, IncidentePublicado incidente) {
		if (tokens.isEmpty()) {
			return;
		}
		Notification notificacion = Notification.builder()
				.setTitle("Te enviaron a una emergencia")
				.setBody(resumen(incidente))
				.build();
		List<Message> mensajes = tokens.stream()
				.map(token -> Message.builder()
						.setToken(token)
						.setNotification(notificacion)
						.putData("incidenteId", String.valueOf(incidente.id()))
						.putData("tipo", "INCIDENTE_ASIGNADO")
						.setAndroidConfig(AndroidConfig.builder().setPriority(AndroidConfig.Priority.HIGH).build())
						.build())
				.toList();
		EscriturasFirebase.registrarFallo(mensajeria.sendEachAsync(mensajes),
				"enviar el push de despacho del incidente " + incidente.id());
	}

	@Override
	public void notificarCiudadanos(List<AvisoParaCiudadano> avisos) {
		if (avisos.isEmpty()) {
			return;
		}
		// El canal es el que crea la app del ciudadano: con la app abierta, sin esto el aviso cae en un canal genérico.
		AndroidConfig android = AndroidConfig.builder()
				.setPriority(AndroidConfig.Priority.HIGH)
				.setNotification(AndroidNotification.builder().setChannelId(CANAL_CIUDADANO).build())
				.build();
		List<Message> mensajes = avisos.stream()
				.map(aviso -> Message.builder()
						.setToken(aviso.tokenPush())
						.setNotification(Notification.builder().setTitle(aviso.titulo()).setBody(aviso.cuerpo()).build())
						.putAllData(aviso.datos())
						.setAndroidConfig(android)
						.build())
				.toList();
		EscriturasFirebase.registrarFallo(mensajeria.sendEachAsync(mensajes),
				"enviar " + mensajes.size() + " avisos a ciudadanos (" + avisos.getFirst().titulo() + ")");
	}

	@Override
	public void notificarTrasladoAsignado(List<String> tokens, AvisoDeTraslado aviso) {
		enviarATripulacion(tokens, aviso, "TRASLADO_ASIGNADO", "Traslado asignado", resumen(aviso));
	}

	@Override
	public void notificarTrasladoRetirado(List<String> tokens, AvisoDeTraslado aviso,
			MotivoCancelacionAtencion motivo) {
		switch (motivo) {
			case CANCELADA_POR_SOLICITANTE -> enviarATripulacion(tokens, aviso, "TRASLADO_CANCELADO",
					"Traslado cancelado", aviso.pasajero() + " · Lo canceló quien lo pidió. Tu unidad quedó libre.");
			case REASIGNADA -> enviarATripulacion(tokens, aviso, "TRASLADO_REASIGNADO", "Traslado reasignado",
					aviso.pasajero() + " · Se lo pasaron a otra unidad. Tu unidad quedó libre.");
			// La central pudo dejar la unidad fuera de servicio: no se promete que quedó libre.
			default -> enviarATripulacion(tokens, aviso, "TRASLADO_CERRADO_POR_CENTRAL",
					"La central cerró tu traslado",
					aviso.pasajero() + " · Lo cerró la central. Revisa en la app cómo quedó tu unidad.");
		}
	}

	@Override
	public void notificarParamedicos(List<AvisoParaParamedico> avisos) {
		if (avisos.isEmpty()) {
			return;
		}
		AndroidConfig android = AndroidConfig.builder().setPriority(AndroidConfig.Priority.HIGH).build();
		List<Message> mensajes = avisos.stream()
				.map(aviso -> Message.builder()
						.setToken(aviso.tokenPush())
						.setNotification(Notification.builder().setTitle(aviso.titulo()).setBody(aviso.cuerpo()).build())
						.putAllData(aviso.datos())
						.setAndroidConfig(android)
						.build())
				.toList();
		EscriturasFirebase.registrarFallo(mensajeria.sendEachAsync(mensajes),
				"enviar " + mensajes.size() + " avisos a paramédicos (" + avisos.getFirst().titulo() + ")");
	}

	private void enviarATripulacion(List<String> tokens, AvisoDeTraslado aviso, String tipo, String titulo,
			String cuerpo) {
		if (tokens.isEmpty()) {
			return;
		}
		Notification notificacion = Notification.builder().setTitle(titulo).setBody(cuerpo).build();
		List<Message> mensajes = tokens.stream()
				.map(token -> Message.builder()
						.setToken(token)
						.setNotification(notificacion)
						.putData("trasladoId", String.valueOf(aviso.trasladoId()))
						.putData("tipo", tipo)
						.setAndroidConfig(AndroidConfig.builder().setPriority(AndroidConfig.Priority.HIGH).build())
						.build())
				.toList();
		EscriturasFirebase.registrarFallo(mensajeria.sendEachAsync(mensajes),
				"enviar el push " + tipo + " del traslado " + aviso.trasladoId());
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
