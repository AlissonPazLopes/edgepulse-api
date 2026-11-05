package com.alissondev.edgepulse.scheduler;

import com.alissondev.edgepulse.model.Dispositivo;
import com.alissondev.edgepulse.model.enums.SeveridadeIncidente;
import com.alissondev.edgepulse.model.enums.StatusDispositivo;
import com.alissondev.edgepulse.model.enums.TipoAnomalia;
import com.alissondev.edgepulse.repository.DispositivoRepository;
import com.alissondev.edgepulse.service.IncidenteService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class HeartbeatWatchdogScheduler {

	private final DispositivoRepository dispositivoRepository;
	private final IncidenteService incidenteService;

	@Scheduled(fixedDelayString = "${edgepulse.watchdog.intervalo-ms:60000}")
	@Transactional
	@CacheEvict(value = "dispositivos", allEntries = true)
	public int verificarTimeoutsHeartbeat() {
		List<Dispositivo> candidatos = new ArrayList<>();
		candidatos.addAll(dispositivoRepository.findByStatus(StatusDispositivo.ONLINE));
		candidatos.addAll(dispositivoRepository.findByStatus(StatusDispositivo.DEGRADADO));

		LocalDateTime agora = LocalDateTime.now();
		int totalEmTimeout = 0;

		for (Dispositivo dispositivo : candidatos) {
			if (dispositivo.getUltimoHeartbeat() == null) {
				continue;
			}

			int intervaloCluster = dispositivo.getCluster().getIntervaloHeartbeatSegundos() != null
					? dispositivo.getCluster().getIntervaloHeartbeatSegundos()
					: 30;
			long limiteToleranciaSegundos = (long) intervaloCluster * 3L;

			long segundosSemPulso = Duration.between(dispositivo.getUltimoHeartbeat(), agora).getSeconds();

			if (segundosSemPulso > limiteToleranciaSegundos) {
				dispositivo.setStatus(StatusDispositivo.OFFLINE);
				dispositivoRepository.save(dispositivo);

				incidenteService.abrirIncidenteSeNaoAtivo(
						dispositivo,
						TipoAnomalia.HEARTBEAT_TIMEOUT,
						SeveridadeIncidente.CRITICA,
						"Timeout de heartbeat detectado pelo Watchdog (" + segundosSemPulso + "s sem comunicação; limite: " + limiteToleranciaSegundos + "s)"
				);
				totalEmTimeout++;
			}
		}

		return totalEmTimeout;
	}

}
