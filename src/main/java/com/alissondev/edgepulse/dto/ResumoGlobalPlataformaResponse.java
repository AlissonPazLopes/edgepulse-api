package com.alissondev.edgepulse.dto;

import java.io.Serializable;
import java.math.BigDecimal;

public record ResumoGlobalPlataformaResponse(
		long totalClusters,
		long clustersAtivos,
		long totalDispositivos,
		long dispositivosOnline,
		long dispositivosDegradados,
		long dispositivosOffline,
		long dispositivosEmAtualizacao,
		long dispositivosManutencao,
		long totalLeiturasTelemetria,
		long totalAnomaliasDetectadas,
		long totalIncidentesAtivos,
		long totalImplantacoesOtaSucesso,
		long totalImplantacoesOtaRollback,
		BigDecimal disponibilidadeMediaGlobalPercentual
) implements Serializable {
}
