# SCS Notification Service

Микросервис уведомлений для Shipment Confirmation System (SCS). Этот сервис отвечает за отправку уведомлений клиентам о статусах их заказов.

## Функциональность

- Получение событий о доставках из Kafka
- Отправка email-уведомлений
- Хранение истории отправленных уведомлений
- Повторные попытки для неудачных отправок
- API для мониторинга и управления уведомлениями

## Технологии

- Java 17
- Spring Boot 3.2
- Spring Kafka
- Spring Data JPA
- PostgreSQL
- Thymeleaf (шаблоны email)
- Spring Mail
- TestContainers
- JUnit 5 & Mockito

## Архитектура

Сервис построен на микросервисной архитектуре и взаимодействует с другими компонентами системы:

1. **Интеграция с Kafka**:
    - Подписка на топик `delivery-updates` для получения событий о доставке
    - Использование Dead Letter Queue (`notification-dlq`) для обработки сбойных сообщений

2. **Взаимодействие с User Service**:
    - REST API для получения информации о пользователях
    - Отказоустойчивое взаимодействие с таймаутами и повторными попытками

3. **Отправка уведомлений**:
    - Асинхронная отправка уведомлений
    - Шаблонизация с использованием Thymeleaf
    - Поддержка различных типов уведомлений (Email, SMS)

4. **Хранение данных**:
    - PostgreSQL для хранения информации о уведомлениях
    - JPA Repository для доступа к данным

## Установка и запуск

### Предварительные требования

- Java 17+
- Maven 3.6+
- PostgreSQL 14+
- Kafka (для локальной разработки можно использовать Docker)

### Настройка окружения

1. Клонируйте репозиторий:
   ```bash
   git clone https://github.com/company/scs-notification-service.git
   cd scs-notification-service
   ```

2. Настройте переменные окружения или application.yml:
   ```bash
   # Пример для Linux/Mac
   export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/scs_notification_db
   export SPRING_DATASOURCE_USERNAME=postgres
   export SPRING_DATASOURCE_PASSWORD=postgres
   export SPRING_KAFKA_BOOTSTRAP_SERVERS=localhost:9092
   export MAIL_USERNAME=your-email@gmail.com
   export MAIL_PASSWORD=your-app-password
   ```

3. Создайте базу данных:
   ```sql
   CREATE DATABASE scs_notification_db;
   ```

### Сборка и запуск

1. Сборка проекта:
   ```bash
   mvn clean package
   ```

2. Запуск:
   ```bash
   java -jar target/scs-notification-service-1.0-SNAPSHOT.jar
   ```

### Запуск с Docker

Вы также можете использовать Docker Compose для запуска всех необходимых компонентов:

```bash
docker-compose up -d
```

## API endpoints

- `GET /api/notifications/user/{userId}` - Получить уведомления пользователя
- `GET /api/notifications/delivery/{deliveryId}` - Получить уведомления для доставки
- `GET /api/notifications/stats` - Получить статистику по уведомлениям
- `GET /api/notifications/{id}` - Получить конкретное уведомление

## Интеграция с Kafka

Сервис слушает следующие топики:
- `delivery-updates` - События о изменениях статуса доставки
- `notification-dlq` - Dead Letter Queue для необработанных сообщений

## Мониторинг и отказоустойчивость

- Интеграция с Spring Actuator для метрик и мониторинга
- Механизм повторных попыток для неудачных отправок
- Асинхронная обработка уведомлений для повышения производительности
- Идемпотентная обработка сообщений из Kafka для гарантии at-least-once

## Тестирование

Проект включает:
- Модульные тесты с Mockito
- Интеграционные тесты с использованием EmbeddedKafka
- Тесты с использованием TestContainers для интеграционного тестирования с реальными сервисами

Запуск тестов:
```bash
mvn test
```

## Дополнительная информация

Для доступа к документации API и мониторингу:
- API Documentation: http://localhost:8082/swagger-ui.html
- Health and metrics: http://localhost:8082/actuator