package com.springboot.MyTodoList.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.Contact;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import com.springboot.MyTodoList.model.Sprint;
import com.springboot.MyTodoList.model.ToDoItem;
import com.springboot.MyTodoList.model.User;
import com.springboot.MyTodoList.service.SprintService;
import com.springboot.MyTodoList.service.ToDoItemService;
import com.springboot.MyTodoList.service.UserService;
import com.springboot.MyTodoList.util.BotCommands;
import com.springboot.MyTodoList.util.BotHelper;
import com.springboot.MyTodoList.util.BotLabels;
import com.springboot.MyTodoList.util.BotMessages;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import java.util.Arrays;
import java.util.LinkedHashMap;

public class UserBotController extends TelegramLongPollingBot {

    private static final Logger logger = LoggerFactory.getLogger(UserBotController.class);
    private ToDoItemService toDoItemService;
    private UserService userService;
    private SprintService sprintService;
    private String botName;
    private Map<Long, User> authorizedUsers = new HashMap<>();
    
    // Variables para manejo de estados de conversación
    private Map<Long, String> chatState = new HashMap<>();
    private Map<Long, Map<String, Object>> temporaryData = new HashMap<>();
    
    // Estados de conversación
    private static final String STATE_NONE = "NONE";
    private static final String STATE_ADDING_TASK = "ADDING_TASK";
    private static final String STATE_ADDING_TASK_HOURS = "ADDING_TASK_HOURS";
    private static final String STATE_ASSIGNING_TASK = "ASSIGNING_TASK";
    private static final String STATE_SELECTING_SPRINT = "SELECTING_SPRINT";
    private static final String STATE_COMPLETING_TASK = "COMPLETING_TASK";
    private static final String STATE_COMPLETING_TASK_HOURS = "COMPLETING_TASK_HOURS";

    private static final String STATE_VIEWING_SPRINTS = "VIEWING_SPRINTS";
private static final String STATE_VIEWING_SPRINT_TASKS = "VIEWING_SPRINT_TASKS";
private static final String STATE_MODIFYING_TASK = "MODIFYING_TASK";
private static final String STATE_CHANGING_TASK_NAME = "CHANGING_TASK_NAME";
private static final String STATE_CHANGING_TASK_STATUS = "CHANGING_TASK_STATUS";
private static final String STATE_ADDING_NEW_TASK = "ADDING_NEW_TASK";
private static final String STATE_ADDING_TASK_PRIORITY = "ADDING_TASK_PRIORITY";

private static final String STATE_ASSIGNING_DEVELOPER = "ASSIGNING_DEVELOPER";

private static final String STATE_ADDING_TASK_ESTIMATED_HOURS = "ADDING_TASK_ESTIMATED_HOURS";
    

