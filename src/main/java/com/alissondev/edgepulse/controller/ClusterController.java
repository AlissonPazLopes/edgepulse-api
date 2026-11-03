package com.alissondev.edgepulse.controller;

import com.alissondev.edgepulse.dto.ClusterRequestDTO;
import com.alissondev.edgepulse.dto.ClusterResponseDTO;
import com.alissondev.edgepulse.service.ClusterService;
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
public class ClusterController {

	private final ClusterService clusterService;

	@PostMapping
	public ResponseEntity<ClusterResponseDTO> criarCluster(@Valid @RequestBody ClusterRequestDTO dto) {
		ClusterResponseDTO criado = clusterService.criarCluster(dto);
		return ResponseEntity.status(HttpStatus.CREATED).body(criado);
	}

	@GetMapping
	public ResponseEntity<List<ClusterResponseDTO>> listarTodos() {
		return ResponseEntity.ok(clusterService.listarTodos());
	}

	@GetMapping("/{id}")
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
