package com.uem.ambulancias.emergencias.repository;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import com.uem.ambulancias.emergencias.domain.Incidente;

import jakarta.persistence.LockModeType;
import org.locationtech.jts.geom.Point;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface IncidenteRepository extends JpaRepository<Incidente, Long> {

	/** SEC-B.1: lectura con bloqueo pesimista de la fila del incidente. Serializa tomas, entregas y cancelaciones. */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select i from Incidente i where i.id = :id")
	Optional<Incidente> buscarParaActualizar(@Param("id") Long id);

	/**
	 * SEC-A.1: incidente ACTIVO o EN_ATENCION a menos de {@code radioM} metros del punto y creado hace menos de
	 * {@code ventanaMin} minutos. Si varios cumplen, devuelve el más cercano.
	 */
	default Optional<Incidente> buscarActivoCercano(Point punto, int radioM, int ventanaMin) {
		Instant creadoDesde = Instant.now().minus(Duration.ofMinutes(ventanaMin));
		return buscarAbiertoMasCercano(punto.getY(), punto.getX(), radioM, creadoDesde);
	}

	@Query(value = """
			with punto as (select ST_SetSRID(ST_MakePoint(:longitud, :latitud), 4326)::geography as g)
			select i.* from incidente i, punto
			where i.estado in ('ACTIVO', 'EN_ATENCION')
			  and i.fecha_hora_creacion > :creadoDesde
			  and ST_DWithin(i.ubicacion::geography, punto.g, :radioM)
			  and ST_Distance(i.ubicacion::geography, punto.g) < :radioM
			order by ST_Distance(i.ubicacion::geography, punto.g)
			limit 1
			""", nativeQuery = true)
	Optional<Incidente> buscarAbiertoMasCercano(@Param("latitud") double latitud, @Param("longitud") double longitud,
			@Param("radioM") double radioM, @Param("creadoDesde") Instant creadoDesde);

}
