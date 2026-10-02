package com.uem.ambulancias.evidencias.service;

/**
 * El servicio que analiza evidencias y resume incidentes. No guarda nada: recibe todo en el pedido y responde en la
 * misma llamada, que puede tardar minutos. Por eso se llama solo desde los workers, nunca desde una petición de una
 * app, y siempre fuera de una transacción.
 *
 * <p>Si no responde bien, lanza {@link FalloDelServicioIa} con lo que conviene hacer.
 */
public interface ServicioDeAnalisis {

	AnalisisRecibido analizar(PedidoDeAnalisis pedido);

	ResumenRecibido resumir(PedidoDeResumen pedido);

}
