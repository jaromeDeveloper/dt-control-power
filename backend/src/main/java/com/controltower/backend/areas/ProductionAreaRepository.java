package com.controltower.backend.areas;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

interface ProductionAreaRepository extends JpaRepository<ProductionArea, String> {

	List<ProductionArea> findByActiveTrueOrderByCodeAsc();
}
