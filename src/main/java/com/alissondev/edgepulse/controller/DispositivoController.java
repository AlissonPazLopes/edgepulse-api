package com.alissondev.edgepulse.controller;

import com.alissondev.edgepulse.dto.AtualizacaoStatusDispositivoDTO;
import com.alissondev.edgepulse.dto.DispositivoRequestDTO;
import com.alissondev.edgepulse.dto.DispositivoResponseDTO;
import com.alissondev.edgepulse.service.DispositivoService;
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
@Tag(name = "Dispositivos Embarcados", description = "Registro, consulta e gestão operacional de nós ESP32")
public class DispositivoController {

	private final DispositivoService dispositivoService;

	@PostMapping
	@Operation(
			summary = "Cadastrar dispositivo embarcado",
			description = "Registra um novo nó embarcado (ESP32) vinculado a um cluster ativo, normalizando o MAC Address e iniciando em status OFFLINE."
	)
	@ApiResponses({
			@ApiResponse(responseCode = "201", description = "Dispositivo registrado com sucesso"),
			@ApiResponse(responseCode = "400", description = "Formato de MAC Address ou campos obrigatórios inválidos"),
			@ApiResponse(responseCode = "404", description = "Cluster de destino não encontrado"),
			@ApiResponse(responseCode = "409", description = "MAC Address já cadastrado na plataforma"),
			@ApiResponse(responseCode = "422", description = "Cluster de destino encontra-se inativo")
	})
	public ResponseEntity<DispositivoResponseDTO> cadastrarDispositivo(@Valid @RequestBody DispositivoRequestDTO dto) {
		DispositivoResponseDTO criado = dispositivoService.cadastrarDispositivo(dto);
		return ResponseEntity.status(HttpStatus.CREATED).body(criado);
	}

	@GetMapping
	@Operation(
			summary = "Listar todos os dispositivos",
			description = "Retorna todos os dispositivos embarcados cadastrados na plataforma."
	)
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Lista de dispositivos retornada com sucesso")
	})
	public ResponseEntity<List<DispositivoResponseDTO>> listarTodos() {
		return ResponseEntity.ok(dispositivoService.listarTodos());
	}

	@GetMapping("/{id}")
	@Operation(
			summary = "Buscar dispositivo por ID",
			description = "Consulta os dados cadastrais, periféricos e status atual de um dispositivo pelo seu ID."
	)
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Dispositivo encontrado com sucesso"),
			@ApiResponse(responseCode = "404", description = "Dispositivo não encontrado")
	})
	public ResponseEntity<DispositivoResponseDTO> buscarPorId(@PathVariable Long id) {
		return ResponseEntity.ok(dispositivoService.buscarPorId(id));
	}

	@GetMapping("/mac/{macAddress}")
	@Operation(
			summary = "Buscar dispositivo por MAC Address",
			description = "Localiza um dispositivo embarcado pelo seu endereço físico MAC (case-insensitive)."
	)
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Dispositivo localizado com sucesso"),
			@ApiResponse(responseCode = "404", description = "Dispositivo não encontrado para o MAC informado")
	})
	public ResponseEntity<DispositivoResponseDTO> buscarPorMacAddress(@PathVariable String macAddress) {
		return ResponseEntity.ok(dispositivoService.buscarPorMacAddress(macAddress));
	}

	@GetMapping("/cluster/{clusterId}")
	@Operation(
			summary = "Listar dispositivos por cluster",
			description = "Retorna todos os nós embarcados pertencentes a um cluster específico."
	)
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Lista de dispositivos do cluster retornada com sucesso")
	})
	public ResponseEntity<List<DispositivoResponseDTO>> listarPorCluster(@PathVariable Long clusterId) {
		return ResponseEntity.ok(dispositivoService.listarPorCluster(clusterId));
	}

	@PatchMapping("/{id}/status")
	@Operation(
			summary = "Atualizar status operacional do dispositivo",
			description = "Altera o status operacional de um dispositivo (ex: ONLINE, MANUTENCAO) com controle de concorrência otimista (@Version)."
	)
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Status do dispositivo atualizado com sucesso"),
			@ApiResponse(responseCode = "400", description = "Novo status inválido"),
			@ApiResponse(responseCode = "404", description = "Dispositivo não encontrado"),
			@ApiResponse(responseCode = "409", description = "Conflito de concorrência otimista ao atualizar o dispositivo")
	})
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
