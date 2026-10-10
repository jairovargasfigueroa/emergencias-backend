package com.uem.ambulancias.emergencias.domain;

import static com.uem.ambulancias.soporte.Conflictos.rechazaCon;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.locationtech.jts.geom.Point;

import com.uem.ambulancias.comun.error.CodigoError;
import com.uem.ambulancias.comun.geo.Geo;
import com.uem.ambulancias.flota.domain.Ambulancia;
import com.uem.ambulancias.flota.domain.TipoUnidad;
import com.uem.ambulancias.usuarios.domain.Usuario;

@Tag("PB-12")
class AtencionTest {

	private static final Instant AHORA = Instant.parse("2026-10-10T15:00:00Z");
	private static final Point LUGAR = Geo.punto(-17.78329, -63.18210);
	private static final Point HOSPITAL = Geo.punto(-17.77000, -63.19000);

	@Test
	@DisplayName("PB-12 · la atención nace en camino y avanza hito por hito hasta la entrega")
	void secuenciaCompleta() {
		Atencion atencion = nueva();
		assertThat(atencion.getEstado()).isEqualTo(EstadoAtencion.EN_CAMINO);

		atencion.marcarHito(EstadoAtencion.EN_EL_LUGAR, LUGAR);
		atencion.marcarHito(EstadoAtencion.PACIENTE_RECOGIDO, LUGAR);
		atencion.marcarHito(EstadoAtencion.EN_HOSPITAL, HOSPITAL);
		atencion.entregar(HOSPITAL, null, "Guardia del hospital");

		assertThat(atencion.getEstado()).isEqualTo(EstadoAtencion.PACIENTE_ENTREGADO);
		assertThat(atencion.getHoraLlegada()).isNotNull();
		assertThat(atencion.getHoraRecogida()).isNotNull();
		assertThat(atencion.getHoraLlegadaHospital()).isNotNull();
		assertThat(atencion.getHoraEntrega()).isNotNull();
		assertThat(atencion.getUbicacionLlegada()).isEqualTo(LUGAR);
		assertThat(atencion.getUbicacionEntrega()).isEqualTo(HOSPITAL);
		assertThat(atencion.getDestinoDescripcion()).isEqualTo("Guardia del hospital");
	}

	@Test
	@DisplayName("PB-12 · no se salta un hito: en camino no se recoge al paciente ni se entrega")
	void sinSaltos() {
		Atencion atencion = nueva();

		rechazaCon(CodigoError.TRANSICION_INVALIDA, () -> atencion.marcarHito(EstadoAtencion.PACIENTE_RECOGIDO, LUGAR));
		rechazaCon(CodigoError.TRANSICION_INVALIDA, () -> atencion.entregar(LUGAR, null, null));
		assertThat(atencion.getEstado()).isEqualTo(EstadoAtencion.EN_CAMINO);
		assertThat(atencion.getHoraRecogida()).isNull();
	}

	@Test
	@DisplayName("PB-12 · no se retrocede ni se repite un hito, así su hora y su lugar nunca se pisan")
	void sinRetrocesos() {
		Atencion atencion = nueva();
		atencion.marcarHito(EstadoAtencion.EN_EL_LUGAR, LUGAR);
		Instant llegada = atencion.getHoraLlegada();
		atencion.marcarHito(EstadoAtencion.PACIENTE_RECOGIDO, LUGAR);

		rechazaCon(CodigoError.TRANSICION_INVALIDA, () -> atencion.marcarHito(EstadoAtencion.EN_EL_LUGAR, HOSPITAL));
		rechazaCon(CodigoError.TRANSICION_INVALIDA,
				() -> atencion.marcarHito(EstadoAtencion.PACIENTE_RECOGIDO, HOSPITAL));

		assertThat(atencion.getHoraLlegada()).isEqualTo(llegada);
		assertThat(atencion.getUbicacionLlegada()).isEqualTo(LUGAR);
	}

	@Test
	@DisplayName("PB-12 · los datos del paciente se corrigen mientras la atención sigue activa, y después ya no")
	void datosDelPaciente() {
		Atencion atencion = nueva();
		atencion.actualizarPaciente("Juan Pérez", null);
		atencion.actualizarPaciente("Juan Pérez Rojas", "4567890 SC");
		assertThat(atencion.getNombrePaciente()).isEqualTo("Juan Pérez Rojas");
		assertThat(atencion.getDocumentoPaciente()).isEqualTo("4567890 SC");

		atencion.cancelar(MotivoCancelacionAtencion.AVERIA);

		rechazaCon(CodigoError.ATENCION_FINALIZADA, () -> atencion.actualizarPaciente("Otro", null));
	}

	@Test
	@DisplayName("PB-12 · cancelar exige motivo, guarda la hora y es definitivo")
	void cancelar() {
		Atencion atencion = nueva();

		assertThatThrownBy(() -> atencion.cancelarPorLaTripulacion(null)).isInstanceOf(RuntimeException.class);
		atencion.cancelarPorLaTripulacion(MotivoCancelacionAtencion.DESVIADA);

		assertThat(atencion.getEstado()).isEqualTo(EstadoAtencion.CANCELADA);
		assertThat(atencion.getMotivoCancelacion()).isEqualTo(MotivoCancelacionAtencion.DESVIADA);
		assertThat(atencion.getHoraCancelacion()).isNotNull();
		rechazaCon(CodigoError.TRANSICION_INVALIDA,
				() -> atencion.cancelarPorLaTripulacion(MotivoCancelacionAtencion.OTRO));
		rechazaCon(CodigoError.TRANSICION_INVALIDA, () -> atencion.marcarHito(EstadoAtencion.EN_EL_LUGAR, LUGAR));
	}

	@ParameterizedTest(name = "{0}")
	@EnumSource(value = MotivoCancelacionAtencion.class,
			names = { "CANCELADA_POR_SOLICITANTE", "REASIGNADA", "CERRADA_POR_CENTRAL", "RECHAZADA_POR_PARAMEDICO" })
	@DisplayName("PB-12 · la tripulación de una emergencia no usa los motivos del sistema ni los de traslados")
	void motivosQueNoSonDeLaTripulacion(MotivoCancelacionAtencion motivo) {
		Atencion atencion = nueva();

		rechazaCon(CodigoError.VALIDACION, () -> atencion.cancelarPorLaTripulacion(motivo));
		assertThat(atencion.getEstado()).isEqualTo(EstadoAtencion.EN_CAMINO);
	}

	@Test
	@DisplayName("PB-12 · una atención ya entregada no se cancela")
	void entregadaNoSeCancela() {
		Atencion atencion = nueva();
		atencion.marcarHito(EstadoAtencion.EN_EL_LUGAR, LUGAR);
		atencion.marcarHito(EstadoAtencion.PACIENTE_RECOGIDO, LUGAR);
		atencion.marcarHito(EstadoAtencion.EN_HOSPITAL, HOSPITAL);
		atencion.entregar(HOSPITAL, null, null);

		rechazaCon(CodigoError.TRANSICION_INVALIDA,
				() -> atencion.cancelarPorLaTripulacion(MotivoCancelacionAtencion.OTRO));
	}

	private static Atencion nueva() {
		Incidente incidente = Incidente.crear(LUGAR, null, AHORA);
		return Atencion.iniciar(incidente, Ambulancia.registrar("ABC123", TipoUnidad.II),
				Usuario.registrarParamedico("Ana Rojas", "70000001"), AHORA);
	}

}
