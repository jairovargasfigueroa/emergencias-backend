package com.uem.ambulancias.emergencias.service;

import java.time.Duration;
import java.time.Instant;

import com.uem.ambulancias.comun.geo.Geo;
import com.uem.ambulancias.emergencias.domain.Horario;
import com.uem.ambulancias.emergencias.domain.ModoHorario;

import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Point;
import org.springframework.stereotype.Service;

/**
 * Traduce "tiene que estar a las diez" en "esta unidad sale a las nueve y veinte". La estimación es en línea
 * recta corregida por un factor: no hace falta más precisión, porque el margen se come el error y acá nadie
 * cobra por kilómetro todavía.
 */
@Service
@RequiredArgsConstructor
public class EstimadorDeTiempos {

	private final TrasladoProperties config;

	/**
	 * Para un traslado con hora de cita. El límite es la última salida que todavía llega; la salida estimada es
	 * ese límite menos el margen, y esa diferencia es también la ventana que se le promete a la familia.
	 */
	public Horario paraCita(Point origen, Point destino, Instant horaCita) {
		long minutosSinMargen = config.minutosAcercamiento() + config.minutosRecogida()
				+ minutosDeViaje(origen, destino);
		Instant limite = horaCita.minus(Duration.ofMinutes(minutosSinMargen));
		return new Horario(ModoHorario.PROGRAMADO, horaCita,
				limite.minus(Duration.ofMinutes(config.minutosMargen())), limite);
	}

	/**
	 * Para un pedido inmediato no hay cita, así que tampoco hay una hora en la que sea tarde. El límite es una
	 * ventana fija: pasada esa hora se le dice a la familia que no se pudo, en vez de tenerla esperando.
	 */
	public Horario paraAhora(Instant ahora) {
		return new Horario(ModoHorario.INMEDIATO, null, ahora,
				ahora.plus(Duration.ofMinutes(config.minutosVentanaInmediato())));
	}

	/** Minutos del tramo con el paciente a bordo. */
	public long minutosDeViaje(Point desde, Point hasta) {
		double kilometros = Geo.metrosEntre(desde, hasta) / 1000 * config.factorCalle();
		return Math.round(kilometros / config.velocidadKmH() * 60);
	}

}
