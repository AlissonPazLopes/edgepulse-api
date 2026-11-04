package com.alissondev.edgepulse.event;

import com.alissondev.edgepulse.model.enums.TipoAnomalia;

import java.time.LocalDateTime;
import java.util.List;

public record AnomaliaDetectadaEvent(
		Long dispositivoId,
		String macAddress,
		List<TipoAnomalia> anomalias,
		String descricao,
		LocalDateTime timestamp
) {
}
