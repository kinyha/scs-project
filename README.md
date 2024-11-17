# Shipment Confirmation System (SCS)

Система подтверждения отгрузки для сети аптек.

## Структура проекта

- `services/` - Микросервисы проекта
  - `scs-delivery-service/` - Основной сервис доставки
  - `scs-notification-service/` - Сервис уведомлений
  - `scs-auth-service/` - Сервис аутентификации
- `infrastructure/` - Конфигурация инфраструктуры
- `docs/` - Документация проекта

## Требования

- Java 17
- Maven 3.8+
- Docker
- Kubernetes
- PostgreSQL 14+
- Kafka 3+
