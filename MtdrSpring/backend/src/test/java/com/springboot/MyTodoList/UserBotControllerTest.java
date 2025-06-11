package com.springboot.MyTodoList;

import com.springboot.MyTodoList.controller.UserBotController;
import com.springboot.MyTodoList.controller.KpiTelegramController;
import com.springboot.MyTodoList.model.Sprint;
import com.springboot.MyTodoList.model.ToDoItem;
import com.springboot.MyTodoList.model.User;
import com.springboot.MyTodoList.service.SprintService;
import com.springboot.MyTodoList.service.ToDoItemService;
import com.springboot.MyTodoList.service.UserService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Contact;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
public class UserBotControllerTest {

    @Mock
    private ToDoItemService toDoItemService;

    @Mock
    private UserService userService;

    @Mock
    private SprintService sprintService;

    @Mock
    private KpiTelegramController kpiController;

    private UserBotController userBotController;

    // Constantes para pruebas
    private static final long CHAT_ID = 12345L;
    private static final String BOT_TOKEN = "7784087324:AAHYk1ZnNOOSUbAN8hjIAnIN3l7nFIRoAoM";
    private static final String BOT_NAME = "Team14JavaBot";

    // Variables para los objetos mock
    private User testUser;
    private org.telegram.telegrambots.meta.api.objects.User mockTelegramUser;

    @BeforeEach
    void setUp() throws Exception {
        // Crear un spy del controlador
        userBotController = spy(new UserBotController(BOT_TOKEN, BOT_NAME, toDoItemService, userService, sprintService, kpiController));
        
        // Evitar la ejecución real del método execute
        doReturn(null).when(userBotController).execute(any(SendMessage.class));
        
        // Configurar el usuario de prueba
        testUser = new User();
        testUser.setID(1);
        testUser.setName("Test User");
        testUser.setUsername("testuser");
        testUser.setRole("Developer");
        testUser.setPhone("+123456789");
        
        // Crear usuario de Telegram
        mockTelegramUser = new org.telegram.telegrambots.meta.api.objects.User();
        mockTelegramUser.setId(CHAT_ID);
        mockTelegramUser.setUserName("testuser");
    }

    @Test
    public void testPhoneVerification_Success() throws TelegramApiException {
        // Crear mocks para esta prueba específica
        Update mockUpdate = mock(Update.class);
        Message mockMessage = mock(Message.class);
        Contact mockContact = mock(Contact.class);
        
        // Configurar comportamiento
        when(mockUpdate.hasMessage()).thenReturn(true);
        when(mockUpdate.getMessage()).thenReturn(mockMessage);
        when(mockMessage.getChatId()).thenReturn(CHAT_ID);
        when(mockMessage.getFrom()).thenReturn(mockTelegramUser);
        when(mockMessage.hasContact()).thenReturn(true);
        when(mockMessage.getContact()).thenReturn(mockContact);
        when(mockContact.getPhoneNumber()).thenReturn("123456789");
        when(userService.getUserByPhone("+123456789")).thenReturn(new ResponseEntity<>(testUser, HttpStatus.OK));
        
        // Ejecutar el método
        userBotController.onUpdateReceived(mockUpdate);
        
        // Verificar
        verify(userService).getUserByPhone("+123456789");
        verify(userBotController, times(2)).execute(any(SendMessage.class));
    }

    @Test
    public void testPhoneVerification_UserNotFound() throws TelegramApiException {
        // Crear mocks para esta prueba específica
        Update mockUpdate = mock(Update.class);
        Message mockMessage = mock(Message.class);
        Contact mockContact = mock(Contact.class);
        
        // Configurar comportamiento
        when(mockUpdate.hasMessage()).thenReturn(true);
        when(mockUpdate.getMessage()).thenReturn(mockMessage);
        when(mockMessage.getChatId()).thenReturn(CHAT_ID);
        when(mockMessage.getFrom()).thenReturn(mockTelegramUser);
        when(mockMessage.hasContact()).thenReturn(true);
        when(mockMessage.getContact()).thenReturn(mockContact);
        when(mockContact.getPhoneNumber()).thenReturn("123456789");
        when(userService.getUserByPhone("+123456789")).thenReturn(new ResponseEntity<>(null, HttpStatus.NOT_FOUND));
        
        // Ejecutar el método
        userBotController.onUpdateReceived(mockUpdate);
        
        // Verificar
        verify(userService).getUserByPhone("+123456789");
        verify(userBotController).execute(any(SendMessage.class));
    }

