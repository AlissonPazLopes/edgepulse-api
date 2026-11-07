package com.alissondev.edgepulse.exception;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

public record ErroPadraoResponse(
		LocalDateTime timestamp,
		Integer status,
		String erro,
		String mensagem,
		String path,
		List<CampoInvalido> camposInvalidos
) implements Serializable {

	public record CampoInvalido(
			String campo,
			String mensagem
	) implements Serializable {
	}

}
