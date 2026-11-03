package com.alissondev.edgepulse.dto;

import com.alissondev.edgepulse.model.enums.StatusDispositivo;
import jakarta.validation.constraints.NotNull;

public record AtualizacaoStatusDispositivoDTO(
		@NotNull(message = "O novo status é obrigatório")
		StatusDispositivo status
) {
}
