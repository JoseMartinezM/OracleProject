package com.springboot.MyTodoList;

import com.springboot.MyTodoList.controller.UserController;
import com.springboot.MyTodoList.controller.KpiTelegramController;
import com.springboot.MyTodoList.controller.UserBotController;
import com.springboot.MyTodoList.controller.GithubController;
import com.springboot.MyTodoList.controller.ReportController;
import com.springboot.MyTodoList.controller.SprintController;
import com.springboot.MyTodoList.controller.ToDoItemBotController;
import com.springboot.MyTodoList.model.User;
import com.springboot.MyTodoList.service.UserService;
import com.springboot.MyTodoList.service.SprintService;
import com.springboot.MyTodoList.service.ToDoItemService;
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

import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@Import(TestConfig.class)
public class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @MockBean
    private SprintService sprintService;

    @MockBean
    private ToDoItemService toDoItemService;

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

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void testGetUserById() throws Exception {
        // Respuesta esperada del servicio
        User user = new User();
        user.setID(1);
        user.setUsername("developer1");
        user.setPassword("password123");
        user.setRole("Developer");
        user.setPhone("+5491112345678");
        user.setName("John Doe");

        when(userService.getUserById(1)).thenReturn(new ResponseEntity<>(user, HttpStatus.OK));

        mockMvc.perform(get("/api/users/1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.username").value("developer1"))
                .andExpect(jsonPath("$.password").value("password123"))
                .andExpect(jsonPath("$.role").value("Developer"))
                .andExpect(jsonPath("$.phone").value("+5491112345678"))
                .andExpect(jsonPath("$.name").value("John Doe"));
    }

    @Test
    public void testGetUserByIdNotFound() throws Exception {
        when(userService.getUserById(999)).thenThrow(new RuntimeException("User not found"));

        mockMvc.perform(get("/api/users/999")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testCreateUser() throws Exception {
        // Datos de entrada
        User inputUser = new User();
        inputUser.setUsername("newuser");
        inputUser.setPassword("securepass");
        inputUser.setRole("Developer");
        inputUser.setPhone("+5491198765432");
        inputUser.setName("Jane Smith");
    
        // Respuesta esperada del servicio
        User savedUser = new User();
        savedUser.setID(5);
        savedUser.setUsername("newuser");
        savedUser.setPassword("securepass");
        savedUser.setRole("Developer");
        savedUser.setPhone("+5491198765432");
        savedUser.setName("Jane Smith");
    
        when(userService.addUser(any(User.class))).thenReturn(savedUser);
    
        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(inputUser)))
                .andExpect(status().isOk())
                .andExpect(header().exists("location"))
                .andExpect(header().string("location", "5"));
    }

    @Test
    public void testUpdateUser() throws Exception {
        // Datos de entrada para actualización
        User updateUser = new User();
        updateUser.setPassword("newpassword");
        updateUser.setPhone("+5491155556666");
        updateUser.setName("John Updated");

        // Respuesta esperada del servicio
        User updatedUser = new User();
        updatedUser.setID(1);
        updatedUser.setUsername("developer1");
        updatedUser.setPassword("newpassword");
        updatedUser.setRole("Developer");
        updatedUser.setPhone("+5491155556666");
        updatedUser.setName("John Updated");

        when(userService.updateUser(eq(1), any(User.class))).thenReturn(updatedUser);

        mockMvc.perform(put("/api/users/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.username").value("developer1"))
                .andExpect(jsonPath("$.password").value("newpassword"))
                .andExpect(jsonPath("$.role").value("Developer"))
                .andExpect(jsonPath("$.phone").value("+5491155556666"))
                .andExpect(jsonPath("$.name").value("John Updated"));
    }

    @Test
    public void testUpdateUserNotFound() throws Exception {
        // Datos de entrada para actualización
        User updateUser = new User();
        updateUser.setPassword("newpassword");
        updateUser.setPhone("+5491155556666");

        when(userService.updateUser(eq(999), any(User.class))).thenReturn(null);

        mockMvc.perform(put("/api/users/999")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateUser)))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testGetAllUsers() throws Exception {
        // Respuesta esperada del servicio
        List<User> users = new ArrayList<>();
        
        User user1 = new User();
        user1.setID(1);
        user1.setUsername("developer1");
        user1.setRole("Developer");
        user1.setName("John Doe");
        
        User user2 = new User();
        user2.setID(2);
        user2.setUsername("manager1");
        user2.setRole("Manager");
        user2.setName("Jane Manager");
        
        users.add(user1);
        users.add(user2);

        when(userService.findAll()).thenReturn(users);

        mockMvc.perform(get("/api/users")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].username").value("developer1"))
                .andExpect(jsonPath("$[0].role").value("Developer"))
                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[1].username").value("manager1"))
                .andExpect(jsonPath("$[1].role").value("Manager"));
    }

    @Test
    public void testDeleteUser() throws Exception {
        when(userService.deleteUser(1)).thenReturn(true);

        mockMvc.perform(delete("/api/users/1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    @Test
    public void testDeleteUserNotFound() throws Exception {
        when(userService.deleteUser(999)).thenThrow(new RuntimeException("User not found"));

        mockMvc.perform(delete("/api/users/999")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testGetUserByUsername() throws Exception {
        // Respuesta esperada del servicio
        User user = new User();
        user.setID(1);
        user.setUsername("developer1");
        user.setPassword("password123");
        user.setRole("Developer");
        user.setPhone("+5491112345678");
        user.setName("John Doe");

        when(userService.getUserByUsername("developer1")).thenReturn(new ResponseEntity<>(user, HttpStatus.OK));

        mockMvc.perform(get("/api/users/username/developer1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.username").value("developer1"))
                .andExpect(jsonPath("$.role").value("Developer"));
    }

    @Test
    public void testGetUserByPhone() throws Exception {
        // Respuesta esperada del servicio
        User user = new User();
        user.setID(1);
        user.setUsername("developer1");
        user.setPassword("password123");
        user.setRole("Developer");
        user.setPhone("+5491112345678");
        user.setName("John Doe");

        when(userService.getUserByPhone("+5491112345678")).thenReturn(new ResponseEntity<>(user, HttpStatus.OK));

        mockMvc.perform(get("/api/users/phone/+5491112345678")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.phone").value("+5491112345678"))
                .andExpect(jsonPath("$.name").value("John Doe"));
    }

    @Test
    public void testGetUsersByRole() throws Exception {
        // Respuesta esperada del servicio
        List<User> developers = new ArrayList<>();
        
        User dev1 = new User();
        dev1.setID(1);
        dev1.setUsername("developer1");
        dev1.setRole("Developer");
        
        User dev2 = new User();
        dev2.setID(3);
        dev2.setUsername("developer2");
        dev2.setRole("Developer");
        
        developers.add(dev1);
        developers.add(dev2);

        when(userService.findByRole("Developer")).thenReturn(developers);

        mockMvc.perform(get("/api/users/role/Developer")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].username").value("developer1"))
                .andExpect(jsonPath("$[0].role").value("Developer"))
                .andExpect(jsonPath("$[1].id").value(3))
                .andExpect(jsonPath("$[1].username").value("developer2"))
                .andExpect(jsonPath("$[1].role").value("Developer"));
    }
}
