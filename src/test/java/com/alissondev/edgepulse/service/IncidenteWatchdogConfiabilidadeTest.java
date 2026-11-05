package com.alissondev.edgepulse.service;

import com.alissondev.edgepulse.dto.ClusterRequestDTO;
import com.alissondev.edgepulse.dto.ClusterResponseDTO;
import com.alissondev.edgepulse.dto.DispositivoRequestDTO;
import com.alissondev.edgepulse.dto.DispositivoResponseDTO;
import com.alissondev.edgepulse.dto.IncidenteResponse;
import com.alissondev.edgepulse.dto.MetricasConfiabilidadeResponse;
import com.alissondev.edgepulse.dto.TelemetriaRequest;
import com.alissondev.edgepulse.model.Dispositivo;
import com.alissondev.edgepulse.model.Incidente;
import com.alissondev.edgepulse.model.enums.AmbienteCluster;
import com.alissondev.edgepulse.model.enums.SeveridadeIncidente;
import com.alissondev.edgepulse.model.enums.StatusDispositivo;
import com.alissondev.edgepulse.model.enums.StatusIncidente;
import com.alissondev.edgepulse.model.enums.TipoAnomalia;
import com.alissondev.edgepulse.repository.ClusterRepository;
import com.alissondev.edgepulse.repository.DispositivoRepository;
import com.alissondev.edgepulse.repository.IncidenteRepository;
import com.alissondev.edgepulse.repository.LeituraTelemetriaRepository;
import com.alissondev.edgepulse.scheduler.HeartbeatWatchdogScheduler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
class IncidenteWatchdogConfiabilidadeTest {

	@Autowired
	private IncidenteService incidenteService;

	@Autowired
	private TelemetriaService telemetriaService;

	@Autowired
	private ClusterService clusterService;

	@Autowired
	private DispositivoService dispositivoService;

	@Autowired
	private HeartbeatWatchdogScheduler watchdogScheduler;

	@Autowired
	private IncidenteRepository incidenteRepository;

	@Autowired
	private LeituraTelemetriaRepository leituraTelemetriaRepository;

	@Autowired
	private DispositivoRepository dispositivoRepository;

	@Autowired
	private ClusterRepository clusterRepository;

	private DispositivoResponseDTO dispositivoPadrao;

	@BeforeEach
	void setUp() {
		incidenteRepository.deleteAll();
		leituraTelemetriaRepository.deleteAll();
		dispositivoRepository.deleteAll();
		clusterRepository.deleteAll();

		ClusterResponseDTO cluster = clusterService.criarCluster(new ClusterRequestDTO(
				"Cluster-Confiabilidade-01",
				AmbienteCluster.PRODUCAO,
				"Planta Industrial B",
				10,
				true
		));

		dispositivoPadrao = dispositivoService.cadastrarDispositivo(new DispositivoRequestDTO(
				cluster.id(),
				"ESP32-Watchdog-01",
				"AA:BB:CC:DD:00:11",
				"ESP32-S3",
				"v1.0.0",
				true,
				true
		));
	}

	@Test
	void deveAbrirIncidentesAutomaticamenteViaEventoDeAnomaliaSemDuplicarEVoltarParaOnlineAoResolver() {
		TelemetriaRequest anomaliaReq = new TelemetriaRequest(
				"AA:BB:CC:DD:00:11",
				new BigDecimal("88.00"), // SUPERAQUECIMENTO (CRITICA)
				new BigDecimal("3.30"),
				80,
				-50,
				150000L,
				false,                   // FALHA_PERIFERICO_DISPLAY (ALTA)
				true
		);

		telemetriaService.ingerirTelemetria(anomaliaReq);
		// Ingerir novamente a mesma anomalia não deve duplicar incidentes ativos
		telemetriaService.ingerirTelemetria(anomaliaReq);

		List<IncidenteResponse> incidentes = incidenteService.listarPorDispositivo(dispositivoPadrao.id());
		assertEquals(2, incidentes.size());

		for (IncidenteResponse inc : incidentes) {
			IncidenteResponse resolvido = incidenteService.resolverIncidente(inc.id(), "Cooler e display reiniciados");
			assertEquals(StatusIncidente.RESOLVIDO, resolvido.status());
			assertNotNull(resolvido.dataResolucao());
			assertTrue(resolvido.tempoReparoSegundos() >= 1L);
		}

		DispositivoResponseDTO dispRestaurado = dispositivoService.buscarPorId(dispositivoPadrao.id());
		assertEquals(StatusDispositivo.ONLINE, dispRestaurado.status());
	}

