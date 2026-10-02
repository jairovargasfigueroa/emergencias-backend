package com.uem.ambulancias.evidencias.domain;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import com.uem.ambulancias.emergencias.domain.Incidente;
import com.uem.ambulancias.evidencias.service.ResumenRecibido;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Una versión del resumen preliminar de un incidente, armado con todas sus alertas y todos los análisis de sus
 * evidencias. Es apoyo informativo para el personal: no es un diagnóstico ni decide el despacho. Las versiones no se
 * pisan; la vigente es la de número más alto.
 */
@Entity
@Table(name = "resumen_incidente",
		uniqueConstraints = @UniqueConstraint(name = "uk_resumen_incidente_version",
				columnNames = { "incidente_id", "version" }))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ResumenIncidente {

	/**
	 * Si una alerta nueva, sin evidencias nuevas, alcanza para reemplazar la versión vigente. El servicio de análisis
	 * solo pide que la propuesta sume evidencias; acá también cuenta una alerta nueva, porque su texto puede cambiar el
	 * resumen. Está en duda con el equipo de IA: si deciden que no, se pasa a {@code false} y la regla queda la suya.
	 */
	public static final boolean ALERTA_NUEVA_CUENTA = true;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "incidente_id", nullable = false)
	private Incidente incidente;

	@Column(nullable = false)
	private int version;

	/** El objeto {@code summary} tal como lo devolvió el servicio. */
	@JdbcTypeCode(SqlTypes.JSON)
	@Column(nullable = false, columnDefinition = "jsonb")
	private String resumen;

	@JdbcTypeCode(SqlTypes.ARRAY)
	@Column(nullable = false, columnDefinition = "bigint[]")
	private Long[] evidenciasUsadas;

	@JdbcTypeCode(SqlTypes.ARRAY)
	@Column(nullable = false, columnDefinition = "bigint[]")
	private Long[] alertasUsadas;

	/** El trabajo que lo pidió. */
	@Column(nullable = false)
	private UUID referenciaTrabajo;

	private String proveedor;

	private String modelo;

	private String versionPrompt;

	/** {@code model}, o {@code single_evidence} si el servicio lo armó sin llamar al modelo. */
	private String metodo;

	private Instant generadoEn;

	@Column(nullable = false)
	private Instant creadoEn;

	/** La versión siguiente a {@code vigente}, o la primera si no hay ninguna. */
	public static ResumenIncidente nuevaVersion(Incidente incidente, ResumenIncidente vigente, UUID referenciaTrabajo,
			ResumenRecibido recibido, Instant ahora) {
		ResumenIncidente resumen = new ResumenIncidente();
		resumen.incidente = incidente;
		resumen.version = vigente == null ? 1 : vigente.version + 1;
		resumen.resumen = recibido.resumenJson();
		resumen.evidenciasUsadas = recibido.evidenciasUsadas().toArray(Long[]::new);
		resumen.alertasUsadas = recibido.alertasUsadas().toArray(Long[]::new);
		resumen.referenciaTrabajo = referenciaTrabajo;
		resumen.proveedor = recibido.procedencia().proveedor();
		resumen.modelo = recibido.procedencia().modelo();
		resumen.versionPrompt = recibido.procedencia().versionPrompt();
		resumen.metodo = recibido.procedencia().metodo();
		resumen.generadoEn = recibido.procedencia().generadoEn();
		resumen.creadoEn = ahora;
		return resumen;
	}

	/**
	 * Si una propuesta reemplaza a esta versión. Tiene que conservar todas sus evidencias y además sumar algo: al menos
	 * una evidencia, o al menos una alerta si {@link #ALERTA_NUEVA_CUENTA}. Así una propuesta que llega tarde, armada
	 * con menos datos, nunca pisa a una más completa.
	 */
	public boolean seReemplazaCon(Collection<Long> evidenciasPropuestas, Collection<Long> alertasPropuestas) {
		List<Long> evidencias = Arrays.asList(evidenciasUsadas);
		List<Long> alertas = Arrays.asList(alertasUsadas);
		if (!evidenciasPropuestas.containsAll(evidencias)) {
			return false;
		}
		boolean sumaEvidencia = !evidencias.containsAll(evidenciasPropuestas);
		boolean sumaAlerta = !alertas.containsAll(alertasPropuestas);
		return sumaEvidencia || (ALERTA_NUEVA_CUENTA && sumaAlerta);
	}

	public List<Long> getEvidenciasUsadas() {
		return List.of(evidenciasUsadas);
	}

	public List<Long> getAlertasUsadas() {
		return List.of(alertasUsadas);
	}

}
