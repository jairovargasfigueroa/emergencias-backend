package com.uem.ambulancias.emergencias.service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.uem.ambulancias.comun.error.SolicitudInvalidaException;
import com.uem.ambulancias.emergencias.domain.Atencion;
import com.uem.ambulancias.emergencias.domain.EstadoIncidente;
import com.uem.ambulancias.emergencias.domain.EstadoTraslado;
import com.uem.ambulancias.emergencias.domain.Incidente;
import com.uem.ambulancias.emergencias.domain.Traslado;
import com.uem.ambulancias.emergencias.dto.ReporteDeOperacionResponse;
import com.uem.ambulancias.emergencias.dto.ReporteDeOperacionResponse.Dia;
import com.uem.ambulancias.emergencias.dto.ReporteDeOperacionResponse.Medida;
import com.uem.ambulancias.emergencias.dto.ReporteDeOperacionResponse.PuntualidadDeTraslados;
import com.uem.ambulancias.emergencias.dto.ReporteDeOperacionResponse.Servicios;
import com.uem.ambulancias.emergencias.dto.ReporteDeOperacionResponse.TiemposDeEmergencia;
import com.uem.ambulancias.emergencias.dto.ReporteDeOperacionResponse.Unidad;
import com.uem.ambulancias.emergencias.repository.AtencionRepository;
import com.uem.ambulancias.emergencias.repository.IncidenteRepository;
import com.uem.ambulancias.emergencias.repository.TrasladoRepository;

import lombok.RequiredArgsConstructor;

