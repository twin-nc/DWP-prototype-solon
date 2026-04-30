package com.netcompany.dcms.shared.health;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/health")
public class HealthController {

    private final HealthCheckService healthCheckService;

    public HealthController(HealthCheckService healthCheckService) {
        this.healthCheckService = healthCheckService;
    }

    @GetMapping("/live")
    public ResponseEntity<Map<String, String>> live() {
        return ResponseEntity.ok(Map.of("status", "UP"));
    }

    @GetMapping("/ready")
    public ResponseEntity<Map<String, String>> ready() {
        boolean dbReachable = healthCheckService.isDatabaseReachable();
        if (dbReachable) {
            return ResponseEntity.ok(Map.of("status", "UP", "db", "UP"));
        }
        return ResponseEntity.status(503).body(Map.of("status", "DOWN", "db", "DOWN"));
    }
}
