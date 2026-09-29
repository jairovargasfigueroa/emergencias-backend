package com.uem.ambulancias.emergencias.repository;

/**
 * Quien pidió la ambulancia y a qué teléfono avisarle, con su propia alerta: al tocar el aviso, su app abre el
 * seguimiento de esa alerta.
 */
public record EmisorConAviso(Long alertaId, String tokenPush) {
}
