package com.uem.ambulancias.cobros.dto;

import java.math.BigDecimal;

import com.uem.ambulancias.cobros.domain.EstadoCobro;
import com.uem.ambulancias.cobros.domain.ModalidadPago;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/** Corregir un cobro: los valores nuevos y por qué. Lo anterior queda guardado. */
public record AjustarCobroRequest(

		@NotNull(message = "Elige quién paga.")
		ModalidadPago modalidad,

		@NotNull(message = "El importe es obligatorio.")
		@PositiveOrZero(message = "El importe no puede ser negativo.")
		@DecimalMax(value = "99999999.99", message = "El importe es demasiado alto.")
		@Digits(integer = 8, fraction = 2, message = "El importe va con dos decimales como mucho.")
		BigDecimal importe,

		@NotNull(message = "Elige el estado del cobro.")
		EstadoCobro estado,

		@NotBlank(message = "Escribe el motivo del ajuste.")
		@Size(max = 1000, message = "El motivo es demasiado largo.")
		String motivo) {
}
