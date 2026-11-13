package com.uem.ambulancias.cobros.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.uem.ambulancias.comun.error.CodigoError;
import com.uem.ambulancias.comun.error.ConflictoException;
import com.uem.ambulancias.emergencias.domain.Atencion;
import com.uem.ambulancias.usuarios.domain.Usuario;

@Tag("PB-29")
class CobroTest {

	private static final Instant AHORA = Instant.parse("2026-11-13T15:00:00Z");

	private final Usuario admin = mock(Usuario.class);

	private Cobro cobro(boolean deTraslado) {
		Atencion atencion = mock(Atencion.class);
		when(atencion.esDeTraslado()).thenReturn(deTraslado);
		return Cobro.registrar(atencion, ModalidadPago.PAGO_DIRECTO, new BigDecimal("350.00"), EstadoCobro.PENDIENTE,
				admin, AHORA);
	}

	@Test
	@DisplayName("PB-29 · el tipo de servicio sale de la atención: emergencia o traslado")
	void tipoDeServicio() {
		assertThat(cobro(false).getTipoServicio()).isEqualTo(TipoServicio.EMERGENCIA);
		assertThat(cobro(true).getTipoServicio()).isEqualTo(TipoServicio.TRASLADO);
	}

	@Test
	@DisplayName("PB-29 · el ajuste aplica lo nuevo y devuelve lo que había")
	void ajuste() {
		Cobro cobro = cobro(false);

		CambioDeCobro cambio = cobro.ajustar(ModalidadPago.SOAT, new BigDecimal("300"), EstadoCobro.PAGADO,
				"Lo cubrió el SOAT", admin, AHORA);

		assertThat(cobro.getImporte()).isEqualByComparingTo("300");
		assertThat(cobro.getEstado()).isEqualTo(EstadoCobro.PAGADO);
		assertThat(cambio.getTipo()).isEqualTo(TipoCambioDeCobro.AJUSTE);
		assertThat(cambio.getImporteAnterior()).isEqualByComparingTo("350");
		assertThat(cambio.getModalidadAnterior()).isEqualTo(ModalidadPago.PAGO_DIRECTO);
		assertThat(cambio.getEstadoAnterior()).isEqualTo(EstadoCobro.PENDIENTE);
		assertThat(cambio.getMotivo()).isEqualTo("Lo cubrió el SOAT");
	}

	@Test
	@DisplayName("PB-29 · un ajuste que no cambia nada se rechaza: no deja un cambio vacío en el historial")
	void ajusteSinCambios() {
		Cobro cobro = cobro(false);

		assertThatThrownBy(() -> cobro.ajustar(ModalidadPago.PAGO_DIRECTO, new BigDecimal("350"),
				EstadoCobro.PENDIENTE, "nada", admin, AHORA))
				.isInstanceOf(ConflictoException.class);
	}

	@Test
	@DisplayName("PB-29 · un cobro anulado ya no se ajusta ni se vuelve a anular")
	void anuladoEsFinal() {
		Cobro cobro = cobro(false);
		CambioDeCobro anulacion = cobro.anular("Error de carga", admin, AHORA);

		assertThat(anulacion.getEstadoNuevo()).isEqualTo(EstadoCobro.ANULADO);
		assertThat(anulacion.getImporteAnterior()).isEqualByComparingTo("350");
		assertThatThrownBy(() -> cobro.anular("otra vez", admin, AHORA))
				.isInstanceOfSatisfying(ConflictoException.class,
						e -> assertThat(e.getCodigo()).isEqualTo(CodigoError.COBRO_ANULADO));
		assertThatThrownBy(() -> cobro.ajustar(ModalidadPago.SOAT, BigDecimal.ONE, EstadoCobro.PAGADO, "x", admin,
				AHORA)).isInstanceOf(ConflictoException.class);
	}

	@Test
	@DisplayName("PB-29 · a anulado no se llega ajustando: se anula, con su motivo")
	void anuladoSoloAnulando() {
		Cobro cobro = cobro(false);

		assertThatThrownBy(() -> cobro.ajustar(ModalidadPago.PAGO_DIRECTO, new BigDecimal("350"),
				EstadoCobro.ANULADO, "x", admin, AHORA)).isInstanceOf(ConflictoException.class);
	}
}
