package com.uem.ambulancias.emergencias.repository;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.uem.ambulancias.emergencias.domain.EstadoAtencion;
import com.uem.ambulancias.emergencias.domain.EstadoIncidente;
import com.uem.ambulancias.emergencias.domain.Incidente;

import jakarta.persistence.LockModeType;
import org.locationtech.jts.geom.Point;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface IncidenteRepository extends JpaRepository<Incidente, Long> {

	/** SEC-B.1: lectura con bloqueo pesimista de la fila del incidente. Serializa tomas, entregas y cancelaciones. */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select i from Incidente i where i.id = :id")
	Optional<Incidente> buscarParaActualizar(@Param("id") Long id);

	/** El incidente con quien lo cerró a mano, si fue una persona: el detalle del panel lo nombra. */
	@Query("select i from Incidente i left join fetch i.cerradoPor where i.id = :id")
	Optional<Incidente> buscarConQuienLoCerro(@Param("id") Long id);

	/**
	 * SEC-A.1: el incidente ACTIVO o EN_ATENCION a menos de {@code radioM} metros del punto y creado hace menos de
	 * {@code ventanaMin} minutos; si varios cumplen, el más cercano. Devuelve solo el id porque esta búsqueda no
	 * bloquea: quien agrupa lo vuelve a leer bloqueado y confirma que siga abierto.
	 */
	default Optional<Long> buscarIdAbiertoCercano(Point punto, int radioM, int ventanaMin) {
		Instant creadoDesde = Instant.now().minus(Duration.ofMinutes(ventanaMin));
		return buscarIdAbiertoMasCercano(punto.getY(), punto.getX(), radioM, creadoDesde);
	}

	@Query(value = """
			with punto as (select ST_SetSRID(ST_MakePoint(:longitud, :latitud), 4326)::geography as g)
			select i.id from incidente i, punto
			where i.estado in ('ACTIVO', 'EN_ATENCION')
			  and i.fecha_hora_creacion > :creadoDesde
			  and ST_DWithin(i.ubicacion::geography, punto.g, :radioM)
			  and ST_Distance(i.ubicacion::geography, punto.g) < :radioM
			order by ST_Distance(i.ubicacion::geography, punto.g)
			limit 1
			""", nativeQuery = true)
	Optional<Long> buscarIdAbiertoMasCercano(@Param("latitud") double latitud, @Param("longitud") double longitud,
			@Param("radioM") double radioM, @Param("creadoDesde") Instant creadoDesde);

	/** Consulta del panel: incidentes en alguno de esos estados, con el orden y la página del pedido. */
	Page<Incidente> findByEstadoIn(Collection<EstadoIncidente> estados, Pageable pageable);

	/**
	 * Los incidentes que nadie está cubriendo: ACTIVO y sin ninguna atención activa encima. Es la lista roja del
	 * centro de control, el más viejo primero, porque el que lleva más tiempo esperando es el que más urge.
	 *
	 * <p>El estado ya debería alcanzar —ME-1 devuelve el incidente a ACTIVO cuando se cancela la última unidad—,
	 * pero se pregunta igual por las atenciones: si alguna vez el estado quedara desfasado, el error sería mandar
	 * otra unidad a un incidente ya cubierto, y eso se paga con una ambulancia menos en la calle.
	 */
	default List<Incidente> buscarSinCubrir() {
		return buscarActivosSinAtencionEn(EstadoAtencion.ACTIVOS);
	}

	@Query("""
			select i from Incidente i
			where i.estado = 'ACTIVO'
			  and not exists (select a from Atencion a where a.incidente = i and a.estado in :activos)
			order by i.fechaHoraCreacion, i.id
			""")
	List<Incidente> buscarActivosSinAtencionEn(@Param("activos") Collection<EstadoAtencion> activos);

}
