package com.nexbuy.modules.admin.forecast;

import com.nexbuy.modules.admin.dto.AdminForecastDto;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/forecast")
@PreAuthorize("hasRole('ADMIN')")
public class AdminForecastController {

    private final AdminForecastService adminForecastService;

    public AdminForecastController(AdminForecastService adminForecastService) {
        this.adminForecastService = adminForecastService;
    }

    @GetMapping
    public ResponseEntity<AdminForecastDto> getForecast() {
        return ResponseEntity.ok(adminForecastService.getForecast());
    }
}