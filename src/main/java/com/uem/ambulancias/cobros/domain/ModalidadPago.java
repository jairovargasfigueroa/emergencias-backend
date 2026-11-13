package com.uem.ambulancias.cobros.domain;

/** Quién paga el servicio. */
public enum ModalidadPago {

	/** Lo cubre un seguro médico o prepago. */
	SEGURO_MEDICO,
	/** Accidente de tránsito: lo cubre el Seguro Obligatorio de Accidentes de Tránsito. */
	SOAT,
	/** Lo paga el paciente o su familia. */
	PAGO_DIRECTO

}
