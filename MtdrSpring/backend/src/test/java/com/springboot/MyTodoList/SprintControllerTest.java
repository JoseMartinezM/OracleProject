package com.springboot.MyTodoList;

import com.springboot.MyTodoList.controller.SprintController;
import com.springboot.MyTodoList.model.Sprint;
import com.springboot.MyTodoList.service.SprintService;
import com.springboot.MyTodoList.service.ToDoItemService;
import com.springboot.MyTodoList.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SprintController.class)
public class SprintControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SprintService sprintService;
    
    @MockBean
    private ToDoItemService toDoItemService;
    
    @MockBean
    private UserService userService; // Añadido el mock de UserService

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void testCreateSprint() throws Exception {
        // Datos de entrada
        Sprint inputSprint = new Sprint();
        inputSprint.setName("Nuevo Sprint");
        inputSprint.setStartDate(LocalDate.parse("2025-05-01"));
        inputSprint.setEndDate(LocalDate.parse("2025-05-15"));
        inputSprint.setStatus("PLANNED");
        inputSprint.setCreatedBy(1);

        // Respuesta esperada del servicio
        Sprint savedSprint = new Sprint();
        savedSprint.setId(5);
        savedSprint.setName("Nuevo Sprint");
        savedSprint.setStartDate(LocalDate.parse("2025-05-01"));
        savedSprint.setEndDate(LocalDate.parse("2025-05-15"));
        savedSprint.setStatus("PLANNED");
        savedSprint.setCreatedBy(1);
        savedSprint.setCreationTs(OffsetDateTime.of(2025, 4, 7, 10, 0, 0, 0, ZoneOffset.UTC));

        when(sprintService.createSprint(any(Sprint.class))).thenReturn(savedSprint);

        mockMvc.perform(post("/api/sprints")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(inputSprint)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.name").value("Nuevo Sprint"))
                .andExpect(jsonPath("$.startDate").value("2025-05-01"))
                .andExpect(jsonPath("$.endDate").value("2025-05-15"))
                .andExpect(jsonPath("$.status").value("PLANNED"))
                .andExpect(jsonPath("$.createdBy").value(1))
                .andExpect(jsonPath("$.creationTs").value("2025-04-07T10:00:00Z"));
    }
}