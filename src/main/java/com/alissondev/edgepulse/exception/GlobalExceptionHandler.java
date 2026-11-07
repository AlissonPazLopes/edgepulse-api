package com.alissondev.edgepulse.exception;

import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.OptimisticLockException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErroPadraoResponse> handleValidationExceptions(
			MethodArgumentNotValidException ex,
			HttpServletRequest request) {
		List<ErroPadraoResponse.CampoInvalido> campos = ex.getBindingResult()
				.getFieldErrors()
				.stream()
				.map(fe -> new ErroPadraoResponse.CampoInvalido(fe.getField(), fe.getDefaultMessage()))
				.toList();

		ErroPadraoResponse body = new ErroPadraoResponse(
				LocalDateTime.now(),
				HttpStatus.BAD_REQUEST.value(),
				HttpStatus.BAD_REQUEST.getReasonPhrase(),
				"Erro de validação nos campos informados",
				request.getRequestURI(),
				campos
		);
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<ErroPadraoResponse> handleIllegalArgument(
			IllegalArgumentException ex,
			HttpServletRequest request) {
		ErroPadraoResponse body = new ErroPadraoResponse(
				LocalDateTime.now(),
				HttpStatus.BAD_REQUEST.value(),
				HttpStatus.BAD_REQUEST.getReasonPhrase(),
				ex.getMessage(),
				request.getRequestURI(),
				List.of()
		);
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
	}

	@ExceptionHandler(IllegalStateException.class)
	public ResponseEntity<ErroPadraoResponse> handleIllegalState(
			IllegalStateException ex,
			HttpServletRequest request) {
		ErroPadraoResponse body = new ErroPadraoResponse(
				LocalDateTime.now(),
				HttpStatus.UNPROCESSABLE_ENTITY.value(),
				HttpStatus.UNPROCESSABLE_ENTITY.getReasonPhrase(),
				ex.getMessage(),
				request.getRequestURI(),
				List.of()
		);
		return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(body);
	}

	@ExceptionHandler(EntityNotFoundException.class)
	public ResponseEntity<ErroPadraoResponse> handleEntityNotFound(
			EntityNotFoundException ex,
			HttpServletRequest request) {
		ErroPadraoResponse body = new ErroPadraoResponse(
				LocalDateTime.now(),
				HttpStatus.NOT_FOUND.value(),
				HttpStatus.NOT_FOUND.getReasonPhrase(),
				ex.getMessage(),
				request.getRequestURI(),
				List.of()
		);
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
	}

	@ExceptionHandler({
			ConflitoConcorrenciaException.class,
			ObjectOptimisticLockingFailureException.class,
			OptimisticLockException.class
	})
	public ResponseEntity<ErroPadraoResponse> handleConcurrencyConflict(
			Exception ex,
			HttpServletRequest request) {
		String mensagem = ex instanceof ConflitoConcorrenciaException
				? ex.getMessage()
				: "Conflito de concorrência detectado: o recurso foi modificado por outra transação simultânea";

		ErroPadraoResponse body = new ErroPadraoResponse(
				LocalDateTime.now(),
				HttpStatus.CONFLICT.value(),
				HttpStatus.CONFLICT.getReasonPhrase(),
				mensagem,
				request.getRequestURI(),
				List.of()
		);
		return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
	}

}
