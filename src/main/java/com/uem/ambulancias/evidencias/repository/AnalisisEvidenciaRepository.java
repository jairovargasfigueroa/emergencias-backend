package com.uem.ambulancias.evidencias.repository;

import com.uem.ambulancias.evidencias.domain.AnalisisEvidencia;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AnalisisEvidenciaRepository extends JpaRepository<AnalisisEvidencia, Long> {

	boolean existsByEvidenciaId(Long evidenciaId);

}
