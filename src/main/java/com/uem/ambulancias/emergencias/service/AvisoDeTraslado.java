package com.uem.ambulancias.emergencias.service;

import java.time.LocalTime;

/**
 * Lo que necesita saber el paramédico del traslado que le tocó sin tener que abrir la app: a quién lleva, para
 * qué hora, a dónde y por dónde se entra a buscarlo.
 *
 * <p>La hora viene ya resuelta en la zona de la empresa: el servidor puede estar en UTC y nadie maneja en UTC.
 * Todo salvo el pasajero puede faltar, porque un pedido inmediato no tiene cita y el destino puede ser un punto
 * del mapa en vez de un centro de salud.
 */
public record AvisoDeTraslado(
		Long trasladoId,
		String pasajero,
		LocalTime horaCita,
		String destino,
		String referenciaOrigen) {
}
