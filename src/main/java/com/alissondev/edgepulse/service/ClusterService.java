package com.alissondev.edgepulse.service;

import com.alissondev.edgepulse.dto.ClusterRequestDTO;
import com.alissondev.edgepulse.dto.ClusterResponseDTO;
import com.alissondev.edgepulse.model.Cluster;
import com.alissondev.edgepulse.repository.ClusterRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ClusterService {

	private final ClusterRepository clusterRepository;

	@Transactional
	@CacheEvict(value = "clusters", allEntries = true)
	public ClusterResponseDTO criarCluster(ClusterRequestDTO dto) {
		if (clusterRepository.existsByNome(dto.nome())) {
			throw new IllegalArgumentException("Já existe um cluster cadastrado com o nome: " + dto.nome());
		}

		Cluster cluster = Cluster.builder()
				.nome(dto.nome())
				.ambiente(dto.ambiente())
				.localizacao(dto.localizacao())
				.intervaloHeartbeatSegundos(dto.intervaloHeartbeatSegundos() != null ? dto.intervaloHeartbeatSegundos() : 30)
				.ativo(dto.ativo() != null ? dto.ativo() : true)
				.dataCriacao(LocalDateTime.now())
				.build();

		Cluster salvo = clusterRepository.save(cluster);
		return ClusterResponseDTO.fromEntity(salvo);
	}

	@Transactional(readOnly = true)
	@Cacheable(value = "clusters")
	public List<ClusterResponseDTO> listarTodos() {
		return clusterRepository.findAll()
				.stream()
				.map(ClusterResponseDTO::fromEntity)
				.toList();
	}

	@Transactional(readOnly = true)
	@Cacheable(value = "clusters", key = "#id")
	public ClusterResponseDTO buscarPorId(Long id) {
		Cluster cluster = buscarEntidadePorId(id);
		return ClusterResponseDTO.fromEntity(cluster);
	}

	@Transactional(readOnly = true)
	public Cluster buscarEntidadePorId(Long id) {
		return clusterRepository.findById(id)
				.orElseThrow(() -> new EntityNotFoundException("Cluster não encontrado com ID: " + id));
	}

}
