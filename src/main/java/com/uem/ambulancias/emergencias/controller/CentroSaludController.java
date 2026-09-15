package com.uem.ambulancias.emergencias.controller;

import java.util.List;

import com.uem.ambulancias.emergencias.dto.CentroSaludResponse;
import com.uem.ambulancias.emergencias.service.CentroSaludService;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/centros-salud")
@RequiredArgsConstructor
public class CentroSaludController {

	private final CentroSaludService centroSaludService;

	@GetMapping
	public List<CentroSaludResponse> listarActivos() {
		return centroSaludService.listarActivos().stream().map(CentroSaludResponse::de).toList();
	}

}
