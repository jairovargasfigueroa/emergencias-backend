package com.uem.ambulancias.usuarios.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.uem.ambulancias.usuarios.domain.SancionCiudadano;

public interface SancionCiudadanoRepository extends JpaRepository<SancionCiudadano, Long> {

	/** Lo que se hizo con la cuenta, lo más reciente primero, con quién lo hizo. */
	@Query("""
			select s from SancionCiudadano s join fetch s.administrador
			where s.ciudadano.id = :ciudadanoId
			order by s.fechaHora desc, s.id desc
			""")
	List<SancionCiudadano> buscarPorCiudadano(@Param("ciudadanoId") Long ciudadanoId);

}
