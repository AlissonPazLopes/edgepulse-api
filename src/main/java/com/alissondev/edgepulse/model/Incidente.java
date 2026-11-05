package com.alissondev.edgepulse.model;

import com.alissondev.edgepulse.model.enums.SeveridadeIncidente;
import com.alissondev.edgepulse.model.enums.StatusIncidente;
import com.alissondev.edgepulse.model.enums.TipoAnomalia;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

import java.time.LocalDateTime;

@Entity
@Table(name = "incidentes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Incidente {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.EAGER, optional = false)
	@JoinColumn(name = "dispositivo_id", nullable = false)
	private Dispositivo dispositivo;

	@Enumerated(EnumType.STRING)
	@Column(name = "tipo_anomalia", length = 40, nullable = false)
	private TipoAnomalia tipoAnomalia;

	@Enumerated(EnumType.STRING)
	@Column(name = "severidade", length = 20, nullable = false)
	private SeveridadeIncidente severidade;

	@Column(name = "descricao", length = 255, nullable = false)
	private String descricao;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", length = 20, nullable = false)
	private StatusIncidente status;

	@Column(name = "data_abertura", nullable = false)
	private LocalDateTime dataAbertura;

	@Column(name = "data_resolucao")
	private LocalDateTime dataResolucao;

	@Column(name = "tempo_reparo_segundos")
	private Long tempoReparoSegundos;

	@PrePersist
	public void prePersist() {
		if (this.dataAbertura == null) {
			this.dataAbertura = LocalDateTime.now();
		}
		if (this.status == null) {
			this.status = StatusIncidente.ABERTO;
		}
	}

}
