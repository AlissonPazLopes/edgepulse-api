package com.alissondev.edgepulse.controller;

import com.alissondev.edgepulse.dto.AtualizacaoStatusDispositivoDTO;
import com.alissondev.edgepulse.dto.DispositivoRequestDTO;
import com.alissondev.edgepulse.dto.DispositivoResponseDTO;
import com.alissondev.edgepulse.service.DispositivoService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dispositivos")
@RequiredArgsConstructor
public class DispositivoController {

	private final DispositivoService dispositivoService;

	@PostMapping
	public ResponseEntity<DispositivoResponseDTO> cadastrarDispositivo(@Valid @RequestBody DispositivoRequestDTO dto) {
		DispositivoResponseDTO criado = dispositivoService.cadastrarDispositivo(dto);
		return ResponseEntity.status(HttpStatus.CREATED).body(criado);
	}

	@GetMapping
	public ResponseEntity<List<DispositivoResponseDTO>> listarTodos() {
		return ResponseEntity.ok(dispositivoService.listarTodos());
	}

	@GetMapping("/{id}")
	public ResponseEntity<DispositivoResponseDTO> buscarPorId(@PathVariable Long id) {
		return ResponseEntity.ok(dispositivoService.buscarPorId(id));
	}

	@GetMapping("/mac/{macAddress}")
	public ResponseEntity<DispositivoResponseDTO> buscarPorMacAddress(@PathVariable String macAddress) {
		return ResponseEntity.ok(dispositivoService.buscarPorMacAddress(macAddress));
	}

	@GetMapping("/cluster/{clusterId}")
	public ResponseEntity<List<DispositivoResponseDTO>> listarPorCluster(@PathVariable Long clusterId) {
		return ResponseEntity.ok(dispositivoService.listarPorCluster(clusterId));
	}

	@PatchMapping("/{id}/status")
	public ResponseEntity<DispositivoResponseDTO> atualizarStatus(
			@PathVariable Long id,
			@Valid @RequestBody AtualizacaoStatusDispositivoDTO dto) {
		return ResponseEntity.ok(dispositivoService.atualizarStatus(id, dto.status()));
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException ex) {
		return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("erro", ex.getMessage()));
	}

	@ExceptionHandler(IllegalStateException.class)
	public ResponseEntity<Map<String, String>> handleIllegalState(IllegalStateException ex) {
		return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(Map.of("erro", ex.getMessage()));
	}

	@ExceptionHandler(EntityNotFoundException.class)
	public ResponseEntity<Map<String, String>> handleNotFound(EntityNotFoundException ex) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("erro", ex.getMessage()));
	}

}
