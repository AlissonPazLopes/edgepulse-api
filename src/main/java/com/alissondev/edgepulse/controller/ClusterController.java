package com.alissondev.edgepulse.controller;

import com.alissondev.edgepulse.dto.ClusterRequestDTO;
import com.alissondev.edgepulse.dto.ClusterResponseDTO;
import com.alissondev.edgepulse.service.ClusterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/clusters")
@RequiredArgsConstructor
@Tag(name = "Clusters", description = "Gerenciamento da topologia de clusters de nós embarcados")
public class ClusterController {

	private final ClusterService clusterService;

	@PostMapping
	@Operation(
			summary = "Criar novo cluster",
			description = "Cadastra um novo cluster lógico de dispositivos embarcados com intervalo de heartbeat configurável."
	)
	@ApiResponses({
			@ApiResponse(responseCode = "201", description = "Cluster criado com sucesso"),
			@ApiResponse(responseCode = "400", description = "Dados de entrada inválidos"),
			@ApiResponse(responseCode = "409", description = "Já existe um cluster com o nome informado")
	})
	public ResponseEntity<ClusterResponseDTO> criarCluster(@Valid @RequestBody ClusterRequestDTO dto) {
		ClusterResponseDTO criado = clusterService.criarCluster(dto);
		return ResponseEntity.status(HttpStatus.CREATED).body(criado);
	}

	@GetMapping
	@Operation(
			summary = "Listar todos os clusters",
			description = "Retorna a lista de clusters cadastrados utilizando cache de leitura."
	)
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Lista de clusters retornada com sucesso")
	})
	public ResponseEntity<List<ClusterResponseDTO>> listarTodos() {
		return ResponseEntity.ok(clusterService.listarTodos());
	}

	@GetMapping("/{id}")
	@Operation(
			summary = "Buscar cluster por ID",
			description = "Recupera os detalhes de um cluster específico pelo seu identificador único."
	)
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Cluster encontrado com sucesso"),
			@ApiResponse(responseCode = "404", description = "Cluster não encontrado")
	})
	public ResponseEntity<ClusterResponseDTO> buscarPorId(@PathVariable Long id) {
		return ResponseEntity.ok(clusterService.buscarPorId(id));
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException ex) {
		return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("erro", ex.getMessage()));
	}

	@ExceptionHandler(EntityNotFoundException.class)
	public ResponseEntity<Map<String, String>> handleNotFound(EntityNotFoundException ex) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("erro", ex.getMessage()));
	}

}
