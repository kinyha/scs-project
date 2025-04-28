package com.pharmacy.scs.notification.service;

import com.pharmacy.scs.notification.service.KafkaListenerService.UserDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Клиент для взаимодействия с User Service.
 * В реальном проекте лучше использовать Spring Cloud Feign или WebClient.
 */
@Service
@Slf4j
public class UserServiceClient {

    private final RestTemplate restTemplate;
    private final String userServiceUrl;

    public UserServiceClient(
            RestTemplate restTemplate,
            @Value("${services.user-service.url:http://localhost:8081}") String userServiceUrl) {
        this.restTemplate = restTemplate;
        this.userServiceUrl = userServiceUrl;
    }

    /**
     * Получает информацию о пользователе по ID
     *
     * @param userId ID пользователя
     * @return Информация о пользователе или null, если пользователь не найден
     */
    public UserDTO getUserById(Long userId) {
        try {
            String url = UriComponentsBuilder.fromHttpUrl(userServiceUrl)
                    .path("/api/users/{userId}")
                    .buildAndExpand(userId)
                    .toUriString();

            log.debug("Requesting user info from: {}", url);

            return restTemplate.getForObject(url, UserDTO.class);

        } catch (RestClientException e) {
            log.error("Failed to get user information for user ID: {}", userId, e);

            // Возвращаем временные данные для тестирования
            // В реальном проекте следует реализовать полноценную обработку ошибок
            if (e.getMessage() != null && e.getMessage().contains(HttpStatus.NOT_FOUND.toString())) {
                log.warn("User not found, using fallback data for testing");
                return createTestUser(userId);
            }

            return null;
        }
    }

    /**
     * Создает тестового пользователя (для разработки и тестирования)
     * В продакшн-версии этот метод следует удалить
     */
    private UserDTO createTestUser(Long userId) {
        UserDTO user = new UserDTO();
        user.setId(userId);
        user.setUsername("Test User");
        user.setEmail("test@example.com");
        user.setPhoneNumber("+11234567890");
        return user;
    }
}