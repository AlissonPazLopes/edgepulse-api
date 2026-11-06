CREATE TABLE releases_firmware (
    id BIGSERIAL PRIMARY KEY,
    versao VARCHAR(30) UNIQUE NOT NULL,
    modelo_hardware_alvo VARCHAR(50) NOT NULL,
    checksum_sha256 VARCHAR(64) NOT NULL,
    tamanho_bytes BIGINT NOT NULL,
    notas_lancamento VARCHAR(255),
    estavel BOOLEAN NOT NULL DEFAULT TRUE,
    data_publicacao TIMESTAMP NOT NULL
);

CREATE TABLE implantacoes_ota (
    id BIGSERIAL PRIMARY KEY,
    dispositivo_id BIGINT NOT NULL REFERENCES dispositivos(id) ON DELETE CASCADE,
    firmware_destino_id BIGINT NOT NULL REFERENCES releases_firmware(id) ON DELETE CASCADE,
    versao_anterior VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL,
    mensagem_erro VARCHAR(255),
    data_inicio TIMESTAMP NOT NULL,
    data_conclusao TIMESTAMP
);
