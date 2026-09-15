package com.uem.ambulancias.flota.service;

import java.time.Instant;
import java.util.Optional;

import com.uem.ambulancias.comun.error.CodigoError;
import com.uem.ambulancias.comun.error.ConflictoException;
import com.uem.ambulancias.comun.error.NoEncontradoException;
import com.uem.ambulancias.flota.domain.Ambulancia;
import com.uem.ambulancias.flota.domain.Asignacion;
import com.uem.ambulancias.flota.exception.ReasignacionRequiereConfirmacionException;
import com.uem.ambulancias.flota.repository.AmbulanciaRepository;
import com.uem.ambulancias.flota.repository.AsignacionRepository;
import com.uem.ambulancias.usuarios.domain.RolUsuario;
import com.uem.ambulancias.usuarios.domain.Usuario;
import com.uem.ambulancias.usuarios.repository.UsuarioRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AsignacionService {

	private final UsuarioRepository usuarios;
	private final AmbulanciaRepository ambulancias;
	private final AsignacionRepository asignaciones;

	/**
	 * Asigna un paramédico activo a una ambulancia activa (R2). Si ya tiene una asignación vigente con
	 * otra ambulancia, pide confirmación; al confirmar, cierra la vigente y crea la nueva con la misma
	 * hora (R3). El bloqueo del paramédico evita que dos peticiones simultáneas dejen dos vigentes.
	 */
	@Transactional
	public Asignacion asignar(Long paramedicoId, Long ambulanciaId, boolean reasignacionConfirmada) {
		Usuario paramedico = usuarios.buscarParaActualizar(paramedicoId, RolUsuario.PARAMEDICO)
				.orElseThrow(() -> new NoEncontradoException("No existe el paramédico " + paramedicoId + "."));
		if (!paramedico.isActivo()) {
			throw new ConflictoException(CodigoError.PARAMEDICO_INACTIVO,
					"El paramédico " + paramedico.getNombreCompleto() + " está desactivado.");
		}

		Ambulancia ambulancia = ambulancias.findById(ambulanciaId)
				.orElseThrow(() -> new NoEncontradoException("No existe la ambulancia " + ambulanciaId + "."));
		if (!ambulancia.isActiva()) {
			throw new ConflictoException(CodigoError.AMBULANCIA_INACTIVA,
					"La ambulancia " + ambulancia.getPlaca() + " está desactivada.");
		}

		Instant ahora = Instant.now();
		Optional<Asignacion> vigente = asignaciones.buscarVigentePorParamedico(paramedicoId);
		if (vigente.isPresent()) {
			Asignacion actual = vigente.get();
			if (actual.getAmbulancia().getId().equals(ambulanciaId)) {
				throw new ConflictoException(CodigoError.YA_ASIGNADO,
						"El paramédico ya está asignado a la ambulancia " + ambulancia.getPlaca() + ".");
			}
			if (!reasignacionConfirmada) {
				throw new ReasignacionRequiereConfirmacionException(actual);
			}
			actual.cerrar(ahora);
		}
		return asignaciones.save(Asignacion.iniciar(paramedico, ambulancia, ahora));
	}

}
