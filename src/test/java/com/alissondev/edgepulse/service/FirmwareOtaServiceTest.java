package com.alissondev.edgepulse.service;

import com.alissondev.edgepulse.dto.ClusterRequestDTO;
import com.alissondev.edgepulse.dto.ClusterResponseDTO;
import com.alissondev.edgepulse.dto.ConfirmacaoOtaRequest;
import com.alissondev.edgepulse.dto.DispositivoRequestDTO;
import com.alissondev.edgepulse.dto.DispositivoResponseDTO;
import com.alissondev.edgepulse.dto.ImplantacaoOtaResponse;
import com.alissondev.edgepulse.dto.IncidenteResponse;
import com.alissondev.edgepulse.dto.IniciarOtaRequest;
import com.alissondev.edgepulse.dto.ReleaseFirmwareRequest;
import com.alissondev.edgepulse.dto.ReleaseFirmwareResponse;
import com.alissondev.edgepulse.model.enums.AmbienteCluster;
import com.alissondev.edgepulse.model.enums.StatusDispositivo;
import com.alissondev.edgepulse.model.enums.StatusImplantacaoOta;
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

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
class FirmwareOtaServiceTest {

	private static final String CHECKSUM_VALIDO = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";
	private static final String CHECKSUM_CORROMPIDO = "ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff";

	@Autowired
	private FirmwareOtaService firmwareOtaService;

	@Autowired
	private ClusterService clusterService;

	@Autowired
	private DispositivoService dispositivoService;

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

	private DispositivoResponseDTO dispositivoOnline;

	@BeforeEach
	void setUp() {
		implantacaoOtaRepository.deleteAll();
		releaseFirmwareRepository.deleteAll();
		incidenteRepository.deleteAll();
		leituraTelemetriaRepository.deleteAll();
		dispositivoRepository.deleteAll();
		clusterRepository.deleteAll();

		ClusterResponseDTO cluster = clusterService.criarCluster(new ClusterRequestDTO(
				"Cluster-OTA-Test",
				AmbienteCluster.PRODUCAO,
				"Planta Automacao",
				30,
				true
		));

		DispositivoResponseDTO cadastrado = dispositivoService.cadastrarDispositivo(new DispositivoRequestDTO(
				cluster.id(),
				"ESP32-OTA-Node-01",
				"AA:BB:CC:DD:EE:01",
				"ESP32-S3",
				"v1.0.0",
				true,
				true
		));

		dispositivoOnline = dispositivoService.atualizarStatus(cadastrado.id(), StatusDispositivo.ONLINE);
	}

	@Test
	void deveBloquearRolloutOtaQuandoHardwareForIncompativel() {
		ReleaseFirmwareResponse releaseIncompativel = firmwareOtaService.publicarRelease(new ReleaseFirmwareRequest(
				"v2.0.0",
				"ESP32-C3", // Dispositivo é ESP32-S3
				CHECKSUM_VALIDO,
				524288L,
				"Release exclusiva para C3",
				true
		));

		IniciarOtaRequest iniciarReq = new IniciarOtaRequest(dispositivoOnline.id(), releaseIncompativel.id());

		IllegalStateException ex = assertThrows(
				IllegalStateException.class,
				() -> firmwareOtaService.iniciarImplantacao(iniciarReq)
		);
		assertTrue(ex.getMessage().contains("Incompatibilidade de hardware"));
	}

	@Test
	void deveConcluirAtualizacaoOtaComSucessoQuandoChecksumSha256Coincidir() {
		ReleaseFirmwareResponse release = firmwareOtaService.publicarRelease(new ReleaseFirmwareRequest(
				"v2.0.0",
				"ESP32-S3",
				CHECKSUM_VALIDO,
				612000L,
				"Suporte a audio I2S otimizado",
				true
		));

		ImplantacaoOtaResponse emAndamento = firmwareOtaService.iniciarImplantacao(
				new IniciarOtaRequest(dispositivoOnline.id(), release.id())
		);

		assertEquals(StatusImplantacaoOta.EM_ANDAMENTO, emAndamento.status());
		assertEquals("v1.0.0", emAndamento.versaoAnterior());
		assertEquals(StatusDispositivo.EM_ATUALIZACAO, dispositivoService.buscarPorId(dispositivoOnline.id()).status());

		ImplantacaoOtaResponse concluida = firmwareOtaService.processarResultadoOta(
				emAndamento.id(),
				new ConfirmacaoOtaRequest(true, CHECKSUM_VALIDO.toUpperCase(), null)
		);

		assertEquals(StatusImplantacaoOta.SUCESSO, concluida.status());
		assertNotNull(concluida.dataConclusao());

		DispositivoResponseDTO dispAtualizado = dispositivoService.buscarPorId(dispositivoOnline.id());
		assertEquals("v2.0.0", dispAtualizado.versaoFirmwareAtual());
		assertEquals(StatusDispositivo.ONLINE, dispAtualizado.status());
	}

	@Test
	void deveExecutarRollbackAutomaticoEAbrirIncidenteFalhaOtaQuandoChecksumDivergir() {
		ReleaseFirmwareResponse release = firmwareOtaService.publicarRelease(new ReleaseFirmwareRequest(
				"v2.1.0",
				"ESP32-S3",
				CHECKSUM_VALIDO,
				640000L,
				"Release com verificacao SHA-256",
				true
		));

		ImplantacaoOtaResponse emAndamento = firmwareOtaService.iniciarImplantacao(
				new IniciarOtaRequest(dispositivoOnline.id(), release.id())
		);

		ImplantacaoOtaResponse rollback = firmwareOtaService.processarResultadoOta(
				emAndamento.id(),
				new ConfirmacaoOtaRequest(true, CHECKSUM_CORROMPIDO, null)
		);

		assertEquals(StatusImplantacaoOta.FALHA_ROLLBACK, rollback.status());
		assertNotNull(rollback.mensagemErro());
		assertTrue(rollback.mensagemErro().contains("Divergência de checksum SHA-256"));

		DispositivoResponseDTO dispAposRollback = dispositivoService.buscarPorId(dispositivoOnline.id());
		assertEquals("v1.0.0", dispAposRollback.versaoFirmwareAtual());
		assertEquals(StatusDispositivo.DEGRADADO, dispAposRollback.status());

		List<IncidenteResponse> incidentes = incidenteService.listarPorDispositivo(dispositivoOnline.id());
		assertEquals(1, incidentes.size());
		assertEquals(TipoAnomalia.FALHA_OTA, incidentes.get(0).tipoAnomalia());
	}

}
