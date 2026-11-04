package com.alissondev.edgepulse.service;

import com.alissondev.edgepulse.dto.ClusterRequestDTO;
import com.alissondev.edgepulse.dto.ClusterResponseDTO;
import com.alissondev.edgepulse.dto.DispositivoRequestDTO;
import com.alissondev.edgepulse.dto.DispositivoResponseDTO;
import com.alissondev.edgepulse.dto.TelemetriaRequest;
import com.alissondev.edgepulse.dto.TelemetriaResponse;
import com.alissondev.edgepulse.event.AnomaliaDetectadaEvent;
import com.alissondev.edgepulse.model.enums.AmbienteCluster;
import com.alissondev.edgepulse.model.enums.StatusDispositivo;
import com.alissondev.edgepulse.model.enums.TipoAnomalia;
import com.alissondev.edgepulse.repository.ClusterRepository;
import com.alissondev.edgepulse.repository.DispositivoRepository;
import com.alissondev.edgepulse.repository.LeituraTelemetriaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@RecordApplicationEvents
class TelemetriaServiceTest {

	@Autowired
	private TelemetriaService telemetriaService;

	@Autowired
	private ClusterService clusterService;

	@Autowired
	private DispositivoService dispositivoService;

	@Autowired
	private LeituraTelemetriaRepository leituraTelemetriaRepository;

	@Autowired
	private DispositivoRepository dispositivoRepository;

	@Autowired
	private ClusterRepository clusterRepository;

	@Autowired
	private ApplicationEvents applicationEvents;

	private DispositivoResponseDTO dispositivoPadrao;

	@BeforeEach
	void setUp() {
		leituraTelemetriaRepository.deleteAll();
		dispositivoRepository.deleteAll();
		clusterRepository.deleteAll();

		ClusterResponseDTO cluster = clusterService.criarCluster(new ClusterRequestDTO(
				"Cluster-Telemetria-01",
				AmbienteCluster.PRODUCAO,
				"Planta Sul",
				30,
				true
		));

		dispositivoPadrao = dispositivoService.cadastrarDispositivo(new DispositivoRequestDTO(
				cluster.id(),
				"ESP32-Termico-01",
				"AA:BB:CC:00:11:22",
				"ESP32-S3",
				"v1.0.0",
				true,
				true
		));
	}

	@Test
	void deveTransicionarDeOfflineParaOnlineNoHeartbeatNormal() {
		TelemetriaRequest request = new TelemetriaRequest(
				"aa:bb:cc:00:11:22",
				new BigDecimal("45.50"),
				new BigDecimal("3.30"),
				85,
				-55,
				180000L,
				true,
				true
		);

		TelemetriaResponse response = telemetriaService.ingerirTelemetria(request);

		assertFalse(response.anomaliaDetectada());
		assertNull(response.detalhesAnomalia());
		assertEquals(StatusDispositivo.ONLINE, response.statusDispositivo());

		DispositivoResponseDTO dispAtualizado = dispositivoService.buscarPorId(dispositivoPadrao.id());
		assertEquals(StatusDispositivo.ONLINE, dispAtualizado.status());
		assertNotNull(dispAtualizado.ultimoHeartbeat());
		assertEquals(0, applicationEvents.stream(AnomaliaDetectadaEvent.class).count());
	}

	@Test
	void deveMudarStatusParaDegradadoEPublicarEventoEmMultiplasAnomalias() {
		TelemetriaRequest requestAnomalo = new TelemetriaRequest(
				"AA:BB:CC:00:11:22",
				new BigDecimal("85.20"), // > 80.0 -> SUPERAQUECIMENTO
				new BigDecimal("2.75"),  // < 3.0  -> SUBTENSAO_ELETRICA
				8,                       // < 10   -> BATERIA_CRITICA
				-78,
				95000L,
				false,                   // display ativo + false -> FALHA_PERIFERICO_DISPLAY
				false                    // audio ativo + false   -> FALHA_PERIFERICO_AUDIO
		);

		TelemetriaResponse response = telemetriaService.ingerirTelemetria(requestAnomalo);

		assertTrue(response.anomaliaDetectada());
		assertNotNull(response.detalhesAnomalia());
		assertEquals(StatusDispositivo.DEGRADADO, response.statusDispositivo());

		DispositivoResponseDTO dispAtualizado = dispositivoService.buscarPorId(dispositivoPadrao.id());
		assertEquals(StatusDispositivo.DEGRADADO, dispAtualizado.status());

		List<AnomaliaDetectadaEvent> eventos = applicationEvents.stream(AnomaliaDetectadaEvent.class).toList();
		assertEquals(1, eventos.size());

		AnomaliaDetectadaEvent evento = eventos.get(0);
		assertEquals(dispositivoPadrao.id(), evento.dispositivoId());
		assertTrue(evento.anomalias().containsAll(List.of(
				TipoAnomalia.SUPERAQUECIMENTO,
				TipoAnomalia.SUBTENSAO_ELETRICA,
				TipoAnomalia.BATERIA_CRITICA,
				TipoAnomalia.FALHA_PERIFERICO_DISPLAY,
				TipoAnomalia.FALHA_PERIFERICO_AUDIO
		)));
	}

	@Test
	void deveBloquearIngestaoSeDispositivoEstiverEmManutencao() {
		dispositivoService.atualizarStatus(dispositivoPadrao.id(), StatusDispositivo.MANUTENCAO);

		TelemetriaRequest request = new TelemetriaRequest(
				"AA:BB:CC:00:11:22",
				new BigDecimal("40.00"),
				new BigDecimal("3.30"),
				90,
				-50,
				200000L,
				true,
				true
		);

		assertThrows(IllegalStateException.class, () -> telemetriaService.ingerirTelemetria(request));
	}

}
