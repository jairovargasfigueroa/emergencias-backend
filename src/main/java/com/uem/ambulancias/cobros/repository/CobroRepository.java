package com.uem.ambulancias.cobros.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.uem.ambulancias.cobros.domain.Cobro;

import jakarta.persistence.LockModeType;

public interface CobroRepository extends JpaRepository<Cobro, Long> {

	@Query("select c from Cobro c join fetch c.registradoPor where c.atencion.id = :atencionId")
	Optional<Cobro> buscarPorAtencion(@Param("atencionId") Long atencionId);

	boolean existsByAtencionId(Long atencionId);

	/** Con bloqueo: dos personas corrigiendo el mismo cobro a la vez no se pisan. */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select c from Cobro c where c.id = :id")
	Optional<Cobro> buscarParaActualizar(@Param("id") Long id);

}
