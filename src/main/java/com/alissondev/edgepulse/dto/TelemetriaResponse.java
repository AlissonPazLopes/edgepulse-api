package com.alissondev.edgepulse.dto;

import com.alissondev.edgepulse.model.LeituraTelemetria;
import com.alissondev.edgepulse.model.enums.StatusDispositivo;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TelemetriaResponse(
		Long id,
		Long dispositivoId,
		String macAddress,
		StatusDispositivo statusDispositivo,
		BigDecimal temperaturaCpuC,
		BigDecimal tensaoAlimentacaoV,
		Integer nivelBateriaPct,
		Integer sinalWifiRssiDbm,
		Long memoriaHeapLivreBytes,
		Boolean statusDisplayOk,
		Boolean statusAudioOk,
		Boolean anomaliaDetectada,
		String detalhesAnomalia,
		LocalDateTime dataHoraLeitura
) implements Serializable {

	public static TelemetriaResponse fromEntity(LeituraTelemetria leitura) {
		return new TelemetriaResponse(
				leitura.getId(),
				leitura.getDispositivo().getId(),
				leitura.getDispositivo().getMacAddress(),
				leitura.getDispositivo().getStatus(),
				leitura.getTemperaturaCpuC(),
				leitura.getTensaoAlimentacaoV(),
				leitura.getNivelBateriaPct(),
				leitura.getSinalWifiRssiDbm(),
				leitura.getMemoriaHeapLivreBytes(),
				leitura.getStatusDisplayOk(),
				leitura.getStatusAudioOk(),
				leitura.getAnomaliaDetectada(),
				leitura.getDetalhesAnomalia(),
				leitura.getDataHoraLeitura()
		);
	}

}
