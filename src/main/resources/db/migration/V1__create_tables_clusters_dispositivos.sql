CREATE TABLE clusters (
    id BIGSERIAL PRIMARY KEY,
    nome VARCHAR(100) UNIQUE NOT NULL,
    ambiente VARCHAR(30) NOT NULL,
    localizacao VARCHAR(120) NOT NULL,
    intervalo_heartbeat_segundos INTEGER NOT NULL DEFAULT 30,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    data_criacao TIMESTAMP NOT NULL
);

CREATE TABLE dispositivos (
    id BIGSERIAL PRIMARY KEY,
    cluster_id BIGINT NOT NULL REFERENCES clusters(id),
    nome VARCHAR(100) NOT NULL,
    mac_address VARCHAR(17) UNIQUE NOT NULL,
    modelo_hardware VARCHAR(50) NOT NULL,
    versao_firmware_atual VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL,
    periferico_display_ativo BOOLEAN NOT NULL DEFAULT FALSE,
    periferico_audio_ativo BOOLEAN NOT NULL DEFAULT FALSE,
    ultimo_heartbeat TIMESTAMP,
    data_cadastro TIMESTAMP NOT NULL,
    versao BIGINT NOT NULL DEFAULT 0
);
