package com.alissondev.edgepulse.dto;

import java.io.Serializable;
import java.math.BigDecimal;

public record MetricasConfiabilidadeResponse(
		Long dispositivoId,
		String nomeDispositivo,
		String macAddress,
		long totalIncidentes,
		long incidentesResolvidos,
		long incidentesAtivos,
		long tempoOperacaoTotalSegundos,
		long tempoDowntimeTotalSegundos,
		double mtbfSegundos,
		double mttrSegundos,
		BigDecimal disponibilidadePercentual
) implements Serializable {
}
