package com.alissondev.edgepulse.service;

import com.alissondev.edgepulse.dto.ClusterRequestDTO;
import com.alissondev.edgepulse.dto.ClusterResponseDTO;
import com.alissondev.edgepulse.dto.DashboardClusterResponse;
import com.alissondev.edgepulse.dto.DispositivoRequestDTO;
import com.alissondev.edgepulse.dto.DispositivoResponseDTO;
import com.alissondev.edgepulse.dto.IncidenteRequest;
import com.alissondev.edgepulse.dto.ResumoGlobalPlataformaResponse;
import com.alissondev.edgepulse.dto.TelemetriaRequest;
import com.alissondev.edgepulse.model.enums.AmbienteCluster;
import com.alissondev.edgepulse.model.enums.SeveridadeIncidente;
import com.alissondev.edgepulse.model.enums.StatusDispositivo;
import com.alissondev.edgepulse.model.enums.StatusSaudeCluster;
import com.alissondev.edgepulse.model.enums.TipoAnomalia;
import com.alissondev.edgepulse.repository.ClusterRepository;
import com.alissondev.edgepulse.repository.DispositivoRepository;
import com.alissondev.edgepulse.repository.ImplantacaoOtaRepository;
import com.alissondev.edgepulse.repository.IncidenteRepository;
import com.alissondev.edgepulse.repository.LeituraTelemetriaRepository;
import com.alissondev.edgepulse.repository.ReleaseFirmwareRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ActiveProfiles("test")
class DashboardServiceTest {

	@Autowired
	private DashboardService dashboardService;

	@Autowired
	private ClusterService clusterService;

	@Autowired
	private DispositivoService dispositivoService;

	@Autowired
	private TelemetriaService telemetriaService;

	@Autowired
	private IncidenteService incidenteService;

	@Autowired
	private ImplantacaoOtaRepository implantacaoOtaRepository;

	@Autowired
	private ReleaseFirmwareRepository releaseFirmwareRepository;

	@Autowired
	private IncidenteRepository incidenteRepository;

	@Autowired
	private LeituraTelemetriaRepository leituraTelemetriaRepository;

	@Autowired
	private DispositivoRepository dispositivoRepository;

	@Autowired
	private ClusterRepository clusterRepository;

	@BeforeEach
	void setUp() {
		implantacaoOtaRepository.deleteAll();
		releaseFirmwareRepository.deleteAll();
		incidenteRepository.deleteAll();
		leituraTelemetriaRepository.deleteAll();
		dispositivoRepository.deleteAll();
		clusterRepository.deleteAll();
	}

	@Test
	void deveClassificarClusterComoSaudavelQuandoTodosDispositivosOnlineSemIncidentes() {
		ClusterResponseDTO cluster = clusterService.criarCluster(new ClusterRequestDTO(
				"Cluster-Saudavel",
				AmbienteCluster.PRODUCAO,
				"Planta Sul",
				30,
				true
		));

		DispositivoResponseDTO d1 = dispositivoService.cadastrarDispositivo(new DispositivoRequestDTO(
				cluster.id(), "Node-01", "AA:BB:CC:00:00:01", "ESP32-S3", "v1.0.0", true, true
		));
		DispositivoResponseDTO d2 = dispositivoService.cadastrarDispositivo(new DispositivoRequestDTO(
				cluster.id(), "Node-02", "AA:BB:CC:00:00:02", "ESP32-S3", "v1.0.0", true, true
		));

		telemetriaService.ingerirTelemetria(new TelemetriaRequest(
				d1.macAddress(), new BigDecimal("42.00"), new BigDecimal("3.30"), 90, -50, 180000L, true, true
		));
		telemetriaService.ingerirTelemetria(new TelemetriaRequest(
				d2.macAddress(), new BigDecimal("46.00"), new BigDecimal("3.30"), 95, -48, 185000L, true, true
		));

		DashboardClusterResponse dashboard = dashboardService.obterDashboardCluster(cluster.id());

		assertEquals(StatusSaudeCluster.SAUDAVEL, dashboard.statusSaudeCluster());
		assertEquals(2L, dashboard.totalDispositivos());
		assertEquals(2L, dashboard.dispositivosOnline());
		assertEquals(0L, dashboard.incidentesAtivos());
		assertEquals(new BigDecimal("44.00"), dashboard.temperaturaMediaClusterC());
		assertEquals(new BigDecimal("100.00"), dashboard.disponibilidadeMediaPercentual());
	}

