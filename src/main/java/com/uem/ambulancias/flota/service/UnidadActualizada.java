package com.uem.ambulancias.flota.service;

/**
 * Cambió algo que la tripulación de esa unidad ve en su app: su estado, su atención, quiénes están en ella o los datos
 * del caso que atiende. Se difunde en tiempo real después del commit, para que el cambio le llegue aunque lo haya hecho
 * otro: la central, el sistema o el compañero desde su teléfono.
 *
 * <p>Toda operación que cambie algo de eso tiene que publicarlo. Es la única forma de que la app se entere sin volver
 * a preguntar.
 */
public record UnidadActualizada(Long ambulanciaId) {
}
