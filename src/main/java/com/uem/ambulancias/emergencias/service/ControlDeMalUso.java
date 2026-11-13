package com.uem.ambulancias.emergencias.service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.uem.ambulancias.emergencias.dto.CiudadanoConFalsasAlarmasResponse;
import com.uem.ambulancias.emergencias.dto.HistorialDeCiudadanoResponse;
import com.uem.ambulancias.emergencias.repository.AlertaRepository;
import com.uem.ambulancias.emergencias.repository.FalsasAlarmasDeCiudadano;
import com.uem.ambulancias.usuarios.domain.Usuario;
import com.uem.ambulancias.usuarios.repository.UsuarioRepository;
import com.uem.ambulancias.usuarios.service.CuentaCiudadanoService;

import lombok.RequiredArgsConstructor;

/**
 * PB-23: lo que la central mira para decidir si una cuenta hace mal uso del servicio. Solo cuentan las falsas
 * alarmas que la central verificó y fundamentó al cerrar el incidente; retirar una alerta o una atención sin
 * traslado no lo son.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ControlDeMalUso {

	private final AlertaRepository alertas;
	private final UsuarioRepository usuarios;
	private final CuentaCiudadanoService cuentas;

	/** Los ciudadanos con al menos una falsa alarma verificada, los que más tienen primero. */
	public List<CiudadanoConFalsasAlarmasResponse> ciudadanosConFalsasAlarmas() {
		List<FalsasAlarmasDeCiudadano> conteos = alertas.contarFalsasAlarmasPorCiudadano();
		Map<Long, Usuario> porId = usuarios.findAllById(conteos.stream().map(FalsasAlarmasDeCiudadano::ciudadanoId)
				.toList()).stream().collect(Collectors.toMap(Usuario::getId, Function.identity()));
		return conteos.stream()
				.sorted(Comparator.comparingLong(FalsasAlarmasDeCiudadano::cantidad).reversed()
						.thenComparing(FalsasAlarmasDeCiudadano::ultima, Comparator.reverseOrder()))
				.map(c -> CiudadanoConFalsasAlarmasResponse.de(porId.get(c.ciudadanoId()), c.cantidad(), c.ultima()))
				.toList();
	}

	public HistorialDeCiudadanoResponse historial(Long ciudadanoId) {
		Usuario ciudadano = cuentas.buscarCuenta(ciudadanoId);
		return HistorialDeCiudadanoResponse.de(ciudadano, alertas.buscarFalsasAlarmas(ciudadanoId),
				alertas.buscarPorEmisor(ciudadanoId), cuentas.sancionesDe(ciudadanoId));
	}

	@Transactional
	public HistorialDeCiudadanoResponse desactivar(Long ciudadanoId, Long administradorId, String motivo) {
		cuentas.desactivar(ciudadanoId, administradorId, motivo);
		return historial(ciudadanoId);
	}

	@Transactional
	public HistorialDeCiudadanoResponse reactivar(Long ciudadanoId, Long administradorId, String motivo) {
		cuentas.reactivar(ciudadanoId, administradorId, motivo);
		return historial(ciudadanoId);
	}

}
