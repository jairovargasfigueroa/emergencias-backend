package com.uem.ambulancias.emergencias.dto;

import com.uem.ambulancias.emergencias.domain.CentroSalud;

/**
 * Un centro de salud. Lleva su punto porque los clientes lo necesitan para ubicarlo en el mapa y para usarlo como
 * origen o destino de un traslado sin tener que marcarlo a mano.
 */
public record CentroSaludResponse(Long id, String nombre, String direccion, double latitud, double longitud) {

	public static CentroSaludResponse de(CentroSalud centroSalud) {
		return new CentroSaludResponse(centroSalud.getId(), centroSalud.getNombre(), centroSalud.getDireccion(),
				centroSalud.getUbicacion().getY(), centroSalud.getUbicacion().getX());
	}

}
