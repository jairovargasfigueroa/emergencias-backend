package com.uem.ambulancias.emergencias.repository;

import java.util.List;
import java.util.Optional;

import com.uem.ambulancias.emergencias.domain.CentroSalud;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CentroSaludRepository extends JpaRepository<CentroSalud, Long> {

	List<CentroSalud> findByActivoTrueOrderByNombreAsc();

	Optional<CentroSalud> findByIdAndActivoTrue(Long id);

}
