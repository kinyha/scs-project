package com.pharmacy.scs.controller;

import com.pharmacy.scs.dto.PerformanceResultDTO;
import com.pharmacy.scs.util.DataGenerator;
import com.pharmacy.scs.util.PerformanceAnalyzer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/performance")
@RequiredArgsConstructor
@Slf4j
public class PerformanceController {

    private final DataGenerator dataGenerator;
    private final PerformanceAnalyzer performanceAnalyzer;

    /**
     * Генерирует тестовые данные
     */
    @PostMapping("/generate")
    public ResponseEntity<Map<String, Object>> generateTestData(
            @RequestParam(defaultValue = "100") int userCount,
            @RequestParam(defaultValue = "10") int deliveriesPerUser) {

        log.info("Запрос на генерацию данных: {} пользователей, {} доставок на пользователя",
                userCount, deliveriesPerUser);

        long startTime = System.currentTimeMillis();
        dataGenerator.generateTestData(userCount, deliveriesPerUser);
        long endTime = System.currentTimeMillis();

        Map<String, Object> result = Map.of(
                "userCount", userCount,
                "deliveriesPerUser", deliveriesPerUser,
                "totalDeliveries", userCount * deliveriesPerUser,
                "executionTimeMs", (endTime - startTime)
        );

        return ResponseEntity.ok(result);
    }

    /**
     * Анализирует производительность запросов к базе данных
     */
    @GetMapping("/analyze")
    public ResponseEntity<List<PerformanceResultDTO>> analyzePerformance() {
        log.info("Запрос на анализ производительности");
        return ResponseEntity.ok(performanceAnalyzer.runPerformanceTests());
    }

    /**
     * Анализирует конкретный запрос с использованием EXPLAIN ANALYZE
     */
    @GetMapping("/explain/{queryType}")
    public ResponseEntity<String> explainQuery(@PathVariable String queryType) {
        log.info("Запрос на EXPLAIN ANALYZE для запроса: {}", queryType);
        return ResponseEntity.ok(performanceAnalyzer.explainQuery(queryType));
    }
}
