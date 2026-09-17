package com.uem.ambulancias.comun.error;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Traduce las excepciones a respuestas de error con un mismo formato (Problem Details) y un campo
 * {@code codigo}.
 */
@RestControllerAdvice
public class ManejadorErrores {

	@ExceptionHandler(ConflictoException.class)
	ProblemDetail conflicto(ConflictoException e) {
		return problema(HttpStatus.CONFLICT, e.getCodigo(), e.getMessage());
	}

	@ExceptionHandler(NoEncontradoException.class)
	ProblemDetail noEncontrado(NoEncontradoException e) {
		return problema(HttpStatus.NOT_FOUND, CodigoError.NO_ENCONTRADO, e.getMessage());
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	ProblemDetail validacion(MethodArgumentNotValidException e) {
		List<Map<String, String>> errores = e.getBindingResult().getFieldErrors().stream()
				.map(error -> Map.of("campo", error.getField(), "mensaje", String.valueOf(error.getDefaultMessage())))
				.toList();
		ProblemDetail problema = problema(HttpStatus.BAD_REQUEST, CodigoError.VALIDACION, "Hay campos inválidos.");
		problema.setProperty("errores", errores);
		return problema;
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	ProblemDetail cuerpoInvalido(HttpMessageNotReadableException e) {
		return problema(HttpStatus.BAD_REQUEST, CodigoError.VALIDACION, "El cuerpo de la petición no es válido.");
	}

	@ExceptionHandler(MissingRequestHeaderException.class)
	ProblemDetail cabeceraFaltante(MissingRequestHeaderException e) {
		return problema(HttpStatus.BAD_REQUEST, CodigoError.VALIDACION, "Falta la cabecera " + e.getHeaderName() + ".");
	}

	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	ProblemDetail tipoInvalido(MethodArgumentTypeMismatchException e) {
		return problema(HttpStatus.BAD_REQUEST, CodigoError.VALIDACION, "El valor de " + e.getName() + " no es válido.");
	}

	public static ProblemDetail problema(HttpStatus estado, CodigoError codigo, String detalle) {
		ProblemDetail problema = ProblemDetail.forStatusAndDetail(estado, detalle);
		problema.setProperty("codigo", codigo.name());
		return problema;
	}

}
