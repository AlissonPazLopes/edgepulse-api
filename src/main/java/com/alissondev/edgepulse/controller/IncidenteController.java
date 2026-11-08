package com.alissondev.edgepulse.controller;

import com.alissondev.edgepulse.dto.IncidenteRequest;
import com.alissondev.edgepulse.dto.IncidenteResponse;
import com.alissondev.edgepulse.dto.MetricasConfiabilidadeResponse;
import com.alissondev.edgepulse.dto.ResolucaoIncidenteRequest;
import com.alissondev.edgepulse.scheduler.HeartbeatWatchdogScheduler;
import com.alissondev.edgepulse.service.IncidenteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Incidentes, Watchdog e Confiabilidade (MTBF/MTTR)", description = "Gestão de incidentes operacionais, varredura de timeout de heartbeat e cálculo de MTBF, MTTR e Disponibilidade")
public class IncidenteController {

	private final IncidenteService incidenteService;
	private final HeartbeatWatchdogScheduler watchdogScheduler;

	@GetMapping
	@Operation(
			summary = "Listar todos os incidentes",
			description = "Retorna todos os incidentes registrados na plataforma."
	)
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Lista de incidentes retornada com sucesso")
	})
	public ResponseEntity<List<IncidenteResponse>> listarTodos() {
		return ResponseEntity.ok(incidenteService.listarTodos());
	}

	@GetMapping("/dispositivo/{dispositivoId}")
	@Operation(
			summary = "Listar incidentes por dispositivo",
			description = "Retorna o histórico de incidentes de um dispositivo específico ordenado por data de abertura decrescente."
	)
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Incidentes do dispositivo retornados com sucesso"),
			@ApiResponse(responseCode = "404", description = "Dispositivo não encontrado")
	})
	public ResponseEntity<List<IncidenteResponse>> listarPorDispositivo(@PathVariable Long dispositivoId) {
		return ResponseEntity.ok(incidenteService.listarPorDispositivo(dispositivoId));
	}

	@PostMapping
	@Operation(
			summary = "Abrir incidente manualmente",
			description = "Registra um novo incidente operacional para um dispositivo caso não exista outro ativo do mesmo tipo."
	)
	@ApiResponses({
			@ApiResponse(responseCode = "201", description = "Incidente aberto com sucesso"),
			@ApiResponse(responseCode = "400", description = "Dados da requisição inválidos"),
			@ApiResponse(responseCode = "404", description = "Dispositivo não encontrado"),
			@ApiResponse(responseCode = "409", description = "Já existe um incidente ativo do mesmo tipo para o dispositivo")
	})
	public ResponseEntity<IncidenteResponse> abrirIncidenteManual(@Valid @RequestBody IncidenteRequest request) {
		IncidenteResponse criado = incidenteService.abrirIncidenteManual(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(criado);
	}

	@PatchMapping("/{id}/resolver")
	@Operation(
			summary = "Resolver incidente e contabilizar MTTR",
			description = "Marca um incidente como RESOLVIDO, calcula o tempo de reparo em segundos e restaura o dispositivo para ONLINE se não houver outras falhas ativas."
	)
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Incidente resolvido com sucesso"),
			@ApiResponse(responseCode = "404", description = "Incidente não encontrado"),
			@ApiResponse(responseCode = "409", description = "Incidente já se encontra resolvido")
	})
	public ResponseEntity<IncidenteResponse> resolverIncidente(
			@PathVariable Long id,
			@RequestBody(required = false) ResolucaoIncidenteRequest request) {
		String nota = request != null ? request.notaResolucao() : null;
		return ResponseEntity.ok(incidenteService.resolverIncidente(id, nota));
	}

	@PostMapping("/watchdog/verificar")
	@Operation(
			summary = "Executar varredura manual do Watchdog de Heartbeat",
			description = "Verifica dispositivos ONLINE ou DEGRADADO cujo último heartbeat excedeu 3x o intervalo do cluster, alterando para OFFLINE e abrindo incidente crítico."
	)
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Varredura executada com sucesso")
	})
	public ResponseEntity<Map<String, Object>> executarWatchdogManualmente() {
		int dispositivosEmTimeout = watchdogScheduler.verificarTimeoutsHeartbeat();
		return ResponseEntity.ok(Map.of(
				"dispositivosEmTimeout", dispositivosEmTimeout,
				"mensagem", "Varredura do Watchdog concluída com sucesso"
		));
	}

	@GetMapping("/dispositivo/{dispositivoId}/confiabilidade")
	@Operation(
			summary = "Calcular métricas de confiabilidade (MTBF, MTTR e Disponibilidade)",
			description = "Calcula os indicadores de engenharia de dependabilidade do dispositivo com base no tempo monitorado e histórico de incidentes."
	)
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Métricas de confiabilidade calculadas com sucesso"),
			@ApiResponse(responseCode = "404", description = "Dispositivo não encontrado")
	})
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
