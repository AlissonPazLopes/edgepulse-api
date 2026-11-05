package com.alissondev.edgepulse.dto;

import com.alissondev.edgepulse.model.Incidente;
import com.alissondev.edgepulse.model.enums.SeveridadeIncidente;
import com.alissondev.edgepulse.model.enums.StatusIncidente;
import com.alissondev.edgepulse.model.enums.TipoAnomalia;

import java.io.Serializable;
import java.time.LocalDateTime;

public record IncidenteResponse(
		Long id,
		Long dispositivoId,
		String nomeDispositivo,
		String macAddress,
		TipoAnomalia tipoAnomalia,
		SeveridadeIncidente severidade,
		String descricao,
		StatusIncidente status,
		LocalDateTime dataAbertura,
		LocalDateTime dataResolucao,
		Long tempoReparoSegundos
) implements Serializable {

	public static IncidenteResponse fromEntity(Incidente incidente) {
		return new IncidenteResponse(
				incidente.getId(),
				incidente.getDispositivo().getId(),
				incidente.getDispositivo().getNome(),
				incidente.getDispositivo().getMacAddress(),
				incidente.getTipoAnomalia(),
				incidente.getSeveridade(),
				incidente.getDescricao(),
				incidente.getStatus(),
				incidente.getDataAbertura(),
				incidente.getDataResolucao(),
				incidente.getTempoReparoSegundos()
		);
	}

}
