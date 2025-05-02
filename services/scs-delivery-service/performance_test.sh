#!/bin/bash
# performance_test.sh

echo "=== Тестирование производительности SCS Delivery Service ==="
echo

# Генерация данных
echo "1. Генерация тестовых данных (1000 пользователей, 10 доставок на пользователя)"
curl -X POST "http://localhost:8081/api/admin/performance/generate?userCount=1000&deliveriesPerUser=10"
echo -e "\n"

# Анализ производительности
echo "2. Анализ производительности запросов"
curl -s "http://localhost:8081/api/admin/performance/analyze" | jq
echo -e "\n"

# EXPLAIN ANALYZE для самого медленного запроса
echo "3. EXPLAIN ANALYZE для сложного запроса"
curl -s "http://localhost:8081/api/admin/performance/explain/complex_query"
echo -e "\n"

# Создание рекомендуемых индексов
echo "4. Создание рекомендуемых индексов"
curl -X POST "http://localhost:8081/api/admin/db/optimize"
echo -e "\n"

# Повторный анализ после оптимизации
echo "5. Анализ производительности после оптимизации"
curl -s "http://localhost:8081/api/admin/performance/analyze" | jq
echo -e "\n"

echo "=== Тест завершен ==="