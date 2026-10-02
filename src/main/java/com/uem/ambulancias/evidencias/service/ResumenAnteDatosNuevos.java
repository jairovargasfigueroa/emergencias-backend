package com.uem.ambulancias.evidencias.service;

import com.uem.ambulancias.emergencias.service.IncidenteConDatosNuevos;
import com.uem.ambulancias.evidencias.repository.AnalisisEvidenciaRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Una alerta nueva o unos detalles nuevos piden rehacer el resumen, pero solo si el incidente ya tiene evidencias
 * analizadas: sin ninguna no hay resumen que rehacer. Corre en la transacción de la alerta, así el pedido queda en la
 * cola si y solo si la alerta se guardó.
 */
@Component
@RequiredArgsConstructor
public class ResumenAnteDatosNuevos {

	private final AnalisisEvidenciaRepository analisis;
	private final ColaDeTrabajosIa cola;

	@EventListener
	public void alSumarseDatos(IncidenteConDatosNuevos evento) {
		if (analisis.existenEnIncidente(evento.incidenteId())) {
			cola.pedirResumen(evento.incidenteId());
		}
	}

}
