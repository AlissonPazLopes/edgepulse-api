package com.alissondev.edgepulse.repository;

import com.alissondev.edgepulse.model.Incidente;
import com.alissondev.edgepulse.model.enums.StatusIncidente;
import com.alissondev.edgepulse.model.enums.TipoAnomalia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface IncidenteRepository extends JpaRepository<Incidente, Long> {

	List<Incidente> findByDispositivoIdOrderByDataAberturaDesc(Long dispositivoId);

	List<Incidente> findByStatusOrderByDataAberturaDesc(StatusIncidente status);

	boolean existsByDispositivoIdAndTipoAnomaliaAndStatusIn(
			Long dispositivoId,
			TipoAnomalia tipoAnomalia,
			Collection<StatusIncidente> statuses
	);

	boolean existsByDispositivoIdAndStatusIn(
			Long dispositivoId,
			Collection<StatusIncidente> statuses
	);

	List<Incidente> findByDispositivoId(Long dispositivoId);

}
