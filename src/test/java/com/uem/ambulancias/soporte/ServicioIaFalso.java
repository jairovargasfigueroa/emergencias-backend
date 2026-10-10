package com.uem.ambulancias.soporte;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import com.uem.ambulancias.evidencias.service.AnalisisRecibido;
import com.uem.ambulancias.evidencias.service.FalloDelServicioIa;
import com.uem.ambulancias.evidencias.service.PedidoDeAnalisis;
import com.uem.ambulancias.evidencias.service.PedidoDeResumen;
import com.uem.ambulancias.evidencias.service.Procedencia;
import com.uem.ambulancias.evidencias.service.ResumenRecibido;
import com.uem.ambulancias.evidencias.service.ServicioDeAnalisis;

/**
 * Reemplaza al microservicio de IA en las pruebas. Responde lo que la prueba le prepara: por defecto un análisis
 * con transcripción y un resumen que usa todas las fuentes que recibió. Anota cada pedido para revisarlo después.
 */
public class ServicioIaFalso implements ServicioDeAnalisis {

	private final Map<Long, FalloDelServicioIa> fallosPorEvidencia = new ConcurrentHashMap<>();
	private final List<PedidoDeAnalisis> analisisPedidos = new CopyOnWriteArrayList<>();
	private final List<PedidoDeResumen> resumenesPedidos = new CopyOnWriteArrayList<>();
	private volatile FalloDelServicioIa falloDelResumen;

	@Override
	public AnalisisRecibido analizar(PedidoDeAnalisis pedido) {
		analisisPedidos.add(pedido);
		FalloDelServicioIa fallo = fallosPorEvidencia.get(pedido.evidenciaId());
		if (fallo != null) {
			throw fallo;
		}
		String modalidad = pedido.mimeType().startsWith("audio/") ? "AUDIO" : "IMAGEN";
		String analisis = """
				{"transcript": "Se escucha a una persona pidiendo ayuda (evidencia %d)",
				 "timeline": [{"startSecond": 0.0, "text": "Pedido de ayuda"}, {"startSecond": 4.5, "text": "Sirena"}],
				 "facts": ["persona en el piso"]}
				""".formatted(pedido.evidenciaId());
		return new AnalisisRecibido(modalidad, "1.0", analisis, procedencia());
	}

	@Override
	public ResumenRecibido resumir(PedidoDeResumen pedido) {
		resumenesPedidos.add(pedido);
		if (falloDelResumen != null) {
			throw falloDelResumen;
		}
		List<Long> evidencias = pedido.evidencias().stream().map(PedidoDeResumen.EvidenciaAnalizada::evidenciaId)
				.toList();
		List<Long> alertas = pedido.alertas().stream().map(PedidoDeResumen.AlertaDelIncidente::alertaId).toList();
		String resumen = """
				{"eventType": "CAIDA", "people": %d, "risks": ["golpe en la cabeza"], "suggestedSeverity": "ALTA"}
				""".formatted(alertas.size());
		return new ResumenRecibido(resumen, evidencias, alertas, procedencia());
	}

	/** La próxima vez que se analice esa evidencia, el servicio responde con ese error. */
	public void fallarAnalisisDe(long evidenciaId, String codigo, FalloDelServicioIa.Desenlace desenlace) {
		fallosPorEvidencia.put(evidenciaId, new FalloDelServicioIa(codigo, desenlace));
	}

	/** Los resúmenes fallan con ese error hasta que se llame a {@link #olvidar()}. */
	public void fallarResumenes(String codigo, FalloDelServicioIa.Desenlace desenlace) {
		falloDelResumen = new FalloDelServicioIa(codigo, desenlace);
	}

	public List<PedidoDeAnalisis> analisisPedidos() {
		return List.copyOf(analisisPedidos);
	}

	public List<PedidoDeResumen> resumenesPedidos() {
		return List.copyOf(resumenesPedidos);
	}

	/** Cada prueba empieza con un servicio que responde bien y sin pedidos anotados. */
	public void olvidar() {
		fallosPorEvidencia.clear();
		analisisPedidos.clear();
		resumenesPedidos.clear();
		falloDelResumen = null;
	}

	private static Procedencia procedencia() {
		return new Procedencia("prueba", "modelo-de-prueba", "prompt-1", Instant.now(), "LLM");
	}

}
