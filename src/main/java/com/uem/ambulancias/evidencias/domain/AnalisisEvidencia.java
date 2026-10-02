package com.uem.ambulancias.evidencias.domain;

import java.time.Instant;
import java.util.UUID;

import com.uem.ambulancias.evidencias.service.AnalisisRecibido;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Lo que el servicio de análisis vio en una evidencia. Es apoyo para el personal, no un diagnóstico. El JSON se guarda
 * tal como vino, porque es lo que se le reenvía sin cambios al pedir el resumen del incidente, y al lado queda con qué
 * se generó.
 */
@Entity
@Table(name = "analisis_evidencia")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AnalisisEvidencia {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/** Un análisis por evidencia. */
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "evidencia_id", nullable = false, unique = true)
	private Evidencia evidencia;

	/** El trabajo que lo pidió, el mismo {@code jobId} que registró el servicio. */
	@Column(nullable = false)
	private UUID referenciaTrabajo;

	/** Como la nombra el servicio: {@code image}, {@code audio} o {@code video}. */
	@Column(nullable = false)
	private String modalidad;

	private String versionEsquema;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(nullable = false, columnDefinition = "jsonb")
	private String analisis;

	private String proveedor;

	private String modelo;

	private String versionPrompt;

	private String metodo;

	/** Cuándo lo generó el servicio, según el servicio. */
	private Instant generadoEn;

	@Column(nullable = false)
	private Instant recibidoEn;

	public static AnalisisEvidencia registrar(Evidencia evidencia, UUID referenciaTrabajo, AnalisisRecibido recibido,
			Instant ahora) {
		AnalisisEvidencia analisis = new AnalisisEvidencia();
		analisis.evidencia = evidencia;
		analisis.referenciaTrabajo = referenciaTrabajo;
		analisis.modalidad = recibido.modalidad();
		analisis.versionEsquema = recibido.versionEsquema();
		analisis.analisis = recibido.analisisJson();
		analisis.proveedor = recibido.procedencia().proveedor();
		analisis.modelo = recibido.procedencia().modelo();
		analisis.versionPrompt = recibido.procedencia().versionPrompt();
		analisis.metodo = recibido.procedencia().metodo();
		analisis.generadoEn = recibido.procedencia().generadoEn();
		analisis.recibidoEn = ahora;
		return analisis;
	}

}
