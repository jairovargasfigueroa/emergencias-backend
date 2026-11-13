package com.uem.ambulancias.emergencias.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.uem.ambulancias.emergencias.domain.Alerta;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AlertaRepository extends JpaRepository<Alerta, Long> {

	/**
	 * PB-23: cuántas alertas de cada ciudadano terminaron en un incidente que la central cerró como falsa alarma,
	 * y la última. Solo los cierres de la central, que llevan el fundamento escrito.
	 */
	@Query("""
			select new com.uem.ambulancias.emergencias.repository.FalsasAlarmasDeCiudadano(e.id, count(a),
			       max(a.fechaHora))
			from Alerta a join a.emisor e join a.incidente i
			where i.motivoCierre = com.uem.ambulancias.emergencias.domain.MotivoCierreIncidente.FALSA_ALARMA_VERIFICADA
			  and i.cerradoPor is not null and e.registradoPor is null
			group by e.id
			""")
	List<FalsasAlarmasDeCiudadano> contarFalsasAlarmasPorCiudadano();

	/** Las alertas de un ciudadano que la central dio por falsa alarma, con el incidente y quién lo cerró. */
	@Query("""
			select a from Alerta a join fetch a.incidente i join fetch i.cerradoPor
			where a.emisor.id = :ciudadanoId
			  and i.motivoCierre = com.uem.ambulancias.emergencias.domain.MotivoCierreIncidente.FALSA_ALARMA_VERIFICADA
			order by a.fechaHora desc, a.id desc
			""")
	List<Alerta> buscarFalsasAlarmas(@Param("ciudadanoId") Long ciudadanoId);

	/** Todas las alertas de un ciudadano, lo más reciente primero, para ver el resto de su historial. */
	@Query("""
			select a from Alerta a join fetch a.incidente
			where a.emisor.id = :ciudadanoId
			order by a.fechaHora desc, a.id desc
			""")
	List<Alerta> buscarPorEmisor(@Param("ciudadanoId") Long ciudadanoId);

	/** Incidente al que está vinculada la alerta: se bloquea antes de tocar nada. */
	@Query("select a.incidente.id from Alerta a where a.id = :id")
	Optional<Long> buscarIncidenteId(@Param("id") Long id);

	/**
	 * Si al incidente le queda algún pedido en pie. Cuando todos los emisores retiraron el suyo, la unidad que va en
	 * camino tiene que saberlo, y si no va ninguna el incidente ya no tiene por qué seguir abierto.
	 */
	@Query("select count(a) > 0 from Alerta a where a.incidente.id = :incidenteId and a.estado <> 'CANCELADA'")
	boolean existeAlgunaVigente(@Param("incidenteId") Long incidenteId);

	/**
	 * A quiénes avisarles de un incidente: los que pidieron la ambulancia, no retiraron su pedido y tienen un teléfono
	 * registrado. Cada uno con su alerta, porque en un incidente puede haber avisos de varias personas.
	 */
	@Query("""
			select new com.uem.ambulancias.emergencias.repository.EmisorConAviso(a.id, e.tokenPush)
			from Alerta a join a.emisor e
			where a.incidente.id = :incidenteId and a.estado <> 'CANCELADA'
			  and e.activo = true and e.tokenPush is not null
			""")
	List<EmisorConAviso> buscarEmisoresConAviso(@Param("incidenteId") Long incidenteId);

	/** Descripciones que dejaron los emisores del incidente, en orden de emisión. */
	@Query("""
			select a.descripcion from Alerta a
			where a.incidente.id = :incidenteId and a.descripcion is not null
			order by a.fechaHora
			""")
	List<String> buscarDescripciones(@Param("incidenteId") Long incidenteId);

	/** Alertas del incidente con su emisor, en orden de emisión. */
	@Query("""
			select a from Alerta a join fetch a.emisor
			where a.incidente.id = :incidenteId
			order by a.fechaHora, a.id
			""")
	List<Alerta> buscarPorIncidente(@Param("incidenteId") Long incidenteId);

	/**
	 * Lo mismo para varios incidentes de una vez, en orden de emisión. Un incidente no tiene nombre propio: lo
	 * único que lo hace reconocible en una lista es lo que escribió el que avisó, y es la primera descripción la
	 * que vale, porque la escribió quien vio el hecho primero.
	 */
	@Query("""
			select new com.uem.ambulancias.emergencias.repository.ReferenciaDeIncidente(a.incidente.id, a.descripcion)
			from Alerta a
			where a.incidente.id in :incidenteIds and a.descripcion is not null
			order by a.fechaHora, a.id
			""")
	List<ReferenciaDeIncidente> buscarDescripcionesPorIncidentes(@Param("incidenteIds") Collection<Long> incidenteIds);

	/** Cantidad de alertas de cada uno de esos incidentes, en una sola consulta agrupada. */
	@Query("""
			select new com.uem.ambulancias.emergencias.repository.AlertasPorIncidente(a.incidente.id, count(a))
			from Alerta a
			where a.incidente.id in :incidenteIds
			group by a.incidente.id
			""")
	List<AlertasPorIncidente> contarPorIncidentes(@Param("incidenteIds") Collection<Long> incidenteIds);

}
