package com.alissondev.edgepulse.service;

import com.alissondev.edgepulse.dto.DashboardClusterResponse;
import com.alissondev.edgepulse.dto.MetricasConfiabilidadeResponse;
import com.alissondev.edgepulse.dto.ResumoGlobalPlataformaResponse;
import com.alissondev.edgepulse.model.Cluster;
import com.alissondev.edgepulse.model.Dispositivo;
import com.alissondev.edgepulse.model.ImplantacaoOta;
import com.alissondev.edgepulse.model.Incidente;
import com.alissondev.edgepulse.model.LeituraTelemetria;
import com.alissondev.edgepulse.model.enums.SeveridadeIncidente;
import com.alissondev.edgepulse.model.enums.StatusDispositivo;
import com.alissondev.edgepulse.model.enums.StatusImplantacaoOta;
import com.alissondev.edgepulse.model.enums.StatusIncidente;
import com.alissondev.edgepulse.model.enums.StatusSaudeCluster;
import com.alissondev.edgepulse.repository.ClusterRepository;
import com.alissondev.edgepulse.repository.DispositivoRepository;
import com.alissondev.edgepulse.repository.ImplantacaoOtaRepository;
import com.alissondev.edgepulse.repository.IncidenteRepository;
import com.alissondev.edgepulse.repository.LeituraTelemetriaRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class DashboardService {

	private static final Set<StatusIncidente> STATUS_INCIDENTES_ATIVOS = Set.of(
			StatusIncidente.ABERTO,
			StatusIncidente.EM_ANALISE
	);

	private final ClusterRepository clusterRepository;
	private final DispositivoRepository dispositivoRepository;
	private final IncidenteRepository incidenteRepository;
	private final LeituraTelemetriaRepository leituraTelemetriaRepository;
	private final ImplantacaoOtaRepository implantacaoOtaRepository;
	private final IncidenteService incidenteService;

	@Transactional(readOnly = true)
	public DashboardClusterResponse obterDashboardCluster(Long clusterId) {
		Cluster cluster = clusterRepository.findById(clusterId)
				.orElseThrow(() -> new EntityNotFoundException("Cluster não encontrado com ID: " + clusterId));

		List<Dispositivo> dispositivos = dispositivoRepository.findByClusterId(clusterId);
		long totalDispositivos = dispositivos.size();

		long online = dispositivos.stream().filter(d -> d.getStatus() == StatusDispositivo.ONLINE).count();
		long degradados = dispositivos.stream().filter(d -> d.getStatus() == StatusDispositivo.DEGRADADO).count();
		long offline = dispositivos.stream().filter(d -> d.getStatus() == StatusDispositivo.OFFLINE).count();
		long emAtualizacao = dispositivos.stream().filter(d -> d.getStatus() == StatusDispositivo.EM_ATUALIZACAO).count();
		long manutencao = dispositivos.stream().filter(d -> d.getStatus() == StatusDispositivo.MANUTENCAO).count();

		long incidentesAtivos = 0L;
		long incidentesCriticosAtivos = 0L;
		List<LeituraTelemetria> leiturasCluster = new ArrayList<>();

		BigDecimal somaDisponibilidade = BigDecimal.ZERO;
		double somaMtbf = 0.0;
		double somaMttr = 0.0;

		for (Dispositivo dispositivo : dispositivos) {
			List<Incidente> incidentesDisp = incidenteRepository.findByDispositivoId(dispositivo.getId());
			for (Incidente inc : incidentesDisp) {
				if (STATUS_INCIDENTES_ATIVOS.contains(inc.getStatus())) {
					incidentesAtivos++;
					if (inc.getSeveridade() == SeveridadeIncidente.CRITICA) {
						incidentesCriticosAtivos++;
					}
				}
			}

			leiturasCluster.addAll(
					leituraTelemetriaRepository.findByDispositivoIdOrderByDataHoraLeituraDesc(dispositivo.getId())
			);

			MetricasConfiabilidadeResponse metricas = incidenteService.calcularMetricasConfiabilidade(dispositivo.getId());
			somaDisponibilidade = somaDisponibilidade.add(metricas.disponibilidadePercentual());
			somaMtbf += metricas.mtbfSegundos();
			somaMttr += metricas.mttrSegundos();
		}

		BigDecimal temperaturaMediaClusterC;
		if (leiturasCluster.isEmpty()) {
			temperaturaMediaClusterC = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
		} else {
			BigDecimal somaTemp = leiturasCluster.stream()
					.map(LeituraTelemetria::getTemperaturaCpuC)
					.reduce(BigDecimal.ZERO, BigDecimal::add);
			temperaturaMediaClusterC = somaTemp.divide(
					BigDecimal.valueOf(leiturasCluster.size()),
					2,
					RoundingMode.HALF_UP
			);
		}

		BigDecimal disponibilidadeMediaPercentual;
		double mtbfMedioSegundos;
		double mttrMedioSegundos;

		if (totalDispositivos == 0) {
			disponibilidadeMediaPercentual = new BigDecimal("100.00");
			mtbfMedioSegundos = 0.0;
			mttrMedioSegundos = 0.0;
		} else {
			disponibilidadeMediaPercentual = somaDisponibilidade.divide(
					BigDecimal.valueOf(totalDispositivos),
					2,
					RoundingMode.HALF_UP
			);
			mtbfMedioSegundos = BigDecimal.valueOf(somaMtbf / totalDispositivos)
					.setScale(2, RoundingMode.HALF_UP)
					.doubleValue();
			mttrMedioSegundos = BigDecimal.valueOf(somaMttr / totalDispositivos)
					.setScale(2, RoundingMode.HALF_UP)
					.doubleValue();
		}

		StatusSaudeCluster statusSaude = definirStatusSaudeCluster(
				totalDispositivos,
				offline,
				degradados,
				incidentesAtivos,
				incidentesCriticosAtivos
		);

		return new DashboardClusterResponse(
				cluster.getId(),
				cluster.getNome(),
				cluster.getAmbiente(),
				totalDispositivos,
				online,
				degradados,
				offline,
				emAtualizacao,
				manutencao,
				incidentesAtivos,
				incidentesCriticosAtivos,
				temperaturaMediaClusterC,
				disponibilidadeMediaPercentual,
				mtbfMedioSegundos,
				mttrMedioSegundos,
				statusSaude
		);
	}

	@Transactional(readOnly = true)
	public List<DashboardClusterResponse> listarDashboardsClustersAtivos() {
		return clusterRepository.findAll()
				.stream()
				.filter(c -> Boolean.TRUE.equals(c.getAtivo()))
				.map(c -> obterDashboardCluster(c.getId()))
				.toList();
	}

	@Transactional(readOnly = true)
	public ResumoGlobalPlataformaResponse obterResumoGlobal() {
		List<Cluster> clusters = clusterRepository.findAll();
		long totalClusters = clusters.size();
		long clustersAtivos = clusters.stream()
				.filter(c -> Boolean.TRUE.equals(c.getAtivo()))
				.count();

		List<Dispositivo> dispositivos = dispositivoRepository.findAll();
		long totalDispositivos = dispositivos.size();
		long online = dispositivos.stream().filter(d -> d.getStatus() == StatusDispositivo.ONLINE).count();
		long degradados = dispositivos.stream().filter(d -> d.getStatus() == StatusDispositivo.DEGRADADO).count();
		long offline = dispositivos.stream().filter(d -> d.getStatus() == StatusDispositivo.OFFLINE).count();
		long emAtualizacao = dispositivos.stream().filter(d -> d.getStatus() == StatusDispositivo.EM_ATUALIZACAO).count();
		long manutencao = dispositivos.stream().filter(d -> d.getStatus() == StatusDispositivo.MANUTENCAO).count();

		long totalLeituras = leituraTelemetriaRepository.count();
		long totalAnomalias = leituraTelemetriaRepository.findByAnomaliaDetectadaTrueOrderByDataHoraLeituraDesc().size();

		long totalIncidentesAtivos = incidenteRepository.findAll()
				.stream()
				.filter(i -> STATUS_INCIDENTES_ATIVOS.contains(i.getStatus()))
				.count();

		List<ImplantacaoOta> implantacoes = implantacaoOtaRepository.findAll();
		long otaSucesso = implantacoes.stream()
				.filter(i -> i.getStatus() == StatusImplantacaoOta.SUCESSO)
				.count();
		long otaRollback = implantacoes.stream()
				.filter(i -> i.getStatus() == StatusImplantacaoOta.FALHA_ROLLBACK)
				.count();

		BigDecimal disponibilidadeMediaGlobal;
		if (totalDispositivos == 0) {
			disponibilidadeMediaGlobal = new BigDecimal("100.00");
		} else {
			BigDecimal somaDisp = BigDecimal.ZERO;
			for (Dispositivo disp : dispositivos) {
				MetricasConfiabilidadeResponse metricas = incidenteService.calcularMetricasConfiabilidade(disp.getId());
				somaDisp = somaDisp.add(metricas.disponibilidadePercentual());
			}
			disponibilidadeMediaGlobal = somaDisp.divide(
					BigDecimal.valueOf(totalDispositivos),
					2,
					RoundingMode.HALF_UP
			);
		}

		return new ResumoGlobalPlataformaResponse(
				totalClusters,
				clustersAtivos,
				totalDispositivos,
				online,
				degradados,
				offline,
				emAtualizacao,
				manutencao,
				totalLeituras,
				totalAnomalias,
				totalIncidentesAtivos,
				otaSucesso,
				otaRollback,
				disponibilidadeMediaGlobal
		);
	}

	private StatusSaudeCluster definirStatusSaudeCluster(
			long totalDispositivos,
			long dispositivosOffline,
			long dispositivosDegradados,
			long incidentesAtivos,
			long incidentesCriticosAtivos) {
		boolean maisDe30PctOffline = totalDispositivos > 0
				&& ((double) dispositivosOffline / totalDispositivos) > 0.30;

		if (maisDe30PctOffline || incidentesCriticosAtivos >= 1) {
			return StatusSaudeCluster.CRITICO;
		}
		if (dispositivosDegradados > 0 || incidentesAtivos > 0) {
			return StatusSaudeCluster.ATENCAO;
		}
		return StatusSaudeCluster.SAUDAVEL;
	}

}
