package com.alissondev.edgepulse.service;

import com.alissondev.edgepulse.dto.ResumoTelemetriaDispositivoResponse;
import com.alissondev.edgepulse.dto.TelemetriaRequest;
import com.alissondev.edgepulse.dto.TelemetriaResponse;
import com.alissondev.edgepulse.event.AnomaliaDetectadaEvent;
import com.alissondev.edgepulse.model.Dispositivo;
import com.alissondev.edgepulse.model.LeituraTelemetria;
import com.alissondev.edgepulse.model.enums.StatusDispositivo;
import com.alissondev.edgepulse.model.enums.TipoAnomalia;
import com.alissondev.edgepulse.repository.DispositivoRepository;
import com.alissondev.edgepulse.repository.LeituraTelemetriaRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TelemetriaService {

	private static final BigDecimal LIMITE_TEMPERATURA_CPU_C = new BigDecimal("80.0");
	private static final BigDecimal LIMITE_TENSAO_MINIMA_V = new BigDecimal("3.0");
	private static final int LIMITE_BATERIA_CRITICA_PCT = 10;

	private final LeituraTelemetriaRepository leituraTelemetriaRepository;
	private final DispositivoRepository dispositivoRepository;
	private final ApplicationEventPublisher eventPublisher;

	@Transactional
	@CacheEvict(value = "dispositivos", allEntries = true)
	public TelemetriaResponse ingerirTelemetria(TelemetriaRequest request) {
		String macNormalizado = normalizarMacAddress(request.macAddress());

		Dispositivo dispositivo = dispositivoRepository.findByMacAddress(macNormalizado)
				.orElseThrow(() -> new EntityNotFoundException("Dispositivo não encontrado com MAC Address: " + macNormalizado));

		if (dispositivo.getStatus() == StatusDispositivo.MANUTENCAO) {
			throw new IllegalStateException("Ingestão bloqueada: o dispositivo " + macNormalizado + " encontra-se em MANUTENCAO");
		}

		boolean displayOk = request.statusDisplayOk() == null || request.statusDisplayOk();
		boolean audioOk = request.statusAudioOk() == null || request.statusAudioOk();

		List<TipoAnomalia> anomaliasDetectadas = avaliarAnomalias(dispositivo, request, displayOk, audioOk);

		LocalDateTime agora = LocalDateTime.now();
		dispositivo.setUltimoHeartbeat(agora);

		boolean possuiAnomalia = !anomaliasDetectadas.isEmpty();
		String detalhesAnomalia = null;

		if (possuiAnomalia) {
			detalhesAnomalia = anomaliasDetectadas.stream()
					.map(Enum::name)
					.collect(Collectors.joining(", "));
			dispositivo.setStatus(StatusDispositivo.DEGRADADO);
		} else {
			dispositivo.setStatus(StatusDispositivo.ONLINE);
		}

		dispositivoRepository.save(dispositivo);

		LeituraTelemetria leitura = LeituraTelemetria.builder()
				.dispositivo(dispositivo)
				.temperaturaCpuC(request.temperaturaCpuC())
				.tensaoAlimentacaoV(request.tensaoAlimentacaoV())
				.nivelBateriaPct(request.nivelBateriaPct())
				.sinalWifiRssiDbm(request.sinalWifiRssiDbm())
				.memoriaHeapLivreBytes(request.memoriaHeapLivreBytes())
				.statusDisplayOk(displayOk)
				.statusAudioOk(audioOk)
				.anomaliaDetectada(possuiAnomalia)
				.detalhesAnomalia(detalhesAnomalia)
				.dataHoraLeitura(agora)
				.build();

		LeituraTelemetria leituraSalva = leituraTelemetriaRepository.save(leitura);

		if (possuiAnomalia) {
			AnomaliaDetectadaEvent event = new AnomaliaDetectadaEvent(
					dispositivo.getId(),
					dispositivo.getMacAddress(),
					anomaliasDetectadas,
					detalhesAnomalia,
					agora
			);
			eventPublisher.publishEvent(event);
		}

		return TelemetriaResponse.fromEntity(leituraSalva);
	}

	@Transactional(readOnly = true)
	public List<TelemetriaResponse> listarHistoricoPorDispositivo(Long dispositivoId) {
		if (!dispositivoRepository.existsById(dispositivoId)) {
			throw new EntityNotFoundException("Dispositivo não encontrado com ID: " + dispositivoId);
		}
		return leituraTelemetriaRepository.findByDispositivoIdOrderByDataHoraLeituraDesc(dispositivoId)
				.stream()
				.map(TelemetriaResponse::fromEntity)
				.toList();
	}

	@Transactional(readOnly = true)
	public TelemetriaResponse buscarUltimaLeitura(Long dispositivoId) {
		LeituraTelemetria leitura = leituraTelemetriaRepository.findFirstByDispositivoIdOrderByDataHoraLeituraDesc(dispositivoId)
				.orElseThrow(() -> new EntityNotFoundException("Nenhuma leitura de telemetria encontrada para o dispositivo ID: " + dispositivoId));
		return TelemetriaResponse.fromEntity(leitura);
	}

	@Transactional(readOnly = true)
	public List<TelemetriaResponse> listarLeiturasComAnomalia() {
		return leituraTelemetriaRepository.findByAnomaliaDetectadaTrueOrderByDataHoraLeituraDesc()
				.stream()
				.map(TelemetriaResponse::fromEntity)
				.toList();
	}

	@Transactional(readOnly = true)
	public ResumoTelemetriaDispositivoResponse obterResumoDispositivo(Long dispositivoId) {
		Dispositivo dispositivo = dispositivoRepository.findById(dispositivoId)
				.orElseThrow(() -> new EntityNotFoundException("Dispositivo não encontrado com ID: " + dispositivoId));

		List<LeituraTelemetria> leituras = leituraTelemetriaRepository.findByDispositivoIdOrderByDataHoraLeituraDesc(dispositivoId);

		if (leituras.isEmpty()) {
			return new ResumoTelemetriaDispositivoResponse(
					dispositivo.getId(),
					dispositivo.getMacAddress(),
					dispositivo.getNome(),
					BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
					BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
					0L,
					0L
			);
		}

		BigDecimal somaTemp = leituras.stream()
				.map(LeituraTelemetria::getTemperaturaCpuC)
				.reduce(BigDecimal.ZERO, BigDecimal::add);

		BigDecimal somaTensao = leituras.stream()
				.map(LeituraTelemetria::getTensaoAlimentacaoV)
				.reduce(BigDecimal.ZERO, BigDecimal::add);

		BigDecimal divisor = BigDecimal.valueOf(leituras.size());
		BigDecimal mediaTemp = somaTemp.divide(divisor, 2, RoundingMode.HALF_UP);
		BigDecimal mediaTensao = somaTensao.divide(divisor, 2, RoundingMode.HALF_UP);

		long totalAnomalias = leituras.stream()
				.filter(l -> Boolean.TRUE.equals(l.getAnomaliaDetectada()))
				.count();

		return new ResumoTelemetriaDispositivoResponse(
				dispositivo.getId(),
				dispositivo.getMacAddress(),
				dispositivo.getNome(),
				mediaTemp,
				mediaTensao,
				leituras.size(),
				totalAnomalias
		);
	}

	private List<TipoAnomalia> avaliarAnomalias(
			Dispositivo dispositivo,
			TelemetriaRequest request,
			boolean displayOk,
			boolean audioOk) {
		List<TipoAnomalia> anomalias = new ArrayList<>();

		if (request.temperaturaCpuC().compareTo(LIMITE_TEMPERATURA_CPU_C) > 0) {
			anomalias.add(TipoAnomalia.SUPERAQUECIMENTO);
		}
		if (request.tensaoAlimentacaoV().compareTo(LIMITE_TENSAO_MINIMA_V) < 0) {
			anomalias.add(TipoAnomalia.SUBTENSAO_ELETRICA);
		}
		if (request.nivelBateriaPct() < LIMITE_BATERIA_CRITICA_PCT) {
			anomalias.add(TipoAnomalia.BATERIA_CRITICA);
		}
		if (dispositivo.isPerifericoDisplayAtivo() && !displayOk) {
			anomalias.add(TipoAnomalia.FALHA_PERIFERICO_DISPLAY);
		}
		if (dispositivo.isPerifericoAudioAtivo() && !audioOk) {
			anomalias.add(TipoAnomalia.FALHA_PERIFERICO_AUDIO);
		}

		return anomalias;
	}

	private String normalizarMacAddress(String macAddress) {
		return macAddress.trim().replace("-", ":").toUpperCase(Locale.ROOT);
	}

}