    public UserBotController(String botToken, String botName, ToDoItemService toDoItemService, UserService userService, SprintService sprintService) {
        super(botToken);
        logger.info("Bot Token: " + botToken);
        logger.info("Bot name: " + botName);
        this.toDoItemService = toDoItemService;
        this.userService = userService;
        this.sprintService = sprintService;
        this.botName = botName;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage()) {
            long chatId = update.getMessage().getChatId();
            String username = update.getMessage().getFrom().getUserName();
            
            logger.info("Mensaje recibido de: " + (username != null ? "@" + username : "Usuario sin username") + " (Chat ID: " + chatId + ")");
            
            // Si el usuario ya está autorizado, procesa normalmente
            if (authorizedUsers.containsKey(chatId)) {
                logger.info("Usuario ya autorizado, procesando solicitud normal");
                processAuthorizedRequest(update);
                return;
            }
            
            // Verificar si el mensaje contiene un contacto
            if (update.getMessage().hasContact()) {
                logger.info("Mensaje contiene contacto, procesando verificación");
                processContactMessage(update);
                return;
            }
            
            // Si el mensaje es el comando de inicio y el usuario no está autorizado
            if (update.getMessage().hasText() && 
                update.getMessage().getText().equals(BotCommands.START_COMMAND.getCommand())) {
                logger.info("Comando de inicio recibido, solicitando número de teléfono");
                requestPhoneNumber(chatId);
                return;
            }
            
            // Si el usuario no está autorizado y no es ninguno de los casos anteriores
            if (!authorizedUsers.containsKey(chatId)) {
                logger.info("Usuario no autorizado, enviando mensaje de verificación");
                sendUnauthorizedMessage(chatId);
                return;
            }
        }
    }

    private void processContactMessage(Update update) {
        Contact contact = update.getMessage().getContact();
        long chatId = update.getMessage().getChatId();
        String phoneNumber = contact.getPhoneNumber();
        
        logger.info("Recibido número de teléfono: " + phoneNumber);
        
        // Asegurarse de que el formato del teléfono tenga el signo +
        if (!phoneNumber.startsWith("+")) {
            phoneNumber = "+" + phoneNumber;
        }
        
        logger.info("Buscando usuario con teléfono: " + phoneNumber);
        
        // Buscar el usuario en la base de datos
        ResponseEntity<User> userResponse = userService.getUserByPhone(phoneNumber);
        
        if (userResponse.getStatusCode() == HttpStatus.OK && userResponse.getBody() != null) {
            User user = userResponse.getBody();
            // Guardar el usuario autorizado en memoria
            authorizedUsers.put(chatId, user);
            
            // Inicializar estado de la conversación
            chatState.put(chatId, STATE_NONE);
            temporaryData.put(chatId, new HashMap<>());
            
            logger.info("Usuario autorizado: " + user.getName() + " (ID: " + user.getID() + ")");
            
            // Mostrar mensaje de bienvenida con el nombre del usuario
            SendMessage welcomeMessage = new SendMessage();
            welcomeMessage.setChatId(chatId);
            welcomeMessage.setText("✅ ¡Verificación exitosa!\n\n¡Bienvenido " + user.getName() + "!\n\nTu rol en el sistema es: " + user.getRole());
            
            try {
                execute(welcomeMessage);
                showMainMenu(chatId, user);
            } catch (TelegramApiException e) {
                logger.error("Error sending welcome message", e);
            }
        } else {
            // Usuario no encontrado en la base de datos
            logger.warn("Usuario no encontrado con el teléfono: " + phoneNumber);
            
            SendMessage unauthorizedMessage = new SendMessage();
            unauthorizedMessage.setChatId(chatId);
            unauthorizedMessage.setText("❌ Tu número no está registrado en el sistema.\n\nPor favor, contacta con tu Manager para solicitar acceso e incluir tu número de teléfono en la base de datos.");
            
            ReplyKeyboardRemove keyboardRemove = new ReplyKeyboardRemove();
            keyboardRemove.setRemoveKeyboard(true);
            unauthorizedMessage.setReplyMarkup(keyboardRemove);
            
            try {
                execute(unauthorizedMessage);
            } catch (TelegramApiException e) {
                logger.error("Error sending unauthorized message", e);
            }
        }
    }

    private void requestPhoneNumber(long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("¡Bienvenido! Este bot requiere verificación de identidad.\n\nPor favor, presiona el botón de abajo para verificar tu acceso. Esto es un requisito de seguridad único y solo lo necesitarás hacer una vez.");
        
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setResizeKeyboard(true);
        keyboardMarkup.setOneTimeKeyboard(true);
        keyboardMarkup.setSelective(true);
        
        List<KeyboardRow> keyboard = new ArrayList<>();
        KeyboardRow row = new KeyboardRow();
        
        KeyboardButton button = new KeyboardButton("📱 VERIFICAR MI IDENTIDAD 📱");
        button.setRequestContact(true);
        row.add(button);
        
        keyboard.add(row);
        keyboardMarkup.setKeyboard(keyboard);
        message.setReplyMarkup(keyboardMarkup);
        
        try {
            execute(message);
        } catch (TelegramApiException e) {
            logger.error("Error requesting phone number", e);
        }
    }

    private void sendUnauthorizedMessage(long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("Para usar este bot, primero necesitas verificar tu identidad. Por favor, usa el comando /start y luego presiona el botón para compartir tu contacto.");
        
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setResizeKeyboard(true);
        keyboardMarkup.setOneTimeKeyboard(true);
        
        List<KeyboardRow> keyboard = new ArrayList<>();
        KeyboardRow row = new KeyboardRow();
        row.add("/start");
        keyboard.add(row);
        
        keyboardMarkup.setKeyboard(keyboard);
        message.setReplyMarkup(keyboardMarkup);
        
        try {
            execute(message);
        } catch (TelegramApiException e) {
            logger.error("Error sending unauthorized message", e);
        }
    }

    private void showMainMenu(long chatId, User user) {
        SendMessage messageToTelegram = new SendMessage();
        messageToTelegram.setChatId(chatId);
        
        String welcomeMessage = "¡Hola " + user.getName() + "!\n" + BotMessages.HELLO_MYTODO_BOT.getMessage();
        messageToTelegram.setText(welcomeMessage);
    
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setResizeKeyboard(true);
        List<KeyboardRow> keyboard = new ArrayList<>();
    
        // Restablecer el estado
        chatState.put(chatId, STATE_NONE);
        temporaryData.put(chatId, new HashMap<>());
    
        // first row
        KeyboardRow row = new KeyboardRow();
        row.add(BotLabels.LIST_ALL_ITEMS.getLabel());
        row.add(BotLabels.ADD_NEW_ITEM.getLabel());
        keyboard.add(row);
    
        // Menú específico para Developer
        if ("Developer".equals(user.getRole())) {
            row = new KeyboardRow();
            // Eliminamos los botones "Agregar Tarea con Horas" y "Mis Tareas Asignadas"
            row.add("✅ Completar Tarea");
            row.add("📋 Ver Sprints");
            keyboard.add(row);
        }
    
        // Si el usuario es Manager, mostrar opciones adicionales
        if ("Manager".equals(user.getRole())) {
            row = new KeyboardRow();
            row.add("👨‍💻 Ver Desarrolladores");
            row.add("📝 Asignar Tarea a Sprint");
            keyboard.add(row);
            
            row = new KeyboardRow();
            row.add("⏱️ Ver Horas por Sprint");
            row.add("📊 Resumen de Tareas");
            keyboard.add(row);
        }
    
        // Última fila
        row = new KeyboardRow();
        row.add(BotLabels.SHOW_MAIN_SCREEN.getLabel());
        row.add(BotLabels.HIDE_MAIN_SCREEN.getLabel());
        keyboard.add(row);
    
        // Set the keyboard
        keyboardMarkup.setKeyboard(keyboard);
    
        // Add the keyboard markup
        messageToTelegram.setReplyMarkup(keyboardMarkup);
    
        try {
            execute(messageToTelegram);
        } catch (TelegramApiException e) {
            logger.error(e.getLocalizedMessage(), e);
        }
    }

    private void processAuthorizedRequest(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageTextFromTelegram = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();
            User currentUser = authorizedUsers.get(chatId);
            
            // Obtener el estado actual
            String currentState = chatState.getOrDefault(chatId, STATE_NONE);
            Map<String, Object> data = temporaryData.getOrDefault(chatId, new HashMap<>());
    
            // Manejar estados de conversación
            switch (currentState) {
                case STATE_ADDING_TASK:
                    handleAddingTaskState(chatId, messageTextFromTelegram, currentUser);
                    return;
                case STATE_ADDING_TASK_HOURS:
                    handleAddingTaskHoursState(chatId, messageTextFromTelegram, currentUser, data);
                    return;
                case STATE_ASSIGNING_TASK:
                    handleAssigningTaskState(chatId, messageTextFromTelegram, currentUser);
                    return;
                case STATE_SELECTING_SPRINT:
                    handleSelectingSprintState(chatId, messageTextFromTelegram, currentUser, data);
                    return;
                case STATE_COMPLETING_TASK:
                    handleCompletingTaskState(chatId, messageTextFromTelegram, currentUser);
                    return;
                case STATE_COMPLETING_TASK_HOURS:
                    handleCompletingTaskHoursState(chatId, messageTextFromTelegram, currentUser, data);
                    return;
                case STATE_VIEWING_SPRINTS:
                    handleViewingSprintsState(chatId, messageTextFromTelegram, currentUser);
                    return;
                case STATE_VIEWING_SPRINT_TASKS:
                    handleViewingSprintTasksState(chatId, messageTextFromTelegram, currentUser, data);
                    return;
                case STATE_MODIFYING_TASK:
                    handleModifyingTaskState(chatId, messageTextFromTelegram, currentUser, data);
                    return;
                case STATE_CHANGING_TASK_NAME:
                    handleChangingTaskNameState(chatId, messageTextFromTelegram, currentUser, data);
                    return;
                case STATE_CHANGING_TASK_STATUS:
                    handleChangingTaskStatusState(chatId, messageTextFromTelegram, currentUser, data);
                    return;
                case STATE_ADDING_NEW_TASK:
                    handleAddingNewTaskState(chatId, messageTextFromTelegram, currentUser);
                    return;
                case STATE_ADDING_TASK_PRIORITY:
                    handleAddingTaskPriorityState(chatId, messageTextFromTelegram, currentUser, data);
                    return;
                case STATE_ASSIGNING_DEVELOPER:
                    handleAssigningDeveloperState(chatId, messageTextFromTelegram, currentUser, data);
                    return;
                case STATE_ADDING_TASK_ESTIMATED_HOURS:
                    handleAddingTaskEstimatedHoursState(chatId, messageTextFromTelegram, currentUser, data);
                    return;
            }
    
            // Procesar comandos y opciones del menú
            if (messageTextFromTelegram.equals(BotCommands.START_COMMAND.getCommand())
                    || messageTextFromTelegram.equals(BotLabels.SHOW_MAIN_SCREEN.getLabel())) {
                
                showMainMenu(chatId, currentUser);
    
            } else if (messageTextFromTelegram.indexOf(BotLabels.DONE.getLabel()) != -1) {
                processDoneCommand(messageTextFromTelegram, chatId);
    
            } else if (messageTextFromTelegram.indexOf(BotLabels.UNDO.getLabel()) != -1) {
                processUndoCommand(messageTextFromTelegram, chatId);
    
            } else if (messageTextFromTelegram.indexOf(BotLabels.DELETE.getLabel()) != -1) {
                processDeleteCommand(messageTextFromTelegram, chatId);
    
            } else if (messageTextFromTelegram.equals(BotCommands.HIDE_COMMAND.getCommand())
                    || messageTextFromTelegram.equals(BotLabels.HIDE_MAIN_SCREEN.getLabel())) {
                
                BotHelper.sendMessageToTelegram(chatId, BotMessages.BYE.getMessage(), this);
    
            } else if (messageTextFromTelegram.equals(BotCommands.TODO_LIST.getCommand())
                    || messageTextFromTelegram.equals(BotLabels.MY_TODO_LIST.getLabel())) {
                
                showTodoList(chatId, currentUser);
            } else if (messageTextFromTelegram.equals(BotLabels.LIST_ALL_ITEMS.getLabel())) {
                
                if ("Manager".equals(currentUser.getRole())) {
                    showSprintsForTaskManagement(chatId);
                } else {
                    // Para desarrolladores, mostrar sus tareas asignadas en formato de chat
                    showDeveloperTasksInChatFormat(chatId, currentUser);
                }
                
            } else if (messageTextFromTelegram.equals(BotCommands.ADD_ITEM.getCommand())
                    || messageTextFromTelegram.equals(BotLabels.ADD_NEW_ITEM.getLabel())) {
                
                startNewTaskCreation(chatId);
                
            } else if (messageTextFromTelegram.equals("✅ Completar Tarea") && "Developer".equals(currentUser.getRole())) {
                startCompletingTask(chatId, currentUser);
                
            } else if (messageTextFromTelegram.equals("📋 Ver Sprints")) {
                showAvailableSprints(chatId);
                
            } else if (messageTextFromTelegram.equals("👨‍💻 Ver Desarrolladores") && "Manager".equals(currentUser.getRole())) {
                showDevelopers(chatId);
                
            } else if (messageTextFromTelegram.equals("📝 Asignar Tarea a Sprint") && "Manager".equals(currentUser.getRole())) {
                startAssigningTaskToSprint(chatId);
                
            } else if (messageTextFromTelegram.equals("⏱️ Ver Horas por Sprint") && "Manager".equals(currentUser.getRole())) {
                showHoursBySprintReport(chatId);
                
            } else if (messageTextFromTelegram.equals("📊 Resumen de Tareas") && "Manager".equals(currentUser.getRole())) {
                showTasksSummary(chatId);
                
            } 
            // Manejar comandos de acción sobre tareas para desarrolladores
            else if ("Developer".equals(currentUser.getRole()) && 
                    (messageTextFromTelegram.startsWith("iniciar ") || 
                    messageTextFromTelegram.startsWith("completar ") || 
                    messageTextFromTelegram.startsWith("reabrir ") || 
                    messageTextFromTelegram.startsWith("ver "))) {
                
                handleDeveloperTaskAction(chatId, messageTextFromTelegram, currentUser);
            }
            else {
                // Asumir que es un nuevo ítem de tarea (estado por defecto)
                addNewTodoItem(messageTextFromTelegram, chatId, currentUser);
            }
        }
    }
    
    
    private void handleAddingTaskState(long chatId, String messageText, User currentUser) {
        // Guardar la descripción de la tarea
        Map<String, Object> data = temporaryData.getOrDefault(chatId, new HashMap<>());
        data.put("description", messageText);
        temporaryData.put(chatId, data);
        
        // Cambiar al estado de pedir horas
        chatState.put(chatId, STATE_ADDING_TASK_HOURS);
        
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("Ahora, por favor, ingresa las horas estimadas para esta tarea (un número, máximo 4 horas):");
        
        try {
            execute(message);
        } catch (TelegramApiException e) {
            logger.error("Error requesting task hours", e);
        }
    }
    
    private void handleAddingTaskHoursState(long chatId, String messageText, User currentUser, Map<String, Object> data) {
        try {
            // Intentar convertir a número
            double hours = Double.parseDouble(messageText);
            String description = (String) data.get("description");
            
            if (hours <= 0) {
                SendMessage errorMessage = new SendMessage();
                errorMessage.setChatId(chatId);
                errorMessage.setText("❌ El número de horas debe ser mayor que cero. Por favor, intenta nuevamente:");
                execute(errorMessage);
                return;
            }
            
            // Si es mayor a 4 horas, subdividir
            if (hours > 4.0) {
                // Calcular cuántas subtareas se necesitan
                int numberOfSubtasks = (int) Math.ceil(hours / 4.0);
                double hoursPerSubtask = hours / numberOfSubtasks;
                
                StringBuilder resultMessage = new StringBuilder();
                resultMessage.append("⚠️ La tarea excede las 4 horas permitidas. Se ha dividido en ").append(numberOfSubtasks).append(" subtareas:\n\n");
                
                for (int i = 1; i <= numberOfSubtasks; i++) {
                    // Crear subtarea
                    ToDoItem subTask = new ToDoItem();
                    subTask.setDescription(description + " (Parte " + i + " de " + numberOfSubtasks + ")");
                    subTask.setCreation_ts(OffsetDateTime.now());
                    subTask.setDone(false);
                    subTask.setCreatedBy(currentUser.getID());
                    subTask.setAssignedTo(currentUser.getID());
                    subTask.setEstimatedHours(hoursPerSubtask);
                    subTask.setStatus("Pending");
                    
                    toDoItemService.addToDoItem(subTask);
                    
                    resultMessage.append("📌 ").append(subTask.getDescription()).append(" - ").append(String.format("%.2f", hoursPerSubtask)).append(" horas\n");
                }
                
                // Mostrar mensaje de éxito
                SendMessage successMessage = new SendMessage();
                successMessage.setChatId(chatId);
                successMessage.setText(resultMessage.toString());
                execute(successMessage);
                
            } else {
                // Crear una sola tarea si es <= 4 horas
                ToDoItem newTask = new ToDoItem();
                newTask.setDescription(description);
                newTask.setCreation_ts(OffsetDateTime.now());
                newTask.setDone(false);
                newTask.setCreatedBy(currentUser.getID());
                newTask.setAssignedTo(currentUser.getID());
                newTask.setEstimatedHours(hours);
                newTask.setStatus("Pending");
                
                toDoItemService.addToDoItem(newTask);
                
                // Mostrar mensaje de éxito
                SendMessage successMessage = new SendMessage();
                successMessage.setChatId(chatId);
                successMessage.setText("✅ Tarea creada exitosamente:\n\n📌 " + description + "\n⏱️ Horas estimadas: " + hours);
                execute(successMessage);
            }
            
            // Restablecer estado
            chatState.put(chatId, STATE_NONE);
            temporaryData.put(chatId, new HashMap<>());
            
            // Mostrar menú principal
            showMainMenu(chatId, currentUser);
            
        } catch (NumberFormatException e) {
            // Manejar error de formato
            SendMessage errorMessage = new SendMessage();
            errorMessage.setChatId(chatId);
            errorMessage.setText("❌ Por favor, ingresa un número válido para las horas:");
            
            try {
                execute(errorMessage);
            } catch (TelegramApiException ex) {
                logger.error("Error sending error message", ex);
            }
        } catch (TelegramApiException e) {
            logger.error("Error processing task creation", e);
        }
    }
    
    // NUEVA FUNCIONALIDAD: ASIGNAR TAREA A SPRINT
    private void startAssigningTaskToSprint(long chatId) {
        chatState.put(chatId, STATE_ASSIGNING_TASK);
        
        // Obtener tareas no asignadas a sprint
        List<ToDoItem> unassignedTasks = toDoItemService.findAll().stream()
                .filter(item -> item.getSprintId() == null && (item.getIsArchived() == null || item.getIsArchived() == 0))
                .collect(Collectors.toList());
        
        if (unassignedTasks.isEmpty()) {
            SendMessage message = new SendMessage();
            message.setChatId(chatId);
            message.setText("No hay tareas disponibles para asignar a sprints.");
            
            try {
                execute(message);
                chatState.put(chatId, STATE_NONE);
                showMainMenu(chatId, authorizedUsers.get(chatId));
            } catch (TelegramApiException e) {
                logger.error("Error sending message", e);
            }
            return;
        }
        
        StringBuilder tasksListMessage = new StringBuilder();
        tasksListMessage.append("Selecciona la tarea que deseas asignar a un sprint ingresando su ID:\n\n");
        
        for (ToDoItem task : unassignedTasks) {
            tasksListMessage.append("ID: ").append(task.getID()).append(" - ");
            tasksListMessage.append(task.getDescription()).append("\n");
            
            User assignedUser = null;
            if (task.getAssignedTo() != null) {
                ResponseEntity<User> userResponse = userService.getUserById(task.getAssignedTo());
                if (userResponse.getStatusCode() == HttpStatus.OK) {
                    assignedUser = userResponse.getBody();
                }
            }
            
            tasksListMessage.append("   👨‍💻 Asignado a: ").append(assignedUser != null ? assignedUser.getName() : "Nadie").append("\n");
            tasksListMessage.append("   ⏱️ Horas estimadas: ").append(task.getEstimatedHours() != null ? task.getEstimatedHours() : "No definidas").append("\n\n");
        }
        
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(tasksListMessage.toString());
        
        try {
            execute(message);
        } catch (TelegramApiException e) {
            logger.error("Error sending tasks list", e);
        }
    }
    
    private void handleAssigningTaskState(long chatId, String messageText, User currentUser) {
        try {
            // Intentar convertir a número para el ID de tarea
            int taskId = Integer.parseInt(messageText);
            
            // Verificar si la tarea existe
            ResponseEntity<ToDoItem> response = toDoItemService.getItemById(taskId);
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                ToDoItem task = response.getBody();
                
                // Verificar si ya está asignada a un sprint
                if (task.getSprintId() != null) {
                    SendMessage errorMessage = new SendMessage();
                    errorMessage.setChatId(chatId);
                    errorMessage.setText("❌ Esta tarea ya está asignada a un sprint. Por favor, selecciona otra tarea.");
                    execute(errorMessage);
                    chatState.put(chatId, STATE_NONE);
                    showMainMenu(chatId, currentUser);
                    return;
                }
                
                // Guardar ID de tarea temporalmente
                Map<String, Object> data = temporaryData.getOrDefault(chatId, new HashMap<>());
                data.put("taskId", taskId);
                temporaryData.put(chatId, data);
                
                // Cambiar al estado de selección de sprint
                chatState.put(chatId, STATE_SELECTING_SPRINT);
                
                // Mostrar sprints disponibles
                List<Sprint> availableSprints = sprintService.findAll();
                if (availableSprints.isEmpty()) {
                    SendMessage errorMessage = new SendMessage();
                    errorMessage.setChatId(chatId);
                    errorMessage.setText("❌ No hay sprints disponibles. Por favor, crea un sprint primero.");
                    execute(errorMessage);
                    chatState.put(chatId, STATE_NONE);
                    showMainMenu(chatId, currentUser);
                    return;
                }
                
                StringBuilder sprintsListMessage = new StringBuilder();
                sprintsListMessage.append("Selecciona el sprint al cual deseas asignar la tarea ingresando su ID:\n\n");
                
                for (Sprint sprint : availableSprints) {
                    sprintsListMessage.append("ID: ").append(sprint.getId()).append(" - ");
                    sprintsListMessage.append(sprint.getName()).append("\n");
                    sprintsListMessage.append("   📅 Periodo: ").append(sprint.getStartDate()).append(" al ").append(sprint.getEndDate()).append("\n");
                    sprintsListMessage.append("   🔄 Estado: ").append(sprint.getStatus()).append("\n\n");
                }
                
                SendMessage message = new SendMessage();
                message.setChatId(chatId);
                message.setText(sprintsListMessage.toString());
                execute(message);
                
            } else {
                SendMessage errorMessage = new SendMessage();
                errorMessage.setChatId(chatId);
                errorMessage.setText("❌ No se encontró ninguna tarea con ese ID. Por favor, intenta nuevamente.");
                execute(errorMessage);
                startAssigningTaskToSprint(chatId);
            }
            
        } catch (NumberFormatException e) {
            // Manejar error de formato
            SendMessage errorMessage = new SendMessage();
            errorMessage.setChatId(chatId);
            errorMessage.setText("❌ Por favor, ingresa un número válido para el ID de la tarea.");
            
            try {
                execute(errorMessage);
                startAssigningTaskToSprint(chatId);
            } catch (TelegramApiException ex) {
                logger.error("Error sending error message", ex);
            }
        } catch (TelegramApiException e) {
            logger.error("Error processing task assignment", e);
        }
    }
    
    private void handleSelectingSprintState(long chatId, String messageText, User currentUser, Map<String, Object> data) {
        try {
            // Intentar convertir a número para el ID del sprint
            int sprintId = Integer.parseInt(messageText);
            int taskId = (int) data.get("taskId");
            
            // Verificar si el sprint existe
            ResponseEntity<Sprint> sprintResponse = sprintService.getSprintById(sprintId);
            if (sprintResponse.getStatusCode() == HttpStatus.OK && sprintResponse.getBody() != null) {
                Sprint sprint = sprintResponse.getBody();
                
                // Asignar la tarea al sprint
                ToDoItem task = toDoItemService.assignToSprint(taskId, sprintId);
                
                if (task != null) {
                    // Actualizar estado a "In Progress"
                    task.setStatus("In Progress");
                    toDoItemService.updateToDoItem(taskId, task);
                    
                    SendMessage successMessage = new SendMessage();
                    successMessage.setChatId(chatId);
                    successMessage.setText("✅ Tarea asignada exitosamente al sprint \"" + sprint.getName() + "\" y marcada como 'En Progreso'.");
                    execute(successMessage);
                } else {
                    SendMessage errorMessage = new SendMessage();
                    errorMessage.setChatId(chatId);
                    errorMessage.setText("❌ No se pudo asignar la tarea al sprint. Verifica que tanto la tarea como el sprint sean válidos.");
                    execute(errorMessage);
                }
            } else {
                SendMessage errorMessage = new SendMessage();
                errorMessage.setChatId(chatId);
                errorMessage.setText("❌ No se encontró ningún sprint con ese ID. Por favor, intenta nuevamente.");
                execute(errorMessage);
                
                // Volver a mostrar los sprints
                List<Sprint> availableSprints = sprintService.findAll();
                StringBuilder sprintsListMessage = new StringBuilder();
                sprintsListMessage.append("Selecciona el sprint al cual deseas asignar la tarea ingresando su ID:\n\n");
                
                for (Sprint sprint : availableSprints) {
                    sprintsListMessage.append("ID: ").append(sprint.getId()).append(" - ");
                    sprintsListMessage.append(sprint.getName()).append("\n");
                    sprintsListMessage.append("   📅 Periodo: ").append(sprint.getStartDate()).append(" al ").append(sprint.getEndDate()).append("\n");
                    sprintsListMessage.append("   🔄 Estado: ").append(sprint.getStatus()).append("\n\n");
                }
                
                SendMessage message = new SendMessage();
                message.setChatId(chatId);
                message.setText(sprintsListMessage.toString());
                execute(message);
                return;
            }
            
            // Restablecer estado
            chatState.put(chatId, STATE_NONE);
            temporaryData.put(chatId, new HashMap<>());
            
            // Mostrar menú principal
            showMainMenu(chatId, currentUser);
            
        } catch (NumberFormatException e) {
            // Manejar error de formato
            SendMessage errorMessage = new SendMessage();
            errorMessage.setChatId(chatId);
            errorMessage.setText("❌ Por favor, ingresa un número válido para el ID del sprint.");
            
            try {
                execute(errorMessage);
            } catch (TelegramApiException ex) {
                logger.error("Error sending error message", ex);
            }
        } catch (TelegramApiException e) {
            logger.error("Error processing sprint assignment", e);
        }
    }
    
    // NUEVA FUNCIONALIDAD: COMPLETAR TAREA
    private void startCompletingTask(long chatId, User currentUser) {
        chatState.put(chatId, STATE_COMPLETING_TASK);
        
        // Obtener tareas asignadas al usuario y no completadas
        List<ToDoItem> assignedTasks = toDoItemService.findByAssignedTo(currentUser.getID()).stream()
                .filter(item -> !"Completed".equals(item.getStatus()) && (item.getIsArchived() == null || item.getIsArchived() == 0))
                .collect(Collectors.toList());
        
        if (assignedTasks.isEmpty()) {
            SendMessage message = new SendMessage();
            message.setChatId(chatId);
            message.setText("No tienes tareas pendientes para completar.");
            
            try {
                execute(message);
                chatState.put(chatId, STATE_NONE);
                showMainMenu(chatId, currentUser);
            } catch (TelegramApiException e) {
                logger.error("Error sending message", e);
            }
            return;
        }
        
        StringBuilder tasksListMessage = new StringBuilder();
        tasksListMessage.append("Selecciona la tarea que has completado ingresando su ID:\n\n");
        
        for (ToDoItem task : assignedTasks) {
            tasksListMessage.append("ID: ").append(task.getID()).append(" - ");
            tasksListMessage.append(task.getDescription()).append("\n");
            
            // Mostrar estado actual
            tasksListMessage.append("   🔄 Estado: ").append(task.getStatus()).append("\n");
            
            // Obtener información del sprint si existe
            if (task.getSprintId() != null) {
                ResponseEntity<Sprint> sprintResponse = sprintService.getSprintById(task.getSprintId());
                if (sprintResponse.getStatusCode() == HttpStatus.OK && sprintResponse.getBody() != null) {
                    tasksListMessage.append("   📋 Sprint: ").append(sprintResponse.getBody().getName()).append("\n");
                }
            }
            
            // Mostrar prioridad si existe
            if (task.getPriority() != null) {
                tasksListMessage.append("   ⚠️ Prioridad: ").append(task.getPriority()).append("\n");
            }
            
            tasksListMessage.append("   ⏱️ Horas estimadas: ").append(task.getEstimatedHours() != null ? task.getEstimatedHours() : "No definidas").append("\n\n");
        }
        
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(tasksListMessage.toString());
        
        try {
            execute(message);
        } catch (TelegramApiException e) {
            logger.error("Error sending tasks list", e);
        }
    }

    private void handleCompletingTaskState(long chatId, String messageText, User currentUser) {
    try {
        // Intentar convertir a número para el ID de tarea
        int taskId = Integer.parseInt(messageText);
        
        // Verificar si la tarea existe y pertenece al usuario
        ResponseEntity<ToDoItem> response = toDoItemService.getItemById(taskId);
        if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
            ToDoItem task = response.getBody();
            
            // Verificar si está asignada al usuario actual
            if (task.getAssignedTo() == null || !task.getAssignedTo().equals(currentUser.getID())) {
                SendMessage errorMessage = new SendMessage();
                errorMessage.setChatId(chatId);
                errorMessage.setText("❌ Esta tarea no está asignada a ti. Por favor, selecciona una de tus tareas.");
                execute(errorMessage);
                startCompletingTask(chatId, currentUser);
                return;
            }
            
            // Verificar si ya está completada
            if ("Completed".equals(task.getStatus())) {
                SendMessage errorMessage = new SendMessage();
                errorMessage.setChatId(chatId);
                errorMessage.setText("❌ Esta tarea ya está completada. Por favor, selecciona una tarea pendiente.");
                execute(errorMessage);
                startCompletingTask(chatId, currentUser);
                return;
            }
            
            // Guardar ID de tarea temporalmente
            Map<String, Object> data = temporaryData.getOrDefault(chatId, new HashMap<>());
            data.put("taskId", taskId);
            temporaryData.put(chatId, data);
            
            // Cambiar al estado de ingresar horas reales
            chatState.put(chatId, STATE_COMPLETING_TASK_HOURS);
            
            SendMessage hoursMessage = new SendMessage();
            hoursMessage.setChatId(chatId);
            hoursMessage.setText("Por favor, ingresa las horas reales que trabajaste en esta tarea:");
            execute(hoursMessage);
            
        } else {
            SendMessage errorMessage = new SendMessage();
            errorMessage.setChatId(chatId);
            errorMessage.setText("❌ No se encontró ninguna tarea con ese ID. Por favor, intenta nuevamente.");
            execute(errorMessage);
            startCompletingTask(chatId, currentUser);
        }
        
    } catch (NumberFormatException e) {
        // Manejar error de formato
        SendMessage errorMessage = new SendMessage();
        errorMessage.setChatId(chatId);
        errorMessage.setText("❌ Por favor, ingresa un número válido para el ID de la tarea.");
        
        try {
            execute(errorMessage);
            startCompletingTask(chatId, currentUser);
        } catch (TelegramApiException ex) {
            logger.error("Error sending error message", ex);
        }
    } catch (TelegramApiException e) {
        logger.error("Error processing task completion", e);
    }
}
    
