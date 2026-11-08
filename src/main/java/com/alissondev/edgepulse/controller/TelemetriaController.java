package com.alissondev.edgepulse.controller;

import com.alissondev.edgepulse.dto.ResumoTelemetriaDispositivoResponse;
import com.alissondev.edgepulse.dto.TelemetriaRequest;
import com.alissondev.edgepulse.dto.TelemetriaResponse;
import com.alissondev.edgepulse.service.TelemetriaService;
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
@Tag(name = "Telemetria e Anomalias", description = "Ingestão de pacotes de telemetria, atualização de heartbeat e detecção automática de anomalias")
public class TelemetriaController {

	private final TelemetriaService telemetriaService;

	@PostMapping
	@Operation(
			summary = "Ingerir pacote de telemetria",
			description = "Recebe leituras de temperatura, tensão, bateria, sinal WiFi RSSI e periféricos do dispositivo, atualiza o heartbeat e aciona o detector automático de anomalias."
	)
	@ApiResponses({
			@ApiResponse(responseCode = "201", description = "Leitura de telemetria processada e armazenada com sucesso"),
			@ApiResponse(responseCode = "400", description = "Dados de telemetria inválidos"),
			@ApiResponse(responseCode = "404", description = "Dispositivo não encontrado para o MAC Address informado"),
			@ApiResponse(responseCode = "409", description = "Conflito de concorrência ao atualizar heartbeat/status do dispositivo"),
			@ApiResponse(responseCode = "422", description = "Ingestão bloqueada pois o dispositivo está em MANUTENCAO")
	})
	public ResponseEntity<TelemetriaResponse> ingerirTelemetria(@Valid @RequestBody TelemetriaRequest request) {
		TelemetriaResponse response = telemetriaService.ingerirTelemetria(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	@GetMapping("/dispositivo/{dispositivoId}")
	@Operation(
			summary = "Listar histórico de telemetria por dispositivo",
			description = "Retorna todas as leituras de telemetria de um dispositivo ordenadas da mais recente para a mais antiga."
	)
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Histórico retornado com sucesso"),
			@ApiResponse(responseCode = "404", description = "Dispositivo não encontrado")
	})
	public ResponseEntity<List<TelemetriaResponse>> listarHistoricoPorDispositivo(@PathVariable Long dispositivoId) {
		return ResponseEntity.ok(telemetriaService.listarHistoricoPorDispositivo(dispositivoId));
	}

	@GetMapping("/dispositivo/{dispositivoId}/ultima")
	@Operation(
			summary = "Buscar última leitura de telemetria",
			description = "Retorna o pacote de telemetria mais recente enviado pelo dispositivo."
	)
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Última leitura retornada com sucesso"),
			@ApiResponse(responseCode = "404", description = "Nenhuma leitura encontrada para o dispositivo")
	})
	public ResponseEntity<TelemetriaResponse> buscarUltimaLeitura(@PathVariable Long dispositivoId) {
		return ResponseEntity.ok(telemetriaService.buscarUltimaLeitura(dispositivoId));
	}

	@GetMapping("/anomalias")
	@Operation(
			summary = "Listar leituras com anomalias detectadas",
			description = "Retorna todas as leituras de telemetria nas quais foram identificadas anomalias térmicas, elétricas ou de periféricos."
	)
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Leituras anômalas retornadas com sucesso")
	})
	public ResponseEntity<List<TelemetriaResponse>> listarLeiturasComAnomalia() {
		return ResponseEntity.ok(telemetriaService.listarLeiturasComAnomalia());
	}

	@GetMapping("/dispositivo/{dispositivoId}/resumo")
	@Operation(
			summary = "Obter resumo estatístico de telemetria do dispositivo",
			description = "Calcula médias de temperatura de CPU e tensão elétrica, total de pacotes recebidos e total de anomalias do nó."
	)
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Resumo estatístico calculado com sucesso"),
			@ApiResponse(responseCode = "404", description = "Dispositivo não encontrado")
	})
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