/** PB-22: el reporte de operación de un período, armado con lo que ya registran incidentes, traslados y atenciones. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReporteDeOperacion {

	/** Un año alcanza para cualquier análisis de la central y evita consultas que recorran toda la base. */
	static final int DIAS_MAXIMOS = 366;

	static final List<String> CRITERIO = List.of(
			"Las emergencias cuentan por el día en que se abrieron y los traslados por el día en que salían.",
			"Los tiempos se calculan solo con los hitos registrados; un caso sin el hito no se estima, se cuenta aparte.",
			"Tiempo de respuesta: desde que se abrió el incidente hasta que llegó la primera unidad.",
			"Una emergencia sin unidad o sin llegada registrada no entra en el tiempo de respuesta.",
			"Un traslado es puntual si la unidad llegó dentro de la ventana de recogida prometida.");

	private final IncidenteRepository incidentes;
	private final TrasladoRepository traslados;
	private final AtencionRepository atenciones;
	private final TrasladoProperties config;

	public ReporteDeOperacionResponse generar(LocalDate desde, LocalDate hasta) {
		if (desde == null || hasta == null || hasta.isBefore(desde)) {
			throw new SolicitudInvalidaException("El período no es válido: la fecha final no puede ser anterior a la inicial.");
		}
		if (Duration.between(desde.atStartOfDay(), hasta.atStartOfDay()).toDays() >= DIAS_MAXIMOS) {
			throw new SolicitudInvalidaException("El período no puede pasar de un año.");
		}
		ZoneId zona = ZoneId.of(config.zona());
		Instant inicio = desde.atStartOfDay(zona).toInstant();
		Instant fin = hasta.plusDays(1).atStartOfDay(zona).toInstant();

		List<Incidente> delPeriodo = incidentes.buscarCreadosEntre(inicio, fin);
		List<Traslado> trasladosDelPeriodo = traslados.buscarEntre(inicio, fin);
		List<Atencion> suyas = atenciones.buscarDeServiciosEntre(inicio, fin);
		Map<Long, List<Atencion>> porIncidente = suyas.stream().filter(a -> a.getIncidente() != null)
				.collect(Collectors.groupingBy(a -> a.getIncidente().getId()));
		Map<Long, List<Atencion>> porTraslado = suyas.stream().filter(a -> a.getTraslado() != null)
				.collect(Collectors.groupingBy(a -> a.getTraslado().getId()));

		return new ReporteDeOperacionResponse(desde, hasta, zona.getId(), Instant.now(),
				servicios(delPeriodo, Incidente::getEstado, EstadoIncidente.class),
				servicios(trasladosDelPeriodo, Traslado::getEstado, EstadoTraslado.class),
				tiempos(delPeriodo, porIncidente),
				puntualidad(trasladosDelPeriodo, porTraslado),
				porDia(desde, hasta, zona, delPeriodo, trasladosDelPeriodo),
				porUnidad(suyas),
				CRITERIO);
	}

	private static <T, E extends Enum<E>> Servicios<E> servicios(List<T> lista, Function<T, E> estado, Class<E> tipo) {
		Map<E, Long> porEstado = new EnumMap<>(tipo);
		for (E valor : tipo.getEnumConstants()) {
			porEstado.put(valor, 0L);
		}
		lista.forEach(item -> porEstado.merge(estado.apply(item), 1L, Long::sum));
		return new Servicios<>(lista.size(), porEstado);
	}

	private static TiemposDeEmergencia tiempos(List<Incidente> lista, Map<Long, List<Atencion>> porIncidente) {
		List<Long> hastaLaToma = new ArrayList<>();
		List<Long> hastaLaLlegada = new ArrayList<>();
		long sinUnidad = 0;
		long sinLlegada = 0;
		for (Incidente incidente : lista) {
			List<Atencion> suyas = porIncidente.getOrDefault(incidente.getId(), List.of());
			if (suyas.isEmpty()) {
				sinUnidad++;
				continue;
			}
			primera(suyas, Atencion::getHoraToma)
					.ifPresent(toma -> hastaLaToma.add(segundos(incidente.getFechaHoraCreacion(), toma)));
			var llegada = primera(suyas, Atencion::getHoraLlegada);
			if (llegada.isPresent()) {
				hastaLaLlegada.add(segundos(incidente.getFechaHoraCreacion(), llegada.get()));
			} else {
				sinLlegada++;
			}
		}
		return new TiemposDeEmergencia(medida(hastaLaToma), medida(hastaLaLlegada), sinUnidad, sinLlegada);
	}

	private static PuntualidadDeTraslados puntualidad(List<Traslado> lista, Map<Long, List<Atencion>> porTraslado) {
		long aTiempo = 0;
		long tarde = 0;
		long sinDato = 0;
		for (Traslado traslado : lista) {
			var llegada = primera(porTraslado.getOrDefault(traslado.getId(), List.of()), Atencion::getHoraLlegada);
			if (llegada.isEmpty() || traslado.getHoraRecogidaHasta() == null) {
				sinDato++;
			} else if (llegada.get().isAfter(traslado.getHoraRecogidaHasta())) {
				tarde++;
			} else {
				aTiempo++;
			}
		}
		return new PuntualidadDeTraslados(aTiempo, tarde, sinDato);
	}

	private static List<Dia> porDia(LocalDate desde, LocalDate hasta, ZoneId zona, List<Incidente> emergencias,
			List<Traslado> lista) {
		Map<LocalDate, Long> deEmergencias = emergencias.stream().collect(Collectors.groupingBy(
				i -> i.getFechaHoraCreacion().atZone(zona).toLocalDate(), Collectors.counting()));
		Map<LocalDate, Long> deTraslados = lista.stream().collect(Collectors.groupingBy(
				t -> t.getHoraSalidaEstimada().atZone(zona).toLocalDate(), Collectors.counting()));
		List<Dia> dias = new ArrayList<>();
		for (LocalDate dia = desde; !dia.isAfter(hasta); dia = dia.plusDays(1)) {
			dias.add(new Dia(dia, deEmergencias.getOrDefault(dia, 0L), deTraslados.getOrDefault(dia, 0L)));
		}
		return dias;
	}

	private static List<Unidad> porUnidad(List<Atencion> lista) {
		Map<String, List<Atencion>> porPlaca = new TreeMap<>();
		lista.forEach(a -> porPlaca.computeIfAbsent(a.getAmbulancia().getPlaca(), p -> new ArrayList<>()).add(a));
		return porPlaca.entrySet().stream().map(entrada -> {
			List<Atencion> suyas = entrada.getValue();
			List<Long> hastaLaLlegada = suyas.stream()
					.filter(a -> a.getIncidente() != null && a.getHoraLlegada() != null)
					.map(a -> segundos(a.getHoraToma(), a.getHoraLlegada())).toList();
			return new Unidad(entrada.getKey(), suyas.stream().filter(a -> a.getIncidente() != null).count(),
					suyas.stream().filter(a -> a.getTraslado() != null).count(), medida(hastaLaLlegada));
		}).toList();
	}

	private static java.util.Optional<Instant> primera(List<Atencion> lista, Function<Atencion, Instant> hito) {
		return lista.stream().map(hito).filter(Objects::nonNull).min(Comparator.naturalOrder());
	}

	private static long segundos(Instant desde, Instant hasta) {
		return Math.max(0, Duration.between(desde, hasta).toSeconds());
	}

	static Medida medida(List<Long> valores) {
		if (valores.isEmpty()) {
			return new Medida(0, null, null);
		}
		List<Long> ordenados = valores.stream().sorted().toList();
		int n = ordenados.size();
		long promedio = Math.round(ordenados.stream().mapToLong(Long::longValue).average().orElse(0));
		long mediana = n % 2 == 1 ? ordenados.get(n / 2)
				: Math.round((ordenados.get(n / 2 - 1) + ordenados.get(n / 2)) / 2.0);
		return new Medida(n, promedio, mediana);
	}

}
