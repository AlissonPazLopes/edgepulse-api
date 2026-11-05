package com.alissondev.edgepulse.controller;

import com.alissondev.edgepulse.dto.ClusterRequestDTO;
import com.alissondev.edgepulse.dto.ClusterResponseDTO;
import com.alissondev.edgepulse.dto.DispositivoRequestDTO;
import com.alissondev.edgepulse.dto.DispositivoResponseDTO;
import com.alissondev.edgepulse.dto.IncidenteRequest;
import com.alissondev.edgepulse.dto.ResolucaoIncidenteRequest;
import com.alissondev.edgepulse.model.enums.AmbienteCluster;
import com.alissondev.edgepulse.model.enums.SeveridadeIncidente;
import com.alissondev.edgepulse.model.enums.TipoAnomalia;
import com.alissondev.edgepulse.repository.ClusterRepository;
import com.alissondev.edgepulse.repository.DispositivoRepository;
import com.alissondev.edgepulse.repository.IncidenteRepository;
import com.alissondev.edgepulse.repository.LeituraTelemetriaRepository;
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
class IncidenteControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private ClusterService clusterService;

	@Autowired
	private DispositivoService dispositivoService;

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
		incidenteRepository.deleteAll();
		leituraTelemetriaRepository.deleteAll();
		dispositivoRepository.deleteAll();
		clusterRepository.deleteAll();

		ClusterResponseDTO cluster = clusterService.criarCluster(new ClusterRequestDTO(
				"Cluster-Incidentes-REST",
				AmbienteCluster.PRODUCAO,
				"DataCenter Leste",
				30,
				true
		));

		dispositivo = dispositivoService.cadastrarDispositivo(new DispositivoRequestDTO(
				cluster.id(),
				"ESP32-Incidente-Node",
				"AA:11:22:33:44:55",
				"ESP32-WROOM",
				"v1.5.0",
				true,
				true
		));
	}

	@Test
	void deveAbrirResolverIncidenteConsultarConfiabilidadeEExecutarWatchdogViaRest() throws Exception {
		IncidenteRequest req = new IncidenteRequest(
				dispositivo.id(),
				TipoAnomalia.BATERIA_CRITICA,
				SeveridadeIncidente.ALTA,
				"Bateria abaixo de 10%"
		);

		String incJson = mockMvc.perform(post("/api/incidentes")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(req)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.status").value("ABERTO"))
				.andExpect(jsonPath("$.severidade").value("ALTA"))
				.andReturn()
				.getResponse()
				.getContentAsString();

		Long incidenteId = objectMapper.readTree(incJson).get("id").asLong();

		ResolucaoIncidenteRequest resolucao = new ResolucaoIncidenteRequest("Bateria substituída em campo");

		mockMvc.perform(patch("/api/incidentes/" + incidenteId + "/resolver")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(resolucao)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("RESOLVIDO"))
				.andExpect(jsonPath("$.tempoReparoSegundos").isNumber());

		mockMvc.perform(get("/api/incidentes/dispositivo/" + dispositivo.id() + "/confiabilidade"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalIncidentes").value(1))
				.andExpect(jsonPath("$.incidentesResolvidos").value(1))
				.andExpect(jsonPath("$.incidentesAtivos").value(0))
				.andExpect(jsonPath("$.disponibilidadePercentual").isNumber());

		mockMvc.perform(post("/api/incidentes/watchdog/verificar"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.dispositivosEmTimeout").value(0));
	}

}
