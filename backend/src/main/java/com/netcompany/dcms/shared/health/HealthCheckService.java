package com.netcompany.dcms.shared.health;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;

@Service
public class HealthCheckService {

    private static final Logger log = LoggerFactory.getLogger(HealthCheckService.class);

    private final DataSource dataSource;

    public HealthCheckService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public boolean isDatabaseReachable() {
        try (Connection conn = dataSource.getConnection()) {
            return conn.isValid(2);
        } catch (Exception e) {
            log.warn("Database health check failed", e);
            return false;
        }
    }
}
