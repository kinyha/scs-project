//package com.pharmacy.scs.exception;
//
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.ErrorResponse;
//import org.springframework.web.bind.annotation.ControllerAdvice;
//import org.springframework.web.bind.annotation.ExceptionHandler;
//
//import static org.apache.kafka.common.requests.DeleteAclsResponse.log;
//
//@ControllerAdvice
//public class KafkaExceptionHandler {
//
//    @ExceptionHandler(KafkaEventPublishException.class)
//    public ResponseEntity<ErrorResponse> handleKafkaEventPublishException(
//            KafkaEventPublishException ex) {
//        log.error("Failed to publish Kafka event");
//
//        ErrorResponse error = new ErrorResponse(
//                "Failed to process delivery event",
//                ex.getMessage()
//        );
//
//        return ResponseEntity
//                .status(HttpStatus.INTERNAL_SERVER_ERROR)
//                .body(error);
//    }
//}
