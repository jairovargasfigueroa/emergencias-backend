package com.uem.ambulancias.evidencias.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import com.uem.ambulancias.emergencias.domain.Incidente;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
import org.hibernate.annotations.Check;

/**
 * Un pedido al servicio de análisis. Esta tabla es la cola: si el servidor se reinicia, lo pendiente sigue acá. El
 * servicio responde en la misma llamada y puede tardar minutos, así que el worker toma el trabajo, lo deja bloqueado
 * mientras llama y lo cierra después. Si el worker se cae a mitad, el bloqueo vence y otro lo vuelve a tomar.
 */
@Entity
@Table(name = "trabajo_ia")
@Check(name = "ck_trabajo_ia_evidencia", constraints = "(tipo = 'ANALISIS') = (evidencia_id is not null)")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TrabajoIa {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/** Lo que viaja como {@code jobId}: el servicio lo devuelve tal cual y queda en los registros de los dos lados. */
	@Column(nullable = false, unique = true)
	private UUID referencia;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private TipoTrabajoIa tipo;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "incidente_id", nullable = false)
	private Incidente incidente;

	/** Solo en los ANALISIS. */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "evidencia_id")
	private Evidencia evidencia;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private EstadoTrabajoIa estado;

	/** Cuántas veces se tomó, contando la actual. */
	@Column(nullable = false)
	private int intentos;

	/** Desde cuándo se puede tomar. Con cada reintento se corre más lejos. */
	@Column(nullable = false)
	private Instant proximoIntento;

	/** Mientras está EN_CURSO, hasta cuándo es del worker que lo tomó. */
	private Instant bloqueadoHasta;

	/** El último código de error del servicio, o el del worker si no llegó a responder. */
	private String ultimoError;

	@Column(nullable = false)
	private Instant creadoEn;

	private Instant terminadoEn;

	public static TrabajoIa analizar(Evidencia evidencia, Instant ahora) {
		TrabajoIa trabajo = nuevo(TipoTrabajoIa.ANALISIS, evidencia.getAlerta().getIncidente(), ahora);
		trabajo.evidencia = evidencia;
		return trabajo;
	}

	public static TrabajoIa resumir(Incidente incidente, Instant ahora) {
		return nuevo(TipoTrabajoIa.RESUMEN, incidente, ahora);
	}

	private static TrabajoIa nuevo(TipoTrabajoIa tipo, Incidente incidente, Instant ahora) {
		TrabajoIa trabajo = new TrabajoIa();
		trabajo.referencia = UUID.randomUUID();
		trabajo.tipo = tipo;
		trabajo.incidente = incidente;
		trabajo.estado = EstadoTrabajoIa.PENDIENTE;
		trabajo.intentos = 0;
		trabajo.proximoIntento = ahora;
		trabajo.creadoEn = ahora;
		return trabajo;
	}

	/** Un worker se lo lleva. Vale también para uno EN_CURSO cuyo bloqueo venció: el anterior se cayó. */
	public void tomar(Instant ahora, Duration bloqueo) {
		estado = EstadoTrabajoIa.EN_CURSO;
		intentos++;
		bloqueadoHasta = ahora.plus(bloqueo);
	}

	public boolean isEnCurso() {
		return estado == EstadoTrabajoIa.EN_CURSO;
	}

	/** Ya se intentó todas las veces que se permite. */
	public boolean agotoIntentos(int maximo) {
		return intentos >= maximo;
	}

	public void completar(Instant ahora) {
		terminar(EstadoTrabajoIa.HECHO, null, ahora);
	}

	/** Vuelve a la cola, para dentro de {@code espera}. */
	public void reintentar(String error, Instant ahora, Duration espera) {
		estado = EstadoTrabajoIa.PENDIENTE;
		ultimoError = error;
		proximoIntento = ahora.plus(espera);
		bloqueadoHasta = null;
	}

	public void fallar(String error, Instant ahora) {
		terminar(EstadoTrabajoIa.FALLIDO, error, ahora);
	}

	public void pasarARevision(String error, Instant ahora) {
		terminar(EstadoTrabajoIa.EN_REVISION, error, ahora);
	}

	private void terminar(EstadoTrabajoIa fin, String error, Instant ahora) {
		estado = fin;
		if (error != null) {
			ultimoError = error;
		}
		bloqueadoHasta = null;
		terminadoEn = ahora;
	}

}
