package com.alissondev.edgepulse.repository;

import com.alissondev.edgepulse.model.Dispositivo;
import com.alissondev.edgepulse.model.enums.StatusDispositivo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DispositivoRepository extends JpaRepository<Dispositivo, Long> {

	boolean existsByMacAddress(String macAddress);

	Optional<Dispositivo> findByMacAddress(String macAddress);

	List<Dispositivo> findByClusterId(Long clusterId);

	List<Dispositivo> findByStatus(StatusDispositivo status);

}
