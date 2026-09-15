package com.uem.ambulancias.comun.error;

/**
 * Códigos que viajan en el campo {@code codigo} de las respuestas de error. Los clientes deciden qué
 * mostrar según este código, no según el texto.
 */
public enum CodigoError {

	VALIDACION,
	NO_ENCONTRADO,
	TRANSICION_INVALIDA,
	PLACA_DUPLICADA,
	AMBULANCIA_INACTIVA,
	AMBULANCIA_EN_ATENCION,
	PARAMEDICO_INACTIVO,
	YA_ASIGNADO,
	REASIGNACION_REQUIERE_CONFIRMACION,
	SIN_SERVICIO,
	AMBULANCIA_NO_DISPONIBLE,
	INCIDENTE_CERRADO,
	INCIDENTE_YA_TOMADO,
	ATENCION_FINALIZADA,
	ATENCION_AJENA

}
