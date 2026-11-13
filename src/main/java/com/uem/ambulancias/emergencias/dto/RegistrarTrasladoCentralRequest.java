package com.uem.ambulancias.emergencias.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Un traslado que alguien pidió por teléfono y la central carga en el panel. El viaje se describe igual que en la
 * app; lo que cambia es quién viaja, que no tiene cuenta: se lo identifica por nombre y teléfono.
 *
 * <p>{@code confirmarDuplicado} va en verdadero cuando la central ya vio el aviso de un pedido parecido y confirma
 * que es otro viaje. {@code pedido.pasajeroId} no se usa.
 */
public record RegistrarTrasladoCentralRequest(

		@NotBlank(message = "El nombre de quien viaja es obligatorio.")
		@Size(max = 255, message = "El nombre es demasiado largo.")
		String pasajeroNombre,

		@NotBlank(message = "El teléfono de contacto es obligatorio.")
		@Pattern(regexp = "\\+?[0-9][0-9 -]{6,28}", message = "El teléfono no es válido.")
		String pasajeroTelefono,

		boolean confirmarDuplicado,

		@NotNull(message = "Faltan los datos del viaje.")
		@Valid
		RegistrarTrasladoRequest pedido) {
}
