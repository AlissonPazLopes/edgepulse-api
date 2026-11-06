package com.alissondev.edgepulse.dto;

import jakarta.validation.constraints.NotNull;

public record ConfirmacaoOtaRequest(
		@NotNull(message = "O indicador de sucesso é obrigatório")
		Boolean sucesso,

		String checksumCalculadoNoDispositivo,

		String mensagemErro
) {
}
