package com.alissondev.edgepulse.model;

import com.alissondev.edgepulse.model.enums.AmbienteCluster;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "clusters")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cluster {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "nome", length = 100, unique = true, nullable = false)
	private String nome;

	@Enumerated(EnumType.STRING)
	@Column(name = "ambiente", length = 30, nullable = false)
	private AmbienteCluster ambiente;

	@Column(name = "localizacao", length = 120, nullable = false)
	private String localizacao;

	@Column(name = "intervalo_heartbeat_segundos", nullable = false)
	private Integer intervaloHeartbeatSegundos;

	@Column(name = "ativo", nullable = false)
	private Boolean ativo;

	@Column(name = "data_criacao", nullable = false, updatable = false)
	private LocalDateTime dataCriacao;

	@PrePersist
	public void prePersist() {
		if (this.dataCriacao == null) {
			this.dataCriacao = LocalDateTime.now();
		}
		if (this.ativo == null) {
			this.ativo = true;
		}
		if (this.intervaloHeartbeatSegundos == null) {
			this.intervaloHeartbeatSegundos = 30;
		}
	}

}
