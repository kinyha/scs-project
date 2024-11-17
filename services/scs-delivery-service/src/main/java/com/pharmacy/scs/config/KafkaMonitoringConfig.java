//package com.pharmacy.scs.config;
//
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.w3c.dom.css.Counter;
//
//@Configuration
//public class KafkaMonitoringConfig {
//
//    @Bean
//    public MeterRegistry meterRegistry() {
//        return new SimpleMeterRegistry();
//    }
//
//    @Bean
//    public Counter kafkaEventsSentCounter(MeterRegistry registry) {
//        return Counter.builder("kafka.events.sent")
//                .description("Number of events sent to Kafka")
//                .register(registry);
//    }
//}
//
//// И используем его в сервисе:
//@Service
//public class KafkaDeliveryEventService {
//    private final Counter kafkaEventsSentCounter;
//
//    public void sendDeliveryEvent(DeliveryEvent event) {
//        try {
//            // ... отправка события
//            kafkaEventsSentCounter.increment();
//        } catch (Exception e) {
//            // ... обработка ошибки
//        }
//    }
//}
