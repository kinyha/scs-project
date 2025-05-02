package com.pharmacy.scs.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class DatabaseOptimizer {

    private final JdbcTemplate jdbcTemplate;

    /**
     * Анализирует индексы и их использование
     */
    public List<Map<String, Object>> analyzeIndexUsage() {
        String sql = """
            SELECT
                relname AS table_name,
                indexrelname AS index_name,
                idx_scan AS index_scans,
                idx_tup_read AS tuples_read,
                idx_tup_fetch AS tuples_fetched
            FROM
                pg_stat_user_indexes
            JOIN
                pg_statio_user_indexes USING (indexrelid)
            ORDER BY
                idx_scan DESC, idx_tup_read DESC
        """;

        return jdbcTemplate.queryForList(sql);
    }

    /**
     * Создает рекомендуемые индексы на основе анализа выполненных запросов
     */
    @Transactional
    public void createRecommendedIndexes() {
        log.info("Создание рекомендуемых индексов");

        // Индекс для поиска по статусу доставки (часто используемый фильтр)
        createIndex("deliveries", "idx_deliveries_status", "status");

        // Индекс для поиска по пользователю (связь один-ко-многим)
        createIndex("deliveries", "idx_deliveries_user_id", "user_id");

        // Индекс для поиска по номеру отслеживания (уникальное значение)
        createIndex("deliveries", "idx_deliveries_tracking_number", "tracking_number");

        // Индекс для диапазона дат (для запросов за период)
        createIndex("deliveries", "idx_deliveries_expected_delivery_time", "expected_delivery_time");

        // Составной индекс для сложных запросов
        createIndex("deliveries", "idx_deliveries_status_dates",
                "status, expected_delivery_time, actual_delivery_time");

        // Индекс для email (поиск пользователя по email)
        createIndex("users", "idx_users_email", "email");

        log.info("Создание рекомендуемых индексов завершено");
    }

    /**
     * Создает индекс, если он еще не существует
     */
    private void createIndex(String tableName, String indexName, String columns) {
        try {
            // Проверяем, существует ли индекс
            String checkSql = """
                SELECT 1 FROM pg_indexes 
                WHERE tablename = ? AND indexname = ?
            """;

            List<Map<String, Object>> result = jdbcTemplate.queryForList(checkSql, tableName, indexName);

            if (result.isEmpty()) {
                // Индекс не существует, создаем его
                String createSql = String.format(
                        "CREATE INDEX %s ON %s (%s)",
                        indexName, tableName, columns
                );

                jdbcTemplate.execute(createSql);
                log.info("Создан индекс: {} на таблице {} для столбцов {}",
                        indexName, tableName, columns);
            } else {
                log.info("Индекс {} на таблице {} уже существует", indexName, tableName);
            }
        } catch (Exception e) {
            log.error("Ошибка при создании индекса {} на таблице {}: {}",
                    indexName, tableName, e.getMessage());
        }
    }

    /**
     * Анализирует таблицы для обновления статистики
     */
    public void analyzeTables() {
        log.info("Анализ таблиц...");

        jdbcTemplate.execute("ANALYZE users");
        jdbcTemplate.execute("ANALYZE deliveries");

        log.info("Анализ таблиц завершен");
    }

    /**
     * Получает статистику по таблицам
     */
    public List<Map<String, Object>> getTableStatistics() {
        String sql = """
            SELECT
                relname AS table_name,
                n_live_tup AS row_count,
                n_dead_tup AS dead_tuples,
                last_vacuum,
                last_analyze
            FROM
                pg_stat_user_tables
            ORDER BY
                n_live_tup DESC
        """;

        return jdbcTemplate.queryForList(sql);
    }
}
