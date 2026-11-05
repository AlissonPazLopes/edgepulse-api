package com.alissondev.edgepulse.service;

import com.alissondev.edgepulse.dto.IncidenteRequest;
import com.alissondev.edgepulse.dto.IncidenteResponse;
import com.alissondev.edgepulse.dto.MetricasConfiabilidadeResponse;
import com.alissondev.edgepulse.event.AnomaliaDetectadaEvent;
import com.alissondev.edgepulse.model.Dispositivo;
import com.alissondev.edgepulse.model.Incidente;
import com.alissondev.edgepulse.model.enums.SeveridadeIncidente;
import com.alissondev.edgepulse.model.enums.StatusDispositivo;
import com.alissondev.edgepulse.model.enums.StatusIncidente;
import com.alissondev.edgepulse.model.enums.TipoAnomalia;
import com.alissondev.edgepulse.repository.DispositivoRepository;
import com.alissondev.edgepulse.repository.IncidenteRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class IncidenteService {

	private static final Set<StatusIncidente> STATUS_ATIVOS = Set.of(
			StatusIncidente.ABERTO,
			StatusIncidente.EM_ANALISE
	);

	private final IncidenteRepository incidenteRepository;
	private final DispositivoRepository dispositivoRepository;

	@Transactional
	@EventListener
	public void processarEventoAnomalia(AnomaliaDetectadaEvent event) {
		Dispositivo dispositivo = dispositivoRepository.findById(event.dispositivoId())
				.orElseThrow(() -> new EntityNotFoundException("Dispositivo não encontrado com ID: " + event.dispositivoId()));

		for (TipoAnomalia tipoAnomalia : event.anomalias()) {
			abrirIncidenteSeNaoAtivo(
					dispositivo,
					tipoAnomalia,
					determinarSeveridade(tipoAnomalia),
					"Anomalia automática detectada na telemetria: " + tipoAnomalia.name()
			);
		}
	}

	@Transactional
	@CacheEvict(value = "dispositivos", allEntries = true)
	public IncidenteResponse abrirIncidenteManual(IncidenteRequest request) {
		Dispositivo dispositivo = dispositivoRepository.findById(request.dispositivoId())
				.orElseThrow(() -> new EntityNotFoundException("Dispositivo não encontrado com ID: " + request.dispositivoId()));

		boolean jaAtivo = incidenteRepository.existsByDispositivoIdAndTipoAnomaliaAndStatusIn(
				dispositivo.getId(),
				request.tipoAnomalia(),
				STATUS_ATIVOS
		);

		if (jaAtivo) {
			throw new IllegalStateException("Já existe um incidente ativo do tipo " + request.tipoAnomalia() + " para este dispositivo");
		}

		SeveridadeIncidente severidade = request.severidade() != null
				? request.severidade()
				: determinarSeveridade(request.tipoAnomalia());

		Incidente incidente = Incidente.builder()
				.dispositivo(dispositivo)
				.tipoAnomalia(request.tipoAnomalia())
				.severidade(severidade)
				.descricao(request.descricao())
				.status(StatusIncidente.ABERTO)
				.dataAbertura(LocalDateTime.now())
				.build();

		Incidente salvo = incidenteRepository.save(incidente);
		return IncidenteResponse.fromEntity(salvo);
	}

	@Transactional
	public void abrirIncidenteSeNaoAtivo(
			Dispositivo dispositivo,
			TipoAnomalia tipoAnomalia,
			SeveridadeIncidente severidade,
			String descricao) {
		boolean jaAtivo = incidenteRepository.existsByDispositivoIdAndTipoAnomaliaAndStatusIn(
				dispositivo.getId(),
				tipoAnomalia,
				STATUS_ATIVOS
		);

		if (jaAtivo) {
			return;
		}

		Incidente incidente = Incidente.builder()
				.dispositivo(dispositivo)
				.tipoAnomalia(tipoAnomalia)
				.severidade(severidade)
				.descricao(descricao)
				.status(StatusIncidente.ABERTO)
				.dataAbertura(LocalDateTime.now())
				.build();

		incidenteRepository.save(incidente);
	}

	@Transactional
	@CacheEvict(value = "dispositivos", allEntries = true)
	public IncidenteResponse resolverIncidente(Long incidenteId) {
		return resolverIncidente(incidenteId, null);
	}

	@Transactional
	@CacheEvict(value = "dispositivos", allEntries = true)
	public IncidenteResponse resolverIncidente(Long incidenteId, String notaResolucao) {
		Incidente incidente = incidenteRepository.findById(incidenteId)
				.orElseThrow(() -> new EntityNotFoundException("Incidente não encontrado com ID: " + incidenteId));

		if (incidente.getStatus() == StatusIncidente.RESOLVIDO) {
			throw new IllegalStateException("O incidente já se encontra RESOLVIDO");
		}

		LocalDateTime dataResolucao = LocalDateTime.now();
		long tempoReparo = Math.max(1L, Duration.between(incidente.getDataAbertura(), dataResolucao).getSeconds());

		incidente.setStatus(StatusIncidente.RESOLVIDO);
		incidente.setDataResolucao(dataResolucao);
		incidente.setTempoReparoSegundos(tempoReparo);

		if (notaResolucao != null && !notaResolucao.isBlank()) {
			String novaDescricao = incidente.getDescricao() + " | Resolução: " + notaResolucao.trim();
			if (novaDescricao.length() > 255) {
				novaDescricao = novaDescricao.substring(0, 255);
			}
			incidente.setDescricao(novaDescricao);
		}

		Incidente salvo = incidenteRepository.save(incidente);

		Dispositivo dispositivo = incidente.getDispositivo();
		boolean possuiOutrosIncidentesAtivos = incidenteRepository.existsByDispositivoIdAndStatusIn(
				dispositivo.getId(),
				STATUS_ATIVOS
		);

		if (!possuiOutrosIncidentesAtivos && dispositivo.getStatus() != StatusDispositivo.MANUTENCAO) {
			dispositivo.setStatus(StatusDispositivo.ONLINE);
			dispositivoRepository.save(dispositivo);
		}

		return IncidenteResponse.fromEntity(salvo);
	}

	@Transactional(readOnly = true)
	public List<IncidenteResponse> listarTodos() {
		return incidenteRepository.findAll()
				.stream()
				.map(IncidenteResponse::fromEntity)
				.toList();
	}

	@Transactional(readOnly = true)
	public List<IncidenteResponse> listarPorDispositivo(Long dispositivoId) {
		if (!dispositivoRepository.existsById(dispositivoId)) {
			throw new EntityNotFoundException("Dispositivo não encontrado com ID: " + dispositivoId);
		}
		return incidenteRepository.findByDispositivoIdOrderByDataAberturaDesc(dispositivoId)
				.stream()
				.map(IncidenteResponse::fromEntity)
				.toList();
	}

	@Transactional(readOnly = true)
	public MetricasConfiabilidadeResponse calcularMetricasConfiabilidade(Long dispositivoId) {
		Dispositivo dispositivo = dispositivoRepository.findById(dispositivoId)
				.orElseThrow(() -> new EntityNotFoundException("Dispositivo não encontrado com ID: " + dispositivoId));

		LocalDateTime agora = LocalDateTime.now();
		long tempoMonitoradoTotal = Math.max(1L, Duration.between(dispositivo.getDataCadastro(), agora).getSeconds());

		List<Incidente> incidentes = incidenteRepository.findByDispositivoId(dispositivoId);
		long totalIncidentes = incidentes.size();

		List<Incidente> resolvidos = incidentes.stream()
				.filter(i -> i.getStatus() == StatusIncidente.RESOLVIDO)
				.toList();

		long incidentesResolvidos = resolvidos.size();
		long incidentesAtivos = incidentes.stream()
				.filter(i -> STATUS_ATIVOS.contains(i.getStatus()))
				.count();

		long somaReparoResolvidos = resolvidos.stream()
				.mapToLong(i -> i.getTempoReparoSegundos() != null ? i.getTempoReparoSegundos() : 0L)
				.sum();

		long downtimeAtivos = incidentes.stream()
				.filter(i -> i.getStatus() != StatusIncidente.RESOLVIDO)
				.mapToLong(i -> Math.max(1L, Duration.between(i.getDataAbertura(), agora).getSeconds()))
				.sum();

		long tempoDowntimeTotal = somaReparoResolvidos + downtimeAtivos;
		if (tempoMonitoradoTotal < tempoDowntimeTotal) {
			tempoMonitoradoTotal = tempoDowntimeTotal;
		}

		long uptimeSegundos = Math.max(0L, tempoMonitoradoTotal - tempoDowntimeTotal);

		double mttrSegundos = incidentesResolvidos > 0
				? (double) somaReparoResolvidos / incidentesResolvidos
				: 0.0;

		double mtbfSegundos;
		BigDecimal disponibilidadePercentual;

		if (totalIncidentes == 0) {
			mtbfSegundos = (double) uptimeSegundos;
			disponibilidadePercentual = new BigDecimal("100.00");
		} else {
			mtbfSegundos = (double) uptimeSegundos / totalIncidentes;
			double somaMtbfMttr = mtbfSegundos + mttrSegundos;
			if (somaMtbfMttr <= 0.0) {
				disponibilidadePercentual = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
			} else {
				double disp = (mtbfSegundos / somaMtbfMttr) * 100.0;
				disponibilidadePercentual = BigDecimal.valueOf(disp).setScale(2, RoundingMode.HALF_UP);
			}
		}

		double mtbfArredondado = BigDecimal.valueOf(mtbfSegundos).setScale(2, RoundingMode.HALF_UP).doubleValue();
		double mttrArredondado = BigDecimal.valueOf(mttrSegundos).setScale(2, RoundingMode.HALF_UP).doubleValue();

		return new MetricasConfiabilidadeResponse(
				dispositivo.getId(),
				dispositivo.getNome(),
				dispositivo.getMacAddress(),
				totalIncidentes,
				incidentesResolvidos,
				incidentesAtivos,
				uptimeSegundos,
				tempoDowntimeTotal,
				mtbfArredondado,
				mttrArredondado,
				disponibilidadePercentual
		);
	}

	public SeveridadeIncidente determinarSeveridade(TipoAnomalia tipoAnomalia) {
		return switch (tipoAnomalia) {
			case SUPERAQUECIMENTO, SUBTENSAO_ELETRICA, HEARTBEAT_TIMEOUT -> SeveridadeIncidente.CRITICA;
			case FALHA_PERIFERICO_DISPLAY, FALHA_PERIFERICO_AUDIO, BATERIA_CRITICA, FALHA_OTA -> SeveridadeIncidente.ALTA;
		};
	}

}
