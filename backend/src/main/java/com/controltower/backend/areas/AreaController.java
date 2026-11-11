package com.controltower.backend.areas;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.controltower.backend.areas.AreaService.AreaResponse;

@RestController
@RequestMapping("/api/v1/areas")
public class AreaController {

	private final AreaService areaService;

	AreaController(AreaService areaService) {
		this.areaService = areaService;
	}

	@GetMapping
	public List<AreaResponse> findActiveAreas() {
		return areaService.findActiveAreas();
	}
}
