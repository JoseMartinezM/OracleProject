package com.springboot.MyTodoList;

import com.springboot.MyTodoList.controller.ToDoItemController;
import com.springboot.MyTodoList.controller.KpiTelegramController;
import com.springboot.MyTodoList.controller.UserBotController;
import com.springboot.MyTodoList.controller.GithubController;
import com.springboot.MyTodoList.controller.ReportController;
import com.springboot.MyTodoList.controller.SprintController;
import com.springboot.MyTodoList.controller.ToDoItemBotController;
import com.springboot.MyTodoList.controller.UserController;
import com.springboot.MyTodoList.model.ToDoItem;
import com.springboot.MyTodoList.service.ToDoItemService;
import com.springboot.MyTodoList.service.UserService;
import com.springboot.MyTodoList.service.SprintService;
import com.springboot.MyTodoList.service.GithubService;
import com.springboot.MyTodoList.service.ReportService;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = ToDoItemController.class, 
            excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, 
                                                 classes = {MyTodoListApplication.class}))
public class ToDoItemControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ToDoItemService toDoItemService;
    
    @MockBean
    private UserService userService;
    
    @MockBean
    private SprintService sprintService;
    
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
    private SprintController sprintController;
    
    @MockBean
    private ToDoItemBotController toDoItemBotController;
    
    @MockBean
    private UserController userController;
    
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void testGetAllToDoItems() throws Exception {
        // Respuesta esperada del servicio
        List<ToDoItem> items = new ArrayList<>();
        
        ToDoItem item1 = new ToDoItem();
        item1.setID(1);
        item1.setDescription("Tarea 1");
        item1.setStatus("Pending");
        item1.setPriority("High");
        item1.setCreatedBy(1);
        
        ToDoItem item2 = new ToDoItem();
        item2.setID(2);
        item2.setDescription("Tarea 2");
        item2.setStatus("In Progress");
        item2.setPriority("Medium");
        item2.setCreatedBy(1);
        
        items.add(item1);
        items.add(item2);

        when(toDoItemService.findAll()).thenReturn(items);

        mockMvc.perform(get("/api/todolist")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].description").value("Tarea 1"))
                .andExpect(jsonPath("$[0].status").value("Pending"))
                .andExpect(jsonPath("$[0].priority").value("High"))
                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[1].description").value("Tarea 2"))
                .andExpect(jsonPath("$[1].status").value("In Progress"));
    }

    @Test
    public void testGetToDoItemById() throws Exception {
        // Respuesta esperada del servicio
        ToDoItem item = new ToDoItem();
        item.setID(1);
        item.setDescription("Desarrollar API REST");
        item.setStatus("In Progress");
        item.setPriority("High");
        item.setCreatedBy(1);
        item.setAssignedTo(2);
        item.setEstimatedHours(8.5);
        item.setSprintId(1);
        item.setCreation_ts(OffsetDateTime.of(2023, 4, 10, 10, 0, 0, 0, ZoneOffset.UTC));

        when(toDoItemService.getItemById(1)).thenReturn(new ResponseEntity<>(item, HttpStatus.OK));

        mockMvc.perform(get("/api/todolist/1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.description").value("Desarrollar API REST"))
                .andExpect(jsonPath("$.status").value("In Progress"))
                .andExpect(jsonPath("$.priority").value("High"))
                .andExpect(jsonPath("$.createdBy").value(1))
                .andExpect(jsonPath("$.assignedTo").value(2))
                .andExpect(jsonPath("$.estimatedHours").value(8.5))
                .andExpect(jsonPath("$.sprintId").value(1));
    }

    @Test
    public void testGetToDoItemByIdNotFound() throws Exception {
        when(toDoItemService.getItemById(999)).thenThrow(new RuntimeException("Item not found"));

        mockMvc.perform(get("/api/todolist/999")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testCreateToDoItem() throws Exception {
        // Datos de entrada
        ToDoItem inputItem = new ToDoItem();
        inputItem.setDescription("Nueva tarea");
        inputItem.setStatus("Pending");
        inputItem.setPriority("Medium");
        inputItem.setCreatedBy(1);
        inputItem.setEstimatedHours(4.0);

        // Respuesta esperada del servicio
        ToDoItem savedItem = new ToDoItem();
        savedItem.setID(5);
        savedItem.setDescription("Nueva tarea");
        savedItem.setStatus("Pending");
        savedItem.setPriority("Medium");
        savedItem.setCreatedBy(1);
        savedItem.setEstimatedHours(4.0);
        savedItem.setCreation_ts(OffsetDateTime.of(2023, 4, 10, 10, 0, 0, 0, ZoneOffset.UTC));

        when(toDoItemService.addToDoItem(any(ToDoItem.class))).thenReturn(savedItem);

        mockMvc.perform(post("/api/todolist")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(inputItem)))
                .andExpect(status().isOk())
                .andExpect(header().exists("location"))
                .andExpect(header().string("location", "5"));
    }

    @Test
    public void testUpdateToDoItem() throws Exception {
        // Datos de entrada para actualización
        ToDoItem updateItem = new ToDoItem();
        updateItem.setDescription("Tarea actualizada");
        updateItem.setStatus("In Progress");
        updateItem.setPriority("High");

        // Respuesta esperada del servicio
        ToDoItem updatedItem = new ToDoItem();
        updatedItem.setID(1);
        updatedItem.setDescription("Tarea actualizada");
        updatedItem.setStatus("In Progress");
        updatedItem.setPriority("High");
        updatedItem.setCreatedBy(1);
        updatedItem.setAssignedTo(2);
        updatedItem.setSprintId(1);

        when(toDoItemService.updateToDoItem(eq(1), any(ToDoItem.class))).thenReturn(updatedItem);

        mockMvc.perform(put("/api/todolist/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateItem)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.description").value("Tarea actualizada"))
                .andExpect(jsonPath("$.status").value("In Progress"))
                .andExpect(jsonPath("$.priority").value("High"))
                .andExpect(jsonPath("$.createdBy").value(1))
                .andExpect(jsonPath("$.assignedTo").value(2))
                .andExpect(jsonPath("$.sprintId").value(1));
    }

    @Test
    public void testUpdateToDoItemNotFound() throws Exception {
        // Datos de entrada para actualización
        ToDoItem updateItem = new ToDoItem();
        updateItem.setDescription("Tarea actualizada");
        updateItem.setStatus("In Progress");

        when(toDoItemService.updateToDoItem(eq(999), any(ToDoItem.class))).thenReturn(null);

        mockMvc.perform(put("/api/todolist/999")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateItem)))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testDeleteToDoItem() throws Exception {
        when(toDoItemService.deleteToDoItem(1)).thenReturn(true);

        mockMvc.perform(delete("/api/todolist/1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    @Test
    public void testDeleteToDoItemNotFound() throws Exception {
        when(toDoItemService.deleteToDoItem(999)).thenThrow(new RuntimeException("Item not found"));

        mockMvc.perform(delete("/api/todolist/999")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testArchiveToDoItem() throws Exception {
        // Respuesta esperada del servicio
        ToDoItem archivedItem = new ToDoItem();
        archivedItem.setID(1);
        archivedItem.setDescription("Tarea archivada");
        archivedItem.setStatus("Completed");
        archivedItem.setIsArchived(1);

        when(toDoItemService.archiveToDoItem(1)).thenReturn(archivedItem);

        mockMvc.perform(put("/api/todolist/1/archive")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.description").value("Tarea archivada"))
                .andExpect(jsonPath("$.isArchived").value(1));
    }

    @Test
    public void testUpdateStatus() throws Exception {
        // Datos de entrada para actualización de estado
        String requestBody = "{\"status\":\"Completed\"}";

        // Respuesta esperada del servicio
        ToDoItem updatedItem = new ToDoItem();
        updatedItem.setID(1);
        updatedItem.setDescription("Tarea completada");
        updatedItem.setStatus("Completed");
        updatedItem.setDone(true);

        when(toDoItemService.updateStatus(eq(1), eq("Completed"))).thenReturn(updatedItem);

        mockMvc.perform(patch("/api/todolist/1/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.description").value("Tarea completada"))
                .andExpect(jsonPath("$.status").value("Completed"));
    }

    @Test
    public void testAssignToSprint() throws Exception {
        // Datos de entrada para asignación a sprint
        String requestBody = "{\"sprintId\":3}";

        // Respuesta esperada del servicio
        ToDoItem updatedItem = new ToDoItem();
        updatedItem.setID(1);
        updatedItem.setDescription("Tarea asignada a sprint");
        updatedItem.setStatus("In Progress");
        updatedItem.setSprintId(3);

        when(toDoItemService.assignToSprint(eq(1), eq(3))).thenReturn(updatedItem);

        mockMvc.perform(patch("/api/todolist/1/sprint")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.description").value("Tarea asignada a sprint"))
                .andExpect(jsonPath("$.sprintId").value(3));
    }

    @Test
    public void testUpdateEstimatedHours() throws Exception {
        // Datos de entrada para actualización de horas estimadas
        String requestBody = "{\"hours\":6.5}";

        // Respuesta esperada del servicio
        ToDoItem updatedItem = new ToDoItem();
        updatedItem.setID(1);
        updatedItem.setDescription("Tarea con horas estimadas");
        updatedItem.setEstimatedHours(6.5);

        when(toDoItemService.updateEstimatedHours(eq(1), eq(6.5))).thenReturn(updatedItem);

        mockMvc.perform(patch("/api/todolist/1/estimated-hours")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.description").value("Tarea con horas estimadas"))
                .andExpect(jsonPath("$.estimatedHours").value(6.5));
    }

    @Test
    public void testUpdateActualHours() throws Exception {
        // Datos de entrada para actualización de horas reales
        String requestBody = "{\"hours\":7.5}";

        // Respuesta esperada del servicio
        ToDoItem updatedItem = new ToDoItem();
        updatedItem.setID(1);
        updatedItem.setDescription("Tarea con horas reales");
        updatedItem.setEstimatedHours(6.0);
        updatedItem.setActualHours(7.5);

        when(toDoItemService.updateActualHours(eq(1), eq(7.5))).thenReturn(updatedItem);

        mockMvc.perform(patch("/api/todolist/1/actual-hours")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.description").value("Tarea con horas reales"))
                .andExpect(jsonPath("$.actualHours").value(7.5));
    }

    @Test
    public void testGetToDoItemsByPriority() throws Exception {
        // Respuesta esperada del servicio
        List<ToDoItem> highPriorityItems = new ArrayList<>();
        
        ToDoItem item1 = new ToDoItem();
        item1.setID(1);
        item1.setDescription("Tarea 1");
        item1.setPriority("High");
        
        ToDoItem item2 = new ToDoItem();
        item2.setID(3);
        item2.setDescription("Tarea 3");
        item2.setPriority("High");
        
        highPriorityItems.add(item1);
        highPriorityItems.add(item2);

        when(toDoItemService.findByPriority("High")).thenReturn(highPriorityItems);

        mockMvc.perform(get("/api/todolist/priority/High")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].description").value("Tarea 1"))
                .andExpect(jsonPath("$[0].priority").value("High"))
                .andExpect(jsonPath("$[1].id").value(3))
                .andExpect(jsonPath("$[1].description").value("Tarea 3"))
                .andExpect(jsonPath("$[1].priority").value("High"));
    }

    @Test
    public void testGetToDoItemsByStatus() throws Exception {
        // Respuesta esperada del servicio
        List<ToDoItem> inProgressItems = new ArrayList<>();
        
        ToDoItem item1 = new ToDoItem();
        item1.setID(2);
        item1.setDescription("Tarea 2");
        item1.setStatus("In Progress");
        
        ToDoItem item2 = new ToDoItem();
        item2.setID(4);
        item2.setDescription("Tarea 4");
        item2.setStatus("In Progress");
        
        inProgressItems.add(item1);
        inProgressItems.add(item2);

        when(toDoItemService.findByStatus("In Progress")).thenReturn(inProgressItems);

        mockMvc.perform(get("/api/todolist/status/In Progress")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(2))
                .andExpect(jsonPath("$[0].description").value("Tarea 2"))
                .andExpect(jsonPath("$[0].status").value("In Progress"))
                .andExpect(jsonPath("$[1].id").value(4))
                .andExpect(jsonPath("$[1].description").value("Tarea 4"))
                .andExpect(jsonPath("$[1].status").value("In Progress"));
    }

    @Test
    public void testGetToDoItemsBySprintId() throws Exception {
        // Respuesta esperada del servicio
        List<ToDoItem> sprintItems = new ArrayList<>();
        
        ToDoItem item1 = new ToDoItem();
        item1.setID(1);
        item1.setDescription("Tarea 1");
        item1.setSprintId(2);
        
        ToDoItem item2 = new ToDoItem();
        item2.setID(3);
        item2.setDescription("Tarea 3");
        item2.setSprintId(2);
        
        sprintItems.add(item1);
        sprintItems.add(item2);

        when(toDoItemService.findBySprintId(2)).thenReturn(sprintItems);

        mockMvc.perform(get("/api/todolist/sprint/2")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].description").value("Tarea 1"))
                .andExpect(jsonPath("$[0].sprintId").value(2))
                .andExpect(jsonPath("$[1].id").value(3))
                .andExpect(jsonPath("$[1].description").value("Tarea 3"))
                .andExpect(jsonPath("$[1].sprintId").value(2));
    }
}