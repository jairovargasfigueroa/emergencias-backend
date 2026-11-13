package com.uem.ambulancias.emergencias.controller;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.uem.ambulancias.emergencias.dto.ReporteDeOperacionResponse;
import com.uem.ambulancias.emergencias.service.ReporteDeOperacion;

import lombok.RequiredArgsConstructor;

/** PB-22: reportes de la operación para la central. */
@RestController
@RequestMapping("/reportes")
@RequiredArgsConstructor
public class ReporteController {

	private final ReporteDeOperacion reporte;

	/** Del día {@code desde} al día {@code hasta}, los dos incluidos, en la zona de la empresa. */
	@GetMapping("/operacion")
	public ReporteDeOperacionResponse operacion(
			@RequestParam("desde") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
			@RequestParam("hasta") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
		return reporte.generar(desde, hasta);
	}

}