	@Test
	void deveClassificarClusterComoAtencaoQuandoHouverDispositivoDegradadoOuIncidenteAltaSeveridade() {
		ClusterResponseDTO cluster = clusterService.criarCluster(new ClusterRequestDTO(
				"Cluster-Atencao",
				AmbienteCluster.PRODUCAO,
				"Planta Central",
				30,
				true
		));

		DispositivoResponseDTO d1 = dispositivoService.cadastrarDispositivo(new DispositivoRequestDTO(
				cluster.id(), "Node-A1", "AA:BB:CC:01:00:01", "ESP32-S3", "v1.0.0", true, true
		));
		DispositivoResponseDTO d2 = dispositivoService.cadastrarDispositivo(new DispositivoRequestDTO(
				cluster.id(), "Node-A2", "AA:BB:CC:01:00:02", "ESP32-S3", "v1.0.0", true, true
		));

		dispositivoService.atualizarStatus(d1.id(), StatusDispositivo.ONLINE);
		dispositivoService.atualizarStatus(d2.id(), StatusDispositivo.DEGRADADO);

		incidenteService.abrirIncidenteManual(new IncidenteRequest(
				d2.id(),
				TipoAnomalia.BATERIA_CRITICA,
				SeveridadeIncidente.ALTA,
				"Bateria em 8%"
		));

		DashboardClusterResponse dashboard = dashboardService.obterDashboardCluster(cluster.id());

		assertEquals(StatusSaudeCluster.ATENCAO, dashboard.statusSaudeCluster());
		assertEquals(1L, dashboard.dispositivosDegradados());
		assertEquals(1L, dashboard.incidentesAtivos());
		assertEquals(0L, dashboard.incidentesCriticosAtivos());
	}

	@Test
	void deveClassificarClusterComoCriticoQuandoMaisDe30PorcentoOfflineOuIncidenteCriticoAtivo() {
		ClusterResponseDTO clusterOffline = clusterService.criarCluster(new ClusterRequestDTO(
				"Cluster-Critico-Offline",
				AmbienteCluster.PRODUCAO,
				"Planta Norte",
				30,
				true
		));

		// 2 dispositivos: 1 ONLINE e 1 OFFLINE (50% > 30%)
		DispositivoResponseDTO d1 = dispositivoService.cadastrarDispositivo(new DispositivoRequestDTO(
				clusterOffline.id(), "Node-C1", "AA:BB:CC:02:00:01", "ESP32-S3", "v1.0.0", false, false
		));
		dispositivoService.cadastrarDispositivo(new DispositivoRequestDTO(
				clusterOffline.id(), "Node-C2", "AA:BB:CC:02:00:02", "ESP32-S3", "v1.0.0", false, false
		));
		dispositivoService.atualizarStatus(d1.id(), StatusDispositivo.ONLINE);

		DashboardClusterResponse dashOffline = dashboardService.obterDashboardCluster(clusterOffline.id());
		assertEquals(StatusSaudeCluster.CRITICO, dashOffline.statusSaudeCluster());

		// Cluster com todos dispositivos ONLINE/DEGRADADO, porém 1 incidente CRITICA ativo
		ClusterResponseDTO clusterIncCritico = clusterService.criarCluster(new ClusterRequestDTO(
				"Cluster-Critico-Incidente",
				AmbienteCluster.PRODUCAO,
				"Planta Leste",
				30,
				true
		));

		DispositivoResponseDTO d3 = dispositivoService.cadastrarDispositivo(new DispositivoRequestDTO(
				clusterIncCritico.id(), "Node-C3", "AA:BB:CC:03:00:01", "ESP32-S3", "v1.0.0", true, true
		));
		telemetriaService.ingerirTelemetria(new TelemetriaRequest(
				d3.macAddress(), new BigDecimal("89.50"), new BigDecimal("3.30"), 80, -55, 160000L, true, true
		));

		DashboardClusterResponse dashIncCritico = dashboardService.obterDashboardCluster(clusterIncCritico.id());
		assertEquals(StatusSaudeCluster.CRITICO, dashIncCritico.statusSaudeCluster());
		assertEquals(1L, dashIncCritico.incidentesCriticosAtivos());

		ResumoGlobalPlataformaResponse global = dashboardService.obterResumoGlobal();
		assertEquals(2L, global.totalClusters());
		assertEquals(3L, global.totalDispositivos());
		assertEquals(1L, global.totalLeiturasTelemetria());
		assertEquals(1L, global.totalAnomaliasDetectadas());
	}

}
