package com.uem.ambulancias.cobros.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.uem.ambulancias.cobros.domain.TarifaReferencia;
import com.uem.ambulancias.cobros.domain.TipoServicio;
import com.uem.ambulancias.cobros.dto.RegistrarTarifaRequest;
import com.uem.ambulancias.cobros.dto.TarifaResponse;
import com.uem.ambulancias.cobros.dto.TarifasResponse;
import com.uem.ambulancias.cobros.repository.TarifaReferenciaRepository;
import com.uem.ambulancias.comun.error.NoEncontradoException;
import com.uem.ambulancias.emergencias.service.TrasladoProperties;
import com.uem.ambulancias.usuarios.domain.RolUsuario;
import com.uem.ambulancias.usuarios.repository.UsuarioRepository;

import lombok.RequiredArgsConstructor;

/** PB-29: tarifas de referencia por tipo de servicio, con su vigencia y su historial. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TarifaService {

	private final TarifaReferenciaRepository tarifas;
	private final UsuarioRepository usuarios;
	private final TrasladoProperties config;

	public TarifasResponse consultar() {
		LocalDate hoy = LocalDate.now(ZoneId.of(config.zona()));
		return new TarifasResponse(TarifaResponse.de(vigenteAl(TipoServicio.EMERGENCIA, hoy)),
				TarifaResponse.de(vigenteAl(TipoServicio.TRASLADO, hoy)),
				tarifas.buscarHistorial().stream().map(TarifaResponse::de).toList());
	}

	/** Una tarifa nueva no pisa la anterior: rige desde su día y la otra queda en el historial. */
	@Transactional
	public TarifasResponse registrar(Long administradorId, RegistrarTarifaRequest datos) {
		var administrador = usuarios.findByIdAndRol(administradorId, RolUsuario.ADMIN)
				.orElseThrow(() -> new NoEncontradoException("No existe el administrador " + administradorId + "."));
		tarifas.save(TarifaReferencia.registrar(datos.tipoServicio(), datos.monto(), datos.vigenteDesde(),
				administrador, Instant.now()));
		return consultar();
	}

	/** La que rige ese día, o nula si todavía no había ninguna. */
	public TarifaReferencia vigenteAl(TipoServicio tipo, LocalDate dia) {
		return tarifas.buscarVigentesAl(tipo, dia).stream().findFirst().orElse(null);
	}

}
