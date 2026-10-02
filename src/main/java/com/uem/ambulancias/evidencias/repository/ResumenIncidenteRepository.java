package com.uem.ambulancias.evidencias.repository;

import java.util.Optional;

import com.uem.ambulancias.evidencias.domain.ResumenIncidente;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ResumenIncidenteRepository extends JpaRepository<ResumenIncidente, Long> {

	/** La versión vigente: la de número más alto. */
	Optional<ResumenIncidente> findFirstByIncidenteIdOrderByVersionDesc(Long incidenteId);

}