    @Test
    public void testCreateTask_Success() throws TelegramApiException, NoSuchFieldException, IllegalAccessException {
        // Crear mocks para esta prueba específica
        Update mockUpdate = mock(Update.class);
        Message mockMessage = mock(Message.class);
        
        // Configurar comportamiento
        when(mockUpdate.hasMessage()).thenReturn(true);
        when(mockUpdate.getMessage()).thenReturn(mockMessage);
        when(mockMessage.getChatId()).thenReturn(CHAT_ID);
        when(mockMessage.getFrom()).thenReturn(mockTelegramUser);
        when(mockMessage.hasText()).thenReturn(true);
        when(mockMessage.getText()).thenReturn("Nueva tarea de prueba");
        
        // Preparar estado del bot
        java.lang.reflect.Field authorizedUsersField = UserBotController.class.getDeclaredField("authorizedUsers");
        authorizedUsersField.setAccessible(true);
        Map<Long, User> authorizedUsers = new HashMap<>();
        authorizedUsers.put(CHAT_ID, testUser);
        authorizedUsersField.set(userBotController, authorizedUsers);
        
        java.lang.reflect.Field chatStateField = UserBotController.class.getDeclaredField("chatState");
        chatStateField.setAccessible(true);
        Map<Long, String> chatState = new HashMap<>();
        chatState.put(CHAT_ID, "ADDING_NEW_TASK");
        chatStateField.set(userBotController, chatState);
        
        java.lang.reflect.Field temporaryDataField = UserBotController.class.getDeclaredField("temporaryData");
        temporaryDataField.setAccessible(true);
        Map<Long, Map<String, Object>> temporaryData = new HashMap<>();
        temporaryData.put(CHAT_ID, new HashMap<>());
        temporaryDataField.set(userBotController, temporaryData);
        
        // Ejecutar el método
        userBotController.onUpdateReceived(mockUpdate);
        
        // Verificar
        verify(userBotController).execute(any(SendMessage.class));
        
        // Verificar que el estado cambió a ADDING_TASK_PRIORITY
        Map<Long, String> updatedChatState = (Map<Long, String>)chatStateField.get(userBotController);
        assertEquals("ADDING_TASK_PRIORITY", updatedChatState.get(CHAT_ID));
        
        Map<Long, Map<String, Object>> updatedTempData = (Map<Long, Map<String, Object>>)temporaryDataField.get(userBotController);
        assertEquals("Nueva tarea de prueba", updatedTempData.get(CHAT_ID).get("description"));
    }

