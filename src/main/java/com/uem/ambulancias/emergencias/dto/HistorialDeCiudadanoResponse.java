package com.uem.ambulancias.emergencias.dto;

import java.time.Instant;
import java.util.List;

import com.uem.ambulancias.emergencias.domain.Alerta;
import com.uem.ambulancias.emergencias.domain.EstadoAlerta;
import com.uem.ambulancias.emergencias.domain.EstadoIncidente;
import com.uem.ambulancias.emergencias.domain.MotivoCancelacionAlerta;
import com.uem.ambulancias.usuarios.domain.SancionCiudadano;
import com.uem.ambulancias.usuarios.domain.TipoSancion;
import com.uem.ambulancias.usuarios.domain.Usuario;

/**
 * Lo que la central revisa antes de decidir sobre una cuenta: las falsas alarmas verificadas con su fundamento,
 * aparte el resto de sus alertas (las que retiró y las que se atendieron), y lo que ya se hizo con la cuenta.
 */
public record HistorialDeCiudadanoResponse(
		Long id,
		String nombre,
		String telefono,
		boolean activo,
		List<FalsaAlarma> falsasAlarmas,
		List<OtraAlerta> otrasAlertas,
		List<Sancion> sanciones) {

	/** Una alerta cuyo incidente la central cerró como falsa alarma, con cómo se verificó y quién lo hizo. */
	public record FalsaAlarma(Long alertaId, Long incidenteId, Instant fechaHora, String fundamento,
			String verificadaPor, Instant fechaCierre, MotivoCancelacionAlerta retiradaComo) {

		static FalsaAlarma de(Alerta alerta) {
			var incidente = alerta.getIncidente();
			return new FalsaAlarma(alerta.getId(), incidente.getId(), alerta.getFechaHora(),
					incidente.getDetalleCierre(), incidente.getCerradoPor().getNombreCompleto(),
					incidente.getFechaHoraCierre(), alerta.getMotivoCancelacion());
		}
	}

	/**
	 * Cualquier otra alerta: atendida, retirada por el propio ciudadano o cerrada por otra causa. Ninguna cuenta como
	 * falsa alarma, aunque la haya retirado diciendo que lo era.
	 */
	public record OtraAlerta(Long alertaId, Long incidenteId, Instant fechaHora, EstadoAlerta estado,
			MotivoCancelacionAlerta motivoRetiro, EstadoIncidente estadoIncidente) {

		static OtraAlerta de(Alerta alerta) {
			return new OtraAlerta(alerta.getId(), alerta.getIncidente().getId(), alerta.getFechaHora(),
					alerta.getEstado(), alerta.getMotivoCancelacion(), alerta.getIncidente().getEstado());
		}
	}

	public record Sancion(TipoSancion tipo, String motivo, Instant fechaHora, String administrador) {

		static Sancion de(SancionCiudadano sancion) {
			return new Sancion(sancion.getTipo(), sancion.getMotivo(), sancion.getFechaHora(),
					sancion.getAdministrador().getNombreCompleto());
		}
	}

	public static HistorialDeCiudadanoResponse de(Usuario ciudadano, List<Alerta> falsas, List<Alerta> todas,
			List<SancionCiudadano> sanciones) {
		var idsFalsas = falsas.stream().map(Alerta::getId).toList();
		return new HistorialDeCiudadanoResponse(ciudadano.getId(), ciudadano.getNombreCompleto(),
				ciudadano.getTelefono(), ciudadano.isActivo(),
				falsas.stream().map(FalsaAlarma::de).toList(),
				todas.stream().filter(a -> !idsFalsas.contains(a.getId())).map(OtraAlerta::de).toList(),
				sanciones.stream().map(Sancion::de).toList());
	}

}
