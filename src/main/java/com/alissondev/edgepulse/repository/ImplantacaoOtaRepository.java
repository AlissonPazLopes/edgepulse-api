package com.alissondev.edgepulse.repository;

import com.alissondev.edgepulse.model.ImplantacaoOta;
import com.alissondev.edgepulse.model.enums.StatusImplantacaoOta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface ImplantacaoOtaRepository extends JpaRepository<ImplantacaoOta, Long> {

	List<ImplantacaoOta> findByDispositivoIdOrderByDataInicioDesc(Long dispositivoId);

	boolean existsByDispositivoIdAndStatusIn(
			Long dispositivoId,
			Collection<StatusImplantacaoOta> statuses
	);

}