	@Test
	void deveDetectarTimeoutNoWatchdogMudarParaOfflineEAbrirIncidenteCritico() {
		Dispositivo entidade = dispositivoRepository.findById(dispositivoPadrao.id()).orElseThrow();
		entidade.setStatus(StatusDispositivo.ONLINE);
		// Limite do cluster é 10s * 3 = 30s; colocamos último heartbeat há 45s
		entidade.setUltimoHeartbeat(LocalDateTime.now().minusSeconds(45));
		dispositivoRepository.save(entidade);

		int emTimeout = watchdogScheduler.verificarTimeoutsHeartbeat();
		assertEquals(1, emTimeout);

		DispositivoResponseDTO atualizado = dispositivoService.buscarPorId(dispositivoPadrao.id());
		assertEquals(StatusDispositivo.OFFLINE, atualizado.status());

		List<IncidenteResponse> incidentes = incidenteService.listarPorDispositivo(dispositivoPadrao.id());
		assertEquals(1, incidentes.size());
		assertEquals(TipoAnomalia.HEARTBEAT_TIMEOUT, incidentes.get(0).tipoAnomalia());
		assertEquals(SeveridadeIncidente.CRITICA, incidentes.get(0).severidade());
	}

	@Test
	void deveCalcularMtbfMttrEDisponibilidadeCorretamente() {
		Dispositivo entidade = dispositivoRepository.findById(dispositivoPadrao.id()).orElseThrow();
		// Dispositivo cadastrado há 1000 segundos
		LocalDateTime agora = LocalDateTime.now();
		entidade.setDataCadastro(agora.minusSeconds(1000));
		dispositivoRepository.save(entidade);

		// Criamos 2 incidentes resolvidos de 100s cada (soma reparo = 200s, MTTR = 100s)
		Incidente inc1 = Incidente.builder()
				.dispositivo(entidade)
				.tipoAnomalia(TipoAnomalia.SUPERAQUECIMENTO)
				.severidade(SeveridadeIncidente.CRITICA)
				.descricao("Falha 1")
				.status(StatusIncidente.RESOLVIDO)
				.dataAbertura(agora.minusSeconds(800))
				.dataResolucao(agora.minusSeconds(700))
				.tempoReparoSegundos(100L)
				.build();

		Incidente inc2 = Incidente.builder()
				.dispositivo(entidade)
				.tipoAnomalia(TipoAnomalia.SUBTENSAO_ELETRICA)
				.severidade(SeveridadeIncidente.CRITICA)
				.descricao("Falha 2")
				.status(StatusIncidente.RESOLVIDO)
				.dataAbertura(agora.minusSeconds(400))
				.dataResolucao(agora.minusSeconds(300))
				.tempoReparoSegundos(100L)
				.build();

		incidenteRepository.saveAll(List.of(inc1, inc2));

		MetricasConfiabilidadeResponse metricas = incidenteService.calcularMetricasConfiabilidade(dispositivoPadrao.id());

		assertEquals(2L, metricas.totalIncidentes());
		assertEquals(2L, metricas.incidentesResolvidos());
		assertEquals(0L, metricas.incidentesAtivos());
		assertEquals(200L, metricas.tempoDowntimeTotalSegundos());
		assertEquals(100.0, metricas.mttrSegundos());
		// Uptime ~ 800s -> MTBF ~ 400s -> Disponibilidade = 400 / (400 + 100) * 100 = 80.00%
		assertTrue(metricas.mtbfSegundos() >= 395.0 && metricas.mtbfSegundos() <= 405.0);
		assertTrue(metricas.disponibilidadePercentual().compareTo(new BigDecimal("79.50")) >= 0
				&& metricas.disponibilidadePercentual().compareTo(new BigDecimal("80.50")) <= 0);
	}

}
