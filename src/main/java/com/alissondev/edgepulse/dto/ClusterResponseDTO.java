package com.alissondev.edgepulse.dto;

import com.alissondev.edgepulse.model.Cluster;
import com.alissondev.edgepulse.model.enums.AmbienteCluster;

import java.io.Serializable;
import java.time.LocalDateTime;

public record ClusterResponseDTO(
		Long id,
		String nome,
		AmbienteCluster ambiente,
		String localizacao,
		Integer intervaloHeartbeatSegundos,
		Boolean ativo,
		LocalDateTime dataCriacao
) implements Serializable {

	public static ClusterResponseDTO fromEntity(Cluster cluster) {
		return new ClusterResponseDTO(
				cluster.getId(),
				cluster.getNome(),
				cluster.getAmbiente(),
				cluster.getLocalizacao(),
				cluster.getIntervaloHeartbeatSegundos(),
				cluster.getAtivo(),
				cluster.getDataCriacao()
		);
	}

}
