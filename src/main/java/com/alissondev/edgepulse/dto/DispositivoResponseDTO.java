package com.alissondev.edgepulse.dto;

import com.alissondev.edgepulse.model.Dispositivo;
import com.alissondev.edgepulse.model.enums.StatusDispositivo;

import java.io.Serializable;
import java.time.LocalDateTime;

public record DispositivoResponseDTO(
		Long id,
		Long clusterId,
		String clusterNome,
		String nome,
		String macAddress,
		String modeloHardware,
		String versaoFirmwareAtual,
		StatusDispositivo status,
		Boolean perifericoDisplayAtivo,
		Boolean perifericoAudioAtivo,
		LocalDateTime ultimoHeartbeat,
		LocalDateTime dataCadastro,
		Long versao
) implements Serializable {

	public static DispositivoResponseDTO fromEntity(Dispositivo dispositivo) {
		return new DispositivoResponseDTO(
				dispositivo.getId(),
				dispositivo.getCluster().getId(),
				dispositivo.getCluster().getNome(),
				dispositivo.getNome(),
				dispositivo.getMacAddress(),
				dispositivo.getModeloHardware(),
				dispositivo.getVersaoFirmwareAtual(),
				dispositivo.getStatus(),
				dispositivo.getPerifericoDisplayAtivo(),
				dispositivo.getPerifericoAudioAtivo(),
				dispositivo.getUltimoHeartbeat(),
				dispositivo.getDataCadastro(),
				dispositivo.getVersao()
		);
	}

}
