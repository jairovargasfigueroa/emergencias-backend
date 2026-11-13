package com.uem.ambulancias.cobros.dto;

import java.math.BigDecimal;

import com.uem.ambulancias.cobros.domain.EstadoCobro;
import com.uem.ambulancias.cobros.domain.ModalidadPago;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

/** El cobro de una atención terminada. El estado es pendiente o pagado: anulado se llega anulándolo. */
public record RegistrarCobroRequest(

		@NotNull(message = "Falta el servicio.")
		Long atencionId,

		@NotNull(message = "Elige quién paga.")
		ModalidadPago modalidad,

		@NotNull(message = "El importe es obligatorio.")
		@PositiveOrZero(message = "El importe no puede ser negativo.")
		@DecimalMax(value = "99999999.99", message = "El importe es demasiado alto.")
		@Digits(integer = 8, fraction = 2, message = "El importe va con dos decimales como mucho.")
		BigDecimal importe,

		@NotNull(message = "Elige el estado del cobro.")
		EstadoCobro estado) {
}
