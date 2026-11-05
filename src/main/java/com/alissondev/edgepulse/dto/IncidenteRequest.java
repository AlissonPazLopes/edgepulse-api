package com.alissondev.edgepulse.dto;

import com.alissondev.edgepulse.model.enums.SeveridadeIncidente;
import com.alissondev.edgepulse.model.enums.TipoAnomalia;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record IncidenteRequest(
		@NotNull(message = "O ID do dispositivo é obrigatório")
		@Positive(message = "O ID do dispositivo deve ser positivo")
		Long dispositivoId,

		@NotNull(message = "O tipo de anomalia é obrigatório")
		TipoAnomalia tipoAnomalia,

		SeveridadeIncidente severidade,

		@NotBlank(message = "A descrição do incidente é obrigatória")
		String descricao
) {
}
