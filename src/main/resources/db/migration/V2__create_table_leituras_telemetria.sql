CREATE TABLE leituras_telemetria (
    id BIGSERIAL PRIMARY KEY,
    dispositivo_id BIGINT NOT NULL REFERENCES dispositivos(id) ON DELETE CASCADE,
    temperatura_cpu_c NUMERIC(5,2) NOT NULL,
    tensao_alimentacao_v NUMERIC(5,2) NOT NULL,
    nivel_bateria_pct INTEGER NOT NULL,
    sinal_wifi_rssi_dbm INTEGER NOT NULL,
    memoria_heap_livre_bytes BIGINT NOT NULL,
    status_display_ok BOOLEAN NOT NULL DEFAULT TRUE,
    status_audio_ok BOOLEAN NOT NULL DEFAULT TRUE,
    anomalia_detectada BOOLEAN NOT NULL DEFAULT FALSE,
    detalhes_anomalia VARCHAR(255),
    data_hora_leitura TIMESTAMP NOT NULL
);
