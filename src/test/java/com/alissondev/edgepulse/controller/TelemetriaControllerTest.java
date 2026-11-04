package com.alissondev.edgepulse.controller;

import com.alissondev.edgepulse.dto.ClusterRequestDTO;
import com.alissondev.edgepulse.dto.ClusterResponseDTO;
import com.alissondev.edgepulse.dto.DispositivoRequestDTO;
import com.alissondev.edgepulse.dto.DispositivoResponseDTO;
import com.alissondev.edgepulse.dto.TelemetriaRequest;
import com.alissondev.edgepulse.model.enums.AmbienteCluster;
import com.alissondev.edgepulse.repository.ClusterRepository;
import com.alissondev.edgepulse.repository.DispositivoRepository;
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

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TelemetriaControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

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

	private DispositivoResponseDTO dispositivo;

	@BeforeEach
	void setUp() {
		leituraTelemetriaRepository.deleteAll();
		dispositivoRepository.deleteAll();
		clusterRepository.deleteAll();

		ClusterResponseDTO cluster = clusterService.criarCluster(new ClusterRequestDTO(
				"Cluster-REST-Telemetria",
				AmbienteCluster.HOMOLOGACAO,
				"Lab Integracao",
				30,
				true
		));

		dispositivo = dispositivoService.cadastrarDispositivo(new DispositivoRequestDTO(
				cluster.id(),
				"ESP32-REST-Node",
				"11:22:33:AA:BB:CC",
				"ESP32-WROOM",
				"v2.1.0",
				true,
				false
		));
	}

	@Test
	void deveIngerirTelemetriaEConsultarEndpointsDeDiagnostico() throws Exception {
		TelemetriaRequest leituraNormal = new TelemetriaRequest(
				"11:22:33:aa:bb:cc",
				new BigDecimal("50.00"),
				new BigDecimal("3.30"),
				80,
				-60,
				150000L,
				true,
				true
		);

		mockMvc.perform(post("/api/telemetria")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(leituraNormal)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.statusDispositivo").value("ONLINE"))
				.andExpect(jsonPath("$.anomaliaDetectada").value(false));

		TelemetriaRequest leituraAnomala = new TelemetriaRequest(
				"11:22:33:AA:BB:CC",
				new BigDecimal("90.00"),
				new BigDecimal("3.10"),
				75,
				-65,
				140000L,
				false,
				true
		);

		mockMvc.perform(post("/api/telemetria")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(leituraAnomala)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.statusDispositivo").value("DEGRADADO"))
				.andExpect(jsonPath("$.anomaliaDetectada").value(true))
				.andExpect(jsonPath("$.detalhesAnomalia").value("SUPERAQUECIMENTO, FALHA_PERIFERICO_DISPLAY"));

		mockMvc.perform(get("/api/telemetria/dispositivo/" + dispositivo.id()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(2));

		mockMvc.perform(get("/api/telemetria/dispositivo/" + dispositivo.id() + "/ultima"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.anomaliaDetectada").value(true));

		mockMvc.perform(get("/api/telemetria/anomalias"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1));

		mockMvc.perform(get("/api/telemetria/dispositivo/" + dispositivo.id() + "/resumo"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalPacotesRecebidos").value(2))
				.andExpect(jsonPath("$.totalAnomalias").value(1))
				.andExpect(jsonPath("$.mediaTemperaturaCpuC").value(70.00))
				.andExpect(jsonPath("$.mediaTensaoAlimentacaoV").value(3.20));
	}

}
