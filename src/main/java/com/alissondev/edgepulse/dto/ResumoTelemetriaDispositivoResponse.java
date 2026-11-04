package com.alissondev.edgepulse.dto;

import java.io.Serializable;
import java.math.BigDecimal;

public record ResumoTelemetriaDispositivoResponse(
		Long dispositivoId,
		String macAddress,
		String nomeDispositivo,
		BigDecimal mediaTemperaturaCpuC,
		BigDecimal mediaTensaoAlimentacaoV,
		long totalPacotesRecebidos,
		long totalAnomalias
) implements Serializable {
}
