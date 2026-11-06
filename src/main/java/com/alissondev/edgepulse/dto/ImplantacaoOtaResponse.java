package com.alissondev.edgepulse.dto;

import com.alissondev.edgepulse.model.ImplantacaoOta;
import com.alissondev.edgepulse.model.enums.StatusDispositivo;
import com.alissondev.edgepulse.model.enums.StatusImplantacaoOta;

import java.io.Serializable;
import java.time.LocalDateTime;

public record ImplantacaoOtaResponse(
		Long id,
		Long dispositivoId,
		String nomeDispositivo,
		String macAddress,
		StatusDispositivo statusDispositivo,
		String versaoAtualDispositivo,
		Long firmwareDestinoId,
		String versaoDestino,
		String versaoAnterior,
		StatusImplantacaoOta status,
		String mensagemErro,
		LocalDateTime dataInicio,
		LocalDateTime dataConclusao
) implements Serializable {

	public static ImplantacaoOtaResponse fromEntity(ImplantacaoOta implantacao) {
		return new ImplantacaoOtaResponse(
				implantacao.getId(),
				implantacao.getDispositivo().getId(),
				implantacao.getDispositivo().getNome(),
				implantacao.getDispositivo().getMacAddress(),
				implantacao.getDispositivo().getStatus(),
				implantacao.getDispositivo().getVersaoFirmwareAtual(),
				implantacao.getFirmwareDestino().getId(),
				implantacao.getFirmwareDestino().getVersao(),
				implantacao.getVersaoAnterior(),
				implantacao.getStatus(),
				implantacao.getMensagemErro(),
				implantacao.getDataInicio(),
				implantacao.getDataConclusao()
		);
	}

}
