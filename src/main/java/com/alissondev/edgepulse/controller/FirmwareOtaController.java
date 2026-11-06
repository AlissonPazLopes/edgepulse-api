package com.alissondev.edgepulse.controller;

import com.alissondev.edgepulse.dto.ConfirmacaoOtaRequest;
import com.alissondev.edgepulse.dto.ImplantacaoOtaResponse;
import com.alissondev.edgepulse.dto.IniciarOtaRequest;
import com.alissondev.edgepulse.dto.ReleaseFirmwareRequest;
import com.alissondev.edgepulse.dto.ReleaseFirmwareResponse;
import com.alissondev.edgepulse.service.FirmwareOtaService;
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
@RequestMapping("/api/ota")
@RequiredArgsConstructor
public class FirmwareOtaController {

	private final FirmwareOtaService firmwareOtaService;

	@PostMapping("/releases")
	public ResponseEntity<ReleaseFirmwareResponse> publicarRelease(@Valid @RequestBody ReleaseFirmwareRequest request) {
		ReleaseFirmwareResponse criada = firmwareOtaService.publicarRelease(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(criada);
	}

	@GetMapping("/releases")
	public ResponseEntity<List<ReleaseFirmwareResponse>> listarReleases() {
		return ResponseEntity.ok(firmwareOtaService.listarReleases());
	}

	@PostMapping("/implantacoes")
	public ResponseEntity<ImplantacaoOtaResponse> iniciarImplantacao(@Valid @RequestBody IniciarOtaRequest request) {
		ImplantacaoOtaResponse iniciada = firmwareOtaService.iniciarImplantacao(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(iniciada);
	}

	@PatchMapping("/implantacoes/{id}/resultado")
	public ResponseEntity<ImplantacaoOtaResponse> processarResultadoOta(
			@PathVariable Long id,
			@Valid @RequestBody ConfirmacaoOtaRequest request) {
		return ResponseEntity.ok(firmwareOtaService.processarResultadoOta(id, request));
	}

	@GetMapping("/implantacoes/dispositivo/{dispositivoId}")
	public ResponseEntity<List<ImplantacaoOtaResponse>> listarHistoricoPorDispositivo(@PathVariable Long dispositivoId) {
		return ResponseEntity.ok(firmwareOtaService.listarHistoricoPorDispositivo(dispositivoId));
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
