package com.alissondev.edgepulse.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "leituras_telemetria")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeituraTelemetria {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.EAGER, optional = false)
	@JoinColumn(name = "dispositivo_id", nullable = false)
	private Dispositivo dispositivo;

	@Column(name = "temperatura_cpu_c", precision = 5, scale = 2, nullable = false)
	private BigDecimal temperaturaCpuC;

	@Column(name = "tensao_alimentacao_v", precision = 5, scale = 2, nullable = false)
	private BigDecimal tensaoAlimentacaoV;

	@Column(name = "nivel_bateria_pct", nullable = false)
	private Integer nivelBateriaPct;

	@Column(name = "sinal_wifi_rssi_dbm", nullable = false)
	private Integer sinalWifiRssiDbm;

	@Column(name = "memoria_heap_livre_bytes", nullable = false)
	private Long memoriaHeapLivreBytes;

	@Column(name = "status_display_ok", nullable = false)
	private Boolean statusDisplayOk;

	@Column(name = "status_audio_ok", nullable = false)
	private Boolean statusAudioOk;

	@Column(name = "anomalia_detectada", nullable = false)
	private Boolean anomaliaDetectada;

	@Column(name = "detalhes_anomalia", length = 255)
	private String detalhesAnomalia;

	@Column(name = "data_hora_leitura", nullable = false, updatable = false)
	private LocalDateTime dataHoraLeitura;

	@PrePersist
	public void prePersist() {
		if (this.dataHoraLeitura == null) {
			this.dataHoraLeitura = LocalDateTime.now();
		}
		if (this.statusDisplayOk == null) {
			this.statusDisplayOk = true;
		}
		if (this.statusAudioOk == null) {
			this.statusAudioOk = true;
		}
		if (this.anomaliaDetectada == null) {
			this.anomaliaDetectada = false;
		}
	}

}
