package com.uem.ambulancias.emergencias.dto;

import java.time.Instant;

import com.uem.ambulancias.usuarios.domain.Usuario;

/** Una fila de la lista de mal uso: el ciudadano, si su cuenta sigue activa y cuántas falsas alarmas tiene. */
public record CiudadanoConFalsasAlarmasResponse(Long id, String nombre, String telefono, boolean activo,
		long falsasAlarmas, Instant ultimaFalsaAlarma) {

	public static CiudadanoConFalsasAlarmasResponse de(Usuario ciudadano, long falsasAlarmas, Instant ultima) {
		return new CiudadanoConFalsasAlarmasResponse(ciudadano.getId(), ciudadano.getNombreCompleto(),
				ciudadano.getTelefono(), ciudadano.isActivo(), falsasAlarmas, ultima);
	}

}
