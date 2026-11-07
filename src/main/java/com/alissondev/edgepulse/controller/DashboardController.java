package com.alissondev.edgepulse.controller;

import com.alissondev.edgepulse.dto.DashboardClusterResponse;
import com.alissondev.edgepulse.dto.ResumoGlobalPlataformaResponse;
import com.alissondev.edgepulse.service.DashboardService;
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
public class DashboardController {

	private final DashboardService dashboardService;

	@GetMapping("/global")
	public ResponseEntity<ResumoGlobalPlataformaResponse> obterResumoGlobal() {
		return ResponseEntity.ok(dashboardService.obterResumoGlobal());
	}

	@GetMapping("/clusters/{clusterId}")
	public ResponseEntity<DashboardClusterResponse> obterDashboardCluster(@PathVariable Long clusterId) {
		return ResponseEntity.ok(dashboardService.obterDashboardCluster(clusterId));
	}

	@GetMapping("/clusters")
	public ResponseEntity<List<DashboardClusterResponse>> listarDashboardsClustersAtivos() {
		return ResponseEntity.ok(dashboardService.listarDashboardsClustersAtivos());
	}

}
