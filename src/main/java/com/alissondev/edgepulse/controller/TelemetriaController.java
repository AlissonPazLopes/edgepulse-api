package com.alissondev.edgepulse.controller;

import com.alissondev.edgepulse.dto.ResumoTelemetriaDispositivoResponse;
import com.alissondev.edgepulse.dto.TelemetriaRequest;
import com.alissondev.edgepulse.dto.TelemetriaResponse;
import com.alissondev.edgepulse.service.TelemetriaService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/telemetria")
@RequiredArgsConstructor
public class TelemetriaController {

	private final TelemetriaService telemetriaService;

	@PostMapping
	public ResponseEntity<TelemetriaResponse> ingerirTelemetria(@Valid @RequestBody TelemetriaRequest request) {
		TelemetriaResponse response = telemetriaService.ingerirTelemetria(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	@GetMapping("/dispositivo/{dispositivoId}")
	public ResponseEntity<List<TelemetriaResponse>> listarHistoricoPorDispositivo(@PathVariable Long dispositivoId) {
		return ResponseEntity.ok(telemetriaService.listarHistoricoPorDispositivo(dispositivoId));
	}

	@GetMapping("/dispositivo/{dispositivoId}/ultima")
	public ResponseEntity<TelemetriaResponse> buscarUltimaLeitura(@PathVariable Long dispositivoId) {
		return ResponseEntity.ok(telemetriaService.buscarUltimaLeitura(dispositivoId));
	}

	@GetMapping("/anomalias")
	public ResponseEntity<List<TelemetriaResponse>> listarLeiturasComAnomalia() {
		return ResponseEntity.ok(telemetriaService.listarLeiturasComAnomalia());
	}

	@GetMapping("/dispositivo/{dispositivoId}/resumo")
	public ResponseEntity<ResumoTelemetriaDispositivoResponse> obterResumoDispositivo(@PathVariable Long dispositivoId) {
		return ResponseEntity.ok(telemetriaService.obterResumoDispositivo(dispositivoId));
	}

	@ExceptionHandler(IllegalStateException.class)
	public ResponseEntity<Map<String, String>> handleIllegalState(IllegalStateException ex) {
		return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(Map.of("erro", ex.getMessage()));
	}

	@ExceptionHandler(EntityNotFoundException.class)
	public ResponseEntity<Map<String, String>> handleNotFound(EntityNotFoundException ex) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("erro", ex.getMessage()));
	}

}
