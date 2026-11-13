package com.uem.ambulancias.cobros.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.uem.ambulancias.cobros.domain.TipoServicio;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record RegistrarTarifaRequest(

		@NotNull(message = "Elige el tipo de servicio.")
		TipoServicio tipoServicio,

		@NotNull(message = "El monto es obligatorio.")
		@PositiveOrZero(message = "El monto no puede ser negativo.")
		@DecimalMax(value = "99999999.99", message = "El monto es demasiado alto.")
		@Digits(integer = 8, fraction = 2, message = "El monto va con dos decimales como mucho.")
		BigDecimal monto,

		@NotNull(message = "Indica desde qué día rige.")
		LocalDate vigenteDesde) {
}
