package com.uem.ambulancias.emergencias.repository;

import java.util.Collection;

import com.uem.ambulancias.emergencias.domain.Atencion;
import com.uem.ambulancias.emergencias.domain.EstadoAtencion;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AtencionRepository extends JpaRepository<Atencion, Long> {

	long countByIncidenteIdAndEstadoIn(Long incidenteId, Collection<EstadoAtencion> estados);

}
