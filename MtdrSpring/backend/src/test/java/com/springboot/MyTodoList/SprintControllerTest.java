package com.springboot.MyTodoList;

import com.springboot.MyTodoList.controller.SprintController;
import com.springboot.MyTodoList.controller.KpiTelegramController;
import com.springboot.MyTodoList.controller.UserBotController;
import com.springboot.MyTodoList.controller.GithubController;
import com.springboot.MyTodoList.controller.ReportController;
import com.springboot.MyTodoList.controller.ToDoItemBotController;
import com.springboot.MyTodoList.controller.UserController;
import com.springboot.MyTodoList.model.Sprint;
import com.springboot.MyTodoList.service.SprintService;
import com.springboot.MyTodoList.service.ToDoItemService;
import com.springboot.MyTodoList.service.UserService;
import com.springboot.MyTodoList.service.GithubService;
import com.springboot.MyTodoList.service.ReportService;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SprintController.class)
@Import(TestConfig.class)
public class SprintControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SprintService sprintService;

    @MockBean
    private ToDoItemService toDoItemService;

    @MockBean
    private UserService userService;

    @MockBean
    private GithubService githubService;

    @MockBean
    private ReportService reportService;

    @MockBean
    private KpiTelegramController kpiController;

    @MockBean
    private UserBotController userBotController;

    @MockBean
    private GithubController githubController;

    @MockBean
    private ReportController reportController;

    @MockBean
    private ToDoItemBotController toDoItemBotController;

    @MockBean
    private UserController userController;

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
    
    @Test
    public void testGetSprintById() throws Exception {
        // Respuesta esperada del servicio
        Sprint sprint = new Sprint();
        sprint.setId(1);
        sprint.setName("Sprint de Prueba");
        sprint.setStartDate(LocalDate.parse("2025-04-01"));
        sprint.setEndDate(LocalDate.parse("2025-04-15"));
        sprint.setStatus("ACTIVE");
        sprint.setCreatedBy(1);
        sprint.setCreationTs(OffsetDateTime.of(2025, 3, 15, 10, 0, 0, 0, ZoneOffset.UTC));

        when(sprintService.getSprintById(1)).thenReturn(new ResponseEntity<>(sprint, HttpStatus.OK));

        mockMvc.perform(get("/api/sprints/1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Sprint de Prueba"))
                .andExpect(jsonPath("$.startDate").value("2025-04-01"))
                .andExpect(jsonPath("$.endDate").value("2025-04-15"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.createdBy").value(1))
                .andExpect(jsonPath("$.creationTs").value("2025-03-15T10:00:00Z"));
    }

    @Test
    public void testGetSprintByIdNotFound() throws Exception {
        when(sprintService.getSprintById(999)).thenThrow(new RuntimeException("Sprint not found"));

        mockMvc.perform(get("/api/sprints/999")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testUpdateSprint() throws Exception {
        // Datos de entrada para actualización
        Sprint updateSprint = new Sprint();
        updateSprint.setName("Sprint Actualizado");
        updateSprint.setStartDate(LocalDate.parse("2025-05-05"));
        updateSprint.setEndDate(LocalDate.parse("2025-05-20"));
        updateSprint.setStatus("ACTIVE");

        // Respuesta esperada del servicio
        Sprint updatedSprint = new Sprint();
        updatedSprint.setId(1);
        updatedSprint.setName("Sprint Actualizado");
        updatedSprint.setStartDate(LocalDate.parse("2025-05-05"));
        updatedSprint.setEndDate(LocalDate.parse("2025-05-20"));
        updatedSprint.setStatus("ACTIVE");
        updatedSprint.setCreatedBy(1);
        updatedSprint.setCreationTs(OffsetDateTime.of(2025, 3, 15, 10, 0, 0, 0, ZoneOffset.UTC));

        when(sprintService.updateSprint(eq(1), any(Sprint.class))).thenReturn(updatedSprint);

        mockMvc.perform(put("/api/sprints/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateSprint)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Sprint Actualizado"))
                .andExpect(jsonPath("$.startDate").value("2025-05-05"))
                .andExpect(jsonPath("$.endDate").value("2025-05-20"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.createdBy").value(1))
                .andExpect(jsonPath("$.creationTs").value("2025-03-15T10:00:00Z"));
    }

    @Test
    public void testUpdateSprintNotFound() throws Exception {
        // Datos de entrada para actualización
        Sprint updateSprint = new Sprint();
        updateSprint.setName("Sprint Actualizado");
        updateSprint.setStartDate(LocalDate.parse("2025-05-05"));
        updateSprint.setEndDate(LocalDate.parse("2025-05-20"));
        updateSprint.setStatus("ACTIVE");

        when(sprintService.updateSprint(eq(999), any(Sprint.class))).thenReturn(null);

        mockMvc.perform(put("/api/sprints/999")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateSprint)))
                .andExpect(status().isNotFound());
    }
}