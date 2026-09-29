package com.uem.ambulancias.emergencias.service;

import com.uem.ambulancias.emergencias.domain.Atencion;
import com.uem.ambulancias.emergencias.domain.Traslado;

/**
 * Un traslado con la unidad que lo está haciendo. La atención es nula mientras nadie salió: esas son justamente
 * las filas que el administrador tiene que mirar.
 */
public record TrasladoConAtencion(Traslado traslado, Atencion atencion) {
}
