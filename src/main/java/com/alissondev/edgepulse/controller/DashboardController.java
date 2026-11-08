package com.alissondev.edgepulse.controller;

import com.alissondev.edgepulse.dto.DashboardClusterResponse;
import com.alissondev.edgepulse.dto.ResumoGlobalPlataformaResponse;
import com.alissondev.edgepulse.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard e Observabilidade", description = "Indicadores executivos de saúde operacional dos clusters, disponibilidade global e telemetria consolidada")
public class DashboardController {

	private final DashboardService dashboardService;

	@GetMapping("/global")
	@Operation(
			summary = "Obter resumo global da plataforma",
			description = "Consolida os totais de clusters, dispositivos por status, leituras de telemetria, anomalias, rollouts OTA e disponibilidade média global."
	)
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Resumo global retornado com sucesso")
	})
	public ResponseEntity<ResumoGlobalPlataformaResponse> obterResumoGlobal() {
		return ResponseEntity.ok(dashboardService.obterResumoGlobal());
	}

	@GetMapping("/clusters/{clusterId}")
	@Operation(
			summary = "Obter dashboard analítico de saúde de um cluster",
			description = "Retorna a distribuição de dispositivos por status, incidentes ativos, médias de temperatura/MTBF/MTTR/disponibilidade e classificação de saúde (SAUDAVEL, ATENCAO ou CRITICO)."
	)
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Dashboard do cluster calculado com sucesso"),
			@ApiResponse(responseCode = "404", description = "Cluster não encontrado")
	})
	public ResponseEntity<DashboardClusterResponse> obterDashboardCluster(@PathVariable Long clusterId) {
		return ResponseEntity.ok(dashboardService.obterDashboardCluster(clusterId));
	}

	@GetMapping("/clusters")
	@Operation(
			summary = "Listar dashboards de todos os clusters ativos",
			description = "Retorna a visão analítica consolidada de saúde operacional para todos os clusters ativos."
	)
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Lista de dashboards retornada com sucesso")
	})
	public ResponseEntity<List<DashboardClusterResponse>> listarDashboardsClustersAtivos() {
		return ResponseEntity.ok(dashboardService.listarDashboardsClustersAtivos());
	}

}
