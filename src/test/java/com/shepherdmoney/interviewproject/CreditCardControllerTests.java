package com.shepherdmoney.interviewproject;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shepherdmoney.interviewproject.vo.request.AddCreditCardToUserPayload;
import com.shepherdmoney.interviewproject.vo.request.CreateUserPayload;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class CreditCardControllerTests {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldReturnOKWhenACardIsCreatedWithExistingUser() throws Exception{
        CreateUserPayload userPayload = new CreateUserPayload();
        userPayload.setName("ivan");
        userPayload.setEmail("ivan@example.com");

        MvcResult response = mockMvc.perform(put("/user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userPayload)))
                .andReturn();

        int userId = Integer.parseInt(response.getResponse().getContentAsString());

        AddCreditCardToUserPayload cardPayload = new AddCreditCardToUserPayload();
        cardPayload.setCardNumber("325119886481");
        cardPayload.setCardIssuanceBank("BoK");
        cardPayload.setUserId(userId);

        mockMvc.perform(post("/credit-card")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cardPayload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isNumber());
    }

    @Test
    void shouldReturnNotFoundIfCardUserNotExist() throws Exception{
        CreateUserPayload userPayload = new CreateUserPayload();
        userPayload.setName("ivan");
        userPayload.setEmail("ivan@example.com");

        MvcResult response = mockMvc.perform(put("/user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userPayload)))
                .andReturn();

        int userId = Integer.parseInt(response.getResponse().getContentAsString());


        AddCreditCardToUserPayload cardPayload = new AddCreditCardToUserPayload();
        cardPayload.setCardNumber("325119886481");
        cardPayload.setCardIssuanceBank("BoK");
        cardPayload.setUserId(userId + 1);

        mockMvc.perform(post("/credit-card")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cardPayload)))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturnBadRequestIfCardRequestNull() throws Exception{
        CreateUserPayload userPayload = new CreateUserPayload();
        userPayload.setEmail("ivan@example.com");

        MvcResult response = mockMvc.perform(put("/user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userPayload)))
                .andReturn();

        int userId = Integer.parseInt(response.getResponse().getContentAsString());


        AddCreditCardToUserPayload cardPayload = new AddCreditCardToUserPayload();
        cardPayload.setCardNumber("325119886481");
        cardPayload.setUserId(userId + 1);

        mockMvc.perform(post("/credit-card")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cardPayload)))
                .andExpect(status().isBadRequest());
    }
}
