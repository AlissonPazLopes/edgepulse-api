package com.alissondev.edgepulse.service;

import com.alissondev.edgepulse.dto.ClusterRequestDTO;
import com.alissondev.edgepulse.dto.ClusterResponseDTO;
import com.alissondev.edgepulse.dto.DispositivoRequestDTO;
import com.alissondev.edgepulse.dto.DispositivoResponseDTO;
import com.alissondev.edgepulse.model.enums.AmbienteCluster;
import com.alissondev.edgepulse.model.enums.StatusDispositivo;
import com.alissondev.edgepulse.repository.ClusterRepository;
import com.alissondev.edgepulse.repository.DispositivoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
class ClusterDispositivoServiceTest {

	@Autowired
	private ClusterService clusterService;

	@Autowired
	private DispositivoService dispositivoService;

	@Autowired
	private ClusterRepository clusterRepository;

	@Autowired
	private DispositivoRepository dispositivoRepository;

	@Autowired
	private CacheManager cacheManager;

	@BeforeEach
	void setUp() {
		dispositivoRepository.deleteAll();
		clusterRepository.deleteAll();
		if (cacheManager.getCache("clusters") != null) {
			cacheManager.getCache("clusters").clear();
		}
		if (cacheManager.getCache("dispositivos") != null) {
			cacheManager.getCache("dispositivos").clear();
		}
	}

	@Test
	void deveImpedirCadastroDeClusterComNomeDuplicado() {
		ClusterRequestDTO request = new ClusterRequestDTO(
				"Cluster-Sul-01",
				AmbienteCluster.PRODUCAO,
				"DataCenter Curitiba",
				30,
				true
		);

		clusterService.criarCluster(request);

		IllegalArgumentException exception = assertThrows(
				IllegalArgumentException.class,
				() -> clusterService.criarCluster(request)
		);
		assertTrue(exception.getMessage().contains("Cluster-Sul-01"));
	}

	@Test
	void deveNormalizarMacAddressParaMaiusculasEInicializarComStatusOffline() {
		ClusterResponseDTO cluster = clusterService.criarCluster(new ClusterRequestDTO(
				"Cluster-Lab-ESP32",
				AmbienteCluster.LABORATORIO,
				"Bancada IoT",
				15,
				true
		));

		DispositivoRequestDTO dispRequest = new DispositivoRequestDTO(
				cluster.id(),
				"ESP32-Node-01",
				"aa:bb:cc:11:22:33",
				"ESP32-WROOM-32",
				"v1.0.0",
				true,
				false
		);

		DispositivoResponseDTO response = dispositivoService.cadastrarDispositivo(dispRequest);

		assertEquals("AA:BB:CC:11:22:33", response.macAddress());
		assertEquals(StatusDispositivo.OFFLINE, response.status());
		assertTrue(response.perifericoDisplayAtivo());
	}

	@Test
	void deveImpedirCadastroDeDispositivoComMacAddressDuplicado() {
		ClusterResponseDTO cluster = clusterService.criarCluster(new ClusterRequestDTO(
				"Cluster-Prod-01",
				AmbienteCluster.PRODUCAO,
				"Planta Industrial A",
				30,
				true
		));

		DispositivoRequestDTO req1 = new DispositivoRequestDTO(
				cluster.id(),
				"ESP32-Sensor-A",
				"aa:bb:cc:dd:ee:ff",
				"ESP32-S3",
				"v1.2.0",
				true,
				true
		);

		DispositivoRequestDTO req2 = new DispositivoRequestDTO(
				cluster.id(),
				"ESP32-Sensor-B",
				"AA:BB:CC:DD:EE:FF",
				"ESP32-S3",
				"v1.2.0",
				false,
				false
		);

		dispositivoService.cadastrarDispositivo(req1);

		assertThrows(IllegalArgumentException.class, () -> dispositivoService.cadastrarDispositivo(req2));
	}

	@Test
	void deveImpedirVinculacaoDeDispositivoAClusterInativo() {
		ClusterResponseDTO clusterInativo = clusterService.criarCluster(new ClusterRequestDTO(
				"Cluster-Desativado",
				AmbienteCluster.HOMOLOGACAO,
				"Sala Cofre",
				60,
				false
		));

		DispositivoRequestDTO request = new DispositivoRequestDTO(
				clusterInativo.id(),
				"ESP32-Orfao",
				"11:22:33:44:55:66",
				"ESP32-C3",
				"v1.0.0",
				false,
				false
		);

		assertThrows(IllegalStateException.class, () -> dispositivoService.cadastrarDispositivo(request));
	}

	@Test
	void deveArmazenarNoCacheELimparAoAtualizarDispositivo() {
		ClusterResponseDTO cluster = clusterService.criarCluster(new ClusterRequestDTO(
				"Cluster-Cache-Test",
				AmbienteCluster.PRODUCAO,
				"Nuvem Edge",
				30,
				true
		));

		DispositivoResponseDTO salvo = dispositivoService.cadastrarDispositivo(new DispositivoRequestDTO(
				cluster.id(),
				"ESP32-Cache-Node",
				"a1:b2:c3:d4:e5:f6",
				"ESP32-WROVER",
				"v2.0.0",
				true,
				true
		));

		// Popula cache na busca por ID
		dispositivoService.buscarPorId(salvo.id());
		assertNotNull(cacheManager.getCache("dispositivos").get(salvo.id()));

		// Atualiza status e verifica limpeza do cache (@CacheEvict)
		DispositivoResponseDTO atualizado = dispositivoService.atualizarStatus(salvo.id(), StatusDispositivo.ONLINE);
		assertEquals(StatusDispositivo.ONLINE, atualizado.status());
		assertNull(cacheManager.getCache("dispositivos").get(salvo.id()));
	}

}
