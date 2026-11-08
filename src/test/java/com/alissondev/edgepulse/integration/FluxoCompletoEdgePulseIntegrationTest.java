package com.alissondev.edgepulse.integration;

import com.alissondev.edgepulse.dto.ClusterRequestDTO;
import com.alissondev.edgepulse.dto.ConfirmacaoOtaRequest;
import com.alissondev.edgepulse.dto.DispositivoRequestDTO;
import com.alissondev.edgepulse.dto.IniciarOtaRequest;
import com.alissondev.edgepulse.dto.ReleaseFirmwareRequest;
import com.alissondev.edgepulse.dto.ResolucaoIncidenteRequest;
import com.alissondev.edgepulse.dto.TelemetriaRequest;
import com.alissondev.edgepulse.model.enums.AmbienteCluster;
import com.alissondev.edgepulse.repository.ClusterRepository;
import com.alissondev.edgepulse.repository.DispositivoRepository;
import com.alissondev.edgepulse.repository.ImplantacaoOtaRepository;
import com.alissondev.edgepulse.repository.IncidenteRepository;
import com.alissondev.edgepulse.repository.LeituraTelemetriaRepository;
import com.alissondev.edgepulse.repository.ReleaseFirmwareRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FluxoCompletoEdgePulseIntegrationTest {

	private static final String SHA256_FIRMWARE_V2 = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

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
	void deveExecutarCicloE2ECompletoDeTelemetriaIncidenteConfiabilidadeEOta() throws Exception {
		// 1. Cria Cluster
		ClusterRequestDTO clusterReq = new ClusterRequestDTO(
				"Cluster-E2E-Industrial",
				AmbienteCluster.PRODUCAO,
				"Planta Automacao E2E",
				30,
				true
		);

		String clusterResp = mockMvc.perform(post("/api/clusters")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(clusterReq)))
				.andExpect(status().isCreated())
				.andReturn()
				.getResponse()
				.getContentAsString();

		Long clusterId = objectMapper.readTree(clusterResp).get("id").asLong();

		// 2. Cadastra Dispositivo ESP32-S3 (inicia OFFLINE)
		DispositivoRequestDTO dispReq = new DispositivoRequestDTO(
				clusterId,
				"ESP32-S3-Gateway-E2E",
				"aa:bb:cc:dd:ee:ff",
				"ESP32-S3",
				"v1.0.0",
				true,
				true
		);

		String dispResp = mockMvc.perform(post("/api/dispositivos")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(dispReq)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.status").value("OFFLINE"))
				.andExpect(jsonPath("$.macAddress").value("AA:BB:CC:DD:EE:FF"))
				.andReturn()
				.getResponse()
				.getContentAsString();

		Long dispositivoId = objectMapper.readTree(dispResp).get("id").asLong();

		// 3. Envia Telemetria Normal -> Transiciona para ONLINE
		TelemetriaRequest telemetriaNormal = new TelemetriaRequest(
				"AA:BB:CC:DD:EE:FF",
				new BigDecimal("47.00"),
				new BigDecimal("3.30"),
				95,
				-52,
				210000L,
				true,
				true
		);

		mockMvc.perform(post("/api/telemetria")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(telemetriaNormal)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.statusDispositivo").value("ONLINE"))
				.andExpect(jsonPath("$.anomaliaDetectada").value(false));

		// 4. Envia Telemetria com Superaquecimento (> 80°C) -> DEGRADADO + Abre Incidente Automático
		TelemetriaRequest telemetriaAnomala = new TelemetriaRequest(
				"AA:BB:CC:DD:EE:FF",
				new BigDecimal("88.50"),
				new BigDecimal("3.30"),
				90,
				-55,
				195000L,
				true,
				true
		);

		mockMvc.perform(post("/api/telemetria")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(telemetriaAnomala)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.statusDispositivo").value("DEGRADADO"))
				.andExpect(jsonPath("$.anomaliaDetectada").value(true))
				.andExpect(jsonPath("$.detalhesAnomalia").value("SUPERAQUECIMENTO"));

		// Verifica incidente aberto automaticamente e cluster em status CRITICO
		String incidentesJson = mockMvc.perform(get("/api/incidentes/dispositivo/" + dispositivoId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].tipoAnomalia").value("SUPERAQUECIMENTO"))
				.andExpect(jsonPath("$[0].severidade").value("CRITICA"))
				.andExpect(jsonPath("$[0].status").value("ABERTO"))
				.andReturn()
				.getResponse()
				.getContentAsString();

		JsonNode incidentesNode = objectMapper.readTree(incidentesJson);
		Long incidenteId = incidentesNode.get(0).get("id").asLong();

		mockMvc.perform(get("/api/dashboard/clusters/" + clusterId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.statusSaudeCluster").value("CRITICO"))
				.andExpect(jsonPath("$.incidentesCriticosAtivos").value(1));

		// 5. Resolve o Incidente -> Dispositivo volta para ONLINE
		mockMvc.perform(patch("/api/incidentes/" + incidenteId + "/resolver")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(new ResolucaoIncidenteRequest("Exaustão térmica normalizada"))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("RESOLVIDO"))
				.andExpect(jsonPath("$.tempoReparoSegundos").isNumber());

		// 6. Consulta Métricas de Confiabilidade MTBF / MTTR / Disponibilidade
		mockMvc.perform(get("/api/incidentes/dispositivo/" + dispositivoId + "/confiabilidade"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalIncidentes").value(1))
				.andExpect(jsonPath("$.incidentesResolvidos").value(1))
				.andExpect(jsonPath("$.incidentesAtivos").value(0))
				.andExpect(jsonPath("$.mttrSegundos").isNumber())
				.andExpect(jsonPath("$.mtbfSegundos").isNumber())
				.andExpect(jsonPath("$.disponibilidadePercentual").isNumber());

		// 7. Publica Release de Firmware OTA e executa atualização com validação SHA-256
		ReleaseFirmwareRequest releaseReq = new ReleaseFirmwareRequest(
				"v2.0.0",
				"ESP32-S3",
				SHA256_FIRMWARE_V2,
				640000L,
				"Release v2.0.0 estável com proteção térmica aprimorada",
				true
		);

		String releaseResp = mockMvc.perform(post("/api/ota/releases")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(releaseReq)))
				.andExpect(status().isCreated())
				.andReturn()
				.getResponse()
				.getContentAsString();

		Long releaseId = objectMapper.readTree(releaseResp).get("id").asLong();

		String otaResp = mockMvc.perform(post("/api/ota/implantacoes")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(new IniciarOtaRequest(dispositivoId, releaseId))))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.status").value("EM_ANDAMENTO"))
				.andExpect(jsonPath("$.statusDispositivo").value("EM_ATUALIZACAO"))
				.andReturn()
				.getResponse()
				.getContentAsString();

		Long implantacaoId = objectMapper.readTree(otaResp).get("id").asLong();

		mockMvc.perform(patch("/api/ota/implantacoes/" + implantacaoId + "/resultado")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(new ConfirmacaoOtaRequest(true, SHA256_FIRMWARE_V2, null))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("SUCESSO"))
				.andExpect(jsonPath("$.versaoAtualDispositivo").value("v2.0.0"))
				.andExpect(jsonPath("$.statusDispositivo").value("ONLINE"));

		// 8. Valida Dashboard do Cluster (SAUDAVEL) e Resumo Global
		mockMvc.perform(get("/api/dashboard/clusters/" + clusterId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.statusSaudeCluster").value("SAUDAVEL"))
				.andExpect(jsonPath("$.dispositivosOnline").value(1))
				.andExpect(jsonPath("$.incidentesAtivos").value(0));

		mockMvc.perform(get("/api/dashboard/global"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalClusters").value(1))
				.andExpect(jsonPath("$.totalDispositivos").value(1))
				.andExpect(jsonPath("$.totalLeiturasTelemetria").value(2))
				.andExpect(jsonPath("$.totalAnomaliasDetectadas").value(1))
				.andExpect(jsonPath("$.totalImplantacoesOtaSucesso").value(1));
	}

}
