package com.uem.ambulancias.emergencias.dto;

import jakarta.validation.constraints.Size;

/**
 * Lo que se puede corregir hasta con la unidad en camino: cómo encontrar la puerta y con quién hablar. Nada de
 * esto cambia una decisión que el sistema ya tomó, y es justo lo que más se arregla sobre la hora.
 */
public record DetallesTrasladoRequest(

		@Size(max = 255, message = "La referencia es demasiado larga.")
		String origenReferencia,

		@Size(max = 255, message = "El nombre del contacto es demasiado largo.")
		String contactoNombre,

		@Size(max = 30, message = "El teléfono del contacto es demasiado largo.")
		String contactoTelefono,

		@Size(max = 2000, message = "Las observaciones son demasiado largas.")
		String observaciones) {
}
