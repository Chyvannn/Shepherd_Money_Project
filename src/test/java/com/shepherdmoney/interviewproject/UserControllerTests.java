package com.shepherdmoney.interviewproject;

import com.fasterxml.jackson.databind.ObjectMapper;
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
public class UserControllerTests {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldReturnOKWhenAUserIsCreated() throws Exception {
        CreateUserPayload payload = new CreateUserPayload();
        payload.setName("ivan");
        payload.setEmail("ivan@example.com");

        mockMvc.perform(put("/user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isNumber());
    }

    @Test
    void shouldReturnOKWhenExistUserIsDeleted() throws Exception {
        CreateUserPayload payload = new CreateUserPayload();
        payload.setName("ivan");
        payload.setEmail("ivan@example.com");

        MvcResult response = mockMvc.perform(put("/user")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
                .andReturn();

        int userId = Integer.parseInt(response.getResponse().getContentAsString());

        mockMvc.perform(delete("/user")
                        .param("userId", String.valueOf(userId)))
                .andExpect(status().isOk());
    }

    @Test
    void shouldReturnBadRequestWhenNotExistUserIsDelete() throws Exception {
        int userId = 10;

        mockMvc.perform(delete("/user")
                        .param("userId", String.valueOf(userId)))
                .andExpect(status().isBadRequest());
    }
}
