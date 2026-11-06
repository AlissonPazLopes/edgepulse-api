package com.alissondev.edgepulse.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record IniciarOtaRequest(
		@NotNull(message = "O ID do dispositivo é obrigatório")
		@Positive(message = "O ID do dispositivo deve ser positivo")
		Long dispositivoId,

		@NotNull(message = "O ID do firmware de destino é obrigatório")
		@Positive(message = "O ID do firmware de destino deve ser positivo")
		Long firmwareDestinoId
) {
}
