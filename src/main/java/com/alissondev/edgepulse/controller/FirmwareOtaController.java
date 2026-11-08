package com.alissondev.edgepulse.controller;

import com.alissondev.edgepulse.dto.ConfirmacaoOtaRequest;
import com.alissondev.edgepulse.dto.ImplantacaoOtaResponse;
import com.alissondev.edgepulse.dto.IniciarOtaRequest;
import com.alissondev.edgepulse.dto.ReleaseFirmwareRequest;
import com.alissondev.edgepulse.dto.ReleaseFirmwareResponse;
import com.alissondev.edgepulse.service.FirmwareOtaService;
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
@RequestMapping("/api/ota")
@RequiredArgsConstructor
@Tag(name = "Firmware OTA e Rollback Automático", description = "Publicação de releases de firmware, validação SHA-256, rollout OTA e rollback automático em caso de falha")
public class FirmwareOtaController {

	private final FirmwareOtaService firmwareOtaService;

	@PostMapping("/releases")
	@Operation(
			summary = "Publicar nova release de firmware",
			description = "Cadastra uma nova imagem de firmware associada a um modelo de hardware alvo e hash criptográfico SHA-256."
	)
	@ApiResponses({
			@ApiResponse(responseCode = "201", description = "Release de firmware publicada com sucesso"),
			@ApiResponse(responseCode = "400", description = "Checksum SHA-256 inválido ou campos obrigatórios ausentes"),
			@ApiResponse(responseCode = "409", description = "Já existe uma release publicada com a mesma versão")
	})
	public ResponseEntity<ReleaseFirmwareResponse> publicarRelease(@Valid @RequestBody ReleaseFirmwareRequest request) {
		ReleaseFirmwareResponse criada = firmwareOtaService.publicarRelease(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(criada);
	}

	@GetMapping("/releases")
	@Operation(
			summary = "Listar releases de firmware",
			description = "Retorna todas as releases de firmware disponíveis para implantação OTA."
	)
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Lista de releases retornada com sucesso")
	})
	public ResponseEntity<List<ReleaseFirmwareResponse>> listarReleases() {
		return ResponseEntity.ok(firmwareOtaService.listarReleases());
	}

	@PostMapping("/implantacoes")
	@Operation(
			summary = "Iniciar rollout de atualização OTA",
			description = "Valida compatibilidade de hardware e estabilidade da release, registra a versão anterior para fallback e altera o status do dispositivo para EM_ATUALIZACAO."
	)
	@ApiResponses({
			@ApiResponse(responseCode = "201", description = "Implantação OTA iniciada com sucesso"),
			@ApiResponse(responseCode = "400", description = "Requisição inválida"),
			@ApiResponse(responseCode = "404", description = "Dispositivo ou release de firmware não encontrados"),
			@ApiResponse(responseCode = "409", description = "Conflito de concorrência otimista ao bloquear o dispositivo para OTA"),
			@ApiResponse(responseCode = "422", description = "Incompatibilidade de hardware, release instável ou dispositivo indisponível")
	})
	public ResponseEntity<ImplantacaoOtaResponse> iniciarImplantacao(@Valid @RequestBody IniciarOtaRequest request) {
		ImplantacaoOtaResponse iniciada = firmwareOtaService.iniciarImplantacao(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(iniciada);
	}

	@PatchMapping("/implantacoes/{id}/resultado")
	@Operation(
			summary = "Processar callback de confirmação ou acionar Rollback Automático",
			description = "Valida o checksum SHA-256 calculado na flash do dispositivo. Em caso de sucesso, efetiva a nova versão; em caso de falha ou divergência de hash, restaura a versão anterior e abre incidente FALHA_OTA."
	)
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Resultado OTA processado com sucesso (SUCESSO ou FALHA_ROLLBACK)"),
			@ApiResponse(responseCode = "400", description = "Payload de confirmação inválido"),
			@ApiResponse(responseCode = "404", description = "Implantação OTA não encontrada"),
			@ApiResponse(responseCode = "409", description = "Conflito de concorrência ao atualizar o dispositivo")
	})
	public ResponseEntity<ImplantacaoOtaResponse> processarResultadoOta(
			@PathVariable Long id,
			@Valid @RequestBody ConfirmacaoOtaRequest request) {
		return ResponseEntity.ok(firmwareOtaService.processarResultadoOta(id, request));
	}

	@GetMapping("/implantacoes/dispositivo/{dispositivoId}")
	@Operation(
			summary = "Listar histórico de implantações OTA por dispositivo",
			description = "Retorna todas as atualizações OTA e eventuais rollbacks ocorridos em um dispositivo específico."
	)
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Histórico OTA retornado com sucesso"),
			@ApiResponse(responseCode = "404", description = "Dispositivo não encontrado")
	})
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
