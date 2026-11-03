package com.alissondev.edgepulse.model;

import com.alissondev.edgepulse.model.enums.StatusDispositivo;
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
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "dispositivos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Dispositivo {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.EAGER, optional = false)
	@JoinColumn(name = "cluster_id", nullable = false)
	private Cluster cluster;

	@Column(name = "nome", length = 100, nullable = false)
	private String nome;

	@Column(name = "mac_address", length = 17, unique = true, nullable = false)
	private String macAddress;

	@Column(name = "modelo_hardware", length = 50, nullable = false)
	private String modeloHardware;

	@Column(name = "versao_firmware_atual", length = 30, nullable = false)
	private String versaoFirmwareAtual;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", length = 30, nullable = false)
	private StatusDispositivo status;

	@Column(name = "periferico_display_ativo", nullable = false)
	private Boolean perifericoDisplayAtivo;

	@Column(name = "periferico_audio_ativo", nullable = false)
	private Boolean perifericoAudioAtivo;

	@Column(name = "ultimo_heartbeat")
	private LocalDateTime ultimoHeartbeat;

	@Column(name = "data_cadastro", nullable = false, updatable = false)
	private LocalDateTime dataCadastro;

	@Version
	@Column(name = "versao", nullable = false)
	private Long versao;

	@PrePersist
	public void prePersist() {
		if (this.dataCadastro == null) {
			this.dataCadastro = LocalDateTime.now();
		}
		if (this.status == null) {
			this.status = StatusDispositivo.OFFLINE;
		}
		if (this.perifericoDisplayAtivo == null) {
			this.perifericoDisplayAtivo = false;
		}
		if (this.perifericoAudioAtivo == null) {
			this.perifericoAudioAtivo = false;
		}
		if (this.versao == null) {
			this.versao = 0L;
		}
	}

}
