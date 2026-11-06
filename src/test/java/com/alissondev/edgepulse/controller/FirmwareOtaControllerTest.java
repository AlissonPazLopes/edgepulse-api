package com.alissondev.edgepulse.controller;

import com.alissondev.edgepulse.dto.ClusterRequestDTO;
import com.alissondev.edgepulse.dto.ClusterResponseDTO;
import com.alissondev.edgepulse.dto.ConfirmacaoOtaRequest;
import com.alissondev.edgepulse.dto.DispositivoRequestDTO;
import com.alissondev.edgepulse.dto.DispositivoResponseDTO;
import com.alissondev.edgepulse.dto.IniciarOtaRequest;
import com.alissondev.edgepulse.dto.ReleaseFirmwareRequest;
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
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FirmwareOtaControllerTest {

	private static final String SHA256_HEX = "a1b2c3d4e5f60718293a4b5c6d7e8f90a1b2c3d4e5f60718293a4b5c6d7e8f90";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private ClusterService clusterService;

	@Autowired
	private DispositivoService dispositivoService;

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

	private DispositivoResponseDTO dispositivo;

	@BeforeEach
	void setUp() {
		implantacaoOtaRepository.deleteAll();
		releaseFirmwareRepository.deleteAll();
		incidenteRepository.deleteAll();
		leituraTelemetriaRepository.deleteAll();
		dispositivoRepository.deleteAll();
		clusterRepository.deleteAll();

		ClusterResponseDTO cluster = clusterService.criarCluster(new ClusterRequestDTO(
				"Cluster-OTA-REST",
				AmbienteCluster.HOMOLOGACAO,
				"Lab Firmware",
				30,
				true
		));

		DispositivoResponseDTO criado = dispositivoService.cadastrarDispositivo(new DispositivoRequestDTO(
				cluster.id(),
				"ESP32-REST-OTA",
				"AA:BB:CC:DD:EE:99",
				"ESP32-WROOM-32",
				"v1.0.0",
				true,
				false
		));

		dispositivo = dispositivoService.atualizarStatus(criado.id(), StatusDispositivo.ONLINE);
	}

	@Test
	void devePublicarReleaseIniciarRolloutEConfirmarSucessoViaRest() throws Exception {
		ReleaseFirmwareRequest releaseReq = new ReleaseFirmwareRequest(
				"v1.5.0",
				"ESP32-WROOM-32",
				SHA256_HEX,
				480000L,
				"Melhoria de estabilidade WiFi",
				true
		);

		String releaseJson = mockMvc.perform(post("/api/ota/releases")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(releaseReq)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.versao").value("v1.5.0"))
				.andReturn()
				.getResponse()
				.getContentAsString();

		Long releaseId = objectMapper.readTree(releaseJson).get("id").asLong();

		IniciarOtaRequest iniciarReq = new IniciarOtaRequest(dispositivo.id(), releaseId);

		String impJson = mockMvc.perform(post("/api/ota/implantacoes")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(iniciarReq)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.status").value("EM_ANDAMENTO"))
				.andExpect(jsonPath("$.statusDispositivo").value("EM_ATUALIZACAO"))
				.andReturn()
				.getResponse()
				.getContentAsString();

		Long implantacaoId = objectMapper.readTree(impJson).get("id").asLong();

		ConfirmacaoOtaRequest confirmacao = new ConfirmacaoOtaRequest(true, SHA256_HEX, null);

		mockMvc.perform(patch("/api/ota/implantacoes/" + implantacaoId + "/resultado")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(confirmacao)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("SUCESSO"))
				.andExpect(jsonPath("$.versaoAtualDispositivo").value("v1.5.0"))
				.andExpect(jsonPath("$.statusDispositivo").value("ONLINE"));

		mockMvc.perform(get("/api/ota/implantacoes/dispositivo/" + dispositivo.id()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1));
	}

	@Test
	void deveRejeitarReleaseComChecksumSha256Invalido() throws Exception {
		ReleaseFirmwareRequest invalida = new ReleaseFirmwareRequest(
				"v9.9.9",
				"ESP32-WROOM-32",
				"checksum-curto-invalido",
				500000L,
				"Invalida",
				true
		);

		mockMvc.perform(post("/api/ota/releases")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(invalida)))
				.andExpect(status().isBadRequest());
	}

}
