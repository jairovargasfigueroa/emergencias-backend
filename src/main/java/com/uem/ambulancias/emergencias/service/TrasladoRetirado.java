package com.uem.ambulancias.emergencias.service;

import com.uem.ambulancias.emergencias.domain.MotivoCancelacionAtencion;

/**
 * A una unidad le sacaron el traslado que tenía: lo canceló quien lo pidió, o el administrador se lo pasó a otra.
 * Se publica para avisarle a la tripulación, que si no sigue manejando hacia un viaje que ya no existe.
 */
public record TrasladoRetirado(Long trasladoId, Long ambulanciaId, MotivoCancelacionAtencion motivo) {
}
