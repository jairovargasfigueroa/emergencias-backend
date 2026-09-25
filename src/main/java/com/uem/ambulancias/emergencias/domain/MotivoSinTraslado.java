package com.uem.ambulancias.emergencias.domain;

/**
 * Cómo terminó una salida que no trasladó a nadie. No es un fallo: es un desenlace normal y frecuente, y además
 * decide con qué estado cierra el incidente.
 */
public enum MotivoSinTraslado {

	/** Se lo atendió en el lugar y no hizo falta llevarlo. */
	ATENDIDO_EN_EL_LUGAR,
	/** El paciente se niega a ser trasladado. */
	PACIENTE_RECHAZO,
	/** Llegaron y no había nadie: el incidente queda como falsa alarma. */
	NO_HABIA_PACIENTE,
	/** Ya se lo habían llevado por otro medio: el incidente queda como atendido externamente. */
	TRASLADO_POR_OTRO_MEDIO,
	/** Falleció en el lugar, sin traslado. */
	FALLECIDO,
	/** Solo en traslados: la unidad llegó y el paciente no estaba listo, y se retiró. */
	PACIENTE_NO_LISTO,
	/** Solo en traslados: el paciente necesita más de lo que la unidad enviada puede dar. */
	UNIDAD_NO_CORRESPONDE

}
