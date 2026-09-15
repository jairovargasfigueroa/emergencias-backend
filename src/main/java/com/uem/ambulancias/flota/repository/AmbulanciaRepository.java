package com.uem.ambulancias.flota.repository;

import java.util.List;
import java.util.Optional;

import com.uem.ambulancias.flota.domain.Ambulancia;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AmbulanciaRepository extends JpaRepository<Ambulancia, Long> {

	boolean existsByPlaca(String placa);

	List<Ambulancia> findAllByOrderByPlacaAsc();

	/** Lectura con bloqueo pesimista: serializa los cambios de estado de la misma ambulancia. */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select a from Ambulancia a where a.id = :id")
	Optional<Ambulancia> buscarParaActualizar(@Param("id") Long id);

}
