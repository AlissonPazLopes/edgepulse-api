package com.alissondev.edgepulse.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "releases_firmware")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReleaseFirmware {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "versao", length = 30, unique = true, nullable = false)
	private String versao;

	@Column(name = "modelo_hardware_alvo", length = 50, nullable = false)
	private String modeloHardwareAlvo;

	@Column(name = "checksum_sha256", length = 64, nullable = false)
	private String checksumSha256;

	@Column(name = "tamanho_bytes", nullable = false)
	private Long tamanhoBytes;

	@Column(name = "notas_lancamento", length = 255)
	private String notasLancamento;

	@Column(name = "estavel", nullable = false)
	private Boolean estavel;

	@Column(name = "data_publicacao", nullable = false, updatable = false)
	private LocalDateTime dataPublicacao;

	@PrePersist
	public void prePersist() {
		if (this.dataPublicacao == null) {
			this.dataPublicacao = LocalDateTime.now();
		}
		if (this.estavel == null) {
			this.estavel = true;
		}
	}

}
