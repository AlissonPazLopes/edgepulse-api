CREATE TABLE incidentes (
    id BIGSERIAL PRIMARY KEY,
    dispositivo_id BIGINT NOT NULL REFERENCES dispositivos(id) ON DELETE CASCADE,
    tipo_anomalia VARCHAR(40) NOT NULL,
    severidade VARCHAR(20) NOT NULL,
    descricao VARCHAR(255) NOT NULL,
    status VARCHAR(20) NOT NULL,
    data_abertura TIMESTAMP NOT NULL,
    data_resolucao TIMESTAMP,
    tempo_reparo_segundos BIGINT
);
