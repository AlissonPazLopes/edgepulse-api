# ⚡ EdgePulse API — Plataforma de Telemetria IoT/ESP32, Tolerância a Falhas e Engenharia de Dependabilidade (MTBF/MTTR)

![Java 21](https://img.shields.io/badge/Java-21%20LTS-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring Boot 3.3](https://img.shields.io/badge/Spring_Boot-3.3.5-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white)
![PostgreSQL 16](https://img.shields.io/badge/PostgreSQL-16-316192?style=for-the-badge&logo=postgresql&logoColor=white)
![Redis 7](https://img.shields.io/badge/Redis-7-DC382D?style=for-the-badge&logo=redis&logoColor=white)
![Flyway](https://img.shields.io/badge/Flyway-Database_Migrations-CC0200?style=for-the-badge&logo=flyway&logoColor=white)
![OpenAPI 3](https://img.shields.io/badge/Swagger-OpenAPI_3.0-85EA2D?style=for-the-badge&logo=swagger&logoColor=black)
![Docker](https://img.shields.io/badge/Docker-Multi--Stage-2496ED?style=for-the-badge&logo=docker&logoColor=white)

---

## 📌 Visão Geral do Projeto

O **EdgePulse API** é uma plataforma backend de nível industrial projetada para **monitoramento de clusters de dispositivos embarcados (ESP32 / ESP32-S3 / ESP32-C3)**, ingestão contínua de telemetria física e diagnóstico de periféricos (displays OLED/TFT e codecs de áudio I2S), **tolerância a falhas com detecção automática de anomalias**, **Watchdog de Heartbeat** e cálculo rigoroso de métricas de **Engenharia de Confiabilidade e Dependabilidade (`MTBF`, `MTTR` e `Disponibilidade`)**, além de gerenciamento de atualizações de firmware **Over-The-Air (OTA)** com validação criptográfica **SHA-256** e **Rollback Automático**.

---

## 🏗️ Arquitetura do Sistema e Fluxo de Eventos

```mermaid
flowchart TD
    ESP32["Nós Embarcados (ESP32 / ESP32-S3)"] -->|POST /api/telemetria| TC["TelemetriaController"]
    TC --> TS["TelemetriaService (Motor de Limiares + Heartbeat)"]
    TS -->|Persiste Leitura| DB[("PostgreSQL 16 (Flyway V1..V4)")]
    TS -->|Invalida Cache| REDIS[("Redis 7 Cache")]
    TS -->|Anomalia Detectada| EVT["AnomaliaDetectadaEvent (Spring Events)"]
    EVT --> IS["IncidenteService (@EventListener)"]
    IS -->|Abre Incidente sem Duplicidade| DB

    WD["HeartbeatWatchdogScheduler (@Scheduled)"] -->|Verifica Timeout > 3x Intervalo| DB
    WD -->|Marca OFFLINE + Abre HEARTBEAT_TIMEOUT| IS

    OTA["FirmwareOtaService"] -->|Valida Hardware + SHA-256| DB
    OTA -->|Divergência de Hash / Erro Flash| RB["Rollback Automático + FALHA_OTA"]
    RB --> EVT
```

### Pilares Arquiteturais
- **Persistência Versionada com Flyway:** 4 migrations SQL (`V1` a `V4`) modelando `clusters`, `dispositivos`, `leituras_telemetria`, `incidentes`, `releases_firmware` e `implantacoes_ota`.
- **Controle de Concorrência Otimista (`@Version`):** Protege a entidade `Dispositivo` contra condições de corrida (*race conditions*) entre pulsos de telemetria simultâneos, varreduras do Watchdog e transições de atualização OTA, retornando `HTTP 409 Conflict` padronizado via `GlobalExceptionHandler`.
- **Cache Distribuído com Redis:** Consultas de topologia de clusters e dispositivos utilizam `@Cacheable`, com invalidação transacional (`@CacheEvict`) em mudanças de estado.
- **Arquitetura Orientada a Eventos Internos:** Desacoplamento entre a ingestão de pacotes de telemetria/OTA e a abertura de incidentes através do `ApplicationEventPublisher` (`AnomaliaDetectadaEvent`).

---

## 📐 Engenharia de Dependabilidade: Fórmulas de MTBF, MTTR e Disponibilidade

O motor matemático do `IncidenteService` calcula os indicadores clássicos de confiabilidade para cada dispositivo embarcado e consolida as médias por cluster no `DashboardService`:

### 1. Tempo Médio Para Reparo (*Mean Time To Repair* — $\text{MTTR}$)
Representa a média aritmética do tempo gasto para mitigar e resolver incidentes operacionais concluídos (`RESOLVIDO`):

$$\text{MTTR} = \frac{\sum_{i=1}^{N_{\text{resolvidos}}} (t_{\text{resolução}, i} - t_{\text{abertura}, i})}{N_{\text{resolvidos}}}$$

### 2. Tempo de Operação Efetiva (*Uptime*) e Tempo Médio Entre Falhas (*Mean Time Between Failures* — $\text{MTBF}$)
A partir do tempo total monitorado desde o cadastro do nó ($T_{\text{total}}$) subtraído do tempo total em falha/reparo ($T_{\text{downtime}}$):

$$\text{Uptime} = T_{\text{total}} - T_{\text{downtime}}$$

$$\text{MTBF} = \frac{\text{Uptime}}{N_{\text{total de incidentes}}}$$

*(Quando o dispositivo não registra falhas, $N_{\text{total}} = 0$, o $\text{MTBF}$ equivale ao próprio $\text{Uptime}$ e a disponibilidade é de $100.00\%$).*

### 3. Disponibilidade Inerente ($\text{Availability}$)
Expressa em percentual com arredondamento `HALF_UP` de 2 casas decimais:

$$\text{Disponibilidade (\%)} = \left( \frac{\text{MTBF}}{\text{MTBF} + \text{MTTR}} \right) \times 100$$

---

## 🚨 Motor de Detecção de Anomalias e Watchdog de Heartbeat

Durante cada ingestão em `POST /api/telemetria`, o `TelemetriaService` inspeciona os parâmetros vitais do ESP32 em tempo real:

| Tipo de Anomalia | Condição de Disparo | Severidade do Incidente | Ação Automática |
| :--- | :--- | :---: | :--- |
| `SUPERAQUECIMENTO` | `temperaturaCpuC > 80.0 °C` | `CRITICA` | Dispositivo `DEGRADADO` + Evento |
| `SUBTENSAO_ELETRICA` | `tensaoAlimentacaoV < 3.0 V` | `CRITICA` | Dispositivo `DEGRADADO` + Evento |
| `BATERIA_CRITICA` | `nivelBateriaPct < 10%` | `ALTA` | Dispositivo `DEGRADADO` + Evento |
| `FALHA_PERIFERICO_DISPLAY` | Display ativo no nó e `statusDisplayOk == false` | `ALTA` | Dispositivo `DEGRADADO` + Evento |
| `FALHA_PERIFERICO_AUDIO` | Áudio I2S ativo no nó e `statusAudioOk == false` | `ALTA` | Dispositivo `DEGRADADO` + Evento |
| `HEARTBEAT_TIMEOUT` | Silêncio $> 3 \times \text{intervaloHeartbeatSegundos}$ do cluster | `CRITICA` | Dispositivo `OFFLINE` + Incidente |
| `FALHA_OTA` | Erro de gravação flash ou divergência de hash SHA-256 | `ALTA` | Rollback de versão + `DEGRADADO` |

---

## 🔄 Gerenciador de Firmware OTA com Validação SHA-256 e Rollback Automático

1. **Publicação de Release (`POST /api/ota/releases`):** Exige versão única, modelo de hardware alvo (`ESP32-S3`, `ESP32-WROOM-32`, etc.) e assinatura hexadecimal **SHA-256 de 64 caracteres**.
2. **Início de Rollout (`POST /api/ota/implantacoes`):**
   - Bloqueia atualização caso o dispositivo esteja `OFFLINE`, em `MANUTENCAO` ou já possua outra implantação ativa.
   - Valida compatibilidade estrita de hardware (`modeloHardwareAlvo == dispositivo.modeloHardware`) e exige `estavel = true`.
   - Salva um snapshot da versão atual em `versaoAnterior` e transiciona o dispositivo para `EM_ATUALIZACAO`.
3. **Callback de Verificação e Rollback (`PATCH /api/ota/implantacoes/{id}/resultado`):**
   - **Sucesso + Checksum SHA-256 Idêntico:** Efetiva a nova versão de firmware no dispositivo, encerra a implantação como `SUCESSO` e restaura o status para `ONLINE`.
   - **Falha Física ou Hash SHA-256 Divergente:** Aciona o **Rollback Automático**, marca a implantação como `FALHA_ROLLBACK`, restaura imediatamente a `versaoAnterior` no registro do dispositivo, altera seu status para `DEGRADADO` e publica um `AnomaliaDetectadaEvent` (`FALHA_OTA`).
