package com.alissondev.edgepulse.dto;

import com.alissondev.edgepulse.model.ReleaseFirmware;

import java.io.Serializable;
import java.time.LocalDateTime;

public record ReleaseFirmwareResponse(
		Long id,
		String versao,
		String modeloHardwareAlvo,
		String checksumSha256,
		Long tamanhoBytes,
		String notasLancamento,
		Boolean estavel,
		LocalDateTime dataPublicacao
) implements Serializable {

	public static ReleaseFirmwareResponse fromEntity(ReleaseFirmware release) {
		return new ReleaseFirmwareResponse(
				release.getId(),
				release.getVersao(),
				release.getModeloHardwareAlvo(),
				release.getChecksumSha256(),
				release.getTamanhoBytes(),
				release.getNotasLancamento(),
				release.getEstavel(),
				release.getDataPublicacao()
		);
	}

}
