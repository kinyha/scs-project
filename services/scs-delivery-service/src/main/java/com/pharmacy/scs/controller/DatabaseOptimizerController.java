package com.pharmacy.scs.controller;

import com.pharmacy.scs.util.DatabaseOptimizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/db")
@RequiredArgsConstructor
@Slf4j
public class DatabaseOptimizerController {

    private final DatabaseOptimizer databaseOptimizer;

    /**
     * Анализирует индексы и их использование
     */
    @GetMapping("/indexes")
    public ResponseEntity<List<Map<String, Object>>> getIndexUsage() {
        log.info("Запрос на анализ использования индексов");
        return ResponseEntity.ok(databaseOptimizer.analyzeIndexUsage());
    }

    /**
     * Создает рекомендуемые индексы
     */
    @PostMapping("/optimize")
    public ResponseEntity<Map<String, String>> createRecommendedIndexes() {
        log.info("Запрос на создание рекомендуемых индексов");

        databaseOptimizer.createRecommendedIndexes();
        databaseOptimizer.analyzeTables();

        return ResponseEntity.ok(Map.of("status", "Оптимизация выполнена успешно"));
    }

    /**
     * Получает статистику по таблицам
     */
    @GetMapping("/statistics")
    public ResponseEntity<List<Map<String, Object>>> getTableStatistics() {
        log.info("Запрос на получение статистики по таблицам");
        return ResponseEntity.ok(databaseOptimizer.getTableStatistics());
    }

    /**
     * Запускает ANALYZE для таблиц
     */
    @PostMapping("/analyze")
    public ResponseEntity<Map<String, String>> analyzeTables() {
        log.info("Запрос на ANALYZE таблиц");

        databaseOptimizer.analyzeTables();

        return ResponseEntity.ok(Map.of("status", "ANALYZE выполнен успешно"));
    }
}
