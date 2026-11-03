package com.alissondev.edgepulse.repository;

import com.alissondev.edgepulse.model.Cluster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ClusterRepository extends JpaRepository<Cluster, Long> {

	boolean existsByNome(String nome);

	Optional<Cluster> findByNome(String nome);

}
