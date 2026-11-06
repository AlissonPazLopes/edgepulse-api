package com.alissondev.edgepulse.repository;

import com.alissondev.edgepulse.model.ReleaseFirmware;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReleaseFirmwareRepository extends JpaRepository<ReleaseFirmware, Long> {

	boolean existsByVersao(String versao);

	Optional<ReleaseFirmware> findByVersao(String versao);

	List<ReleaseFirmware> findByModeloHardwareAlvoOrderByDataPublicacaoDesc(String modeloHardwareAlvo);

}
