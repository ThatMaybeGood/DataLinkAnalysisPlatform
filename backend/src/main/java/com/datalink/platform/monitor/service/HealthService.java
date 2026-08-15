package com.datalink.platform.monitor.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 健康探活：返回应用与数据库状态，数据库异常时降级为 DEGRADED，不抛异常。
 */
@Service
public class HealthService {

    private final JdbcTemplate jdbcTemplate;

    public HealthService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Map<String, Object> status() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("status", "UP");
        map.put("app", "datalink-backend");
        map.put("version", "0.1.0");
        map.put("time", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

        try {
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            map.put("db", "UP");
        } catch (Exception e) {
            map.put("db", "DOWN");
            map.put("status", "DEGRADED");
        }
        return map;
    }
}
