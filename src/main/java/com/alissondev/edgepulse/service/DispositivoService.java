package com.alissondev.edgepulse.service;

import com.alissondev.edgepulse.dto.DispositivoRequestDTO;
import com.alissondev.edgepulse.dto.DispositivoResponseDTO;
import com.alissondev.edgepulse.model.Cluster;
import com.alissondev.edgepulse.model.Dispositivo;
import com.alissondev.edgepulse.model.enums.StatusDispositivo;
import com.alissondev.edgepulse.repository.ClusterRepository;
import com.alissondev.edgepulse.repository.DispositivoRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class DispositivoService {

	private final DispositivoRepository dispositivoRepository;
	private final ClusterRepository clusterRepository;

	@Transactional
	@CacheEvict(value = "dispositivos", allEntries = true)
	public DispositivoResponseDTO cadastrarDispositivo(DispositivoRequestDTO dto) {
		String macNormalizado = normalizarMacAddress(dto.macAddress());

		if (dispositivoRepository.existsByMacAddress(macNormalizado)) {
			throw new IllegalArgumentException("Já existe um dispositivo registrado com o MAC Address: " + macNormalizado);
		}

		Cluster cluster = clusterRepository.findById(dto.clusterId())
				.orElseThrow(() -> new EntityNotFoundException("Cluster não encontrado com ID: " + dto.clusterId()));

		if (Boolean.FALSE.equals(cluster.getAtivo())) {
			throw new IllegalStateException("Não é permitido vincular dispositivo a um cluster inativo: " + cluster.getNome());
		}

		Dispositivo dispositivo = Dispositivo.builder()
				.cluster(cluster)
				.nome(dto.nome())
				.macAddress(macNormalizado)
				.modeloHardware(dto.modeloHardware())
				.versaoFirmwareAtual(dto.versaoFirmwareAtual())
				.status(StatusDispositivo.OFFLINE)
				.perifericoDisplayAtivo(dto.perifericoDisplayAtivo() != null ? dto.perifericoDisplayAtivo() : false)
				.perifericoAudioAtivo(dto.perifericoAudioAtivo() != null ? dto.perifericoAudioAtivo() : false)
				.dataCadastro(LocalDateTime.now())
				.versao(0L)
				.build();

		Dispositivo salvo = dispositivoRepository.save(dispositivo);
		return DispositivoResponseDTO.fromEntity(salvo);
	}

	@Transactional(readOnly = true)
	@Cacheable(value = "dispositivos")
	public List<DispositivoResponseDTO> listarTodos() {
		return dispositivoRepository.findAll()
				.stream()
				.map(DispositivoResponseDTO::fromEntity)
				.toList();
	}

	@Transactional(readOnly = true)
	@Cacheable(value = "dispositivos", key = "#id")
	public DispositivoResponseDTO buscarPorId(Long id) {
		Dispositivo dispositivo = dispositivoRepository.findById(id)
				.orElseThrow(() -> new EntityNotFoundException("Dispositivo não encontrado com ID: " + id));
		return DispositivoResponseDTO.fromEntity(dispositivo);
	}

	@Transactional(readOnly = true)
	@Cacheable(value = "dispositivos", key = "'mac_' + #macAddress.toUpperCase()")
	public DispositivoResponseDTO buscarPorMacAddress(String macAddress) {
		String macNormalizado = normalizarMacAddress(macAddress);
		Dispositivo dispositivo = dispositivoRepository.findByMacAddress(macNormalizado)
				.orElseThrow(() -> new EntityNotFoundException("Dispositivo não encontrado com MAC Address: " + macNormalizado));
		return DispositivoResponseDTO.fromEntity(dispositivo);
	}

	@Transactional(readOnly = true)
	@Cacheable(value = "dispositivos", key = "'cluster_' + #clusterId")
	public List<DispositivoResponseDTO> listarPorCluster(Long clusterId) {
		return dispositivoRepository.findByClusterId(clusterId)
				.stream()
				.map(DispositivoResponseDTO::fromEntity)
				.toList();
	}

	@Transactional
	@CacheEvict(value = "dispositivos", allEntries = true)
	public DispositivoResponseDTO atualizarStatus(Long id, StatusDispositivo novoStatus) {
		Dispositivo dispositivo = dispositivoRepository.findById(id)
				.orElseThrow(() -> new EntityNotFoundException("Dispositivo não encontrado com ID: " + id));
		dispositivo.setStatus(novoStatus);
		Dispositivo salvo = dispositivoRepository.save(dispositivo);
		return DispositivoResponseDTO.fromEntity(salvo);
	}

	@Transactional
	@CacheEvict(value = "dispositivos", allEntries = true)
	public DispositivoResponseDTO atualizarHardware(Long id, String versaoFirmware, Boolean displayAtivo, Boolean audioAtivo) {
		Dispositivo dispositivo = dispositivoRepository.findById(id)
				.orElseThrow(() -> new EntityNotFoundException("Dispositivo não encontrado com ID: " + id));
		if (versaoFirmware != null && !versaoFirmware.isBlank()) {
			dispositivo.setVersaoFirmwareAtual(versaoFirmware);
		}
		if (displayAtivo != null) {
			dispositivo.setPerifericoDisplayAtivo(displayAtivo);
		}
		if (audioAtivo != null) {
			dispositivo.setPerifericoAudioAtivo(audioAtivo);
		}
		Dispositivo salvo = dispositivoRepository.save(dispositivo);
		return DispositivoResponseDTO.fromEntity(salvo);
	}

	private String normalizarMacAddress(String macAddress) {
		return macAddress.trim().replace("-", ":").toUpperCase(Locale.ROOT);
	}

}