private void handleCompletingTaskHoursState(long chatId, String messageText, User currentUser, Map<String, Object> data) {
    try {
        // Intentar convertir a número para las horas
        double hours = Double.parseDouble(messageText);
        int taskId = (int) data.get("taskId");
        
        if (hours <= 0) {
            SendMessage errorMessage = new SendMessage();
            errorMessage.setChatId(chatId);
            errorMessage.setText("❌ El número de horas debe ser mayor que cero. Por favor, intenta nuevamente:");
            execute(errorMessage);
            return;
        }
        
        // Actualizar la tarea
        ResponseEntity<ToDoItem> response = toDoItemService.getItemById(taskId);
        if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
            ToDoItem task = response.getBody();
            
            // Actualizar horas reales
            task.setActualHours(hours);
            
            // Marcar como completada
            task.setStatus("Completed");
            task.setDone(true);
            
            // Guardar cambios
            toDoItemService.updateToDoItem(taskId, task);
            
            // Mensaje de éxito
            StringBuilder successMessage = new StringBuilder();
            successMessage.append("✅ Tarea completada exitosamente:\n\n");
            successMessage.append("📌 ").append(task.getDescription()).append("\n");
            
            // Mostrar prioridad si existe
            if (task.getPriority() != null) {
                successMessage.append("⚠️ Prioridad: ").append(task.getPriority()).append("\n");
            }
            
            successMessage.append("⏱️ Horas estimadas: ").append(task.getEstimatedHours() != null ? task.getEstimatedHours() : "No definidas").append("\n");
            successMessage.append("⏱️ Horas reales: ").append(hours).append("\n");
            
            // Calcular diferencia de horas si hay estimación
            if (task.getEstimatedHours() != null) {
                double diff = hours - task.getEstimatedHours();
                if (Math.abs(diff) < 0.01) {
                    successMessage.append("🎯 Completada exactamente en el tiempo estimado.\n");
                } else if (diff > 0) {
                    successMessage.append("⚠️ Excedió el tiempo estimado por ").append(String.format("%.2f", diff)).append(" horas.\n");
                } else {
                    successMessage.append("👍 Completada ").append(String.format("%.2f", Math.abs(diff))).append(" horas antes de lo estimado.\n");
                }
            }
            
            // Información del sprint si existe
            if (task.getSprintId() != null) {
                ResponseEntity<Sprint> sprintResponse = sprintService.getSprintById(task.getSprintId());
                if (sprintResponse.getStatusCode() == HttpStatus.OK && sprintResponse.getBody() != null) {
                    successMessage.append("📋 Sprint: ").append(sprintResponse.getBody().getName()).append("\n");
                }
            }
            
            SendMessage message = new SendMessage();
            message.setChatId(chatId);
            message.setText(successMessage.toString());
            execute(message);
        }
        
        // Restablecer estado
        chatState.put(chatId, STATE_NONE);
        temporaryData.put(chatId, new HashMap<>());
        
        // Mostrar menú principal
        showMainMenu(chatId, currentUser);
        
    } catch (NumberFormatException e) {
        // Manejar error de formato
        SendMessage errorMessage = new SendMessage();
        errorMessage.setChatId(chatId);
        errorMessage.setText("❌ Por favor, ingresa un número válido para las horas.");
        
        try {
            execute(errorMessage);
        } catch (TelegramApiException ex) {
            logger.error("Error sending error message", ex);
        }
    } catch (TelegramApiException e) {
        logger.error("Error processing hours input", e);
    }
}
    
    // Métodos auxiliares para ver información adicional
    private void showAssignedTasks(long chatId, User currentUser) {
        List<ToDoItem> assignedTasks = toDoItemService.findByAssignedTo(currentUser.getID()).stream()
                .filter(item -> (item.getIsArchived() == null || item.getIsArchived() == 0))
                .collect(Collectors.toList());
        
        if (assignedTasks.isEmpty()) {
            SendMessage message = new SendMessage();
            message.setChatId(chatId);
            message.setText("No tienes tareas asignadas actualmente.");
            
            try {
                execute(message);
                showMainMenu(chatId, currentUser);
            } catch (TelegramApiException e) {
                logger.error("Error sending message", e);
            }
            return;
        }
        
        StringBuilder tasksListMessage = new StringBuilder();
        tasksListMessage.append("📋 TAREAS ASIGNADAS A TI:\n\n");
        
        // Agrupar por estado
        Map<String, List<ToDoItem>> tasksByStatus = assignedTasks.stream()
                .collect(Collectors.groupingBy(ToDoItem::getStatus));
        
        for (String status : new String[]{"Pending", "In Progress", "In Review", "Completed"}) {
            List<ToDoItem> tasks = tasksByStatus.getOrDefault(status, new ArrayList<>());
            if (!tasks.isEmpty()) {
                tasksListMessage.append("🔹 ").append(status.toUpperCase()).append(" (").append(tasks.size()).append(")\n\n");
                
                for (ToDoItem task : tasks) {
                    tasksListMessage.append("ID: ").append(task.getID()).append(" - ");
                    tasksListMessage.append(task.getDescription()).append("\n");
                    
                    // Mostrar sprint si existe
                    if (task.getSprintId() != null) {
                        ResponseEntity<Sprint> sprintResponse = sprintService.getSprintById(task.getSprintId());
                        if (sprintResponse.getStatusCode() == HttpStatus.OK && sprintResponse.getBody() != null) {
                            tasksListMessage.append("   📋 Sprint: ").append(sprintResponse.getBody().getName()).append("\n");
                        }
                    }
                    
                    tasksListMessage.append("   ⏱️ Horas estimadas: ").append(task.getEstimatedHours() != null ? task.getEstimatedHours() : "No definidas").append("\n");
                    if (task.getActualHours() != null) {
                        tasksListMessage.append("   ⏱️ Horas reales: ").append(task.getActualHours()).append("\n");
                    }
                    tasksListMessage.append("\n");
                }
            }
        }
        
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(tasksListMessage.toString());
        
        try {
            execute(message);
            showMainMenu(chatId, currentUser);
        } catch (TelegramApiException e) {
            logger.error("Error sending assigned tasks", e);
        }
    }
    
    private void showAvailableSprints(long chatId) {
        List<Sprint> sprints = sprintService.findAll();
        
        if (sprints.isEmpty()) {
            SendMessage message = new SendMessage();
            message.setChatId(chatId);
            message.setText("No hay sprints disponibles en el sistema.");
            
            try {
                execute(message);
                showMainMenu(chatId, authorizedUsers.get(chatId));
            } catch (TelegramApiException e) {
                logger.error("Error sending message", e);
            }
            return;
        }
        
        StringBuilder sprintsListMessage = new StringBuilder();
        sprintsListMessage.append("📋 SPRINTS DISPONIBLES:\n\n");
        
        for (Sprint sprint : sprints) {
            sprintsListMessage.append("ID: ").append(sprint.getId()).append(" - ");
            sprintsListMessage.append(sprint.getName()).append("\n");
            sprintsListMessage.append("   📅 Periodo: ").append(sprint.getStartDate()).append(" al ").append(sprint.getEndDate()).append("\n");
            sprintsListMessage.append("   🔄 Estado: ").append(sprint.getStatus()).append("\n");
            
            // Contar tareas en este sprint
            List<ToDoItem> sprintTasks = toDoItemService.findBySprintId(sprint.getId());
            
            // Contar por estado
            long pendingCount = sprintTasks.stream().filter(t -> "Pending".equals(t.getStatus())).count();
            long inProgressCount = sprintTasks.stream().filter(t -> "In Progress".equals(t.getStatus())).count();
            long completedCount = sprintTasks.stream().filter(t -> "Completed".equals(t.getStatus())).count();
            
            sprintsListMessage.append("   📊 Tareas: ").append(sprintTasks.size())
                .append(" (⏳ ").append(pendingCount)
                .append(" | 🔄 ").append(inProgressCount)
                .append(" | ✅ ").append(completedCount).append(")\n\n");
        }
        
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(sprintsListMessage.toString());
        
        try {
            execute(message);
            showMainMenu(chatId, authorizedUsers.get(chatId));
        } catch (TelegramApiException e) {
            logger.error("Error sending sprints list", e);
        }
    }
    
    private void showHoursBySprintReport(long chatId) {
        List<Sprint> sprints = sprintService.findAll();
        
        if (sprints.isEmpty()) {
            SendMessage message = new SendMessage();
            message.setChatId(chatId);
            message.setText("No hay sprints disponibles para generar el reporte.");
            
            try {
                execute(message);
                showMainMenu(chatId, authorizedUsers.get(chatId));
            } catch (TelegramApiException e) {
                logger.error("Error sending message", e);
            }
            return;
        }
        
        StringBuilder reportMessage = new StringBuilder();
        reportMessage.append("📊 REPORTE DE HORAS POR SPRINT:\n\n");
        
        for (Sprint sprint : sprints) {
            reportMessage.append("🔸 ").append(sprint.getName()).append("\n");
            
            List<ToDoItem> sprintTasks = toDoItemService.findBySprintId(sprint.getId());
            
            double totalEstimated = sprintTasks.stream()
                    .filter(t -> t.getEstimatedHours() != null)
                    .mapToDouble(ToDoItem::getEstimatedHours)
                    .sum();
                    
            double totalActual = sprintTasks.stream()
                    .filter(t -> t.getActualHours() != null)
                    .mapToDouble(ToDoItem::getActualHours)
                    .sum();
                    
            double totalPending = sprintTasks.stream()
                    .filter(t -> !"Completed".equals(t.getStatus()) && t.getEstimatedHours() != null)
                    .mapToDouble(ToDoItem::getEstimatedHours)
                    .sum();
                    
            reportMessage.append("   ⏱️ Total horas estimadas: ").append(String.format("%.2f", totalEstimated)).append("\n");
            reportMessage.append("   ⏱️ Total horas trabajadas: ").append(String.format("%.2f", totalActual)).append("\n");
            reportMessage.append("   ⏳ Horas pendientes estimadas: ").append(String.format("%.2f", totalPending)).append("\n");
            
            // Calcular eficiencia en tareas completadas
            long completedTasksCount = sprintTasks.stream()
                    .filter(t -> "Completed".equals(t.getStatus()) && t.getEstimatedHours() != null && t.getActualHours() != null)
                    .count();
                    
            if (completedTasksCount > 0) {
                double estimatedCompleted = sprintTasks.stream()
                        .filter(t -> "Completed".equals(t.getStatus()) && t.getEstimatedHours() != null)
                        .mapToDouble(ToDoItem::getEstimatedHours)
                        .sum();
                        
                double actualCompleted = sprintTasks.stream()
                        .filter(t -> "Completed".equals(t.getStatus()) && t.getActualHours() != null)
                        .mapToDouble(ToDoItem::getActualHours)
                        .sum();
                        
                double efficiency = (estimatedCompleted / actualCompleted) * 100;
                reportMessage.append("   📈 Eficiencia: ").append(String.format("%.2f", efficiency)).append("%\n");
            }
            
            reportMessage.append("\n");
        }
        
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(reportMessage.toString());
        
        try {
            execute(message);
            showMainMenu(chatId, authorizedUsers.get(chatId));
        } catch (TelegramApiException e) {
            logger.error("Error sending hours report", e);
        }
    }
    
    private void showTasksSummary(long chatId) {
        // Obtener todas las tareas
        List<ToDoItem> allTasks = toDoItemService.findAll();
        
        StringBuilder summaryMessage = new StringBuilder();
        summaryMessage.append("📊 RESUMEN DE TAREAS:\n\n");
        
        // Contar tareas por estado
        long pendingCount = allTasks.stream().filter(t -> "Pending".equals(t.getStatus())).count();
        long inProgressCount = allTasks.stream().filter(t -> "In Progress".equals(t.getStatus())).count();
        long inReviewCount = allTasks.stream().filter(t -> "In Review".equals(t.getStatus())).count();
        long completedCount = allTasks.stream().filter(t -> "Completed".equals(t.getStatus())).count();
        
        summaryMessage.append("🔹 Total de tareas: ").append(allTasks.size()).append("\n");
        summaryMessage.append("   ⏳ Pendientes: ").append(pendingCount).append("\n");
        summaryMessage.append("   🔄 En progreso: ").append(inProgressCount).append("\n");
        summaryMessage.append("   👁️ En revisión: ").append(inReviewCount).append("\n");
        summaryMessage.append("   ✅ Completadas: ").append(completedCount).append("\n\n");
        
        // Obtener tareas sin asignar
        long unassignedCount = allTasks.stream().filter(t -> t.getAssignedTo() == null).count();
        summaryMessage.append("🔹 Tareas sin asignar: ").append(unassignedCount).append("\n\n");
        
        // Obtener tareas sin sprint
        long withoutSprintCount = allTasks.stream().filter(t -> t.getSprintId() == null).count();
        summaryMessage.append("🔹 Tareas sin sprint: ").append(withoutSprintCount).append("\n\n");
        
        // Obtener tareas con horas estimadas pero sin horas reales (incompletas)
        long estimatedButNotCompletedCount = allTasks.stream()
                .filter(t -> t.getEstimatedHours() != null && t.getActualHours() == null)
                .count();
        summaryMessage.append("🔹 Tareas estimadas pero no completadas: ").append(estimatedButNotCompletedCount).append("\n\n");
        
        // Obtener desarrolladores con tareas asignadas
        Map<Integer, Long> taskCountByDeveloper = allTasks.stream()
                .filter(t -> t.getAssignedTo() != null)
                .collect(Collectors.groupingBy(ToDoItem::getAssignedTo, Collectors.counting()));
                
        summaryMessage.append("🔹 Tareas por desarrollador:\n\n");
        
        for (Map.Entry<Integer, Long> entry : taskCountByDeveloper.entrySet()) {
            ResponseEntity<User> userResponse = userService.getUserById(entry.getKey());
            if (userResponse.getStatusCode() == HttpStatus.OK && userResponse.getBody() != null) {
                User developer = userResponse.getBody();
                summaryMessage.append("   👨‍💻 ").append(developer.getName())
                        .append(": ").append(entry.getValue()).append(" tareas\n");
            }
        }
        
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(summaryMessage.toString());
        
        try {
            execute(message);
            showMainMenu(chatId, authorizedUsers.get(chatId));
        } catch (TelegramApiException e) {
            logger.error("Error sending tasks summary", e);
        }
    }

    // Métodos originales existentes
    private void processDoneCommand(String messageText, long chatId) {
        String done = messageText.substring(0, messageText.indexOf(BotLabels.DASH.getLabel()));
        Integer id = Integer.valueOf(done);

        try {
            ResponseEntity<ToDoItem> response = toDoItemService.getItemById(id);
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                ToDoItem item = response.getBody();
                item.setDone(true);
                item.setStatus("Completed"); // Actualizar también el estado
                toDoItemService.updateToDoItem(id, item);
                BotHelper.sendMessageToTelegram(chatId, BotMessages.ITEM_DONE.getMessage(), this);
            }
        } catch (Exception e) {
            logger.error(e.getLocalizedMessage(), e);
        }
    }

    private void processUndoCommand(String messageText, long chatId) {
        String undo = messageText.substring(0, messageText.indexOf(BotLabels.DASH.getLabel()));
        Integer id = Integer.valueOf(undo);

        try {
            ResponseEntity<ToDoItem> response = toDoItemService.getItemById(id);
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                ToDoItem item = response.getBody();
                item.setDone(false);
                item.setStatus("Pending"); // Restablecer el estado
                toDoItemService.updateToDoItem(id, item);
                BotHelper.sendMessageToTelegram(chatId, BotMessages.ITEM_UNDONE.getMessage(), this);
            }
        } catch (Exception e) {
            logger.error(e.getLocalizedMessage(), e);
        }
    }

    private void processDeleteCommand(String messageText, long chatId) {
        String delete = messageText.substring(0, messageText.indexOf(BotLabels.DASH.getLabel()));
        Integer id = Integer.valueOf(delete);

        try {
            toDoItemService.deleteToDoItem(id);
            BotHelper.sendMessageToTelegram(chatId, BotMessages.ITEM_DELETED.getMessage(), this);
        } catch (Exception e) {
            logger.error(e.getLocalizedMessage(), e);
        }
    }

    private void showTodoList(long chatId, User currentUser) {
        List<ToDoItem> allItems;
        
        // Si es un Manager, mostrar todas las tareas
        if ("Manager".equals(currentUser.getRole())) {
            allItems = toDoItemService.findAll();
        } else {
            // Si es un Developer, mostrar solo las tareas asignadas a él
            allItems = toDoItemService.findByAssignedTo(currentUser.getID());
        }
        
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setResizeKeyboard(true);
        List<KeyboardRow> keyboard = new ArrayList<>();

        // command back to main screen
        KeyboardRow mainScreenRowTop = new KeyboardRow();
        mainScreenRowTop.add(BotLabels.SHOW_MAIN_SCREEN.getLabel());
        keyboard.add(mainScreenRowTop);

        KeyboardRow firstRow = new KeyboardRow();
        firstRow.add(BotLabels.ADD_NEW_ITEM.getLabel());
        keyboard.add(firstRow);

        KeyboardRow myTodoListTitleRow = new KeyboardRow();
        myTodoListTitleRow.add(BotLabels.MY_TODO_LIST.getLabel());
        keyboard.add(myTodoListTitleRow);

        List<ToDoItem> activeItems = allItems.stream().filter(item -> item.isDone() == false)
                .collect(Collectors.toList());

        for (ToDoItem item : activeItems) {
            KeyboardRow currentRow = new KeyboardRow();
            currentRow.add(item.getDescription());
            currentRow.add(item.getID() + BotLabels.DASH.getLabel() + BotLabels.DONE.getLabel());
            keyboard.add(currentRow);
        }

        List<ToDoItem> doneItems = allItems.stream().filter(item -> item.isDone() == true)
                .collect(Collectors.toList());

                for (ToDoItem item : doneItems) {
                    KeyboardRow currentRow = new KeyboardRow();
                    currentRow.add(item.getDescription());
                    currentRow.add(item.getID() + BotLabels.DASH.getLabel() + BotLabels.UNDO.getLabel());
                    currentRow.add(item.getID() + BotLabels.DASH.getLabel() + BotLabels.DELETE.getLabel());
                    keyboard.add(currentRow);
                }
        
                // command back to main screen
                KeyboardRow mainScreenRowBottom = new KeyboardRow();
                mainScreenRowBottom.add(BotLabels.SHOW_MAIN_SCREEN.getLabel());
                keyboard.add(mainScreenRowBottom);
        
                keyboardMarkup.setKeyboard(keyboard);
        
                SendMessage messageToTelegram = new SendMessage();
                messageToTelegram.setChatId(chatId);
                messageToTelegram.setText(BotLabels.MY_TODO_LIST.getLabel());
                messageToTelegram.setReplyMarkup(keyboardMarkup);
        
                try {
                    execute(messageToTelegram);
                } catch (TelegramApiException e) {
                    logger.error(e.getLocalizedMessage(), e);
                }
            }
        
            private void requestNewTodoItem(long chatId) {
                try {
                    SendMessage messageToTelegram = new SendMessage();
                    messageToTelegram.setChatId(chatId);
                    messageToTelegram.setText(BotMessages.TYPE_NEW_TODO_ITEM.getMessage());
                    // hide keyboard
                    ReplyKeyboardRemove keyboardMarkup = new ReplyKeyboardRemove(true);
                    messageToTelegram.setReplyMarkup(keyboardMarkup);
        
                    // send message
                    execute(messageToTelegram);
                } catch (Exception e) {
                    logger.error(e.getLocalizedMessage(), e);
                }
            }
        
            private void addNewTodoItem(String description, long chatId, User currentUser) {
                try {
                    ToDoItem newItem = new ToDoItem();
                    newItem.setDescription(description);
                    newItem.setCreation_ts(OffsetDateTime.now());
                    newItem.setDone(false);
                    newItem.setCreatedBy(currentUser.getID());
                    newItem.setStatus("Pending");
                    
                    // Si es un developer, autoasignarse la tarea
                    if ("Developer".equals(currentUser.getRole())) {
                        newItem.setAssignedTo(currentUser.getID());
                    }
                    
                    toDoItemService.addToDoItem(newItem);
        
                    SendMessage messageToTelegram = new SendMessage();
                    messageToTelegram.setChatId(chatId);
                    messageToTelegram.setText(BotMessages.NEW_ITEM_ADDED.getMessage());
        
                    execute(messageToTelegram);
                    
                    // Mostrar el menú principal después de agregar la tarea
                    showMainMenu(chatId, currentUser);
                } catch (Exception e) {
                    logger.error(e.getLocalizedMessage(), e);
                }
            }

            private void showSprintsForTaskManagement(long chatId) {
                // Establecer el estado
                chatState.put(chatId, STATE_VIEWING_SPRINTS);
                temporaryData.put(chatId, new HashMap<>());
                
                List<Sprint> sprints = sprintService.findAll();
                
                StringBuilder sprintsMessage = new StringBuilder();
                sprintsMessage.append("📋 SELECCIONA UN SPRINT INGRESANDO SU ID:\n\n");
                
                if (sprints.isEmpty()) {
                    sprintsMessage.append("No hay sprints disponibles en el sistema.");
                } else {
                    for (Sprint sprint : sprints) {
                        sprintsMessage.append("ID: ").append(sprint.getId()).append(" - ");
                        sprintsMessage.append(sprint.getName()).append("\n");
                        sprintsMessage.append("   📅 Periodo: ").append(sprint.getStartDate()).append(" al ").append(sprint.getEndDate()).append("\n");
                        sprintsMessage.append("   🔄 Estado: ").append(sprint.getStatus()).append("\n\n");
                    }
                }
                
                // Añadir opción para ver todas las tareas sin sprint
                sprintsMessage.append("📌 Para ver todas las tareas sin asignar a sprint, escribe '0'\n");
                sprintsMessage.append("🔙 Para volver al menú principal, escribe 'menu'");
                
                SendMessage message = new SendMessage();
                message.setChatId(chatId);
                message.setText(sprintsMessage.toString());
                
                try {
                    execute(message);
                } catch (TelegramApiException e) {
                    logger.error("Error sending sprints list", e);
                }
            }
            
            private void handleViewingSprintsState(long chatId, String messageText, User currentUser) {
                // Si el usuario quiere volver al menú principal
                if (messageText.equalsIgnoreCase("menu")) {
                    chatState.put(chatId, STATE_NONE);
                    showMainMenu(chatId, currentUser);
                    return;
                }
                
                try {
                    int sprintId = Integer.parseInt(messageText);
                    
                    // Guardar datos para el siguiente estado
                    Map<String, Object> data = temporaryData.getOrDefault(chatId, new HashMap<>());
                    data.put("sprintId", sprintId);
                    temporaryData.put(chatId, data);
                    
                    // Cambiar estado
                    chatState.put(chatId, STATE_VIEWING_SPRINT_TASKS);
                    
                    // Mostrar tareas del sprint o tareas sin sprint
                    if (sprintId == 0) {
                        showTasksWithoutSprint(chatId);
                    } else {
                        showTasksForSprint(chatId, sprintId);
                    }
                    
                } catch (NumberFormatException e) {
                    SendMessage errorMessage = new SendMessage();
                    errorMessage.setChatId(chatId);
                    errorMessage.setText("❌ Por favor, ingresa un número válido para el ID del sprint o 'menu' para volver.");
                    
                    try {
                        execute(errorMessage);
                    } catch (TelegramApiException ex) {
                        logger.error("Error sending error message", ex);
                    }
                }
            }
            
            private void showTasksWithoutSprint(long chatId) {
                List<ToDoItem> tasksWithoutSprint = toDoItemService.findAll().stream()
                        .filter(item -> item.getSprintId() == null && (item.getIsArchived() == null || item.getIsArchived() == 0))
                        .collect(Collectors.toList());
                        
                showTasksList(chatId, tasksWithoutSprint, "TAREAS SIN SPRINT ASIGNADO");
            }
            
            private void showTasksForSprint(long chatId, int sprintId) {
                // Verificar si el sprint existe
                ResponseEntity<Sprint> sprintResponse = sprintService.getSprintById(sprintId);
                if (sprintResponse.getStatusCode() != HttpStatus.OK || sprintResponse.getBody() == null) {
                    SendMessage errorMessage = new SendMessage();
                    errorMessage.setChatId(chatId);
                    errorMessage.setText("❌ No se encontró ningún sprint con ese ID. Por favor, intenta nuevamente.");
                    
                    try {
                        execute(errorMessage);
                        showSprintsForTaskManagement(chatId);
                    } catch (TelegramApiException e) {
                        logger.error("Error sending error message", e);
                    }
                    return;
                }
                
                Sprint sprint = sprintResponse.getBody();
                List<ToDoItem> sprintTasks = toDoItemService.findBySprintId(sprintId);
                
                showTasksList(chatId, sprintTasks, "TAREAS DEL SPRINT: " + sprint.getName());
            }
            
            private void showTasksList(long chatId, List<ToDoItem> tasks, String title) {
                StringBuilder tasksMessage = new StringBuilder();
                tasksMessage.append("📋 ").append(title).append("\n\n");
                
                if (tasks.isEmpty()) {
                    tasksMessage.append("No hay tareas disponibles en esta categoría.");
                } else {
                    tasksMessage.append("Selecciona una tarea ingresando su ID para modificarla:\n\n");
                    
                    for (ToDoItem task : tasks) {
                        tasksMessage.append("ID: ").append(task.getID()).append(" - ");
                        tasksMessage.append(task.getDescription()).append("\n");
                        
                        // Mostrar desarrollador asignado
                        if (task.getAssignedTo() != null) {
                            ResponseEntity<User> userResponse = userService.getUserById(task.getAssignedTo());
                            if (userResponse.getStatusCode() == HttpStatus.OK && userResponse.getBody() != null) {
                                User developer = userResponse.getBody();
                                tasksMessage.append("   👨‍💻 Asignado a: ").append(developer.getName()).append("\n");
                            } else {
                                tasksMessage.append("   👨‍💻 Asignado a: Usuario desconocido\n");
                            }
                        } else {
                            tasksMessage.append("   👨‍💻 Sin asignar\n");
                        }
                        
                        // Mostrar estado y horas
                        tasksMessage.append("   🔄 Estado: ").append(task.getStatus()).append("\n");
                        if (task.getEstimatedHours() != null) {
                            tasksMessage.append("   ⏱️ Horas estimadas: ").append(task.getEstimatedHours()).append("\n");
                        }
                        if (task.getActualHours() != null) {
                            tasksMessage.append("   ⏱️ Horas reales: ").append(task.getActualHours()).append("\n");
                        }
                        tasksMessage.append("\n");
                    }
                }
                
                tasksMessage.append("🔙 Para volver a la lista de sprints, escribe 'sprints'\n");
                tasksMessage.append("🏠 Para volver al menú principal, escribe 'menu'");
                
                SendMessage message = new SendMessage();
                message.setChatId(chatId);
                message.setText(tasksMessage.toString());
                
                try {
                    execute(message);
                } catch (TelegramApiException e) {
                    logger.error("Error sending tasks list", e);
                }
            }
            
            private void handleViewingSprintTasksState(long chatId, String messageText, User currentUser, Map<String, Object> data) {
                // Si el usuario quiere volver al menú principal
                if (messageText.equalsIgnoreCase("menu")) {
                    chatState.put(chatId, STATE_NONE);
                    showMainMenu(chatId, currentUser);
                    return;
                }
                
                // Si el usuario quiere volver a la lista de sprints
                if (messageText.equalsIgnoreCase("sprints")) {
                    showSprintsForTaskManagement(chatId);
                    return;
                }
                
                try {
                    int taskId = Integer.parseInt(messageText);
                    
                    // Verificar si la tarea existe
                    ResponseEntity<ToDoItem> response = toDoItemService.getItemById(taskId);
                    if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                        ToDoItem task = response.getBody();
                        
                        // Guardar ID de tarea para el siguiente estado
                        data.put("taskId", taskId);
                        temporaryData.put(chatId, data);
                        
                        // Cambiar estado
                        chatState.put(chatId, STATE_MODIFYING_TASK);
                        
                        // Mostrar opciones de modificación
                        showTaskModificationOptions(chatId, task);
                        
                    } else {
                        SendMessage errorMessage = new SendMessage();
                        errorMessage.setChatId(chatId);
                        errorMessage.setText("❌ No se encontró ninguna tarea con ese ID. Por favor, intenta nuevamente.");
                        execute(errorMessage);
                        
                        // Volver a mostrar las tareas del sprint actual
                        int sprintId = (int) data.get("sprintId");
                        if (sprintId == 0) {
                            showTasksWithoutSprint(chatId);
                        } else {
                            showTasksForSprint(chatId, sprintId);
                        }
                    }
                    
                } catch (NumberFormatException e) {
                    SendMessage errorMessage = new SendMessage();
                    errorMessage.setChatId(chatId);
                    errorMessage.setText("❌ Por favor, ingresa un número válido para el ID de la tarea, 'sprints' para volver a la lista de sprints, o 'menu' para volver al menú principal.");
                    
                    try {
                        execute(errorMessage);
                    } catch (TelegramApiException ex) {
                        logger.error("Error sending error message", ex);
                    }
                } catch (TelegramApiException e) {
                    logger.error("Error processing task selection", e);
                }
            }
            
            private void showTaskModificationOptions(long chatId, ToDoItem task) {
                StringBuilder optionsMessage = new StringBuilder();
                optionsMessage.append("📝 OPCIONES PARA LA TAREA:\n\n");
                optionsMessage.append("ID: ").append(task.getID()).append("\n");
                optionsMessage.append("Descripción: ").append(task.getDescription()).append("\n");
                optionsMessage.append("Estado actual: ").append(task.getStatus()).append("\n\n");
                
                optionsMessage.append("Selecciona una opción:\n\n");
                optionsMessage.append("1️⃣ Cambiar nombre/descripción\n");
                optionsMessage.append("2️⃣ Cambiar estado (Pending, In Progress, In Review, Completed)\n");
                optionsMessage.append("3️⃣ Eliminar tarea\n\n");
                
                optionsMessage.append("🔙 Para volver a la lista de tareas, escribe 'tareas'\n");
                optionsMessage.append("🏠 Para volver al menú principal, escribe 'menu'");
                
                SendMessage message = new SendMessage();
                message.setChatId(chatId);
                message.setText(optionsMessage.toString());
                
                try {
                    execute(message);
                } catch (TelegramApiException e) {
                    logger.error("Error sending task modification options", e);
                }
            }
            
            private void handleModifyingTaskState(long chatId, String messageText, User currentUser, Map<String, Object> data) {
                // Si el usuario quiere volver al menú principal
                if (messageText.equalsIgnoreCase("menu")) {
                    chatState.put(chatId, STATE_NONE);
                    showMainMenu(chatId, currentUser);
                    return;
                }
                
                // Si el usuario quiere volver a la lista de tareas
                if (messageText.equalsIgnoreCase("tareas")) {
                    chatState.put(chatId, STATE_VIEWING_SPRINT_TASKS);
                    int sprintId = (int) data.get("sprintId");
                    if (sprintId == 0) {
                        showTasksWithoutSprint(chatId);
                    } else {
                        showTasksForSprint(chatId, sprintId);
                    }
                    return;
                }
                
                int taskId = (int) data.get("taskId");
                
                switch (messageText) {
                    case "1":
                        // Cambiar nombre/descripción
                        chatState.put(chatId, STATE_CHANGING_TASK_NAME);
                        SendMessage nameMessage = new SendMessage();
                        nameMessage.setChatId(chatId);
                        nameMessage.setText("Por favor, ingresa la nueva descripción para la tarea:");
                        try {
                            execute(nameMessage);
                        } catch (TelegramApiException e) {
                            logger.error("Error sending message", e);
                        }
                        break;
                        
                    case "2":
                        // Cambiar estado
                        chatState.put(chatId, STATE_CHANGING_TASK_STATUS);
                        SendMessage statusMessage = new SendMessage();
                        statusMessage.setChatId(chatId);
                        statusMessage.setText("Selecciona el nuevo estado para la tarea:\n\n" +
                                "1️⃣ Pending\n" +
                                "2️⃣ In Progress\n" +
                                "3️⃣ In Review\n" +
                                "4️⃣ Completed");
                        try {
                            execute(statusMessage);
                        } catch (TelegramApiException e) {
                            logger.error("Error sending message", e);
                        }
                        break;
                        
                    case "3":
                        // Eliminar tarea
                        try {
                            toDoItemService.deleteToDoItem(taskId);
                            SendMessage successMessage = new SendMessage();
                            successMessage.setChatId(chatId);
                            successMessage.setText("✅ Tarea eliminada exitosamente.");
                            execute(successMessage);
                            
                            // Volver a mostrar las tareas del sprint
                            chatState.put(chatId, STATE_VIEWING_SPRINT_TASKS);
                            int sprintId = (int) data.get("sprintId");
                            if (sprintId == 0) {
                                showTasksWithoutSprint(chatId);
                            } else {
                                showTasksForSprint(chatId, sprintId);
                            }
                        } catch (Exception e) {
                            logger.error("Error deleting task", e);
                            SendMessage errorMessage = new SendMessage();
                            errorMessage.setChatId(chatId);
                            errorMessage.setText("❌ Error al eliminar la tarea. Por favor, intenta nuevamente.");
                            try {
                                execute(errorMessage);
                            } catch (TelegramApiException ex) {
                                logger.error("Error sending error message", ex);
                            }
                        }
                        break;
                        
                    default:
                        SendMessage errorMessage = new SendMessage();
                        errorMessage.setChatId(chatId);
                        errorMessage.setText("❌ Opción no válida. Por favor, selecciona una opción del 1 al 3, 'tareas' para volver a la lista de tareas, o 'menu' para volver al menú principal.");
                        try {
                            execute(errorMessage);
                        } catch (TelegramApiException e) {
                            logger.error("Error sending error message", e);
                        }
                        break;
                }
            }
            
            private void handleChangingTaskNameState(long chatId, String messageText, User currentUser, Map<String, Object> data) {
                int taskId = (int) data.get("taskId");
                
                try {
                    // Obtener la tarea actual
                    ResponseEntity<ToDoItem> response = toDoItemService.getItemById(taskId);
                    if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                        ToDoItem task = response.getBody();
                        
                        // Actualizar descripción
                        task.setDescription(messageText);
                        
                        // Guardar cambios
                        toDoItemService.updateToDoItem(taskId, task);
                        
                        // Mensaje de éxito
                        SendMessage successMessage = new SendMessage();
                        successMessage.setChatId(chatId);
                        successMessage.setText("✅ Descripción actualizada exitosamente.");
                        execute(successMessage);
                        
                        // Volver a mostrar opciones de modificación
                        chatState.put(chatId, STATE_MODIFYING_TASK);
                        showTaskModificationOptions(chatId, task);
                    } else {
                        SendMessage errorMessage = new SendMessage();
                        errorMessage.setChatId(chatId);
                        errorMessage.setText("❌ No se pudo encontrar la tarea para actualizar.");
                        execute(errorMessage);
                        
                        // Volver al menú principal
                        chatState.put(chatId, STATE_NONE);
                        showMainMenu(chatId, currentUser);
                    }
                } catch (Exception e) {
                    logger.error("Error updating task description", e);
                    SendMessage errorMessage = new SendMessage();
                    errorMessage.setChatId(chatId);
                    errorMessage.setText("❌ Error al actualizar la descripción. Por favor, intenta nuevamente.");
                    try {
                        execute(errorMessage);
                    } catch (TelegramApiException ex) {
                        logger.error("Error sending error message", ex);
                    }
                }
            }
            
            private void handleChangingTaskStatusState(long chatId, String messageText, User currentUser, Map<String, Object> data) {
                int taskId = (int) data.get("taskId");
                String newStatus = null;
                
                switch (messageText) {
                    case "1":
                        newStatus = "Pending";
                        break;
                    case "2":
                        newStatus = "In Progress";
                        break;
                    case "3":
                        newStatus = "In Review";
                        break;
                    case "4":
                        newStatus = "Completed";
                        break;
                    default:
                        SendMessage errorMessage = new SendMessage();
                        errorMessage.setChatId(chatId);
                        errorMessage.setText("❌ Opción no válida. Por favor, selecciona una opción del 1 al 4.");
                        try {
                            execute(errorMessage);
                        } catch (TelegramApiException e) {
                            logger.error("Error sending error message", e);
                        }
                        return;
                }
                
                try {
                    // Obtener la tarea actual
                    ResponseEntity<ToDoItem> response = toDoItemService.getItemById(taskId);
                    if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                        ToDoItem task = response.getBody();
                        
                        // Actualizar estado
                        task.setStatus(newStatus);
                        
                        // Si cambia a Completed, actualizar también el campo done
                        if ("Completed".equals(newStatus)) {
                            task.setDone(true);
                        } else {
                            task.setDone(false);
                        }
                        
                        // Guardar cambios
                        toDoItemService.updateToDoItem(taskId, task);
                        
                        // Mensaje de éxito
                        SendMessage successMessage = new SendMessage();
                        successMessage.setChatId(chatId);
                        successMessage.setText("✅ Estado actualizado exitosamente a '" + newStatus + "'.");
                        execute(successMessage);
                        
                        // Volver a mostrar opciones de modificación
                        chatState.put(chatId, STATE_MODIFYING_TASK);
                        showTaskModificationOptions(chatId, task);
                    } else {
                        SendMessage errorMessage = new SendMessage();
                        errorMessage.setChatId(chatId);
                        errorMessage.setText("❌ No se pudo encontrar la tarea para actualizar.");
                        execute(errorMessage);
                        
                        // Volver al menú principal
                        chatState.put(chatId, STATE_NONE);
                        showMainMenu(chatId, currentUser);
                    }
                } catch (Exception e) {
                    logger.error("Error updating task status", e);
                    SendMessage errorMessage = new SendMessage();
                    errorMessage.setChatId(chatId);
                    errorMessage.setText("❌ Error al actualizar el estado. Por favor, intenta nuevamente.");
                    try {
                        execute(errorMessage);
                    } catch (TelegramApiException ex) {
                        logger.error("Error sending error message", ex);
                    }
                }
            }

            private void startNewTaskCreation(long chatId) {
                // Establecer el estado
                chatState.put(chatId, STATE_ADDING_NEW_TASK);
                temporaryData.put(chatId, new HashMap<>());
                
                SendMessage message = new SendMessage();
                message.setChatId(chatId);
                message.setText("📝 NUEVA TAREA\n\nPor favor, escribe la descripción de la tarea que deseas crear:");
                
                // Ocultar teclado para que sea más limpio
                ReplyKeyboardRemove keyboardRemove = new ReplyKeyboardRemove(true);
                message.setReplyMarkup(keyboardRemove);
                
                try {
                    execute(message);
                } catch (TelegramApiException e) {
                    logger.error("Error starting task creation", e);
                }
            }
            
            private void handleAddingNewTaskState(long chatId, String messageText, User currentUser) {
                // Guardar la descripción de la tarea
                Map<String, Object> data = temporaryData.getOrDefault(chatId, new HashMap<>());
                data.put("description", messageText);
                temporaryData.put(chatId, data);
                
                // Cambiar al estado de selección de prioridad
                chatState.put(chatId, STATE_ADDING_TASK_PRIORITY);
                
                SendMessage message = new SendMessage();
                message.setChatId(chatId);
                message.setText("Por favor, selecciona la prioridad de la tarea:\n\n" +
                        "1️⃣ Low (Baja)\n" +
                        "2️⃣ Medium (Media)\n" +
                        "3️⃣ High (Alta)\n" +
                        "4️⃣ Critical (Crítica)");
                
                try {
                    execute(message);
                } catch (TelegramApiException e) {
                    logger.error("Error requesting task priority", e);
                }
            }
            
            private void handleAddingTaskPriorityState(long chatId, String messageText, User currentUser, Map<String, Object> data) {
                String priority = null;
                
                switch (messageText) {
                    case "1":
                        priority = "Low";
                        break;
                    case "2":
                        priority = "Medium";
                        break;
                    case "3":
                        priority = "High";
                        break;
                    case "4":
                        priority = "Critical";
                        break;
                    default:
                        SendMessage errorMessage = new SendMessage();
                        errorMessage.setChatId(chatId);
                        errorMessage.setText("❌ Por favor, selecciona una opción válida (1-4):");
                        
                        try {
                            execute(errorMessage);
                        } catch (TelegramApiException e) {
                            logger.error("Error sending error message", e);
                        }
                        return;
                }
                
                // Guardar la prioridad
                data.put("priority", priority);
                temporaryData.put(chatId, data);
                
                // Cambiar al estado de ingresar horas estimadas
                chatState.put(chatId, STATE_ADDING_TASK_ESTIMATED_HOURS);
                
                SendMessage hoursMessage = new SendMessage();
                hoursMessage.setChatId(chatId);
                hoursMessage.setText("Por favor, ingresa las horas estimadas para completar esta tarea (un número, máximo 4 horas):");
                
                try {
                    execute(hoursMessage);
                } catch (TelegramApiException e) {
                    logger.error("Error requesting estimated hours", e);
                }
            }

            private void showDevelopersForAssignment(long chatId) {
                List<User> developers = userService.findByRole("Developer");
                
                StringBuilder message = new StringBuilder();
                message.append("Por favor, selecciona un desarrollador para asignar la tarea ingresando su ID:\n\n");
                
                if (developers.isEmpty()) {
                    message.append("No hay desarrolladores disponibles en el sistema.\n");
                } else {
                    for (User dev : developers) {
                        message.append("ID: ").append(dev.getID()).append(" - ");
                        message.append("Nombre: ").append(dev.getName()).append(" - ");
                        message.append("Username: ").append(dev.getUsername()).append("\n");
                    }
                }
                
                message.append("\n0️⃣ Para dejar la tarea sin asignar, escribe '0'");
                
                SendMessage messageToTelegram = new SendMessage();
                messageToTelegram.setChatId(chatId);
                messageToTelegram.setText(message.toString());
                
                try {
                    execute(messageToTelegram);
                } catch (TelegramApiException e) {
                    logger.error("Error showing developers", e);
                }
            }
            
            private void handleAssigningDeveloperState(long chatId, String messageText, User currentUser, Map<String, Object> data) {
                try {
                    int developerId = Integer.parseInt(messageText);
                    
                    // Si se eligió 0, la tarea queda sin asignar
                    if (developerId == 0) {
                        data.put("assignedTo", null);
                        createTaskWithSavedData(chatId, currentUser, data);
                        return;
                    }
                    
                    // Verificar si el desarrollador existe
                    ResponseEntity<User> response = userService.getUserById(developerId);
                    if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                        User developer = response.getBody();
                        
                        // Verificar que sea un desarrollador
                        if (!"Developer".equals(developer.getRole())) {
                            SendMessage errorMessage = new SendMessage();
                            errorMessage.setChatId(chatId);
                            errorMessage.setText("❌ El usuario seleccionado no es un desarrollador. Por favor, selecciona un desarrollador válido.");
                            execute(errorMessage);
                            showDevelopersForAssignment(chatId);
                            return;
                        }
                        
                        // Guardar el ID del desarrollador
                        data.put("assignedTo", developerId);
                        createTaskWithSavedData(chatId, currentUser, data);
                        
                    } else {
                        SendMessage errorMessage = new SendMessage();
                        errorMessage.setChatId(chatId);
                        errorMessage.setText("❌ No se encontró ningún usuario con ese ID. Por favor, intenta nuevamente.");
                        execute(errorMessage);
                        showDevelopersForAssignment(chatId);
                    }
                } catch (NumberFormatException e) {
                    SendMessage errorMessage = new SendMessage();
                    errorMessage.setChatId(chatId);
                    errorMessage.setText("❌ Por favor, ingresa un número válido para el ID del desarrollador.");
                    
                    try {
                        execute(errorMessage);
                        showDevelopersForAssignment(chatId);
                    } catch (TelegramApiException ex) {
                        logger.error("Error sending error message", ex);
                    }
                } catch (TelegramApiException e) {
                    logger.error("Error assigning developer", e);
                }
            }
            
            private void createTaskWithSavedData(long chatId, User currentUser, Map<String, Object> data) {
                try {
                    // Obtener datos guardados
                    String description = (String) data.get("description");
                    String priority = (String) data.get("priority");
                    Integer assignedTo = (Integer) data.get("assignedTo");
                    Double estimatedHours = (Double) data.get("estimatedHours");
                    
                    // Crear nueva tarea
                    ToDoItem newTask = new ToDoItem();
                    newTask.setDescription(description);
                    newTask.setCreation_ts(OffsetDateTime.now());
                    newTask.setDone(false);
                    newTask.setCreatedBy(currentUser.getID());
                    newTask.setStatus("Pending");
                    newTask.setPriority(priority);
                    newTask.setAssignedTo(assignedTo);
                    newTask.setEstimatedHours(estimatedHours);
                    
                    // Si excede las 4 horas, subdividir automáticamente
                    if (estimatedHours != null && estimatedHours > 4.0) {
                        // Calcular cuántas subtareas se necesitan
                        int numberOfSubtasks = (int) Math.ceil(estimatedHours / 4.0);
                        double hoursPerSubtask = estimatedHours / numberOfSubtasks;
                        
                        StringBuilder resultMessage = new StringBuilder();
                        resultMessage.append("⚠️ La tarea excede las 4 horas permitidas. Se ha dividido en ").append(numberOfSubtasks).append(" subtareas:\n\n");
                        
                        for (int i = 1; i <= numberOfSubtasks; i++) {
                            // Crear subtarea
                            ToDoItem subTask = new ToDoItem();
                            subTask.setDescription(description + " (Parte " + i + " de " + numberOfSubtasks + ")");
                            subTask.setCreation_ts(OffsetDateTime.now());
                            subTask.setDone(false);
                            subTask.setCreatedBy(currentUser.getID());
                            subTask.setAssignedTo(assignedTo);
                            subTask.setEstimatedHours(hoursPerSubtask);
                            subTask.setStatus("Pending");
                            subTask.setPriority(priority);
                            
                            toDoItemService.addToDoItem(subTask);
                            
                            resultMessage.append("📌 ").append(subTask.getDescription()).append(" - ").append(String.format("%.2f", hoursPerSubtask)).append(" horas\n");
                        }
                        
                        // Obtener nombre del desarrollador si fue asignado
                        String developerName = "Sin asignar";
                        if (assignedTo != null) {
                            ResponseEntity<User> response = userService.getUserById(assignedTo);
                            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                                developerName = response.getBody().getName();
                            }
                        }
                        
                        resultMessage.append("\n⚠️ Prioridad: ").append(priority).append("\n");
                        resultMessage.append("👨‍💻 Asignadas a: ").append(developerName).append("\n");
                        
                        SendMessage message = new SendMessage();
                        message.setChatId(chatId);
                        message.setText(resultMessage.toString());
                        execute(message);
                        
                    } else {
                        // Guardar la tarea si está dentro del límite de 4 horas
                        toDoItemService.addToDoItem(newTask);
                        
                        // Obtener nombre del desarrollador si fue asignado
                        String developerName = "Sin asignar";
                        if (assignedTo != null) {
                            ResponseEntity<User> response = userService.getUserById(assignedTo);
                            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                                developerName = response.getBody().getName();
                            }
                        }
                        
                        // Mensaje de éxito
                        StringBuilder successMessage = new StringBuilder();
                        successMessage.append("✅ Tarea creada exitosamente:\n\n");
                        successMessage.append("📌 ").append(description).append("\n");
                        successMessage.append("🔄 Estado: Pending\n");
                        successMessage.append("⚠️ Prioridad: ").append(priority).append("\n");
                        successMessage.append("⏱️ Horas estimadas: ").append(estimatedHours).append("\n");
                        successMessage.append("👨‍💻 Asignada a: ").append(developerName).append("\n");
                        
                        SendMessage message = new SendMessage();
                        message.setChatId(chatId);
                        message.setText(successMessage.toString());
                        execute(message);
                    }
                    
                    // Restablecer estado
                    chatState.put(chatId, STATE_NONE);
                    temporaryData.put(chatId, new HashMap<>());
                    
                    // Mostrar menú principal
                    showMainMenu(chatId, currentUser);
                    
                } catch (Exception e) {
                    logger.error("Error creating new task", e);
                    SendMessage errorMessage = new SendMessage();
                    errorMessage.setChatId(chatId);
                    errorMessage.setText("❌ Error al crear la tarea. Por favor, intenta nuevamente.");
                    
                    try {
                        execute(errorMessage);
                        // Volver al menú principal
                        chatState.put(chatId, STATE_NONE);
                        showMainMenu(chatId, currentUser);
                    } catch (TelegramApiException ex) {
                        logger.error("Error sending error message", ex);
                    }
                }
            }

            private void showDeveloperTasksInChatFormat(long chatId, User currentUser) {
    // Obtener tareas asignadas al desarrollador
    List<ToDoItem> assignedTasks = toDoItemService.findByAssignedTo(currentUser.getID()).stream()
            .filter(item -> (item.getIsArchived() == null || item.getIsArchived() == 0))
            .collect(Collectors.toList());
    
    if (assignedTasks.isEmpty()) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("No tienes tareas asignadas actualmente. ¡Disfruta tu tiempo libre! 😎");
        
        try {
            execute(message);
            showMainMenu(chatId, currentUser);
        } catch (TelegramApiException e) {
            logger.error("Error sending message", e);
        }
        return;
    }
    
    // Agrupar tareas por estado
    Map<String, List<ToDoItem>> tasksByStatus = new LinkedHashMap<>();
    // Definir el orden de los estados
    for (String status : Arrays.asList("In Progress", "Pending", "In Review", "Completed")) {
        tasksByStatus.put(status, new ArrayList<>());
    }
    
    // Llenar los grupos con las tareas
    for (ToDoItem task : assignedTasks) {
        String status = task.getStatus();
        if (!tasksByStatus.containsKey(status)) {
            tasksByStatus.put(status, new ArrayList<>());
        }
        tasksByStatus.get(status).add(task);
    }
    
    // Preparar mensajes uno por estado, para simular un formato de chat
    for (Map.Entry<String, List<ToDoItem>> entry : tasksByStatus.entrySet()) {
        String status = entry.getKey();
        List<ToDoItem> tasks = entry.getValue();
        
        if (tasks.isEmpty()) {
            continue;
        }
        
        // Crear mensaje para este grupo de tareas
        StringBuilder statusMessage = new StringBuilder();
        String emoji;
        switch (status) {
            case "In Progress":
                emoji = "🔄";
                break;
            case "Pending":
                emoji = "⏳";
                break;
            case "In Review":
                emoji = "👁️";
                break;
            case "Completed":
                emoji = "✅";
                break;
            default:
                emoji = "📌";
        }
        
        statusMessage.append(emoji).append(" ").append(status.toUpperCase()).append(" (").append(tasks.size()).append(")\n\n");
        
        for (ToDoItem task : tasks) {
            statusMessage.append("• <b>").append(task.getDescription()).append("</b>\n");
            
            // Añadir información del sprint si existe
            if (task.getSprintId() != null) {
                ResponseEntity<Sprint> sprintResponse = sprintService.getSprintById(task.getSprintId());
                if (sprintResponse.getStatusCode() == HttpStatus.OK && sprintResponse.getBody() != null) {
                    statusMessage.append("  📋 Sprint: ").append(sprintResponse.getBody().getName()).append("\n");
                }
            }
            
            // Añadir información de prioridad
            String priorityEmoji = "⚠️";
            if (task.getPriority() != null) {
                switch (task.getPriority()) {
                    case "Low":
                        priorityEmoji = "🟢";
                        break;
                    case "Medium":
                        priorityEmoji = "🟡";
                        break;
                    case "High":
                        priorityEmoji = "🟠";
                        break;
                    case "Critical":
                        priorityEmoji = "🔴";
                        break;
                }
                statusMessage.append("  ").append(priorityEmoji).append(" Prioridad: ").append(task.getPriority()).append("\n");
            }
            
            // Añadir información de horas
            if (task.getEstimatedHours() != null) {
                statusMessage.append("  ⏱️ Estimado: ").append(task.getEstimatedHours()).append(" horas\n");
            }
            
            if (task.getActualHours() != null) {
                statusMessage.append("  ⏱️ Real: ").append(task.getActualHours()).append(" horas\n");
            }
            
            // Añadir ID y botones de acción
            statusMessage.append("  🆔 ID: ").append(task.getID()).append("\n");
            
            // Acciones disponibles según el estado
            if ("In Progress".equals(status)) {
                statusMessage.append("  ➡️ Acciones: Completar (escribe 'completar ").append(task.getID()).append("')\n");
            } else if ("Pending".equals(status)) {
                statusMessage.append("  ➡️ Acciones: Iniciar (escribe 'iniciar ").append(task.getID()).append("')\n");
            } else if ("Completed".equals(status)) {
                statusMessage.append("  ➡️ Acciones: Reabrir (escribe 'reabrir ").append(task.getID()).append("')\n");
            }
            
            statusMessage.append("\n");
        }
        
        // Enviar mensaje para este estado
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(statusMessage.toString());
        message.enableHtml(true);
        
        try {
            execute(message);
        } catch (TelegramApiException e) {
            logger.error("Error sending status message", e);
        }
    }
    
    // Mensaje de instrucciones
    SendMessage instructionsMessage = new SendMessage();
    instructionsMessage.setChatId(chatId);
    instructionsMessage.setText("Para realizar acciones sobre una tarea, escribe el comando seguido del ID de la tarea. Ejemplos:\n" +
            "• 'iniciar 123' - Cambia una tarea a estado 'In Progress'\n" +
            "• 'completar 123' - Marca una tarea como completada\n" +
            "• 'reabrir 123' - Reabre una tarea completada\n" +
            "• 'ver 123' - Muestra detalles completos de una tarea");
    
    try {
        execute(instructionsMessage);
        // Añadir listeners para estos comandos
        addTaskActionListeners(chatId, currentUser);
    } catch (TelegramApiException e) {
        logger.error("Error sending instructions message", e);
    }
    
    // Volver al menú principal
    showMainMenu(chatId, currentUser);
}

