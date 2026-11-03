package com.alissondev.edgepulse.controller;

import com.alissondev.edgepulse.dto.AtualizacaoStatusDispositivoDTO;
import com.alissondev.edgepulse.dto.ClusterRequestDTO;
import com.alissondev.edgepulse.dto.DispositivoRequestDTO;
import com.alissondev.edgepulse.model.enums.AmbienteCluster;
import com.alissondev.edgepulse.model.enums.StatusDispositivo;
import com.alissondev.edgepulse.repository.ClusterRepository;
import com.alissondev.edgepulse.repository.DispositivoRepository;
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
class ClusterDispositivoControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private ClusterRepository clusterRepository;

	@Autowired
	private DispositivoRepository dispositivoRepository;

	@BeforeEach
	void setUp() {
		dispositivoRepository.deleteAll();
		clusterRepository.deleteAll();
	}

	@Test
	void deveCriarClusterECadastrarDispositivoViaRest() throws Exception {
		ClusterRequestDTO clusterDto = new ClusterRequestDTO(
				"Cluster-Norte",
				AmbienteCluster.PRODUCAO,
				"Manaus Polo Industrial",
				25,
				true
		);

		String clusterJson = mockMvc.perform(post("/api/clusters")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(clusterDto)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").isNumber())
				.andExpect(jsonPath("$.nome").value("Cluster-Norte"))
				.andReturn()
				.getResponse()
				.getContentAsString();

		Long clusterId = objectMapper.readTree(clusterJson).get("id").asLong();

		DispositivoRequestDTO dispDto = new DispositivoRequestDTO(
				clusterId,
				"ESP32-Display-Audio-01",
				"00:1a:2b:3c:4d:5e",
				"ESP32-S3-DevKitC",
				"v1.4.2",
				true,
				true
		);

		String dispJson = mockMvc.perform(post("/api/dispositivos")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(dispDto)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.macAddress").value("00:1A:2B:3C:4D:5E"))
				.andExpect(jsonPath("$.status").value("OFFLINE"))
				.andReturn()
				.getResponse()
				.getContentAsString();

		Long dispId = objectMapper.readTree(dispJson).get("id").asLong();

		mockMvc.perform(get("/api/dispositivos/mac/00:1a:2b:3c:4d:5e"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.nome").value("ESP32-Display-Audio-01"));

		mockMvc.perform(get("/api/dispositivos/cluster/" + clusterId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].id").value(dispId));

		AtualizacaoStatusDispositivoDTO statusDto = new AtualizacaoStatusDispositivoDTO(StatusDispositivo.MANUTENCAO);
		mockMvc.perform(patch("/api/dispositivos/" + dispId + "/status")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(statusDto)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("MANUTENCAO"));
	}

	@Test
	void deveRejeitarMacAddressEmFormatoInvalido() throws Exception {
		DispositivoRequestDTO dispInvalido = new DispositivoRequestDTO(
				1L,
				"ESP32-Invalido",
				"MAC-INVALIDO-123",
				"ESP32",
				"v1.0.0",
				false,
				false
		);

		mockMvc.perform(post("/api/dispositivos")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(dispInvalido)))
				.andExpect(status().isBadRequest());
	}

}
