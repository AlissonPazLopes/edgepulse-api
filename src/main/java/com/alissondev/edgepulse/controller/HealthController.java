package com.alissondev.edgepulse.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/health")
@Tag(name = "Health Check", description = "Verificação de disponibilidade da API e do motor de telemetria")
public class HealthController {

	@GetMapping
	@Operation(
			summary = "Verificar status de saúde da API",
			description = "Retorna o status operacional do serviço edgepulse-api e do motor de ingestão de telemetria."
	)
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Serviço ativo e operacional")
	})
	public ResponseEntity<Map<String, Object>> checkHealth() {
		Map<String, Object> response = new LinkedHashMap<>();
		response.put("status", "UP");
		response.put("servico", "edgepulse-api");
		response.put("motorTelemetria", "ATIVO");
		response.put("timestamp", Instant.now().toString());
		return ResponseEntity.ok(response);
	}

}
