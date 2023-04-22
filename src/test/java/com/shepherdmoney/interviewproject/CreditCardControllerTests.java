package com.shepherdmoney.interviewproject;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shepherdmoney.interviewproject.model.CreditCard;
import com.shepherdmoney.interviewproject.vo.request.AddCreditCardToUserPayload;
import com.shepherdmoney.interviewproject.vo.request.CreateUserPayload;
import com.shepherdmoney.interviewproject.vo.request.UpdateBalancePayload;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
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
        int userId = 1;

        AddCreditCardToUserPayload cardPayload = new AddCreditCardToUserPayload();
        cardPayload.setCardNumber("111");
        cardPayload.setCardIssuanceBank("BoK");
        cardPayload.setUserId(userId);

        mockMvc.perform(post("/credit-card")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cardPayload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isNumber());
    }

    @Test
    void shouldReturnNotFoundWhenCardUserNotExist() throws Exception{
        int userId = 10;

        AddCreditCardToUserPayload cardPayload = new AddCreditCardToUserPayload();
        cardPayload.setCardNumber("222");
        cardPayload.setCardIssuanceBank("BoK");
        cardPayload.setUserId(userId);

        mockMvc.perform(post("/credit-card")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cardPayload)))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturnBadRequestWhenCardRequestNull() throws Exception{
        int userId = 10;


        AddCreditCardToUserPayload cardPayload = new AddCreditCardToUserPayload();
        cardPayload.setCardNumber("333");
        cardPayload.setUserId(userId);

        mockMvc.perform(post("/credit-card")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cardPayload)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnEmptyListWhenCardUserNotFound() throws Exception {
        int userId = 10;
        mockMvc.perform(get("/credit-card:all")
                        .param("userId", String.valueOf(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void shouldReturnAListWhenGetCardByUser() throws Exception {
        int userId = 1;
        mockMvc.perform(get("/credit-card:all")
                        .param("userId", String.valueOf(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.*", hasSize(4)));
    }

    @Test
    void shouldReturnUserIdWhenCardUserExist() throws Exception {
        String creditCardNumber = "567";
        mockMvc.perform(get("/credit-card:user-id")
                .param("creditCardNumber", creditCardNumber))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(3));
    }

    @Test
    void shouldReturnUserIdWhenCardUserNotExist() throws Exception {
        String creditCardNumber = "777";
        mockMvc.perform(get("/credit-card:user-id")
                        .param("creditCardNumber", creditCardNumber))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnBadRequestWhenCardNotFound() throws Exception {
        UpdateBalancePayload[] payload = new UpdateBalancePayload[1];
        payload[0] = new UpdateBalancePayload();
        payload[0].setCreditCardNumber("999");
        LocalDate date = LocalDate.parse("2023-04-10");
        Instant instant = date.atStartOfDay(ZoneId.of("America/Los_Angeles")).toInstant();
        payload[0].setTransactionTime(instant);
        payload[0].setTransactionAmount(10.0);

        mockMvc.perform(post("/credit-card:update-balance")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnOKWhenInsertATransToCard() throws Exception {
        UpdateBalancePayload[] payload = new UpdateBalancePayload[1];
        payload[0] = new UpdateBalancePayload();
        payload[0].setCreditCardNumber("123");
        LocalDate date = LocalDate.parse("2022-04-10");
        Instant instant = date.atStartOfDay(ZoneId.of("America/Los_Angeles")).toInstant();
        payload[0].setTransactionTime(instant);
        payload[0].setTransactionAmount(10.0);

        mockMvc.perform(post("/credit-card:update-balance")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk());
    }

}
