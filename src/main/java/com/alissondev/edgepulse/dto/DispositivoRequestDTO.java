package com.alissondev.edgepulse.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

public record DispositivoRequestDTO(
		@NotNull(message = "O ID do cluster é obrigatório")
		@Positive(message = "O ID do cluster deve ser positivo")
		Long clusterId,

		@NotBlank(message = "O nome do dispositivo é obrigatório")
		String nome,

		@NotBlank(message = "O MAC Address é obrigatório")
		@Pattern(
				regexp = "^([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})$",
				message = "MAC Address inválido. Formato esperado: XX:XX:XX:XX:XX:XX"
		)
		String macAddress,

		@NotBlank(message = "O modelo de hardware é obrigatório")
		String modeloHardware,

		@NotBlank(message = "A versão de firmware é obrigatória")
		String versaoFirmwareAtual,

		Boolean perifericoDisplayAtivo,

		Boolean perifericoAudioAtivo
) {
}
