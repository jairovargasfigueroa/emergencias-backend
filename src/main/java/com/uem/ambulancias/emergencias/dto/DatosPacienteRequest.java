package com.uem.ambulancias.emergencias.dto;

import jakarta.validation.constraints.Size;

/**
 * Reemplaza los datos del paciente. Un campo vacío los borra.
 */
public record DatosPacienteRequest(

		@Size(max = 255, message = "El nombre del paciente es demasiado largo.")
		String nombrePaciente,

		@Size(max = 255, message = "El documento del paciente es demasiado largo.")
		String documentoPaciente) {
}
