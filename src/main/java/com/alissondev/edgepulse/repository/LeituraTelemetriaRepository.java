package com.alissondev.edgepulse.repository;

import com.alissondev.edgepulse.model.LeituraTelemetria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LeituraTelemetriaRepository extends JpaRepository<LeituraTelemetria, Long> {

	List<LeituraTelemetria> findByDispositivoIdOrderByDataHoraLeituraDesc(Long dispositivoId);

	Optional<LeituraTelemetria> findFirstByDispositivoIdOrderByDataHoraLeituraDesc(Long dispositivoId);

	List<LeituraTelemetria> findByAnomaliaDetectadaTrueOrderByDataHoraLeituraDesc();

}
