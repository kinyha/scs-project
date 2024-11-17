package com.pharmacy.scs.delivery;



import com.fasterxml.jackson.databind.ObjectMapper;
import com.pharmacy.scs.dto.DeliveryDTO;
import com.pharmacy.scs.dto.UserDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class DeliveryIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void createDeliveryTest() throws Exception {
        // First create a user
        UserDTO userDTO = new UserDTO();
        userDTO.setEmail("test@example.com");
        userDTO.setPhoneNumber("1234567890");

        ResultActions userResult = mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userDTO)))
                .andExpect(status().isCreated());

        UserDTO createdUser = objectMapper.readValue(
                userResult.andReturn().getResponse().getContentAsString(),
                UserDTO.class
        );

        // Then create a delivery
        DeliveryDTO deliveryDTO = new DeliveryDTO();
        deliveryDTO.setTrackingNumber("TRACK123");
        deliveryDTO.setUserId(createdUser.getId());
        deliveryDTO.setStatus("PENDING");

        mockMvc.perform(post("/api/v1/deliveries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(deliveryDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.trackingNumber").value("TRACK123"))
                .andExpect(jsonPath("$.userId").value(createdUser.getId()))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }
}
