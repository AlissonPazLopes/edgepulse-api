package com.alissondev.edgepulse.model;

import com.alissondev.edgepulse.model.enums.StatusImplantacaoOta;
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
@Table(name = "implantacoes_ota")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImplantacaoOta {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.EAGER, optional = false)
	@JoinColumn(name = "dispositivo_id", nullable = false)
	private Dispositivo dispositivo;

	@ManyToOne(fetch = FetchType.EAGER, optional = false)
	@JoinColumn(name = "firmware_destino_id", nullable = false)
	private ReleaseFirmware firmwareDestino;

	@Column(name = "versao_anterior", length = 30, nullable = false)
	private String versaoAnterior;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", length = 30, nullable = false)
	private StatusImplantacaoOta status;

	@Column(name = "mensagem_erro", length = 255)
	private String mensagemErro;

	@Column(name = "data_inicio", nullable = false, updatable = false)
	private LocalDateTime dataInicio;

	@Column(name = "data_conclusao")
	private LocalDateTime dataConclusao;

	@PrePersist
	public void prePersist() {
		if (this.dataInicio == null) {
			this.dataInicio = LocalDateTime.now();
		}
		if (this.status == null) {
			this.status = StatusImplantacaoOta.PENDENTE;
		}
	}

}
