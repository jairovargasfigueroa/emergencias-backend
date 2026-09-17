package com.uem.ambulancias.comun.web;

import java.util.List;

import org.springframework.data.domain.Page;

/**
 * Una página de resultados con un JSON estable: el {@code Page} de Spring Data no se serializa directo.
 * {@code pagina} empieza en 0.
 */
public record PaginaResponse<T>(List<T> contenido, int pagina, int tamano, long totalElementos, int totalPaginas) {

	public static <T> PaginaResponse<T> de(Page<T> pagina) {
		return new PaginaResponse<>(pagina.getContent(), pagina.getNumber(), pagina.getSize(),
				pagina.getTotalElements(), pagina.getTotalPages());
	}

}
