package com.alissondev.edgepulse.dto;

import com.alissondev.edgepulse.model.enums.AmbienteCluster;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ClusterRequestDTO(
		@NotBlank(message = "O nome do cluster é obrigatório")
		String nome,

		@NotNull(message = "O ambiente do cluster é obrigatório")
		AmbienteCluster ambiente,

		@NotBlank(message = "A localização do cluster é obrigatória")
		String localizacao,

		@Positive(message = "O intervalo de heartbeat deve ser positivo")
		Integer intervaloHeartbeatSegundos,

		Boolean ativo
) {
}
