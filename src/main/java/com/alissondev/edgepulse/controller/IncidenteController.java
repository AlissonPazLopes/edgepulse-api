package com.alissondev.edgepulse.controller;

import com.alissondev.edgepulse.dto.IncidenteRequest;
import com.alissondev.edgepulse.dto.IncidenteResponse;
import com.alissondev.edgepulse.dto.MetricasConfiabilidadeResponse;
import com.alissondev.edgepulse.dto.ResolucaoIncidenteRequest;
import com.alissondev.edgepulse.scheduler.HeartbeatWatchdogScheduler;
import com.alissondev.edgepulse.service.IncidenteService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/incidentes")
@RequiredArgsConstructor
public class IncidenteController {

	private final IncidenteService incidenteService;
	private final HeartbeatWatchdogScheduler watchdogScheduler;

	@GetMapping
	public ResponseEntity<List<IncidenteResponse>> listarTodos() {
		return ResponseEntity.ok(incidenteService.listarTodos());
	}

	@GetMapping("/dispositivo/{dispositivoId}")
	public ResponseEntity<List<IncidenteResponse>> listarPorDispositivo(@PathVariable Long dispositivoId) {
		return ResponseEntity.ok(incidenteService.listarPorDispositivo(dispositivoId));
	}

	@PostMapping
	public ResponseEntity<IncidenteResponse> abrirIncidenteManual(@Valid @RequestBody IncidenteRequest request) {
		IncidenteResponse criado = incidenteService.abrirIncidenteManual(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(criado);
	}

	@PatchMapping("/{id}/resolver")
	public ResponseEntity<IncidenteResponse> resolverIncidente(
			@PathVariable Long id,
			@RequestBody(required = false) ResolucaoIncidenteRequest request) {
		String nota = request != null ? request.notaResolucao() : null;
		return ResponseEntity.ok(incidenteService.resolverIncidente(id, nota));
	}

	@PostMapping("/watchdog/verificar")
	public ResponseEntity<Map<String, Object>> executarWatchdogManualmente() {
		int dispositivosEmTimeout = watchdogScheduler.verificarTimeoutsHeartbeat();
		return ResponseEntity.ok(Map.of(
				"dispositivosEmTimeout", dispositivosEmTimeout,
				"mensagem", "Varredura do Watchdog concluída com sucesso"
		));
	}

	@GetMapping("/dispositivo/{dispositivoId}/confiabilidade")
	public ResponseEntity<MetricasConfiabilidadeResponse> consultarMetricasConfiabilidade(@PathVariable Long dispositivoId) {
		return ResponseEntity.ok(incidenteService.calcularMetricasConfiabilidade(dispositivoId));
	}

	@ExceptionHandler(IllegalStateException.class)
	public ResponseEntity<Map<String, String>> handleIllegalState(IllegalStateException ex) {
		return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("erro", ex.getMessage()));
	}

	@ExceptionHandler(EntityNotFoundException.class)
	public ResponseEntity<Map<String, String>> handleNotFound(EntityNotFoundException ex) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("erro", ex.getMessage()));
	}

}