private void addTaskActionListeners(long chatId, User currentUser) {
    // No es necesario realmente añadir listeners aquí, 
    // pero podemos modificar processAuthorizedRequest para manejar estos comandos.
    // Lo dejamos como un método separado por si queremos expandir la funcionalidad después.
}

private void handleDeveloperTaskAction(long chatId, String messageText, User currentUser) {
    String[] parts = messageText.split(" ", 2);
    if (parts.length < 2) {
        SendMessage errorMessage = new SendMessage();
        errorMessage.setChatId(chatId);
        errorMessage.setText("❌ Formato incorrecto. Usa 'comando ID', por ejemplo: 'iniciar 123'");
        
        try {
            execute(errorMessage);
        } catch (TelegramApiException e) {
            logger.error("Error sending error message", e);
        }
        return;
    }
    
    String action = parts[0].toLowerCase();
    int taskId;
    
    try {
        taskId = Integer.parseInt(parts[1]);
    } catch (NumberFormatException e) {
        SendMessage errorMessage = new SendMessage();
        errorMessage.setChatId(chatId);
        errorMessage.setText("❌ El ID de la tarea debe ser un número válido.");
        
        try {
            execute(errorMessage);
        } catch (TelegramApiException ex) {
            logger.error("Error sending error message", ex);
        }
        return;
    }
    
    // Verificar si la tarea existe y pertenece al desarrollador
    ResponseEntity<ToDoItem> response = toDoItemService.getItemById(taskId);
    if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
        SendMessage errorMessage = new SendMessage();
        errorMessage.setChatId(chatId);
        errorMessage.setText("❌ No se encontró ninguna tarea con ese ID.");
        
        try {
            execute(errorMessage);
        } catch (TelegramApiException e) {
            logger.error("Error sending error message", e);
        }
        return;
    }
    
    ToDoItem task = response.getBody();
    
    // Verificar que la tarea esté asignada al desarrollador actual
    if (task.getAssignedTo() == null || !task.getAssignedTo().equals(currentUser.getID())) {
        SendMessage errorMessage = new SendMessage();
        errorMessage.setChatId(chatId);
        errorMessage.setText("❌ Esta tarea no está asignada a ti. Solo puedes realizar acciones sobre tus propias tareas.");
        
        try {
            execute(errorMessage);
        } catch (TelegramApiException e) {
            logger.error("Error sending error message", e);
        }
        return;
    }
    
    // Procesar la acción
    switch (action) {
        case "iniciar":
            if ("In Progress".equals(task.getStatus())) {
                SendMessage message = new SendMessage();
                message.setChatId(chatId);
                message.setText("ℹ️ Esta tarea ya está en progreso.");
                try {
                    execute(message);
                } catch (TelegramApiException e) {
                    logger.error("Error sending message", e);
                }
            } else {
                task.setStatus("In Progress");
                task.setDone(false);
                toDoItemService.updateToDoItem(taskId, task);
                
                SendMessage message = new SendMessage();
                message.setChatId(chatId);
                message.setText("✅ Tarea \"" + task.getDescription() + "\" iniciada correctamente.");
                try {
                    execute(message);
                    // Actualizar la vista de tareas
                    showDeveloperTasksInChatFormat(chatId, currentUser);
                } catch (TelegramApiException e) {
                    logger.error("Error sending message", e);
                }
            }
            break;
            
        case "completar":
            if ("Completed".equals(task.getStatus())) {
                SendMessage message = new SendMessage();
                message.setChatId(chatId);
                message.setText("ℹ️ Esta tarea ya está completada.");
                try {
                    execute(message);
                } catch (TelegramApiException e) {
                    logger.error("Error sending message", e);
                }
            } else {
                // Si la tarea tiene horas estimadas, pedir horas reales
                if (task.getEstimatedHours() != null) {
                    // Guardar datos temporales para el proceso de completar tarea
                    Map<String, Object> data = temporaryData.getOrDefault(chatId, new HashMap<>());
                    data.put("taskId", taskId);
                    temporaryData.put(chatId, data);
                    
                    // Cambiar al estado de ingresar horas reales
                    chatState.put(chatId, STATE_COMPLETING_TASK_HOURS);
                    
                    SendMessage hoursMessage = new SendMessage();
                    hoursMessage.setChatId(chatId);
                    hoursMessage.setText("Por favor, ingresa las horas reales que trabajaste en esta tarea:");
                    try {
                        execute(hoursMessage);
                    } catch (TelegramApiException e) {
                        logger.error("Error sending message", e);
                    }
                } else {
                    // Si no tiene horas estimadas, simplemente completarla
                    task.setStatus("Completed");
                    task.setDone(true);
                    toDoItemService.updateToDoItem(taskId, task);
                    
                    SendMessage message = new SendMessage();
                    message.setChatId(chatId);
                    message.setText("✅ Tarea \"" + task.getDescription() + "\" completada correctamente.");
                    try {
                        execute(message);
                        // Actualizar la vista de tareas
                        showDeveloperTasksInChatFormat(chatId, currentUser);
                    } catch (TelegramApiException e) {
                        logger.error("Error sending message", e);
                    }
                }
            }
            break;
            
        case "reabrir":
            if (!"Completed".equals(task.getStatus())) {
                SendMessage message = new SendMessage();
                message.setChatId(chatId);
                message.setText("ℹ️ Esta tarea no está completada, no se puede reabrir.");
                try {
                    execute(message);
                } catch (TelegramApiException e) {
                    logger.error("Error sending message", e);
                }
            } else {
                task.setStatus("In Progress");
                task.setDone(false);
                toDoItemService.updateToDoItem(taskId, task);
                
                SendMessage message = new SendMessage();
                message.setChatId(chatId);
                message.setText("✅ Tarea \"" + task.getDescription() + "\" reabierta correctamente y marcada como 'En Progreso'.");
                try {
                    execute(message);
                    // Actualizar la vista de tareas
                    showDeveloperTasksInChatFormat(chatId, currentUser);
                } catch (TelegramApiException e) {
                    logger.error("Error sending message", e);
                }
            }
            break;
            
        case "ver":
            showDetailedTaskView(chatId, task);
            break;
            
        default:
            SendMessage errorMessage = new SendMessage();
            errorMessage.setChatId(chatId);
            errorMessage.setText("❌ Comando no reconocido. Los comandos disponibles son: iniciar, completar, reabrir, ver.");
            
            try {
                execute(errorMessage);
            } catch (TelegramApiException e) {
                logger.error("Error sending error message", e);
            }
    }
}

