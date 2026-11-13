package com.uem.ambulancias.emergencias.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import com.uem.ambulancias.emergencias.domain.EstadoIncidente;
import com.uem.ambulancias.emergencias.domain.EstadoTraslado;

/**
 * PB-22: cómo anduvo la operación en un período. Las emergencias cuentan por el día en que se abrieron y los
 * traslados por el día en que salían, igual que el historial y la agenda del panel, así los totales cuadran con lo que
 * se ve allí.
 *
 * <p>Los tiempos se calculan solo con hitos registrados. Lo que no tiene el hito no se estima: se cuenta aparte, como
 * dato incompleto, y {@code criterio} lo explica.
 */
public record ReporteDeOperacionResponse(
		LocalDate desde,
		LocalDate hasta,
		String zona,
		Instant generadoEn,
		Servicios<EstadoIncidente> emergencias,
		Servicios<EstadoTraslado> traslados,
		TiemposDeEmergencia tiemposDeEmergencia,
		PuntualidadDeTraslados puntualidadDeTraslados,
		List<Dia> porDia,
		List<Unidad> porUnidad,
		List<String> criterio) {

	/** Cuántos hubo y cómo terminó cada uno. Los que siguen abiertos van con su estado actual. */
	public record Servicios<E extends Enum<E>>(long total, Map<E, Long> porEstado) {
	}

	/**
	 * Desde que se abrió el incidente: hasta que una unidad lo tomó, y hasta que la primera llegó al lugar (el
	 * tiempo de respuesta). {@code sinUnidad} no tuvo ninguna; {@code sinLlegada} tuvo pero ninguna registró la
	 * llegada.
	 */
	public record TiemposDeEmergencia(Medida hastaLaToma, Medida hastaLaLlegada, long sinUnidad, long sinLlegada) {
	}

	/** Promedio y mediana en segundos, sobre {@code medidos} casos. Nulos si no hubo ninguno. */
	public record Medida(long medidos, Long promedioSegundos, Long medianaSegundos) {
	}

	/**
	 * Si la unidad llegó dentro de la ventana de recogida que se le prometió a la familia. {@code sinDato}: no hubo
	 * llegada registrada o el traslado no tenía ventana.
	 */
	public record PuntualidadDeTraslados(long aTiempo, long tarde, long sinDato) {
	}

	public record Dia(LocalDate dia, long emergencias, long traslados) {
	}

	/**
	 * Lo que hizo cada unidad con los servicios del período. {@code hastaLaLlegada}: desde que tomó cada emergencia
	 * hasta que llegó al lugar.
	 */
	public record Unidad(String placa, long emergencias, long traslados, Medida hastaLaLlegada) {
	}

}
