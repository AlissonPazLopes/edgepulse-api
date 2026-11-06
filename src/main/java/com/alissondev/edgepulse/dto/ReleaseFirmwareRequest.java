package com.alissondev.edgepulse.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

public record ReleaseFirmwareRequest(
		@NotBlank(message = "A versão do firmware é obrigatória")
		String versao,

		@NotBlank(message = "O modelo de hardware alvo é obrigatório")
		String modeloHardwareAlvo,

		@NotBlank(message = "O checksum SHA-256 é obrigatório")
		@Pattern(
				regexp = "^[a-fA-F0-9]{64}$",
				message = "O checksum SHA-256 deve conter exatamente 64 caracteres hexadecimais"
		)
		String checksumSha256,

		@NotNull(message = "O tamanho em bytes é obrigatório")
		@Positive(message = "O tamanho em bytes deve ser positivo")
		Long tamanhoBytes,

		String notasLancamento,

		Boolean estavel
) {
}
