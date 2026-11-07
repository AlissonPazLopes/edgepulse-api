package com.alissondev.edgepulse.controller;

import com.alissondev.edgepulse.dto.ClusterRequestDTO;
import com.alissondev.edgepulse.dto.ClusterResponseDTO;
import com.alissondev.edgepulse.dto.DispositivoRequestDTO;
import com.alissondev.edgepulse.dto.DispositivoResponseDTO;
import com.alissondev.edgepulse.exception.ConflitoConcorrenciaException;
import com.alissondev.edgepulse.exception.ErroPadraoResponse;
import com.alissondev.edgepulse.exception.GlobalExceptionHandler;
import com.alissondev.edgepulse.model.Dispositivo;
import com.alissondev.edgepulse.model.enums.AmbienteCluster;
import com.alissondev.edgepulse.model.enums.StatusDispositivo;
import com.alissondev.edgepulse.repository.ClusterRepository;
import com.alissondev.edgepulse.repository.DispositivoRepository;
import com.alissondev.edgepulse.repository.ImplantacaoOtaRepository;
import com.alissondev.edgepulse.repository.IncidenteRepository;
import com.alissondev.edgepulse.repository.LeituraTelemetriaRepository;
import com.alissondev.edgepulse.repository.ReleaseFirmwareRepository;
import com.alissondev.edgepulse.service.ClusterService;
import com.alissondev.edgepulse.service.DispositivoService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DashboardExceptionHandlerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private ClusterService clusterService;

	@Autowired
	private DispositivoService dispositivoService;

	@Autowired
	private GlobalExceptionHandler globalExceptionHandler;

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

	private ClusterResponseDTO clusterPadrao;
	private DispositivoResponseDTO dispositivoPadrao;

	@BeforeEach
	void setUp() {
		implantacaoOtaRepository.deleteAll();
		releaseFirmwareRepository.deleteAll();
		incidenteRepository.deleteAll();
		leituraTelemetriaRepository.deleteAll();
		dispositivoRepository.deleteAll();
		clusterRepository.deleteAll();

		clusterPadrao = clusterService.criarCluster(new ClusterRequestDTO(
				"Cluster-Dashboard-REST",
				AmbienteCluster.PRODUCAO,
				"Centro de Operacoes",
				30,
				true
		));

		dispositivoPadrao = dispositivoService.cadastrarDispositivo(new DispositivoRequestDTO(
				clusterPadrao.id(),
				"ESP32-Dash-01",
				"AA:BB:CC:DD:EE:10",
				"ESP32-S3",
				"v1.0.0",
				true,
				true
		));
		dispositivoService.atualizarStatus(dispositivoPadrao.id(), StatusDispositivo.ONLINE);
	}

	@Test
	void deveConsultarEndpointsDeDashboardGlobalEPorClusterViaRest() throws Exception {
		mockMvc.perform(get("/api/dashboard/global"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalClusters").value(1))
				.andExpect(jsonPath("$.clustersAtivos").value(1))
				.andExpect(jsonPath("$.totalDispositivos").value(1))
				.andExpect(jsonPath("$.dispositivosOnline").value(1));

		mockMvc.perform(get("/api/dashboard/clusters/" + clusterPadrao.id()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.clusterId").value(clusterPadrao.id()))
				.andExpect(jsonPath("$.nomeCluster").value("Cluster-Dashboard-REST"))
				.andExpect(jsonPath("$.statusSaudeCluster").value("SAUDAVEL"));

		mockMvc.perform(get("/api/dashboard/clusters"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].statusSaudeCluster").value("SAUDAVEL"));
	}

	@Test
	void deveRetornarErroPadrao400E404ViaGlobalExceptionHandler() throws Exception {
		// HTTP 404 Not Found no DashboardController
		mockMvc.perform(get("/api/dashboard/clusters/999999"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.status").value(404))
				.andExpect(jsonPath("$.erro").value("Not Found"))
				.andExpect(jsonPath("$.path").value("/api/dashboard/clusters/999999"))
				.andExpect(jsonPath("$.mensagem").isNotEmpty());

		// HTTP 400 Bad Request com detalhamento de campos inválidos
		DispositivoRequestDTO invalido = new DispositivoRequestDTO(
				null,
				"",
				"MAC-INVALIDO",
				"",
				"",
				false,
				false
		);

		mockMvc.perform(post("/api/dispositivos")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(invalido)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.erro").value("Bad Request"))
				.andExpect(jsonPath("$.camposInvalidos").isArray())
				.andExpect(jsonPath("$.camposInvalidos.length()").value(5));
	}

	@Test
	void deveDetectarConflitoOptimisticLockingERetornarHttp409ConflictPadronizado() {
		// Simula duas transações simultâneas lendo a mesma versão (@Version) do Dispositivo
		Dispositivo tx1 = dispositivoRepository.findById(dispositivoPadrao.id()).orElseThrow();
		Dispositivo tx2 = dispositivoRepository.findById(dispositivoPadrao.id()).orElseThrow();

		tx1.setVersaoFirmwareAtual("v1.1.0");
		dispositivoRepository.saveAndFlush(tx1);

		tx2.setVersaoFirmwareAtual("v1.2.0");
		ObjectOptimisticLockingFailureException optimisticEx = assertThrows(
				ObjectOptimisticLockingFailureException.class,
				() -> dispositivoRepository.saveAndFlush(tx2)
		);

		MockHttpServletRequest request = new MockHttpServletRequest("PATCH", "/api/dispositivos/" + dispositivoPadrao.id() + "/status");
		ResponseEntity<ErroPadraoResponse> response409 = globalExceptionHandler.handleConcurrencyConflict(optimisticEx, request);

		assertEquals(HttpStatus.CONFLICT, response409.getStatusCode());
		assertNotNull(response409.getBody());
		assertEquals(409, response409.getBody().status());
		assertEquals("Conflict", response409.getBody().erro());
		assertEquals("/api/dispositivos/" + dispositivoPadrao.id() + "/status", response409.getBody().path());

		// Também valida ConflitoConcorrenciaException customizada
		ConflitoConcorrenciaException customEx = new ConflitoConcorrenciaException("Disputa simultânea de rollout OTA");
		ResponseEntity<ErroPadraoResponse> responseCustom409 = globalExceptionHandler.handleConcurrencyConflict(customEx, request);
		assertEquals(409, responseCustom409.getBody().status());
		assertEquals("Disputa simultânea de rollout OTA", responseCustom409.getBody().mensagem());
	}

}
