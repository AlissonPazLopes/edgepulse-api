package com.alissondev.edgepulse.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;

public record TelemetriaRequest(
		@NotBlank(message = "O MAC Address é obrigatório")
		@Pattern(
				regexp = "^([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})$",
				message = "MAC Address inválido. Formato esperado: XX:XX:XX:XX:XX:XX"
		)
		String macAddress,

		@NotNull(message = "A temperatura da CPU é obrigatória")
		BigDecimal temperaturaCpuC,

		@NotNull(message = "A tensão de alimentação é obrigatória")
		BigDecimal tensaoAlimentacaoV,

		@NotNull(message = "O nível de bateria é obrigatório")
		@Min(value = 0, message = "O nível de bateria deve ser no mínimo 0")
		@Max(value = 100, message = "O nível de bateria deve ser no máximo 100")
		Integer nivelBateriaPct,

		@NotNull(message = "O sinal WiFi RSSI é obrigatório")
		Integer sinalWifiRssiDbm,

		@NotNull(message = "A memória heap livre é obrigatória")
		Long memoriaHeapLivreBytes,

		Boolean statusDisplayOk,

		Boolean statusAudioOk
) {
}
