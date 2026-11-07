package com.alissondev.edgepulse.dto;

import com.alissondev.edgepulse.model.enums.AmbienteCluster;
import com.alissondev.edgepulse.model.enums.StatusSaudeCluster;

import java.io.Serializable;
import java.math.BigDecimal;

public record DashboardClusterResponse(
		Long clusterId,
		String nomeCluster,
		AmbienteCluster ambiente,
		long totalDispositivos,
		long dispositivosOnline,
		long dispositivosDegradados,
		long dispositivosOffline,
		long dispositivosEmAtualizacao,
		long dispositivosManutencao,
		long incidentesAtivos,
		long incidentesCriticosAtivos,
		BigDecimal temperaturaMediaClusterC,
		BigDecimal disponibilidadeMediaPercentual,
		double mtbfMedioSegundos,
		double mttrMedioSegundos,
		StatusSaudeCluster statusSaudeCluster
) implements Serializable {
}
