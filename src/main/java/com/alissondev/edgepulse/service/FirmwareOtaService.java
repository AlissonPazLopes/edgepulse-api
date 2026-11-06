package com.alissondev.edgepulse.service;

import com.alissondev.edgepulse.dto.ConfirmacaoOtaRequest;
import com.alissondev.edgepulse.dto.ImplantacaoOtaResponse;
import com.alissondev.edgepulse.dto.IniciarOtaRequest;
import com.alissondev.edgepulse.dto.ReleaseFirmwareRequest;
import com.alissondev.edgepulse.dto.ReleaseFirmwareResponse;
import com.alissondev.edgepulse.event.AnomaliaDetectadaEvent;
import com.alissondev.edgepulse.model.Dispositivo;
import com.alissondev.edgepulse.model.ImplantacaoOta;
import com.alissondev.edgepulse.model.ReleaseFirmware;
import com.alissondev.edgepulse.model.enums.StatusDispositivo;
import com.alissondev.edgepulse.model.enums.StatusImplantacaoOta;
import com.alissondev.edgepulse.model.enums.TipoAnomalia;
import com.alissondev.edgepulse.repository.DispositivoRepository;
import com.alissondev.edgepulse.repository.ImplantacaoOtaRepository;
import com.alissondev.edgepulse.repository.ReleaseFirmwareRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class FirmwareOtaService {

	private static final Set<StatusImplantacaoOta> STATUS_OTA_ATIVOS = Set.of(
			StatusImplantacaoOta.PENDENTE,
			StatusImplantacaoOta.EM_ANDAMENTO
	);

	private final ReleaseFirmwareRepository releaseFirmwareRepository;
	private final ImplantacaoOtaRepository implantacaoOtaRepository;
	private final DispositivoRepository dispositivoRepository;
	private final ApplicationEventPublisher eventPublisher;

	@Transactional
	public ReleaseFirmwareResponse publicarRelease(ReleaseFirmwareRequest request) {
		if (releaseFirmwareRepository.existsByVersao(request.versao())) {
			throw new IllegalArgumentException("Já existe uma release de firmware publicada com a versão: " + request.versao());
		}

		ReleaseFirmware release = ReleaseFirmware.builder()
				.versao(request.versao().trim())
				.modeloHardwareAlvo(request.modeloHardwareAlvo().trim())
				.checksumSha256(request.checksumSha256().trim().toLowerCase(Locale.ROOT))
				.tamanhoBytes(request.tamanhoBytes())
				.notasLancamento(request.notasLancamento())
				.estavel(request.estavel() != null ? request.estavel() : true)
				.dataPublicacao(LocalDateTime.now())
				.build();

		ReleaseFirmware salva = releaseFirmwareRepository.save(release);
		return ReleaseFirmwareResponse.fromEntity(salva);
	}

	@Transactional(readOnly = true)
	public List<ReleaseFirmwareResponse> listarReleases() {
		return releaseFirmwareRepository.findAll()
				.stream()
				.map(ReleaseFirmwareResponse::fromEntity)
				.toList();
	}

	@Transactional
	@CacheEvict(value = "dispositivos", allEntries = true)
	public ImplantacaoOtaResponse iniciarImplantacao(IniciarOtaRequest request) {
		Dispositivo dispositivo = dispositivoRepository.findById(request.dispositivoId())
				.orElseThrow(() -> new EntityNotFoundException("Dispositivo não encontrado com ID: " + request.dispositivoId()));

		ReleaseFirmware release = releaseFirmwareRepository.findById(request.firmwareDestinoId())
				.orElseThrow(() -> new EntityNotFoundException("Release de firmware não encontrada com ID: " + request.firmwareDestinoId()));

		if (dispositivo.getStatus() == StatusDispositivo.OFFLINE
				|| dispositivo.getStatus() == StatusDispositivo.MANUTENCAO) {
			throw new IllegalStateException("Não é possível iniciar OTA em dispositivo com status " + dispositivo.getStatus());
		}

		boolean possuiOtaAtiva = implantacaoOtaRepository.existsByDispositivoIdAndStatusIn(
				dispositivo.getId(),
				STATUS_OTA_ATIVOS
		);
		if (possuiOtaAtiva) {
			throw new IllegalStateException("O dispositivo já possui uma atualização OTA ativa em andamento");
		}

		if (!release.getModeloHardwareAlvo().equals(dispositivo.getModeloHardware())) {
			throw new IllegalStateException("Incompatibilidade de hardware: firmware exige "
					+ release.getModeloHardwareAlvo() + ", mas o dispositivo é " + dispositivo.getModeloHardware());
		}

		if (!Boolean.TRUE.equals(release.getEstavel())) {
			throw new IllegalStateException("Não é permitido implantar uma release de firmware marcada como instável: " + release.getVersao());
		}

		if (release.getVersao().equals(dispositivo.getVersaoFirmwareAtual())) {
			throw new IllegalStateException("O dispositivo já está executando a versão de firmware alvo: " + release.getVersao());
		}

		String versaoAnterior = dispositivo.getVersaoFirmwareAtual();

		dispositivo.setStatus(StatusDispositivo.EM_ATUALIZACAO);
		dispositivoRepository.save(dispositivo);

		ImplantacaoOta implantacao = ImplantacaoOta.builder()
				.dispositivo(dispositivo)
				.firmwareDestino(release)
				.versaoAnterior(versaoAnterior)
				.status(StatusImplantacaoOta.EM_ANDAMENTO)
				.dataInicio(LocalDateTime.now())
				.build();

		ImplantacaoOta salva = implantacaoOtaRepository.save(implantacao);
		return ImplantacaoOtaResponse.fromEntity(salva);
	}

	@Transactional
	@CacheEvict(value = "dispositivos", allEntries = true)
	public ImplantacaoOtaResponse processarResultadoOta(Long implantacaoId, ConfirmacaoOtaRequest request) {
		ImplantacaoOta implantacao = implantacaoOtaRepository.findById(implantacaoId)
				.orElseThrow(() -> new EntityNotFoundException("Implantação OTA não encontrada com ID: " + implantacaoId));

		if (!STATUS_OTA_ATIVOS.contains(implantacao.getStatus())) {
			throw new IllegalStateException("Esta implantação OTA já foi finalizada com status: " + implantacao.getStatus());
		}

		Dispositivo dispositivo = implantacao.getDispositivo();
		ReleaseFirmware release = implantacao.getFirmwareDestino();
		LocalDateTime agora = LocalDateTime.now();

		boolean checksumValido = request.checksumCalculadoNoDispositivo() != null
				&& request.checksumCalculadoNoDispositivo().trim().equalsIgnoreCase(release.getChecksumSha256());

		if (Boolean.TRUE.equals(request.sucesso()) && checksumValido) {
			implantacao.setStatus(StatusImplantacaoOta.SUCESSO);
			implantacao.setDataConclusao(agora);
			implantacao.setMensagemErro(null);

			dispositivo.setVersaoFirmwareAtual(release.getVersao());
			dispositivo.setStatus(StatusDispositivo.ONLINE);
			dispositivoRepository.save(dispositivo);
		} else {
			String motivoErro;
			if (!Boolean.TRUE.equals(request.sucesso())) {
				motivoErro = request.mensagemErro() != null && !request.mensagemErro().isBlank()
						? request.mensagemErro().trim()
						: "Falha reportada pelo dispositivo durante gravação da partição OTA";
			} else {
				motivoErro = "Divergência de checksum SHA-256 na imagem OTA (esperado: "
						+ release.getChecksumSha256() + ", calculado: " + request.checksumCalculadoNoDispositivo() + ")";
			}

			if (motivoErro.length() > 255) {
				motivoErro = motivoErro.substring(0, 255);
			}

			implantacao.setStatus(StatusImplantacaoOta.FALHA_ROLLBACK);
			implantacao.setMensagemErro(motivoErro);
			implantacao.setDataConclusao(agora);

			// Rollback Automático: restaura a versão anterior e marca dispositivo como DEGRADADO
			dispositivo.setVersaoFirmwareAtual(implantacao.getVersaoAnterior());
			dispositivo.setStatus(StatusDispositivo.DEGRADADO);
			dispositivoRepository.save(dispositivo);

			AnomaliaDetectadaEvent event = new AnomaliaDetectadaEvent(
					dispositivo.getId(),
					dispositivo.getMacAddress(),
					List.of(TipoAnomalia.FALHA_OTA),
					motivoErro,
					agora
			);
			eventPublisher.publishEvent(event);
		}

		ImplantacaoOta salva = implantacaoOtaRepository.save(implantacao);
		return ImplantacaoOtaResponse.fromEntity(salva);
	}

	@Transactional(readOnly = true)
	public List<ImplantacaoOtaResponse> listarHistoricoPorDispositivo(Long dispositivoId) {
		if (!dispositivoRepository.existsById(dispositivoId)) {
			throw new EntityNotFoundException("Dispositivo não encontrado com ID: " + dispositivoId);
		}
		return implantacaoOtaRepository.findByDispositivoIdOrderByDataInicioDesc(dispositivoId)
				.stream()
				.map(ImplantacaoOtaResponse::fromEntity)
				.toList();
	}

}
