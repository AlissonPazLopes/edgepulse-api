package com.alissondev.edgepulse.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

	@Bean
	public OpenAPI edgePulseOpenAPI() {
		return new OpenAPI()
				.info(new Info()
						.title("EdgePulse API - Plataforma de Telemetria IoT/ESP32, Tolerância a Falhas e Dependabilidade")
						.description("API RESTful de engenharia de confiabilidade para monitoramento em tempo real de "
								+ "clusters de nós embarcados (ESP32), ingestão de telemetria de sensores e periféricos "
								+ "(display OLED/TFT e áudio I2S), detecção automática de anomalias, Watchdog de "
								+ "Heartbeat, cálculo de métricas de dependabilidade (MTBF, MTTR e Disponibilidade) e "
								+ "atualizações de firmware Over-The-Air (OTA) com verificação SHA-256 e Rollback Automático.")
						.version("1.0.0")
						.contact(new Contact()
								.name("Alisson Paz Lopes")
								.url("https://github.com/AlissonPazLopes/edgepulse-api"))
						.license(new License()
								.name("MIT License")
								.url("https://opensource.org/licenses/MIT")));
	}

}