private void showDetailedTaskView(long chatId, ToDoItem task) {
    StringBuilder detailMessage = new StringBuilder();
    detailMessage.append("📝 <b>DETALLES DE LA TAREA</b>\n\n");
    
    detailMessage.append("<b>ID:</b> ").append(task.getID()).append("\n");
    detailMessage.append("<b>Descripción:</b> ").append(task.getDescription()).append("\n");
    detailMessage.append("<b>Estado:</b> ").append(task.getStatus()).append("\n");
    
    if (task.getPriority() != null) {
        detailMessage.append("<b>Prioridad:</b> ").append(task.getPriority()).append("\n");
    }
    
    if (task.getEstimatedHours() != null) {
        detailMessage.append("<b>Horas estimadas:</b> ").append(task.getEstimatedHours()).append("\n");
    }
    
    if (task.getActualHours() != null) {
        detailMessage.append("<b>Horas reales:</b> ").append(task.getActualHours()).append("\n");
    }
    
    if (task.getCreation_ts() != null) {
        detailMessage.append("<b>Fecha de creación:</b> ").append(task.getCreation_ts()).append("\n");
    }
    
    // Información del sprint
    if (task.getSprintId() != null) {
        ResponseEntity<Sprint> sprintResponse = sprintService.getSprintById(task.getSprintId());
        if (sprintResponse.getStatusCode() == HttpStatus.OK && sprintResponse.getBody() != null) {
            Sprint sprint = sprintResponse.getBody();
            detailMessage.append("<b>Sprint:</b> ").append(sprint.getName()).append("\n");
            detailMessage.append("<b>Periodo del Sprint:</b> ").append(sprint.getStartDate()).append(" al ").append(sprint.getEndDate()).append("\n");
        }
    }
    
    // Información del creador
    if (task.getCreatedBy() != null) {
        ResponseEntity<User> userResponse = userService.getUserById(task.getCreatedBy());
        if (userResponse.getStatusCode() == HttpStatus.OK && userResponse.getBody() != null) {
            User creator = userResponse.getBody();
            detailMessage.append("<b>Creado por:</b> ").append(creator.getName()).append(" (").append(creator.getRole()).append(")\n");
        }
    }
    
    // Notas adicionales
    if (task.getSteps() != null && !task.getSteps().isEmpty()) {
        detailMessage.append("\n<b>Pasos/Notas:</b>\n").append(task.getSteps()).append("\n");
    }
    
    SendMessage message = new SendMessage();
    message.setChatId(chatId);
    message.setText(detailMessage.toString());
    message.enableHtml(true);
    
    try {
        execute(message);
    } catch (TelegramApiException e) {
        logger.error("Error sending detailed task view", e);
    }
}

