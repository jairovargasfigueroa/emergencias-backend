package com.uem.ambulancias.flota.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import com.uem.ambulancias.flota.domain.Ambulancia;

import jakarta.persistence.LockModeType;
import org.locationtech.jts.geom.Point;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AmbulanciaRepository extends JpaRepository<Ambulancia, Long> {

	boolean existsByPlaca(String placa);

	List<Ambulancia> findAllByOrderByPlacaAsc();

	/**
	 * SEC-A: ambulancias DISPONIBLES y activas, ordenadas por cercanía al punto. La cercanía ordena, no filtra: las
	 * que aún no tienen posición van al final.
	 */
	@Query(value = """
			select a.* from ambulancia a
			where a.estado = 'DISPONIBLE' and a.activa
			order by ST_Distance(a.ultima_posicion::geography,
			                     ST_SetSRID(ST_MakePoint(:longitud, :latitud), 4326)::geography) asc nulls last
			""", nativeQuery = true)
	List<Ambulancia> buscarDisponiblesPorCercania(@Param("latitud") double latitud, @Param("longitud") double longitud);

	/** Lectura con bloqueo pesimista: serializa los cambios de estado de la misma ambulancia. */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select a from Ambulancia a where a.id = :id")
	Optional<Ambulancia> buscarParaActualizar(@Param("id") Long id);

	/**
	 * Guarda la última posición solo si la anterior se guardó antes de {@code guardadaAntesDe}. Actualiza únicamente
	 * esas dos columnas, así nunca pisa un cambio de estado hecho a la vez por otra transacción.
	 */
	@Modifying
	@Query("""
			update Ambulancia a set a.ultimaPosicion = :posicion, a.ultimaPosicionEn = :momento
			where a.id = :id and (a.ultimaPosicionEn is null or a.ultimaPosicionEn <= :guardadaAntesDe)
			""")
	int guardarUltimaPosicion(@Param("id") Long id, @Param("posicion") Point posicion,
			@Param("momento") Instant momento, @Param("guardadaAntesDe") Instant guardadaAntesDe);

}
