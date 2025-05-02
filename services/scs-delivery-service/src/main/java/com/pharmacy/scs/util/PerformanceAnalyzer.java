package com.pharmacy.scs.util;

import com.pharmacy.scs.dto.PerformanceResultDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class PerformanceAnalyzer {

    private final JdbcTemplate jdbcTemplate;

    // Карта тестовых запросов
    private final Map<String, String> testQueries = Map.of(
            "all_deliveries", "SELECT * FROM deliveries",
            "user_deliveries", "SELECT * FROM deliveries WHERE user_id = 1",
            "pending_deliveries", "SELECT * FROM deliveries WHERE status = 'PENDING'",
            "date_range", "SELECT * FROM deliveries WHERE expected_delivery_time BETWEEN NOW() - INTERVAL '7 days' AND NOW() + INTERVAL '7 days'",
            "status_count", "SELECT status, COUNT(*) FROM deliveries GROUP BY status",
            "user_stats", "SELECT u.id, u.username, COUNT(d.id) as delivery_count FROM users u JOIN deliveries d ON u.id = d.user_id GROUP BY u.id, u.username",
            "complex_query", "SELECT u.username, d.tracking_number, d.status, d.expected_delivery_time, d.actual_delivery_time " +
                    "FROM users u JOIN deliveries d ON u.id = d.user_id " +
                    "WHERE d.status IN ('DELIVERED', 'COMPLETED') " +
                    "AND d.actual_delivery_time > d.expected_delivery_time"
    );

    /**
     * Запускает все тесты производительности и возвращает результаты
     */
    public List<PerformanceResultDTO> runPerformanceTests() {
        List<PerformanceResultDTO> results = new ArrayList<>();

        for (Map.Entry<String, String> entry : testQueries.entrySet()) {
            String queryName = entry.getKey();
            String querySql = entry.getValue();

            log.info("Выполнение теста для запроса: {}", queryName);

            try {
                long startTime = System.currentTimeMillis();

                // Выполняем запрос и получаем количество строк
                int rowCount = jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM (" + querySql + ") AS temp", Integer.class);

                long endTime = System.currentTimeMillis();
                long executionTime = endTime - startTime;

                PerformanceResultDTO result = new PerformanceResultDTO();
                result.setQueryName(queryName);
                result.setQuerySql(querySql);
                result.setRowCount(rowCount);
                result.setExecutionTimeMs(executionTime);

                // Получаем план запроса
                String explainPlan = explainQuery(queryName);
                result.setExplainPlan(explainPlan);

                results.add(result);
                log.info("Запрос '{}' выполнен за {} мс, получено {} строк",
                        queryName, executionTime, rowCount);

            } catch (Exception e) {
                log.error("Ошибка при выполнении теста для запроса: {}", queryName, e);

                PerformanceResultDTO result = new PerformanceResultDTO();
                result.setQueryName(queryName);
                result.setQuerySql(querySql);
                result.setError("Ошибка: " + e.getMessage());

                results.add(result);
            }
        }

        return results;
    }

    /**
     * Выполняет EXPLAIN ANALYZE для указанного запроса
     */
    public String explainQuery(String queryType) {
        if (!testQueries.containsKey(queryType)) {
            return "Запрос с типом '" + queryType + "' не найден";
        }

        String querySql = testQueries.get(queryType);
        String explainSql = "EXPLAIN (ANALYZE, BUFFERS, FORMAT TEXT) " + querySql;

        try {
            List<Map<String, Object>> explainResults = jdbcTemplate.queryForList(explainSql);

            StringBuilder sb = new StringBuilder();
            for (Map<String, Object> row : explainResults) {
                for (Object value : row.values()) {
                    sb.append(value).append("\n");
                }
            }

            return sb.toString();

        } catch (Exception e) {
            log.error("Ошибка при выполнении EXPLAIN ANALYZE для запроса: {}", queryType, e);
            return "Ошибка: " + e.getMessage();
        }
    }
}
