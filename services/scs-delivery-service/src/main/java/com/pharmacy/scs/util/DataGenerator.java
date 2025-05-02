package com.pharmacy.scs.util;

import com.pharmacy.scs.entity.Delivery;
import com.pharmacy.scs.entity.DeliveryStatus;
import com.pharmacy.scs.entity.User;
import com.pharmacy.scs.repository.DeliveryRepository;
import com.pharmacy.scs.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataGenerator {

    private final UserRepository userRepository;
    private final DeliveryRepository deliveryRepository;
    private static final Random random = new Random();

    /**
     * Генерирует тестовые данные пользователей
     *
     * @param count количество пользователей
     * @return список созданных пользователей
     */
    @Transactional
    public List<User> generateUsers(int count) {
        log.info("Генерация {} пользователей", count);
        List<User> users = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            User user = new User();
            user.setUsername("user_" + UUID.randomUUID().toString().substring(0, 8));
            user.setEmail(user.getUsername() + "@example.com");
            user.setPassword("password" + i);
            user.setPhoneNumber("+7" + (9000000000L + random.nextInt(999999999)));
            user.setRole("USER");

            users.add(user);
        }

        return userRepository.saveAll(users);
    }

    /**
     * Генерирует тестовые данные доставок
     *
     * @param users список пользователей
     * @param deliveriesPerUser количество доставок на пользователя
     * @return список созданных доставок
     */
    @Transactional
    public List<Delivery> generateDeliveries(List<User> users, int deliveriesPerUser) {
        log.info("Генерация доставок для {} пользователей, {} доставок на пользователя",
                users.size(), deliveriesPerUser);

        List<Delivery> deliveries = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        for (User user : users) {
            for (int i = 0; i < deliveriesPerUser; i++) {
                DeliveryStatus status = getRandomStatus();

                Delivery delivery = new Delivery();
                delivery.setTrackingNumber("TN" + UUID.randomUUID().toString().substring(0, 10));
                delivery.setUser(user);
                delivery.setDeliveryAddress(generateRandomAddress());

                // Случайная дата ожидаемой доставки от текущего дня до +10 дней
                LocalDateTime expectedDelivery = now.plusDays(random.nextInt(10) + 1)
                        .plusHours(random.nextInt(24))
                        .plusMinutes(random.nextInt(60));
                delivery.setExpectedDeliveryTime(expectedDelivery);

                // Для завершенных доставок устанавливаем фактическое время доставки
                if (status == DeliveryStatus.COMPLETED || status == DeliveryStatus.DELIVERED) {
                    LocalDateTime actualDelivery = expectedDelivery
                            .plusHours(random.nextInt(5) - 2) // +/- 2 часа от ожидаемого времени
                            .plusMinutes(random.nextInt(60));

                    delivery.setActualDeliveryTime(actualDelivery);
                }

                delivery.setStatus(status);
                deliveries.add(delivery);
            }
        }

        return deliveryRepository.saveAll(deliveries);
    }

    /**
     * Генерирует случайный статус доставки с разной вероятностью
     */
    private DeliveryStatus getRandomStatus() {
        int rand = random.nextInt(100);

        if (rand < 20) return DeliveryStatus.CREATED;
        else if (rand < 45) return DeliveryStatus.PENDING;
        else if (rand < 70) return DeliveryStatus.IN_TRANSIT;
        else if (rand < 85) return DeliveryStatus.DELIVERED;
        else if (rand < 95) return DeliveryStatus.COMPLETED;
        else return DeliveryStatus.CANCELLED;
    }

    /**
     * Генерирует случайный адрес доставки
     */
    private String generateRandomAddress() {
        String[] streets = {"Ленина", "Пушкина", "Гагарина", "Мира", "Советская", "Октябрьская", "Строителей"};
        String[] cities = {"Москва", "Санкт-Петербург", "Казань", "Екатеринбург", "Новосибирск", "Краснодар"};

        String street = streets[random.nextInt(streets.length)];
        String city = cities[random.nextInt(cities.length)];
        int houseNumber = random.nextInt(100) + 1;
        int apartment = random.nextInt(200) + 1;

        return String.format("г. %s, ул. %s, д. %d, кв. %d", city, street, houseNumber, apartment);
    }

    /**
     * Генерирует полный набор тестовых данных
     *
     * @param userCount количество пользователей
     * @param deliveriesPerUser количество доставок на пользователя
     */
    @Transactional
    public void generateTestData(int userCount, int deliveriesPerUser) {
        log.info("Начало генерации тестовых данных: {} пользователей, {} доставок на пользователя",
                userCount, deliveriesPerUser);

        long startTime = System.currentTimeMillis();

        List<User> users = generateUsers(userCount);
        List<Delivery> deliveries = generateDeliveries(users, deliveriesPerUser);

        long endTime = System.currentTimeMillis();

        log.info("Генерация завершена: создано {} пользователей и {} доставок за {} мс",
                users.size(), deliveries.size(), (endTime - startTime));
    }
}
