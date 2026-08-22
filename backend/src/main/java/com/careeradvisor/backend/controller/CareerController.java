package com.careeradvisor.backend.controller;

import com.careeradvisor.backend.dto.CareerDto;
import com.careeradvisor.backend.service.CareerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/careers")
public class CareerController {

    private final CareerService careerService;

    public CareerController(CareerService careerService) {
        this.careerService = careerService;
    }

    @GetMapping
    public ResponseEntity<List<CareerDto>> getAllCareers() {
        return ResponseEntity.ok(careerService.getAllCareers());
    }

    @GetMapping("/{idOrTitle}")
    public ResponseEntity<CareerDto> getCareer(@PathVariable String idOrTitle) {
        return careerService.getCareerByIdOrTitle(idOrTitle)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