    @Test
    public void testAddingTaskEstimatedHoursState() throws TelegramApiException, NoSuchFieldException, IllegalAccessException {
        // Crear mocks para esta prueba específica
        Update mockUpdate = mock(Update.class);
        Message mockMessage = mock(Message.class);
        
        // Configurar comportamiento
        when(mockUpdate.hasMessage()).thenReturn(true);
        when(mockUpdate.getMessage()).thenReturn(mockMessage);
        when(mockMessage.getChatId()).thenReturn(CHAT_ID);
        when(mockMessage.getFrom()).thenReturn(mockTelegramUser);
        when(mockMessage.hasText()).thenReturn(true);
        when(mockMessage.getText()).thenReturn("4");
        
        // Preparar estado del bot
        java.lang.reflect.Field authorizedUsersField = UserBotController.class.getDeclaredField("authorizedUsers");
        authorizedUsersField.setAccessible(true);
        Map<Long, User> authorizedUsers = new HashMap<>();
        authorizedUsers.put(CHAT_ID, testUser);
        authorizedUsersField.set(userBotController, authorizedUsers);
        
        java.lang.reflect.Field chatStateField = UserBotController.class.getDeclaredField("chatState");
        chatStateField.setAccessible(true);
        Map<Long, String> chatState = new HashMap<>();
        chatState.put(CHAT_ID, "ADDING_TASK_ESTIMATED_HOURS");
        chatStateField.set(userBotController, chatState);
        
        java.lang.reflect.Field temporaryDataField = UserBotController.class.getDeclaredField("temporaryData");
        temporaryDataField.setAccessible(true);
        Map<Long, Map<String, Object>> temporaryData = new HashMap<>();
        Map<String, Object> data = new HashMap<>();
        data.put("description", "Nueva tarea");
        data.put("priority", "High");
        temporaryData.put(CHAT_ID, data);
        temporaryDataField.set(userBotController, temporaryData);
        
        // Mock para simular la creación de una tarea
        ToDoItem mockTask = new ToDoItem();
        mockTask.setID(1);
        mockTask.setDescription("Nueva tarea");
        mockTask.setPriority("High");
        mockTask.setEstimatedHours(4.0);
        when(toDoItemService.addToDoItem(any(ToDoItem.class))).thenReturn(mockTask);
        
        // Ejecutar el método
        userBotController.onUpdateReceived(mockUpdate);
        
        // Verificar
        verify(userBotController, times(2)).execute(any(SendMessage.class));
        
        // Verificar que el estado volvió a NONE
        Map<Long, String> updatedChatState = (Map<Long, String>)chatStateField.get(userBotController);
        assertEquals("NONE", updatedChatState.get(CHAT_ID));
    }

    @Test
    public void testViewTasksCompletedInSprint() throws TelegramApiException, NoSuchFieldException, IllegalAccessException {
        // Crear mocks para esta prueba específica
        Update mockUpdate = mock(Update.class);
        Message mockMessage = mock(Message.class);
        
        // Configurar comportamiento
        when(mockUpdate.hasMessage()).thenReturn(true);
        when(mockUpdate.getMessage()).thenReturn(mockMessage);
        when(mockMessage.getChatId()).thenReturn(CHAT_ID);
        when(mockMessage.getFrom()).thenReturn(mockTelegramUser);
        when(mockMessage.hasText()).thenReturn(true);
        when(mockMessage.getText()).thenReturn("menu");
        
        // Preparar estado del bot
        java.lang.reflect.Field authorizedUsersField = UserBotController.class.getDeclaredField("authorizedUsers");
        authorizedUsersField.setAccessible(true);
        Map<Long, User> authorizedUsers = new HashMap<>();
        authorizedUsers.put(CHAT_ID, testUser);
        authorizedUsersField.set(userBotController, authorizedUsers);
        
        java.lang.reflect.Field chatStateField = UserBotController.class.getDeclaredField("chatState");
        chatStateField.setAccessible(true);
        Map<Long, String> chatState = new HashMap<>();
        chatState.put(CHAT_ID, "VIEWING_SPRINT_TASKS");
        chatStateField.set(userBotController, chatState);
        
        java.lang.reflect.Field temporaryDataField = UserBotController.class.getDeclaredField("temporaryData");
        temporaryDataField.setAccessible(true);
        Map<Long, Map<String, Object>> temporaryData = new HashMap<>();
        Map<String, Object> data = new HashMap<>();
        data.put("sprintId", 1);
        temporaryData.put(CHAT_ID, data);
        temporaryDataField.set(userBotController, temporaryData);
        
        // Ejecutar el método
        userBotController.onUpdateReceived(mockUpdate);
        
        // Verificar
        verify(userBotController).execute(any(SendMessage.class));
    }
}