private void handleAddingTaskEstimatedHoursState(long chatId, String messageText, User currentUser, Map<String, Object> data) {
    try {
        // Intentar convertir a número
        double hours = Double.parseDouble(messageText);
        
        if (hours <= 0) {
            SendMessage errorMessage = new SendMessage();
            errorMessage.setChatId(chatId);
            errorMessage.setText("❌ El número de horas debe ser mayor que cero. Por favor, intenta nuevamente:");
            execute(errorMessage);
            return;
        }
        
        // Guardar las horas estimadas
        data.put("estimatedHours", hours);
        temporaryData.put(chatId, data);
        
        // Si es un developer, autoasignarse la tarea y terminar
        // Si es un developer, autoasignarse la tarea y terminar
if ("Developer".equals(currentUser.getRole())) {
    data.put("assignedTo", currentUser.getID());
    createTaskWithSavedData(chatId, currentUser, data);
} else {
    // Si es manager, pasar al estado de asignación de desarrollador
    chatState.put(chatId, STATE_ASSIGNING_DEVELOPER);
    showDevelopersForAssignment(chatId);
}
        
    } catch (NumberFormatException e) {
        // Manejar error de formato
        SendMessage errorMessage = new SendMessage();
        errorMessage.setChatId(chatId);
        errorMessage.setText("❌ Por favor, ingresa un número válido para las horas:");
        
        try {
            execute(errorMessage);
        } catch (TelegramApiException ex) {
            logger.error("Error sending error message", ex);
        }
    } catch (TelegramApiException e) {
        logger.error("Error processing hours input", e);
    }
}
        
            private void showDevelopers(long chatId) {
                List<User> developers = userService.findByRole("Developer");
                
                StringBuilder message = new StringBuilder("Desarrolladores disponibles:\n\n");
                for (User dev : developers) {
                    message.append("ID: ").append(dev.getID()).append(" - ");
                    message.append("Nombre: ").append(dev.getName()).append(" - ");
                    message.append("Username: ").append(dev.getUsername()).append("\n");
                }
                
                SendMessage messageToTelegram = new SendMessage();
                messageToTelegram.setChatId(chatId);
                messageToTelegram.setText(message.toString());
                
                try {
                    execute(messageToTelegram);
                    BotHelper.sendMessageToTelegram(chatId, "Selecciona una opción:", this);
                    showMainMenu(chatId, authorizedUsers.get(chatId));
                } catch (TelegramApiException e) {
                    logger.error("Error showing developers", e);
                }
            }
        
            @Override
            public String getBotUsername() {
                return botName;
            }
        }