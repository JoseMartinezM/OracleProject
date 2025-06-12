//UserBotController.java - Versión Mejorada con UX para Developer

package com.springboot.MyTodoList.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Contact;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
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

public class UserBotController extends TelegramLongPollingBot {

   private static final Logger logger = LoggerFactory.getLogger(UserBotController.class);
   
   // Services
   private ToDoItemService toDoItemService;
   private UserService userService;
   private SprintService sprintService;
   private KpiTelegramController kpiController;
   private String botName;
   
   // Session management
   private Map<Long, User> authorizedUsers = new HashMap<>();
   private Map<Long, String> chatState = new HashMap<>();
   private Map<Long, Map<String, Object>> temporaryData = new HashMap<>();
   
   // Estados de conversación
   private static final String STATE_NONE = "NONE";
   private static final String STATE_ADDING_NEW_TASK = "ADDING_NEW_TASK";
   private static final String STATE_ADDING_TASK_ESTIMATED_HOURS = "ADDING_TASK_ESTIMATED_HOURS";
   private static final String STATE_COMPLETING_TASK_HOURS = "COMPLETING_TASK_HOURS";
   private static final String STATE_CHANGING_TASK_NAME = "CHANGING_TASK_NAME";
   private static final String STATE_SETTING_ESTIMATED_HOURS = "SETTING_ESTIMATED_HOURS";
   private static final String STATE_SELECTING_SPRINT_FOR_TASK = "SELECTING_SPRINT_FOR_TASK";
   private static final String STATE_KPI_MENU = "KPI_MENU";
   private static final String STATE_KPI_SELECTING_SPRINT = "KPI_SELECTING_SPRINT";
   private static final String STATE_KPI_SELECTING_DEVELOPER = "KPI_SELECTING_DEVELOPER";

   // Constructor
   @Autowired
   public UserBotController(String botToken, String botName, 
                          ToDoItemService toDoItemService, 
                          UserService userService, 
                          SprintService sprintService,
                          KpiTelegramController kpiController) {
       super(botToken);
       logger.info("Bot Token: " + botToken);
       logger.info("Bot name: " + botName);
       this.toDoItemService = toDoItemService;
       this.userService = userService;
       this.sprintService = sprintService;
       this.botName = botName;
       this.kpiController = kpiController;
   }

   // ==================== MAIN TELEGRAM HANDLERS ====================

   @Override
   public void onUpdateReceived(Update update) {
       if (update.hasCallbackQuery()) {
           handleCallbackQuery(update.getCallbackQuery());
       } else if (update.hasMessage()) {
           long chatId = update.getMessage().getChatId();
           String username = update.getMessage().getFrom().getUserName();
           
           logger.info("Mensaje recibido de: " + (username != null ? "@" + username : "Usuario sin username") + " (Chat ID: " + chatId + ")");
           
           if (authorizedUsers.containsKey(chatId)) {
               logger.info("Usuario ya autorizado, procesando solicitud normal");
               processAuthorizedRequest(update);
               return;
           }
           
           if (update.getMessage().hasContact()) {
               logger.info("Mensaje contiene contacto, procesando verificación");
               processContactMessage(update);
               return;
           }
           
           if (update.getMessage().hasText() && 
               update.getMessage().getText().equals(BotCommands.START_COMMAND.getCommand())) {
               logger.info("Comando de inicio recibido, solicitando número de teléfono");
               requestPhoneNumber(chatId);
               return;
           }
           
           if (!authorizedUsers.containsKey(chatId)) {
               logger.info("Usuario no autorizado, enviando mensaje de verificación");
               sendUnauthorizedMessage(chatId);
               return;
           }
       }
   }

   @Override
   public String getBotUsername() {
       return botName;
   }

   // ==================== CALLBACK QUERY HANDLER ====================

   private void handleCallbackQuery(CallbackQuery callbackQuery) {
       String callbackData = callbackQuery.getData();
       long chatId = callbackQuery.getMessage().getChatId();
       int messageId = callbackQuery.getMessage().getMessageId();
       User currentUser = authorizedUsers.get(chatId);
       
       logger.info("Callback recibido: " + callbackData);
       
       if (currentUser == null) {
           return;
       }

       try {
           String[] parts = callbackData.split(":");
           String action = parts[0];
           
           switch (action) {
               case "main_menu":
                   deleteMessage(chatId, messageId);
                   showMainMenu(chatId, currentUser);
                   break;
                   
               case "list_tasks":
                   deleteMessage(chatId, messageId);
                   if ("Manager".equals(currentUser.getRole())) {
                       showSprintsSelectionWithButtons(chatId);
                   } else {
                       showDeveloperTasksWithButtons(chatId, currentUser);
                   }
                   break;
                   
               case "add_task":
                   deleteMessage(chatId, messageId);
                   startNewTaskCreationWithSprint(chatId, currentUser);
                   break;
                   
               case "complete_task":
                   deleteMessage(chatId, messageId);
                   showTasksToCompleteWithButtons(chatId, currentUser);
                   break;
                   
               case "view_sprints":
                   deleteMessage(chatId, messageId);
                   if ("Manager".equals(currentUser.getRole())) {
                       showSprintsWithButtons(chatId);
                   } else {
                       showDeveloperSprintSelection(chatId, currentUser);
                   }
                   break;
                   
               case "view_developers":
                   deleteMessage(chatId, messageId);
                   showDevelopersWithButtons(chatId);
                   break;
                   
               case "hours_report":
                   deleteMessage(chatId, messageId);
                   showHoursBySprintReport(chatId);
                   break;
                   
               case "tasks_summary":
                   deleteMessage(chatId, messageId);
                   showTasksSummaryMenu(chatId);
                   break;
                   
               case "summary_general":
                   deleteMessage(chatId, messageId);
                   showTasksSummary(chatId);
                   break;
                   
               case "summary_by_sprint":
                   deleteMessage(chatId, messageId);
                   showSprintSummarySelection(chatId);
                   break;
                   
               case "sprint_summary":
                   if (parts.length > 1) {
                       int sprintId = Integer.parseInt(parts[1]);
                       deleteMessage(chatId, messageId);
                       showSprintTasksSummary(chatId, sprintId);
                   }
                   break;
                   
               case "dev_sprint_stats":
                   if (parts.length > 1) {
                       int sprintId = Integer.parseInt(parts[1]);
                       deleteMessage(chatId, messageId);
                       showDeveloperSprintStats(chatId, sprintId, currentUser);
                   }
                   break;
                   
               case "view_kpis":
                   deleteMessage(chatId, messageId);
                   if (kpiController != null) {
                       chatState.put(chatId, STATE_KPI_MENU);
                       Map<String, Object> data = temporaryData.getOrDefault(chatId, new HashMap<>());
                       data.clear();
                       temporaryData.put(chatId, data);
                       kpiController.showKpiMenuWithButtons(chatId, this, currentUser);
                   }
                   break;
                   
               case "priority":
                   handlePrioritySelection(chatId, messageId, parts[1], currentUser);
                   break;
                   
               case "assign_dev":
                   if (parts.length > 1) {
                       handleDeveloperAssignment(chatId, messageId, Integer.parseInt(parts[1]), currentUser);
                   }
                   break;
                   
               case "sprint_select":
                   if (parts.length > 1) {
                       handleSprintSelection(chatId, messageId, Integer.parseInt(parts[1]), currentUser);
                   }
                   break;
                   
               case "task_action":
                   if (parts.length > 2) {
                       handleTaskAction(chatId, messageId, parts[1], Integer.parseInt(parts[2]), currentUser);
                   }
                   break;
                   
               case "task_status":
                   if (parts.length > 2) {
                       handleTaskStatusChange(chatId, messageId, Integer.parseInt(parts[1]), parts[2], currentUser);
                   }
                   break;
                
               case "kpi_personal_final":
                   if (parts.length > 2) {
                       int developerId = Integer.parseInt(parts[1]);
                       int sprintId = Integer.parseInt(parts[2]);
                       deleteMessage(chatId, messageId);
                       if (kpiController != null) {
                           kpiController.showPersonalKpiBySprint(chatId, this, developerId, sprintId);
                       }
                   }
                   break;
                   
               case "task_complete":
                   if (parts.length > 1) {
                       handleTaskCompletion(chatId, messageId, Integer.parseInt(parts[1]), currentUser);
                   }
                   break;
                   
               case "task_update_status":
                   if (parts.length > 2) {
                       handleDirectTaskStatusUpdate(chatId, messageId, Integer.parseInt(parts[1]), parts[2], currentUser);
                   }
                   break;
                   
               case "view_sprint_tasks":
                   if (parts.length > 1) {
                       int sprintId = Integer.parseInt(parts[1]);
                       deleteMessage(chatId, messageId);
                       showSprintTasksWithButtons(chatId, sprintId);
                   }
                   break;
                   
               case "modify_task":
                   if (parts.length > 1) {
                       int taskId = Integer.parseInt(parts[1]);
                       showTaskModificationOptionsWithButtons(chatId, messageId, taskId);
                   }
                   break;
                   
               case "delete_task":
                   if (parts.length > 1) {
                       handleTaskDeletion(chatId, messageId, Integer.parseInt(parts[1]), currentUser);
                   }
                   break;
                   
               case "edit_task_name":
                   if (parts.length > 1) {
                       startTaskNameEdit(chatId, messageId, Integer.parseInt(parts[1]), currentUser);
                   }
                   break;
                   
               case "set_estimated_hours":
                   if (parts.length > 1) {
                       startEstimatedHoursEdit(chatId, messageId, Integer.parseInt(parts[1]), currentUser);
                   }
                   break;
                   
               case "assign_task_sprint":
                   if (parts.length > 1) {
                       startSprintAssignment(chatId, messageId, Integer.parseInt(parts[1]), currentUser);
                   }
                   break;
                   
               case "change_status":
                   if (parts.length > 1) {
                       showTaskStatusOptions(chatId, messageId, Integer.parseInt(parts[1]));
                   }
                   break;
                   
               case "kpi_completed_tasks":
                   deleteMessage(chatId, messageId);
                   kpiController.showSprintSelectionForKpiWithButtons(chatId, this, "completedTasks");
                   break;
                   
               case "kpi_team_sprint":
                   deleteMessage(chatId, messageId);
                   kpiController.showSprintSelectionForKpiWithButtons(chatId, this, "teamSprint");
                   break;
                   
               case "kpi_personal_sprint":
                   deleteMessage(chatId, messageId);
                   kpiController.showDeveloperSelectionForKpiWithButtons(chatId, this, "personalSprint");
                   break;
                   
               case "kpi_sprint":
                   if (parts.length > 2) {
                       String kpiType = parts[1];
                       int sprintId = Integer.parseInt(parts[2]);
                       deleteMessage(chatId, messageId);
                       handleKpiSprintSelection(chatId, kpiType, sprintId, currentUser);
                   }
                   break;
                   
               case "kpi_developer":
                   if (parts.length > 2) {
                       String kpiType = parts[1];
                       int developerId = Integer.parseInt(parts[2]);
                       deleteMessage(chatId, messageId);
                       handleKpiDeveloperSelection(chatId, kpiType, developerId, currentUser);
                   }
                   break;
                   
               default:
                   logger.warn("Callback no reconocido: " + callbackData);
                   break;
           }
       } catch (Exception e) {
           logger.error("Error procesando callback: " + callbackData, e);
       }
   }

   // ==================== AUTHENTICATION METHODS ====================

   private void processContactMessage(Update update) {
       Contact contact = update.getMessage().getContact();
       long chatId = update.getMessage().getChatId();
       String phoneNumber = contact.getPhoneNumber();
       
       logger.info("Recibido número de teléfono: " + phoneNumber);
       
       if (!phoneNumber.startsWith("+")) {
           phoneNumber = "+" + phoneNumber;
       }
       
       logger.info("Buscando usuario con teléfono: " + phoneNumber);
       
       ResponseEntity<User> userResponse = userService.getUserByPhone(phoneNumber);
       
       if (userResponse.getStatusCode() == HttpStatus.OK && userResponse.getBody() != null) {
           User user = userResponse.getBody();
           authorizedUsers.put(chatId, user);
           
           chatState.put(chatId, STATE_NONE);
           temporaryData.put(chatId, new HashMap<>());
           
           logger.info("Usuario autorizado: " + user.getName() + " (ID: " + user.getID() + ")");
           
           SendMessage welcomeMessage = new SendMessage();
           welcomeMessage.setChatId(chatId);
           welcomeMessage.setText("✅ ¡Acceso verificado exitosamente!");
           
           try {
               execute(welcomeMessage);
               showMainMenu(chatId, user);
           } catch (TelegramApiException e) {
               logger.error("Error sending welcome message", e);
           }
       } else {
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

   // ==================== MENU DISPLAY METHODS ====================

   private void showMainMenu(long chatId, User user) {
       SendMessage messageToTelegram = new SendMessage();
       messageToTelegram.setChatId(chatId);
       
       String welcomeMessage = String.format(
   "✨ %s ✨\n" +
   "━━━━━━━━━━━━━━━━━━━\n\n" +
   "%s <b>%s</b>\n" +
   "🔹 <i>%s</i>\n\n" +
   "🎯 <b>%s</b>\n" +
   "%s\n\n" +
   "💡 %s",
   
   user.getRole().equals("Manager") ? "COMMAND CENTER" : "DEV WORKSPACE",
   user.getRole().equals("Manager") ? "👑" : "🚀",
   user.getName(),
   user.getRole().equals("Manager") ? "Project Leader &amp; Team Commander" : "Code Architect &amp; Problem Solver",
   user.getRole().equals("Manager") ? "READY TO LEAD" : "READY TO BUILD",
   user.getRole().equals("Manager") ? 
       "🎖️ Orchestrate team success\n🔥 Drive project excellence\n📊 Monitor performance metrics" :
       "⚡ Transform ideas into reality\n🛠️ Craft efficient solutions\n🏆 Deliver exceptional code",
   user.getRole().equals("Manager") ? "What strategic move will you make today?" : "What will you create today?"
);
       
       messageToTelegram.setText(welcomeMessage);
       messageToTelegram.enableHtml(true);
   
       chatState.put(chatId, STATE_NONE);
       temporaryData.put(chatId, new HashMap<>());
   
       InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup();
       List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
   
       // Botones comunes para todos los roles
       List<InlineKeyboardButton> row1 = new ArrayList<>();
       InlineKeyboardButton listTasksBtn = new InlineKeyboardButton();
       listTasksBtn.setText("📋 Ver Tareas");
       listTasksBtn.setCallbackData("list_tasks");
       row1.add(listTasksBtn);
       
       InlineKeyboardButton addTaskBtn = new InlineKeyboardButton();
       addTaskBtn.setText("➕ Nueva Tarea");
       addTaskBtn.setCallbackData("add_task");
       row1.add(addTaskBtn);
       keyboard.add(row1);
   
       // Botones específicos para Developer
       if ("Developer".equals(user.getRole())) {
           List<InlineKeyboardButton> row2 = new ArrayList<>();
           InlineKeyboardButton completeBtn = new InlineKeyboardButton();
           completeBtn.setText("✅ Completar Tarea");
           completeBtn.setCallbackData("complete_task");
           row2.add(completeBtn);
           
           InlineKeyboardButton sprintsBtn = new InlineKeyboardButton();
           sprintsBtn.setText("📊 Ver Sprints");
           sprintsBtn.setCallbackData("view_sprints");
           row2.add(sprintsBtn);
           keyboard.add(row2);
       }
   
       // Botones específicos para Manager
       if ("Manager".equals(user.getRole())) {
           List<InlineKeyboardButton> row2 = new ArrayList<>();
           InlineKeyboardButton devsBtn = new InlineKeyboardButton();
           devsBtn.setText("👨‍💻 Equipo");
           devsBtn.setCallbackData("view_developers");
           row2.add(devsBtn);
           
           InlineKeyboardButton sprintsBtn = new InlineKeyboardButton();
           sprintsBtn.setText("📋 Sprints");
           sprintsBtn.setCallbackData("view_sprints");
           row2.add(sprintsBtn);
           keyboard.add(row2);
           
           List<InlineKeyboardButton> row3 = new ArrayList<>();
           InlineKeyboardButton hoursBtn = new InlineKeyboardButton();
           hoursBtn.setText("⏱️ Reporte Horas");
           hoursBtn.setCallbackData("hours_report");
           row3.add(hoursBtn);
           
           InlineKeyboardButton summaryBtn = new InlineKeyboardButton();
           summaryBtn.setText("📊 Resumen Tareas");
           summaryBtn.setCallbackData("tasks_summary");
           row3.add(summaryBtn);
           keyboard.add(row3);
           
           List<InlineKeyboardButton> row4 = new ArrayList<>();
           InlineKeyboardButton kpiBtn = new InlineKeyboardButton();
           kpiBtn.setText("📈 Reportes KPI");
           kpiBtn.setCallbackData("view_kpis");
           row4.add(kpiBtn);
           keyboard.add(row4);
       }
   
       inlineKeyboard.setKeyboard(keyboard);
       messageToTelegram.setReplyMarkup(inlineKeyboard);
   
       try {
           execute(messageToTelegram);
       } catch (TelegramApiException e) {
           logger.error(e.getLocalizedMessage(), e);
       }
   }

   private void showSprintsSelectionWithButtons(long chatId) {
    List<Sprint> sprints = sprintService.findAll();
    
    SendMessage message = new SendMessage();
    message.setChatId(chatId);
    message.setText("📋 <b>Selecciona un Sprint</b>\n\nElige el sprint para ver sus tareas:");
    message.enableHtml(true);
    
    InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup();
    List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
    
    if (!sprints.isEmpty()) {
        for (Sprint sprint : sprints) {
            List<InlineKeyboardButton> row = new ArrayList<>();
            InlineKeyboardButton button = new InlineKeyboardButton();
            // Solo mostrar el nombre del sprint
            button.setText(sprint.getName());
            button.setCallbackData("view_sprint_tasks:" + sprint.getId());
            row.add(button);
            keyboard.add(row);
        }
    }
    
    // Botón para tareas sin sprint
    List<InlineKeyboardButton> row = new ArrayList<>();
    InlineKeyboardButton noSprintBtn = new InlineKeyboardButton();
    noSprintBtn.setText("📌 Tareas Sin Sprint");
    noSprintBtn.setCallbackData("view_sprint_tasks:0");
    row.add(noSprintBtn);
    keyboard.add(row);
    
    // Botón de regreso
    List<InlineKeyboardButton> backRow = new ArrayList<>();
    InlineKeyboardButton backBtn = new InlineKeyboardButton();
    backBtn.setText("🔙 Menú Principal");
    backBtn.setCallbackData("main_menu");
    backRow.add(backBtn);
    keyboard.add(backRow);
    
    inlineKeyboard.setKeyboard(keyboard);
    message.setReplyMarkup(inlineKeyboard);
    
    try {
        execute(message);
    } catch (TelegramApiException e) {
        logger.error("Error sending sprints selection", e);
    }
}

private void showSprintTasksWithButtons(long chatId, int sprintId) {
    List<ToDoItem> tasks;
    String title;
    
    if (sprintId == 0) {
        tasks = toDoItemService.findAll().stream()
                .filter(item -> item.getSprintId() == null && (item.getIsArchived() == null || item.getIsArchived() == 0))
                .collect(Collectors.toList());
        title = "📌 <b>TAREAS SIN SPRINT</b>";
    } else {
        ResponseEntity<Sprint> sprintResponse = sprintService.getSprintById(sprintId);
        if (sprintResponse.getStatusCode() != HttpStatus.OK || sprintResponse.getBody() == null) {
            SendMessage errorMessage = new SendMessage();
            errorMessage.setChatId(chatId);
            errorMessage.setText("❌ Sprint no encontrado");
            try {
                execute(errorMessage);
            } catch (TelegramApiException e) {
                logger.error("Error sending error message", e);
            }
            return;
        }
        
        Sprint sprint = sprintResponse.getBody();
        tasks = toDoItemService.findBySprintId(sprintId);
        title = "📋 <b>SPRINT: " + sprint.getName() + "</b>";
    }
    
    if (tasks.isEmpty()) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(title + "\n\n❌ No hay tareas disponibles.");
        message.enableHtml(true);
        
        InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        
        List<InlineKeyboardButton> backRow = new ArrayList<>();
        InlineKeyboardButton backBtn = new InlineKeyboardButton();
        backBtn.setText("🔙 Volver a Sprints");
        backBtn.setCallbackData("list_tasks");
        backRow.add(backBtn);
        keyboard.add(backRow);
        
        inlineKeyboard.setKeyboard(keyboard);
        message.setReplyMarkup(inlineKeyboard);
        
        try {
            execute(message);
        } catch (TelegramApiException e) {
            logger.error("Error sending empty tasks message", e);
        }
        return;
    }
    
    // Agrupar tareas por estado
    Map<String, List<ToDoItem>> tasksByStatus = tasks.stream()
            .collect(Collectors.groupingBy(ToDoItem::getStatus));
    
    StringBuilder messageText = new StringBuilder();
    messageText.append(title).append("\n\n");
    
    for (String status : Arrays.asList("Pending", "In Progress", "In Review", "Completed")) {
        List<ToDoItem> statusTasks = tasksByStatus.getOrDefault(status, new ArrayList<>());
        if (!statusTasks.isEmpty()) {
            String emoji = getStatusEmoji(status);
            messageText.append(emoji).append(" <b>").append(status.toUpperCase()).append("</b> (").append(statusTasks.size()).append(")\n");
            
            for (ToDoItem task : statusTasks.subList(0, Math.min(3, statusTasks.size()))) {
                messageText.append("• ").append(task.getDescription());
               if (task.getAssignedTo() != null) {
                   ResponseEntity<User> userResponse = userService.getUserById(task.getAssignedTo());
                   if (userResponse.getStatusCode() == HttpStatus.OK && userResponse.getBody() != null) {
                       messageText.append(" (").append(userResponse.getBody().getName()).append(")");
                   }
               }
               messageText.append("\n");
           }
           
           if (statusTasks.size() > 3) {
               messageText.append("  ... y ").append(statusTasks.size() - 3).append(" más\n");
           }
           messageText.append("\n");
       }
   }
   
   SendMessage message = new SendMessage();
   message.setChatId(chatId);
   message.setText(messageText.toString());
   message.enableHtml(true);
   
   InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup();
   List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
   
   // Mostrar tareas principales para modificar rápidamente
   int count = 0;
   for (ToDoItem task : tasks) {
       if (count >= 8) break; // Mostrar máximo 8 tareas
       
       List<InlineKeyboardButton> taskRow = new ArrayList<>();
       InlineKeyboardButton taskBtn = new InlineKeyboardButton();
       taskBtn.setText("📝 " + (task.getDescription().length() > 25 ? 
           task.getDescription().substring(0, 25) + "..." : task.getDescription()));
       taskBtn.setCallbackData("modify_task:" + task.getID());
       taskRow.add(taskBtn);
       keyboard.add(taskRow);
       count++;
   }
   
   // Botón de regreso
   List<InlineKeyboardButton> backRow = new ArrayList<>();
   InlineKeyboardButton backBtn = new InlineKeyboardButton();
   backBtn.setText("🔙 Volver a Sprints");
   backBtn.setCallbackData("list_tasks");
   backRow.add(backBtn);
   keyboard.add(backRow);
   
   inlineKeyboard.setKeyboard(keyboard);
   message.setReplyMarkup(inlineKeyboard);
   
   try {
       execute(message);
   } catch (TelegramApiException e) {
       logger.error("Error sending sprint tasks", e);
   }
}

private void showSprintsWithButtons(long chatId) {
   List<Sprint> sprints = sprintService.findAll();
   
   if (sprints.isEmpty()) {
       SendMessage message = new SendMessage();
       message.setChatId(chatId);
       message.setText("No hay sprints disponibles en el sistema.");
       
       InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup();
       List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
       
       List<InlineKeyboardButton> backRow = new ArrayList<>();
       InlineKeyboardButton backBtn = new InlineKeyboardButton();
       backBtn.setText("🔙 Menú Principal");
       backBtn.setCallbackData("main_menu");
       backRow.add(backBtn);
       keyboard.add(backRow);
       
       inlineKeyboard.setKeyboard(keyboard);
       message.setReplyMarkup(inlineKeyboard);
       
       try {
           execute(message);
       } catch (TelegramApiException e) {
           logger.error("Error sending message", e);
       }
       return;
   }
   
   StringBuilder sprintsMessage = new StringBuilder();
   sprintsMessage.append("📋 <b>SPRINTS DISPONIBLES</b>\n\n");
   
   for (Sprint sprint : sprints) {
       sprintsMessage.append("🔸 <b>").append(sprint.getName()).append("</b>\n");
       sprintsMessage.append("   📅 ").append(sprint.getStartDate()).append(" - ").append(sprint.getEndDate()).append("\n");
       
       // Contar tareas en este sprint
       List<ToDoItem> sprintTasks = toDoItemService.findBySprintId(sprint.getId());
       long pendingCount = sprintTasks.stream().filter(t -> "Pending".equals(t.getStatus())).count();
       long inProgressCount = sprintTasks.stream().filter(t -> "In Progress".equals(t.getStatus())).count();
       long completedCount = sprintTasks.stream().filter(t -> "Completed".equals(t.getStatus())).count();
       
       sprintsMessage.append("   📊 Tareas: ").append(sprintTasks.size())
           .append(" (⏳").append(pendingCount)
           .append(" 🔄").append(inProgressCount) 
           .append(" ✅").append(completedCount).append(")\n\n");
   }
   
   SendMessage message = new SendMessage();
   message.setChatId(chatId);
   message.setText(sprintsMessage.toString());
   message.enableHtml(true);
   
   InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup();
   List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
   
   // Botón de regreso
   List<InlineKeyboardButton> backRow = new ArrayList<>();
   InlineKeyboardButton backBtn = new InlineKeyboardButton();
   backBtn.setText("🔙 Menú Principal");
   backBtn.setCallbackData("main_menu");
   backRow.add(backBtn);
   keyboard.add(backRow);
   
   inlineKeyboard.setKeyboard(keyboard);
   message.setReplyMarkup(inlineKeyboard);
   
   try {
       execute(message);
   } catch (TelegramApiException e) {
       logger.error("Error sending sprints list", e);
   }
}

// ==================== NEW DEVELOPER SPRINT SELECTION ====================

private void showDeveloperSprintSelection(long chatId, User currentUser) {
   List<Sprint> sprints = sprintService.findAll();
   
   if (sprints.isEmpty()) {
       SendMessage message = new SendMessage();
       message.setChatId(chatId);
       message.setText("📊 <b>MIS SPRINTS</b>\n\nNo hay sprints disponibles en el sistema.");
       message.enableHtml(true);
       
       InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup();
       List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
       
       List<InlineKeyboardButton> backRow = new ArrayList<>();
       InlineKeyboardButton backBtn = new InlineKeyboardButton();
       backBtn.setText("🔙 Menú Principal");
       backBtn.setCallbackData("main_menu");
       backRow.add(backBtn);
       keyboard.add(backRow);
       
       inlineKeyboard.setKeyboard(keyboard);
       message.setReplyMarkup(inlineKeyboard);
       
       try {
           execute(message);
       } catch (TelegramApiException e) {
           logger.error("Error sending message", e);
       }
       return;
   }
   
   SendMessage message = new SendMessage();
   message.setChatId(chatId);
   message.setText("📊 <b>MIS SPRINTS</b>\n\nSelecciona un sprint para ver tus estadísticas y progreso:");
   message.enableHtml(true);
   
   InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup();
   List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
   
   for (Sprint sprint : sprints) {
       // Verificar si el desarrollador tiene tareas en este sprint
       List<ToDoItem> myTasksInSprint = toDoItemService.findBySprintId(sprint.getId()).stream()
               .filter(task -> task.getAssignedTo() != null && task.getAssignedTo().equals(currentUser.getID()))
               .collect(Collectors.toList());
       
       if (!myTasksInSprint.isEmpty()) {
           List<InlineKeyboardButton> row = new ArrayList<>();
           InlineKeyboardButton button = new InlineKeyboardButton();
           
           long completedCount = myTasksInSprint.stream()
                   .filter(t -> "Completed".equals(t.getStatus())).count();
           
           button.setText("📋 " + sprint.getName() + " (" + completedCount + "/" + myTasksInSprint.size() + " ✅)");
           button.setCallbackData("dev_sprint_stats:" + sprint.getId());
           row.add(button);
           keyboard.add(row);
       }
   }
   
   if (keyboard.isEmpty()) {
       message.setText("📊 <b>MIS SPRINTS</b>\n\nActualmente no tienes tareas asignadas en ningún sprint.");
   }
   
   // Botón de regreso
   List<InlineKeyboardButton> backRow = new ArrayList<>();
   InlineKeyboardButton backBtn = new InlineKeyboardButton();
   backBtn.setText("🔙 Menú Principal");
   backBtn.setCallbackData("main_menu");
   backRow.add(backBtn);
   keyboard.add(backRow);
   
   inlineKeyboard.setKeyboard(keyboard);
   message.setReplyMarkup(inlineKeyboard);
   
   try {
       execute(message);
   } catch (TelegramApiException e) {
       logger.error("Error sending developer sprint selection", e);
   }
}

// ==================== NEW DEVELOPER SPRINT STATS ====================

private void showDeveloperSprintStats(long chatId, int sprintId, User currentUser) {
   ResponseEntity<Sprint> sprintResponse = sprintService.getSprintById(sprintId);
   if (sprintResponse.getStatusCode() != HttpStatus.OK || sprintResponse.getBody() == null) {
       SendMessage errorMessage = new SendMessage();
       errorMessage.setChatId(chatId);
       errorMessage.setText("❌ Sprint no encontrado");
       try {
           execute(errorMessage);
       } catch (TelegramApiException e) {
           logger.error("Error sending error message", e);
       }
       return;
   }
   
   Sprint sprint = sprintResponse.getBody();
   List<ToDoItem> allSprintTasks = toDoItemService.findBySprintId(sprintId);
   
   // Mis tareas en este sprint
   List<ToDoItem> myTasks = allSprintTasks.stream()
           .filter(task -> task.getAssignedTo() != null && task.getAssignedTo().equals(currentUser.getID()))
           .collect(Collectors.toList());
   
   // Estadísticas personales
   long myCompletedCount = myTasks.stream().filter(t -> "Completed".equals(t.getStatus())).count();
   long myInProgressCount = myTasks.stream().filter(t -> "In Progress".equals(t.getStatus())).count();
   long myPendingCount = myTasks.stream().filter(t -> "Pending".equals(t.getStatus())).count();
   
   double myEstimatedHours = myTasks.stream()
           .filter(t -> t.getEstimatedHours() != null)
           .mapToDouble(ToDoItem::getEstimatedHours)
           .sum();
   
   double myActualHours = myTasks.stream()
           .filter(t -> t.getActualHours() != null)
           .mapToDouble(ToDoItem::getActualHours)
           .sum();
   
   // Estadísticas del equipo (resumen)
   long totalTeamTasks = allSprintTasks.size();
   long totalCompletedTasks = allSprintTasks.stream().filter(t -> "Completed".equals(t.getStatus())).count();
   
   // Obtener desarrolladores únicos en el sprint
   List<Integer> developerIds = allSprintTasks.stream()
           .filter(t -> t.getAssignedTo() != null)
           .map(ToDoItem::getAssignedTo)
           .distinct()
           .collect(Collectors.toList());
   
   StringBuilder statsMessage = new StringBuilder();
   statsMessage.append("📊 <b>ESTADÍSTICAS - ").append(sprint.getName()).append("</b>\n\n");
   
   // Mis estadísticas
   statsMessage.append("👤 <b>MIS ESTADÍSTICAS</b>\n");
   statsMessage.append("📋 Total de tareas: ").append(myTasks.size()).append("\n");
   statsMessage.append("✅ Completadas: ").append(myCompletedCount).append("\n");
   statsMessage.append("🔄 En progreso: ").append(myInProgressCount).append("\n");
   statsMessage.append("⏳ Pendientes: ").append(myPendingCount).append("\n");
   
   if (myTasks.size() > 0) {
       double myProgress = (double) myCompletedCount / myTasks.size() * 100;
       statsMessage.append("📈 Mi progreso: ").append(String.format("%.1f", myProgress)).append("%\n");
   }
   
   statsMessage.append("⏱️ Horas estimadas: ").append(String.format("%.1f", myEstimatedHours)).append("h\n");
   statsMessage.append("⏱️ Horas trabajadas: ").append(String.format("%.1f", myActualHours)).append("h\n");
   
   if (myActualHours > 0 && myEstimatedHours > 0) {
       double myEfficiency = (myEstimatedHours / myActualHours) * 100;
       String efficiencyEmoji = myEfficiency > 100 ? "🟢" : (myEfficiency >= 85 ? "🟡" : "🔴");
       statsMessage.append("📊 Mi eficiencia: ").append(efficiencyEmoji).append(" ")
               .append(String.format("%.1f", myEfficiency)).append("%\n");
   }
   
   // Estadísticas del equipo
   statsMessage.append("\n👥 <b>RESUMEN DEL EQUIPO</b>\n");
   statsMessage.append("📋 Total de tareas del sprint: ").append(totalTeamTasks).append("\n");
   statsMessage.append("✅ Tareas completadas: ").append(totalCompletedTasks).append("\n");
   
   if (totalTeamTasks > 0) {
       double teamProgress = (double) totalCompletedTasks / totalTeamTasks * 100;
       statsMessage.append("📈 Progreso del equipo: ").append(String.format("%.1f", teamProgress)).append("%\n");
   }
   
   statsMessage.append("👨‍💻 Desarrolladores activos: ").append(developerIds.size()).append("\n");
   
   // Top performers (solo nombres y tareas completadas)
   statsMessage.append("\n🏆 <b>TOP PERFORMERS</b>\n");
   Map<Integer, Long> completedByDev = allSprintTasks.stream()
           .filter(t -> "Completed".equals(t.getStatus()) && t.getAssignedTo() != null)
           .collect(Collectors.groupingBy(ToDoItem::getAssignedTo, Collectors.counting()));
   
   completedByDev.entrySet().stream()
           .sorted(Map.Entry.<Integer, Long>comparingByValue().reversed())
           .limit(3)
           .forEach(entry -> {
               ResponseEntity<User> userResponse = userService.getUserById(entry.getKey());
               if (userResponse.getStatusCode() == HttpStatus.OK && userResponse.getBody() != null) {
                   User dev = userResponse.getBody();
                   String medal = entry.getKey().equals(currentUser.getID()) ? " 🔥" : "";
                   statsMessage.append("• ").append(dev.getName()).append(": ")
                           .append(entry.getValue()).append(" tareas").append(medal).append("\n");
               }
           });
   
   SendMessage message = new SendMessage();
   message.setChatId(chatId);
   message.setText(statsMessage.toString());
   message.enableHtml(true);
   
   InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup();
   List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
   
   // Botón para ver mis tareas de este sprint
   if (!myTasks.isEmpty()) {
       List<InlineKeyboardButton> taskRow = new ArrayList<>();
       InlineKeyboardButton taskBtn = new InlineKeyboardButton();
       taskBtn.setText("📋 Ver Mis Tareas del Sprint");
       taskBtn.setCallbackData("view_sprint_tasks:" + sprintId);
       taskRow.add(taskBtn);
       keyboard.add(taskRow);
   }
   
   // Botón de regreso
   List<InlineKeyboardButton> backRow = new ArrayList<>();
   InlineKeyboardButton backBtn = new InlineKeyboardButton();
   backBtn.setText("🔙 Mis Sprints");
   backBtn.setCallbackData("view_sprints");
   backRow.add(backBtn);
   keyboard.add(backRow);
   
   inlineKeyboard.setKeyboard(keyboard);
   message.setReplyMarkup(inlineKeyboard);
   
   try {
       execute(message);
   } catch (TelegramApiException e) {
       logger.error("Error sending developer sprint stats", e);
   }
}

// ==================== IMPROVED TASK DISPLAY METHODS ====================

private void showDeveloperTasksWithButtons(long chatId, User currentUser) {
   // Solo mostrar tareas incompletas (no Completed)
   List<ToDoItem> assignedTasks = toDoItemService.findByAssignedTo(currentUser.getID()).stream()
           .filter(item -> (item.getIsArchived() == null || item.getIsArchived() == 0) 
                   && !"Completed".equals(item.getStatus()))
           .collect(Collectors.toList());
   
   if (assignedTasks.isEmpty()) {
       SendMessage message = new SendMessage();
       message.setChatId(chatId);
       message.setText("📝 <b>Mis Tareas Pendientes</b>\n\n¡Excelente! No tienes tareas pendientes. ¡Sigue así! 🎉");
       message.enableHtml(true);
       
       InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup();
       List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
       
       List<InlineKeyboardButton> backRow = new ArrayList<>();
       InlineKeyboardButton backBtn = new InlineKeyboardButton();
       backBtn.setText("🔙 Menú Principal");
       backBtn.setCallbackData("main_menu");
       backRow.add(backBtn);
       keyboard.add(backRow);
       
       inlineKeyboard.setKeyboard(keyboard);
       message.setReplyMarkup(inlineKeyboard);
       
       try {
           execute(message);
       } catch (TelegramApiException e) {
           logger.error("Error sending message", e);
       }
       return;
   }
   
   StringBuilder headerMessage = new StringBuilder();
   headerMessage.append("📝 <b>MIS TAREAS PENDIENTES</b>\n\n");
   headerMessage.append("📊 Total: ").append(assignedTasks.size()).append(" tareas\n");
   
   // Contar por estado
   long pendingCount = assignedTasks.stream().filter(t -> "Pending".equals(t.getStatus())).count();
   long inProgressCount = assignedTasks.stream().filter(t -> "In Progress".equals(t.getStatus())).count();
   long inReviewCount = assignedTasks.stream().filter(t -> "In Review".equals(t.getStatus())).count();
   
   headerMessage.append("⏳ Pendientes: ").append(pendingCount);
   headerMessage.append(" | 🔄 En progreso: ").append(inProgressCount);
   headerMessage.append(" | 👁️ En revisión: ").append(inReviewCount).append("\n\n");
   
   SendMessage headerMsg = new SendMessage();
   headerMsg.setChatId(chatId);
   headerMsg.setText(headerMessage.toString());
   headerMsg.enableHtml(true);
   
   try {
       execute(headerMsg);
   } catch (TelegramApiException e) {
       logger.error("Error sending header message", e);
   }
   
   // Mostrar cada tarea como una tarjeta individual
   for (ToDoItem task : assignedTasks) {
       StringBuilder taskMessage = new StringBuilder();
       
       String statusEmoji = getStatusEmoji(task.getStatus());
       String priorityEmoji = task.getPriority() != null ? getPriorityEmoji(task.getPriority()) : "⚪";
       
       taskMessage.append(statusEmoji).append(" ").append(priorityEmoji).append(" <b>").append(task.getDescription()).append("</b>\n");
       taskMessage.append("🔹 Estado: ").append(task.getStatus()).append("\n");
       
       if (task.getPriority() != null) {
           taskMessage.append("🔹 Prioridad: ").append(task.getPriority()).append("\n");
       }
       
       if (task.getEstimatedHours() != null) {
           taskMessage.append("🔹 Tiempo estimado: ").append(task.getEstimatedHours()).append(" horas\n");
       }
       
       // Mostrar sprint si existe
       if (task.getSprintId() != null) {
           ResponseEntity<Sprint> sprintResponse = sprintService.getSprintById(task.getSprintId());
           if (sprintResponse.getStatusCode() == HttpStatus.OK && sprintResponse.getBody() != null) {
               taskMessage.append("🔹 Sprint: ").append(sprintResponse.getBody().getName()).append("\n");
           }
       }
       
       SendMessage taskMsg = new SendMessage();
       taskMsg.setChatId(chatId);
       taskMsg.setText(taskMessage.toString());
       taskMsg.enableHtml(true);
       
       InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup();
       List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
       
       // Botones de cambio de estado en una sola fila
       List<InlineKeyboardButton> statusRow = new ArrayList<>();
       
       String currentStatus = task.getStatus();
       
       // Mostrar solo los estados siguientes posibles
       if (!"Pending".equals(currentStatus)) {
           InlineKeyboardButton pendingBtn = new InlineKeyboardButton();
           pendingBtn.setText("⏳ Pending");
           pendingBtn.setCallbackData("task_update_status:" + task.getID() + ":Pending");
           statusRow.add(pendingBtn);
       }
       
       if (!"In Progress".equals(currentStatus)) {
           InlineKeyboardButton progressBtn = new InlineKeyboardButton();
           progressBtn.setText("🔄 En Progreso");
           progressBtn.setCallbackData("task_update_status:" + task.getID() + ":In Progress");
           statusRow.add(progressBtn);
       }
       
       if (!"In Review".equals(currentStatus)) {
           InlineKeyboardButton reviewBtn = new InlineKeyboardButton();
           reviewBtn.setText("👁️ En Revisión");
           reviewBtn.setCallbackData("task_update_status:" + task.getID() + ":In Review");
           statusRow.add(reviewBtn);
       }
       
       // Botón de completar siempre disponible
       InlineKeyboardButton completeBtn = new InlineKeyboardButton();
       completeBtn.setText("✅ Completar");
       completeBtn.setCallbackData("task_complete:" + task.getID());
       statusRow.add(completeBtn);
       
       keyboard.add(statusRow);
       
       inlineKeyboard.setKeyboard(keyboard);
       taskMsg.setReplyMarkup(inlineKeyboard);
       
       try {
           execute(taskMsg);
       } catch (TelegramApiException e) {
           logger.error("Error sending task message", e);
       }
   }
   
   // Mensaje de navegación final
   SendMessage navMessage = new SendMessage();
   navMessage.setChatId(chatId);
   navMessage.setText("━━━━━━━━━━━━━━━━━━━━");
   
   InlineKeyboardMarkup navKeyboard = new InlineKeyboardMarkup();
   List<List<InlineKeyboardButton>> navButtons = new ArrayList<>();
   
   List<InlineKeyboardButton> navRow = new ArrayList<>();
   InlineKeyboardButton mainMenuBtn = new InlineKeyboardButton();
   mainMenuBtn.setText("🏠 Menú Principal");
   mainMenuBtn.setCallbackData("main_menu");
   navRow.add(mainMenuBtn);
   navButtons.add(navRow);
   
   navKeyboard.setKeyboard(navButtons);
   navMessage.setReplyMarkup(navKeyboard);
   
   try {
       execute(navMessage);
   } catch (TelegramApiException e) {
       logger.error("Error sending navigation message", e);
   }
}

private void showTasksToCompleteWithButtons(long chatId, User currentUser) {
   List<ToDoItem> assignedTasks = toDoItemService.findByAssignedTo(currentUser.getID()).stream()
           .filter(item -> !"Completed".equals(item.getStatus()) && (item.getIsArchived() == null || item.getIsArchived() == 0))
           .collect(Collectors.toList());
   
   if (assignedTasks.isEmpty()) {
       SendMessage message = new SendMessage();
       message.setChatId(chatId);
       message.setText("✅ <b>COMPLETAR TAREAS</b>\n\n¡Perfecto! No tienes tareas pendientes para completar. 🎉");
       message.enableHtml(true);
       
       InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup();
       List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
       
       List<InlineKeyboardButton> backRow = new ArrayList<>();
       InlineKeyboardButton backBtn = new InlineKeyboardButton();
       backBtn.setText("🔙 Menú Principal");
       backBtn.setCallbackData("main_menu");
       backRow.add(backBtn);
       keyboard.add(backRow);
       
       inlineKeyboard.setKeyboard(keyboard);
       message.setReplyMarkup(inlineKeyboard);
       
       try {
           execute(message);
       } catch (TelegramApiException e) {
           logger.error("Error sending message", e);
       }
       return;
   }
   
   StringBuilder messageText = new StringBuilder();
   messageText.append("✅ <b>COMPLETAR TAREAS</b>\n\n");
   messageText.append("Selecciona la tarea que has terminado:\n");
   messageText.append("📊 Tareas disponibles: ").append(assignedTasks.size()).append("\n\n");
   
   SendMessage message = new SendMessage();
   message.setChatId(chatId);
   message.setText(messageText.toString());
   message.enableHtml(true);
   
   InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup();
   List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
   
   for (ToDoItem task : assignedTasks) {
       List<InlineKeyboardButton> row = new ArrayList<>();
       InlineKeyboardButton button = new InlineKeyboardButton();
       
       String taskText = task.getDescription();
       if (taskText.length() > 40) {
           taskText = taskText.substring(0, 40) + "...";
       }
       
       String status = getStatusEmoji(task.getStatus());
       String priority = task.getPriority() != null ? getPriorityEmoji(task.getPriority()) : "";
       
       button.setText(status + " " + priority + " " + taskText);
       button.setCallbackData("task_complete:" + task.getID());
       row.add(button);
       keyboard.add(row);
   }
   
   // Botón de regreso
   List<InlineKeyboardButton> backRow = new ArrayList<>();
   InlineKeyboardButton backBtn = new InlineKeyboardButton();
   backBtn.setText("🔙 Menú Principal");
   backBtn.setCallbackData("main_menu");
   backRow.add(backBtn);
   keyboard.add(backRow);
   
   inlineKeyboard.setKeyboard(keyboard);
   message.setReplyMarkup(inlineKeyboard);
   
   try {
       execute(message);
   } catch (TelegramApiException e) {
       logger.error("Error sending tasks to complete", e);
   }
}

// ==================== TASK CREATION METHODS ====================

private void startNewTaskCreationWithSprint(long chatId, User currentUser) {
   chatState.put(chatId, STATE_SELECTING_SPRINT_FOR_TASK);
   temporaryData.put(chatId, new HashMap<>());
   
   List<Sprint> sprints = sprintService.findAll();
   
   SendMessage message = new SendMessage();
   message.setChatId(chatId);
   message.setText("📝 <b>NUEVA TAREA</b>\n\nPrimero, selecciona el sprint al que deseas asignar esta tarea:");
   message.enableHtml(true);
   
   InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup();
   List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
   
   for (Sprint sprint : sprints) {
       List<InlineKeyboardButton> row = new ArrayList<>();
       InlineKeyboardButton button = new InlineKeyboardButton();
       button.setText(sprint.getName());
       button.setCallbackData("sprint_select:" + sprint.getId());
       row.add(button);
       keyboard.add(row);
   }
   
   // Opción para no asignar a sprint
   List<InlineKeyboardButton> noSprintRow = new ArrayList<>();
   InlineKeyboardButton noSprintBtn = new InlineKeyboardButton();
   noSprintBtn.setText("📌 Sin Sprint (asignar después)");
   noSprintBtn.setCallbackData("sprint_select:0");
   noSprintRow.add(noSprintBtn);
   keyboard.add(noSprintRow);
   
   // Botón de cancelar
   List<InlineKeyboardButton> cancelRow = new ArrayList<>();
   InlineKeyboardButton cancelBtn = new InlineKeyboardButton();
   cancelBtn.setText("❌ Cancelar");
   cancelBtn.setCallbackData("main_menu");
   cancelRow.add(cancelBtn);
   keyboard.add(cancelRow);
   
   inlineKeyboard.setKeyboard(keyboard);
   message.setReplyMarkup(inlineKeyboard);
   
   try {
       execute(message);
   } catch (TelegramApiException e) {
       logger.error("Error starting task creation", e);
   }
}

private void showDeveloperSelectionForTask(long chatId, Map<String, Object> data) {
   List<User> developers = userService.findByRole("Developer");
   
   SendMessage message = new SendMessage();
   message.setChatId(chatId);
   message.setText("👨‍💻 <b>ASIGNAR DESARROLLADOR</b>\n\nSelecciona el desarrollador para esta tarea:");
   message.enableHtml(true);
   
   InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup();
   List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
   
   for (User dev : developers) {
       List<InlineKeyboardButton> row = new ArrayList<>();
       InlineKeyboardButton button = new InlineKeyboardButton();
       // Solo mostrar el nombre del desarrollador
       button.setText(dev.getName());
       button.setCallbackData("assign_dev:" + dev.getID());
       row.add(button);
       keyboard.add(row);
   }
   
   // Opción para no asignar
   List<InlineKeyboardButton> unassignedRow = new ArrayList<>();
   InlineKeyboardButton unassignedBtn = new InlineKeyboardButton();
   unassignedBtn.setText("📌 Dejar sin asignar");
   unassignedBtn.setCallbackData("assign_dev:0");
   unassignedRow.add(unassignedBtn);
   keyboard.add(unassignedRow);
   
   // Botón de cancelar
   List<InlineKeyboardButton> cancelRow = new ArrayList<>();
   InlineKeyboardButton cancelBtn = new InlineKeyboardButton();
   cancelBtn.setText("❌ Cancelar");
   cancelBtn.setCallbackData("main_menu");
   cancelRow.add(cancelBtn);
   keyboard.add(cancelRow);
   
   inlineKeyboard.setKeyboard(keyboard);
   message.setReplyMarkup(inlineKeyboard);
   
   try {
       execute(message);
   } catch (TelegramApiException e) {
       logger.error("Error showing developer selection", e);
   }
}

// ==================== TASK MODIFICATION METHODS ====================

private void showTaskModificationOptionsWithButtons(long chatId, int messageId, int taskId) {
   try {
       ResponseEntity<ToDoItem> response = toDoItemService.getItemById(taskId);
       if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
           return;
       }
       
       ToDoItem task = response.getBody();
       
       StringBuilder messageText = new StringBuilder();
       messageText.append("📝 <b>MODIFICAR TAREA</b>\n\n");
       messageText.append("<b>Nombre:</b> ").append(task.getDescription()).append("\n");
       messageText.append("<b>Estado:</b> ").append(task.getStatus()).append("\n");
       if (task.getPriority() != null) {
           messageText.append("<b>Prioridad:</b> ").append(task.getPriority()).append("\n");
       }
       if (task.getEstimatedHours() != null) {
           messageText.append("<b>Horas estimadas:</b> ").append(task.getEstimatedHours()).append("\n");
       }
       
       // Mostrar sprint si existe
       if (task.getSprintId() != null) {
           ResponseEntity<Sprint> sprintResponse = sprintService.getSprintById(task.getSprintId());
           if (sprintResponse.getStatusCode() == HttpStatus.OK && sprintResponse.getBody() != null) {
               messageText.append("<b>Sprint:</b> ").append(sprintResponse.getBody().getName()).append("\n");
           }
       } else {
           messageText.append("<b>Sprint:</b> Sin asignar\n");
       }
       
       EditMessageText editMessage = new EditMessageText();
       editMessage.setChatId(chatId);
       editMessage.setMessageId(messageId);
       editMessage.setText(messageText.toString());
       editMessage.enableHtml(true);
       
       InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup();
       List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
       
       // Opciones de modificación
       List<InlineKeyboardButton> row1 = new ArrayList<>();
       InlineKeyboardButton editNameBtn = new InlineKeyboardButton();
       editNameBtn.setText("✏️ Cambiar Nombre");
       editNameBtn.setCallbackData("edit_task_name:" + taskId);
       row1.add(editNameBtn);
       keyboard.add(row1);
       
       List<InlineKeyboardButton> row2 = new ArrayList<>();
       InlineKeyboardButton editStatusBtn = new InlineKeyboardButton();
       editStatusBtn.setText("🔄 Cambiar Estado");
       editStatusBtn.setCallbackData("change_status:" + taskId);
       row2.add(editStatusBtn);
       keyboard.add(row2);
       
       List<InlineKeyboardButton> row3 = new ArrayList<>();
       InlineKeyboardButton setHoursBtn = new InlineKeyboardButton();
       setHoursBtn.setText("⏱️ Definir Horas Estimadas");
       setHoursBtn.setCallbackData("set_estimated_hours:" + taskId);
       row3.add(setHoursBtn);
       keyboard.add(row3);
       
       // Si no tiene sprint, permitir asignarlo
       if (task.getSprintId() == null) {
           List<InlineKeyboardButton> row4 = new ArrayList<>();
           InlineKeyboardButton assignSprintBtn = new InlineKeyboardButton();
           assignSprintBtn.setText("📋 Asignar a Sprint");
           assignSprintBtn.setCallbackData("assign_task_sprint:" + taskId);
           row4.add(assignSprintBtn);
           keyboard.add(row4);
       }
       
       // Botón de regreso
       List<InlineKeyboardButton> backRow = new ArrayList<>();
       InlineKeyboardButton backBtn = new InlineKeyboardButton();
       backBtn.setText("🔙 Volver");
       backBtn.setCallbackData("list_tasks");
       backRow.add(backBtn);
       keyboard.add(backRow);
       
       inlineKeyboard.setKeyboard(keyboard);
       editMessage.setReplyMarkup(inlineKeyboard);
       
       execute(editMessage);
       
   } catch (TelegramApiException e) {
       logger.error("Error showing task modification options", e);
   }
}

private void showTaskStatusOptions(long chatId, int messageId, int taskId) {
   try {
       EditMessageText editMessage = new EditMessageText();
       editMessage.setChatId(chatId);
       editMessage.setMessageId(messageId);
       editMessage.setText("🔄 <b>CAMBIAR ESTADO</b>\n\nSelecciona el nuevo estado para la tarea:");
       editMessage.enableHtml(true);
       
       InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup();
       List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
       
       String[] statuses = {"Pending", "In Progress", "In Review", "Completed"};
       String[] emojis = {"⏳", "🔄", "👁️", "✅"};
       
       for (int i = 0; i < statuses.length; i++) {
           List<InlineKeyboardButton> row = new ArrayList<>();
           InlineKeyboardButton button = new InlineKeyboardButton();
           button.setText(emojis[i] + " " + statuses[i]);
           button.setCallbackData("task_status:" + taskId + ":" + statuses[i]);
           row.add(button);
           keyboard.add(row);
       }
       
       // Botón de regreso
       List<InlineKeyboardButton> backRow = new ArrayList<>();
       InlineKeyboardButton backBtn = new InlineKeyboardButton();
       backBtn.setText("🔙 Volver a Tarea");
       backBtn.setCallbackData("modify_task:" + taskId);
       backRow.add(backBtn);
       keyboard.add(backRow);
       
       inlineKeyboard.setKeyboard(keyboard);
       editMessage.setReplyMarkup(inlineKeyboard);
       
       execute(editMessage);
       
   } catch (TelegramApiException e) {
       logger.error("Error showing task status options", e);
   }
}

private void startEstimatedHoursEdit(long chatId, int messageId, int taskId, User currentUser) {
   try {
       Map<String, Object> data = temporaryData.getOrDefault(chatId, new HashMap<>());
       data.put("taskId", taskId);
       data.put("editMessageId", messageId);
       temporaryData.put(chatId, data);
       
       chatState.put(chatId, STATE_SETTING_ESTIMATED_HOURS);
       
       EditMessageText editMessage = new EditMessageText();
       editMessage.setChatId(chatId);
       editMessage.setMessageId(messageId);
       editMessage.setText("⏱️ <b>DEFINIR HORAS ESTIMADAS</b>\n\nEscribe el número de horas estimadas para esta tarea:");
       editMessage.enableHtml(true);
       
       execute(editMessage);
       
   } catch (TelegramApiException e) {
       logger.error("Error starting estimated hours edit", e);
   }
}

private void startSprintAssignment(long chatId, int messageId, int taskId, User currentUser) {
   try {
       Map<String, Object> data = temporaryData.getOrDefault(chatId, new HashMap<>());
       data.put("taskId", taskId);
       temporaryData.put(chatId, data);
       
       List<Sprint> sprints = sprintService.findAll();
       
       EditMessageText editMessage = new EditMessageText();
       editMessage.setChatId(chatId);
       editMessage.setMessageId(messageId);
       editMessage.setText("📋 <b>ASIGNAR A SPRINT</b>\n\nSelecciona el sprint para esta tarea:");
       editMessage.enableHtml(true);
       
       InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup();
       List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
       
       for (Sprint sprint : sprints) {
           List<InlineKeyboardButton> row = new ArrayList<>();
           InlineKeyboardButton button = new InlineKeyboardButton();
           button.setText(sprint.getName());
           button.setCallbackData("sprint_select:" + sprint.getId());
           row.add(button);
           keyboard.add(row);
       }
       
       // Botón de regreso
       List<InlineKeyboardButton> backRow = new ArrayList<>();
       InlineKeyboardButton backBtn = new InlineKeyboardButton();
       backBtn.setText("🔙 Volver a Tarea");
       backBtn.setCallbackData("modify_task:" + taskId);
       backRow.add(backBtn);
       keyboard.add(backRow);
       
       inlineKeyboard.setKeyboard(keyboard);
       editMessage.setReplyMarkup(inlineKeyboard);
       
       execute(editMessage);
       
   } catch (TelegramApiException e) {
       logger.error("Error starting sprint assignment", e);
   }
}

private void startTaskNameEdit(long chatId, int messageId, int taskId, User currentUser) {
   try {
       Map<String, Object> data = temporaryData.getOrDefault(chatId, new HashMap<>());
       data.put("taskId", taskId);
       data.put("editMessageId", messageId);
       temporaryData.put(chatId, data);
       
       chatState.put(chatId, STATE_CHANGING_TASK_NAME);
       
       EditMessageText editMessage = new EditMessageText();
       editMessage.setChatId(chatId);
       editMessage.setMessageId(messageId);
       editMessage.setText("✏️ <b>CAMBIAR NOMBRE</b>\n\nEscribe el nuevo nombre para la tarea:");
       editMessage.enableHtml(true);
       
       execute(editMessage);
       
   } catch (TelegramApiException e) {
       logger.error("Error starting task name edit", e);
   }
}

// ==================== DEVELOPER AND TEAM DISPLAY METHODS ====================

private void showDevelopersWithButtons(long chatId) {
   List<User> developers = userService.findByRole("Developer");
   
   StringBuilder message = new StringBuilder("👨‍💻 <b>EQUIPO DE DESARROLLO</b>\n\n");
   
   for (User dev : developers) {
       message.append("🔸 <b>").append(dev.getName()).append("</b>\n");
       message.append("   📧 ").append(dev.getUsername()).append("\n");
       
       // Contar tareas asignadas con más detalle
       List<ToDoItem> devTasks = toDoItemService.findByAssignedTo(dev.getID());
       long pendingTasks = devTasks.stream().filter(t -> "Pending".equals(t.getStatus())).count();
       long inProgressTasks = devTasks.stream().filter(t -> "In Progress".equals(t.getStatus())).count();
       long completedTasks = devTasks.stream().filter(t -> "Completed".equals(t.getStatus())).count();
       
       // Calcular horas trabajadas
       double totalHours = devTasks.stream()
           .filter(t -> t.getActualHours() != null)
           .mapToDouble(ToDoItem::getActualHours)
           .sum();
       
       message.append("   📊 Tareas: ").append(devTasks.size())
           .append(" (⏳").append(pendingTasks)
           .append(" 🔄").append(inProgressTasks)
           .append(" ✅").append(completedTasks).append(")\n");
       message.append("   ⏱️ Horas trabajadas: ").append(String.format("%.1f", totalHours)).append("h\n");
       
       // Mostrar eficiencia si hay datos
       if (completedTasks > 0) {
           double estimatedHours = devTasks.stream()
               .filter(t -> "Completed".equals(t.getStatus()) && t.getEstimatedHours() != null)
               .mapToDouble(ToDoItem::getEstimatedHours)
               .sum();
           
           double actualHours = devTasks.stream()
               .filter(t -> "Completed".equals(t.getStatus()) && t.getActualHours() != null)
               .mapToDouble(ToDoItem::getActualHours)
               .sum();
           
           if (actualHours > 0) {
               double efficiency = (estimatedHours / actualHours) * 100;
               String efficiencyEmoji = efficiency > 100 ? "🟢" : (efficiency >= 85 ? "🟡" : "🔴");
               message.append("   📈 Eficiencia: ").append(efficiencyEmoji).append(" ")
                   .append(String.format("%.1f", efficiency)).append("%\n");
           }
       }
       
       message.append("\n");
   }
   
   SendMessage messageToTelegram = new SendMessage();
   messageToTelegram.setChatId(chatId);
   messageToTelegram.setText(message.toString());
   messageToTelegram.enableHtml(true);
   
   InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup();
   List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
   
   // Botón de regreso
   List<InlineKeyboardButton> backRow = new ArrayList<>();
   InlineKeyboardButton backBtn = new InlineKeyboardButton();
   backBtn.setText("🔙 Menú Principal");
   backBtn.setCallbackData("main_menu");
   backRow.add(backBtn);
   keyboard.add(backRow);
   
   inlineKeyboard.setKeyboard(keyboard);
   messageToTelegram.setReplyMarkup(inlineKeyboard);
   
   try {
       execute(messageToTelegram);
   } catch (TelegramApiException e) {
       logger.error("Error showing developers", e);
   }
}

// ==================== SUMMARY AND REPORT METHODS ====================

private void showTasksSummaryMenu(long chatId) {
   SendMessage message = new SendMessage();
   message.setChatId(chatId);
   message.setText("📊 <b>RESUMEN DE TAREAS</b>\n\n¿Qué tipo de resumen deseas ver?");
   message.enableHtml(true);
   
   InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup();
   List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
   
   List<InlineKeyboardButton> row1 = new ArrayList<>();
   InlineKeyboardButton generalBtn = new InlineKeyboardButton();
   generalBtn.setText("📈 Resumen General");
   generalBtn.setCallbackData("summary_general");
   row1.add(generalBtn);
   keyboard.add(row1);
   
   List<InlineKeyboardButton> row2 = new ArrayList<>();
   InlineKeyboardButton sprintBtn = new InlineKeyboardButton();
   sprintBtn.setText("📋 Resumen por Sprint");
   sprintBtn.setCallbackData("summary_by_sprint");
   row2.add(sprintBtn);
   keyboard.add(row2);
   
   // Botón de regreso
   List<InlineKeyboardButton> backRow = new ArrayList<>();
   InlineKeyboardButton backBtn = new InlineKeyboardButton();
   backBtn.setText("🔙 Menú Principal");
   backBtn.setCallbackData("main_menu");
   backRow.add(backBtn);
   keyboard.add(backRow);
   
   inlineKeyboard.setKeyboard(keyboard);
   message.setReplyMarkup(inlineKeyboard);
   
   try {
       execute(message);
   } catch (TelegramApiException e) {
       logger.error("Error sending tasks summary menu", e);
   }
}

private void showSprintSummarySelection(long chatId) {
   List<Sprint> sprints = sprintService.findAll();
   
   SendMessage message = new SendMessage();
   message.setChatId(chatId);
   message.setText("📋 <b>RESUMEN POR SPRINT</b>\n\nSelecciona el sprint:");
   message.enableHtml(true);
   
   InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup();
   List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
   
   for (Sprint sprint : sprints) {
       List<InlineKeyboardButton> row = new ArrayList<>();
       InlineKeyboardButton button = new InlineKeyboardButton();
       button.setText(sprint.getName());
       button.setCallbackData("sprint_summary:" + sprint.getId());
       row.add(button);
       keyboard.add(row);
   }
   
   // Botón de regreso
   List<InlineKeyboardButton> backRow = new ArrayList<>();
   InlineKeyboardButton backBtn = new InlineKeyboardButton();
   backBtn.setText("🔙 Volver");
   backBtn.setCallbackData("tasks_summary");
   backRow.add(backBtn);
   keyboard.add(backRow);
   
   inlineKeyboard.setKeyboard(keyboard);
   message.setReplyMarkup(inlineKeyboard);
   
   try {
       execute(message);
   } catch (TelegramApiException e) {
       logger.error("Error sending sprint summary selection", e);
   }
}

private void showSprintTasksSummary(long chatId, int sprintId) {
   ResponseEntity<Sprint> sprintResponse = sprintService.getSprintById(sprintId);
   if (sprintResponse.getStatusCode() != HttpStatus.OK || sprintResponse.getBody() == null) {
       return;
   }
   
   Sprint sprint = sprintResponse.getBody();
   List<ToDoItem> sprintTasks = toDoItemService.findBySprintId(sprintId);
   
   StringBuilder summaryMessage = new StringBuilder();
   summaryMessage.append("📋 <b>RESUMEN - ").append(sprint.getName()).append("</b>\n\n");
   summaryMessage.append("📅 Periodo: ").append(sprint.getStartDate()).append(" - ").append(sprint.getEndDate()).append("\n\n");
   
   long pendingCount = sprintTasks.stream().filter(t -> "Pending".equals(t.getStatus())).count();
   long inProgressCount = sprintTasks.stream().filter(t -> "In Progress".equals(t.getStatus())).count();
   long inReviewCount = sprintTasks.stream().filter(t -> "In Review".equals(t.getStatus())).count();
   long completedCount = sprintTasks.stream().filter(t -> "Completed".equals(t.getStatus())).count();
   
   summaryMessage.append("🔹 <b>Total de tareas:</b> ").append(sprintTasks.size()).append("\n");
   summaryMessage.append("   ⏳ Pendientes: ").append(pendingCount).append("\n");
   summaryMessage.append("   🔄 En progreso: ").append(inProgressCount).append("\n");
   summaryMessage.append("   👁️ En revisión: ").append(inReviewCount).append("\n");
   summaryMessage.append("   ✅ Completadas: ").append(completedCount).append("\n\n");
   
   // Progreso
   double progress = sprintTasks.isEmpty() ? 0 : (double) completedCount / sprintTasks.size() * 100;
   summaryMessage.append("📈 <b>Progreso:</b> ").append(String.format("%.1f", progress)).append("%\n\n");
   
   // Horas
   double totalEstimated = sprintTasks.stream()
           .filter(t -> t.getEstimatedHours() != null)
           .mapToDouble(ToDoItem::getEstimatedHours)
           .sum();
           
   double totalActual = sprintTasks.stream()
           .filter(t -> t.getActualHours() != null)
           .mapToDouble(ToDoItem::getActualHours)
           .sum();
   
   summaryMessage.append("⏱️ <b>Horas estimadas:</b> ").append(String.format("%.1f", totalEstimated)).append("h\n");
   summaryMessage.append("⏱️ <b>Horas trabajadas:</b> ").append(String.format("%.1f", totalActual)).append("h\n");
   
   if (totalActual > 0 && totalEstimated > 0) {
       double efficiency = (totalEstimated / totalActual) * 100;
       String efficiencyEmoji = efficiency > 100 ? "🟢" : (efficiency >= 85 ? "🟡" : "🔴");
       summaryMessage.append("📊 <b>Eficiencia:</b> ").append(efficiencyEmoji).append(" ")
           .append(String.format("%.1f", efficiency)).append("%\n");
   }
   
   SendMessage message = new SendMessage();
   message.setChatId(chatId);
   message.setText(summaryMessage.toString());
   message.enableHtml(true);
   
   InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup();
   List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
   
   List<InlineKeyboardButton> backRow = new ArrayList<>();
   InlineKeyboardButton backBtn = new InlineKeyboardButton();
   backBtn.setText("🔙 Volver");
   backBtn.setCallbackData("summary_by_sprint");
   backRow.add(backBtn);
   keyboard.add(backRow);
   
   inlineKeyboard.setKeyboard(keyboard);
   message.setReplyMarkup(inlineKeyboard);
   
   try {
       execute(message);
   } catch (TelegramApiException e) {
       logger.error("Error sending sprint tasks summary", e);
   }
}

private void showHoursBySprintReport(long chatId) {
   List<Sprint> sprints = sprintService.findAll();
   
   if (sprints.isEmpty()) {
       SendMessage message = new SendMessage();
       message.setChatId(chatId);
       message.setText("No hay sprints disponibles para generar el reporte.");
       
       InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup();
       List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
       
       List<InlineKeyboardButton> backRow = new ArrayList<>();
       InlineKeyboardButton backBtn = new InlineKeyboardButton();
       backBtn.setText("🔙 Menú Principal");
       backBtn.setCallbackData("main_menu");
       backRow.add(backBtn);
       keyboard.add(backRow);
       
       inlineKeyboard.setKeyboard(keyboard);
       message.setReplyMarkup(inlineKeyboard);
       
       try {
           execute(message);
       } catch (TelegramApiException e) {
           logger.error("Error sending message", e);
       }
       return;
   }
   
   StringBuilder reportMessage = new StringBuilder();
   reportMessage.append("📊 <b>REPORTE DE HORAS POR SPRINT</b>\n\n");
   
   for (Sprint sprint : sprints) {
       reportMessage.append("🔸 <b>").append(sprint.getName()).append("</b>\n");
       
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
       
       // Calcular progreso
       long completedTasks = sprintTasks.stream().filter(t -> "Completed".equals(t.getStatus())).count();
       double progress = sprintTasks.isEmpty() ? 0 : (double) completedTasks / sprintTasks.size() * 100;
               
       reportMessage.append("   📈 Progreso: ").append(String.format("%.1f", progress)).append("%\n");
       reportMessage.append("   ⏱️ Horas estimadas: ").append(String.format("%.2f", totalEstimated)).append("h\n");
       reportMessage.append("   ⏱️ Horas trabajadas: ").append(String.format("%.2f", totalActual)).append("h\n");
       reportMessage.append("   ⏳ Horas pendientes: ").append(String.format("%.2f", totalPending)).append("h\n");
       
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
                   
           if (actualCompleted > 0) {
               double efficiency = (estimatedCompleted / actualCompleted) * 100;
               String efficiencyEmoji = efficiency > 100 ? "🟢" : (efficiency >= 85 ? "🟡" : "🔴");
               reportMessage.append("   📈 Eficiencia: ").append(efficiencyEmoji).append(" ").append(String.format("%.1f", efficiency)).append("%\n");
           }
       }
       
       reportMessage.append("\n");
   }
   
   SendMessage message = new SendMessage();
   message.setChatId(chatId);
   message.setText(reportMessage.toString());
   message.enableHtml(true);
   
   InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup();
   List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
   
   List<InlineKeyboardButton> backRow = new ArrayList<>();
   InlineKeyboardButton backBtn = new InlineKeyboardButton();
   backBtn.setText("🔙 Menú Principal");
   backBtn.setCallbackData("main_menu");
   backRow.add(backBtn);
   keyboard.add(backRow);
   
   inlineKeyboard.setKeyboard(keyboard);
   message.setReplyMarkup(inlineKeyboard);
   
   try {
       execute(message);
   } catch (TelegramApiException e) {
       logger.error("Error sending hours report", e);
   }
}

private void showTasksSummary(long chatId) {
   List<ToDoItem> allTasks = toDoItemService.findAll();
   
   StringBuilder summaryMessage = new StringBuilder();
   summaryMessage.append("📊 <b>RESUMEN GENERAL DE TAREAS</b>\n\n");
   
   long pendingCount = allTasks.stream().filter(t -> "Pending".equals(t.getStatus())).count();
   long inProgressCount = allTasks.stream().filter(t -> "In Progress".equals(t.getStatus())).count();
   long inReviewCount = allTasks.stream().filter(t -> "In Review".equals(t.getStatus())).count();
   long completedCount = allTasks.stream().filter(t -> "Completed".equals(t.getStatus())).count();
   
   summaryMessage.append("🔹 <b>Total de tareas:</b> ").append(allTasks.size()).append("\n");
   summaryMessage.append("   ⏳ Pendientes: ").append(pendingCount).append("\n");
   summaryMessage.append("   🔄 En progreso: ").append(inProgressCount).append("\n");
   summaryMessage.append("   👁️ En revisión: ").append(inReviewCount).append("\n");
   summaryMessage.append("   ✅ Completadas: ").append(completedCount).append("\n\n");
   
   // Progreso general
   double generalProgress = allTasks.isEmpty() ? 0 : (double) completedCount / allTasks.size() * 100;
   summaryMessage.append("📈 <b>Progreso general:</b> ").append(String.format("%.1f", generalProgress)).append("%\n\n");
   
   long unassignedCount = allTasks.stream().filter(t -> t.getAssignedTo() == null).count();
   summaryMessage.append("🔹 <b>Tareas sin asignar:</b> ").append(unassignedCount).append("\n\n");
   
   long withoutSprintCount = allTasks.stream().filter(t -> t.getSprintId() == null).count();
   summaryMessage.append("🔹 <b>Tareas sin sprint:</b> ").append(withoutSprintCount).append("\n\n");
   
   // Horas totales
   double totalEstimatedHours = allTasks.stream()
           .filter(t -> t.getEstimatedHours() != null)
           .mapToDouble(ToDoItem::getEstimatedHours)
           .sum();
   
   double totalActualHours = allTasks.stream()
           .filter(t -> t.getActualHours() != null)
           .mapToDouble(ToDoItem::getActualHours)
           .sum();
   
   summaryMessage.append("⏱️ <b>Horas estimadas totales:</b> ").append(String.format("%.1f", totalEstimatedHours)).append("h\n");
   summaryMessage.append("⏱️ <b>Horas trabajadas totales:</b> ").append(String.format("%.1f", totalActualHours)).append("h\n\n");
   
   // Obtener tareas con horas estimadas pero sin horas reales (incompletas)
   long estimatedButNotCompletedCount = allTasks.stream()
           .filter(t -> t.getEstimatedHours() != null && t.getActualHours() == null)
           .count();
   summaryMessage.append("🔹 <b>Tareas estimadas pero no completadas:</b> ").append(estimatedButNotCompletedCount).append("\n\n");
   
   // Obtener desarrolladores con tareas asignadas
   Map<Integer, Long> taskCountByDeveloper = allTasks.stream()
           .filter(t -> t.getAssignedTo() != null)
           .collect(Collectors.groupingBy(ToDoItem::getAssignedTo, Collectors.counting()));
           
   summaryMessage.append("🔹 <b>Tareas por desarrollador:</b>\n\n");
   
   for (Map.Entry<Integer, Long> entry : taskCountByDeveloper.entrySet()) {
       ResponseEntity<User> userResponse = userService.getUserById(entry.getKey());
       if (userResponse.getStatusCode() == HttpStatus.OK && userResponse.getBody() != null) {
           User developer = userResponse.getBody();
           long devCompletedTasks = allTasks.stream()
               .filter(t -> t.getAssignedTo() != null && t.getAssignedTo().equals(entry.getKey()) && "Completed".equals(t.getStatus()))
               .count();
           
           summaryMessage.append("   👨‍💻 ").append(developer.getName())
                   .append(": ").append(entry.getValue()).append(" total (✅").append(devCompletedTasks).append(")\n");
       }
   }
   
   SendMessage message = new SendMessage();
   message.setChatId(chatId);
   message.setText(summaryMessage.toString());
   message.enableHtml(true);
   
   InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup();
   List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
   
   List<InlineKeyboardButton> backRow = new ArrayList<>();
   InlineKeyboardButton backBtn = new InlineKeyboardButton();
   backBtn.setText("🔙 Volver");
   backBtn.setCallbackData("tasks_summary");
   backRow.add(backBtn);
   keyboard.add(backRow);
   
   inlineKeyboard.setKeyboard(keyboard);
   message.setReplyMarkup(inlineKeyboard);
   
   try {
       execute(message);
   } catch (TelegramApiException e) {
       logger.error("Error sending tasks summary", e);
   }
}

// ==================== CALLBACK HANDLERS ====================

private void handlePrioritySelection(long chatId, int messageId, String priority, User currentUser) {
   Map<String, Object> data = temporaryData.getOrDefault(chatId, new HashMap<>());
   data.put("priority", priority);
   temporaryData.put(chatId, data);
   
   // Actualizar mensaje
   EditMessageText editMessage = new EditMessageText();
   editMessage.setChatId(chatId);
   editMessage.setMessageId(messageId);
   editMessage.setText("✅ Prioridad seleccionada: " + priority + "\n\n⏱️ Ahora ingresa las horas estimadas:");
   
   try {
       execute(editMessage);
       chatState.put(chatId, STATE_ADDING_TASK_ESTIMATED_HOURS);
   } catch (TelegramApiException e) {
       logger.error("Error updating priority message", e);
   }
}

private void handleDeveloperAssignment(long chatId, int messageId, int developerId, User currentUser) {
   Map<String, Object> data = temporaryData.getOrDefault(chatId, new HashMap<>());
   
   if (developerId == 0) {
       data.put("assignedTo", null);
   } else {
       data.put("assignedTo", developerId);
   }
   
   createTaskWithSavedData(chatId, currentUser, data);
   
   try {
       DeleteMessage deleteMessage = new DeleteMessage();
       deleteMessage.setChatId(chatId);
       deleteMessage.setMessageId(messageId);
       execute(deleteMessage);
   } catch (TelegramApiException e) {
       logger.error("Error deleting message", e);
   }
}

private void handleSprintSelection(long chatId, int messageId, int sprintId, User currentUser) {
   Map<String, Object> data = temporaryData.getOrDefault(chatId, new HashMap<>());
   
   // Si estamos en el flujo de crear nueva tarea
   if (STATE_SELECTING_SPRINT_FOR_TASK.equals(chatState.get(chatId))) {
       data.put("sprintId", sprintId == 0 ? null : sprintId);
       temporaryData.put(chatId, data);
       
       // Cambiar al estado de agregar nombre de tarea
       chatState.put(chatId, STATE_ADDING_NEW_TASK);
       
       EditMessageText editMessage = new EditMessageText();
       editMessage.setChatId(chatId);
       editMessage.setMessageId(messageId);
       
       String sprintName = "Sin Sprint";
       if (sprintId != 0) {
           ResponseEntity<Sprint> sprintResponse = sprintService.getSprintById(sprintId);
           if (sprintResponse.getStatusCode() == HttpStatus.OK && sprintResponse.getBody() != null) {
               sprintName = sprintResponse.getBody().getName();
           }
       }
       
       editMessage.setText("✅ Sprint seleccionado: " + sprintName + "\n\n📝 Ahora escribe el nombre de la tarea:");
       editMessage.enableHtml(true);
       
       try {
           execute(editMessage);
       } catch (TelegramApiException e) {
           logger.error("Error updating sprint selection message", e);
       }
       return;
   }
   
   // Flujo original para asignar tarea existente a sprint
   int taskId = (int) data.get("taskId");
   
   try {
       ToDoItem task = toDoItemService.assignToSprint(taskId, sprintId);
       
       if (task != null) {
           task.setStatus("In Progress");
           toDoItemService.updateToDoItem(taskId, task);
           
           ResponseEntity<Sprint> sprintResponse = sprintService.getSprintById(sprintId);
           String sprintName = "Sprint";
           if (sprintResponse.getStatusCode() == HttpStatus.OK && sprintResponse.getBody() != null) {
               sprintName = sprintResponse.getBody().getName();
           }
           
           EditMessageText editMessage = new EditMessageText();
           editMessage.setChatId(chatId);
           editMessage.setMessageId(messageId);
           editMessage.setText("✅ Tarea asignada exitosamente al sprint \"" + sprintName + "\" y marcada como 'En Progreso'.");
           
           execute(editMessage);
       }
       
       // Limpiar estado
       chatState.put(chatId, STATE_NONE);
       temporaryData.put(chatId, new HashMap<>());
       
       // Mostrar menú principal después de un delay
       Thread.sleep(2000);
       showMainMenu(chatId, currentUser);
       
   } catch (Exception e) {
       logger.error("Error assigning task to sprint", e);
   }
}

private void handleTaskAction(long chatId, int messageId, String action, int taskId, User currentUser) {
   try {
       ResponseEntity<ToDoItem> response = toDoItemService.getItemById(taskId);
       if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
           return;
       }
       
       ToDoItem task = response.getBody();
       
       // Verificar que la tarea pertenezca al usuario
       if (task.getAssignedTo() == null || !task.getAssignedTo().equals(currentUser.getID())) {
           EditMessageText editMessage = new EditMessageText();
           editMessage.setChatId(chatId);
           editMessage.setMessageId(messageId);
           editMessage.setText("❌ Esta tarea no está asignada a ti.");
           execute(editMessage);
           return;
       }
       
       switch (action) {
           case "start":
               task.setStatus("In Progress");
               task.setDone(false);
               toDoItemService.updateToDoItem(taskId, task);
               
               EditMessageText startMessage = new EditMessageText();
               startMessage.setChatId(chatId);
               startMessage.setMessageId(messageId);
               startMessage.setText("✅ Tarea iniciada: " + task.getDescription());
               execute(startMessage);
               break;
               
           case "reopen":
               task.setStatus("In Progress");
               task.setDone(false);
               toDoItemService.updateToDoItem(taskId, task);
               
               EditMessageText reopenMessage = new EditMessageText();
               reopenMessage.setChatId(chatId);
               reopenMessage.setMessageId(messageId);
               reopenMessage.setText("🔄 Tarea reabierta: " + task.getDescription());
               execute(reopenMessage);
               break;
       }
       
       // Actualizar vista después de un delay
       Thread.sleep(1500);
       deleteMessage(chatId, messageId);
       showDeveloperTasksWithButtons(chatId, currentUser);
       
   } catch (Exception e) {
       logger.error("Error handling task action", e);
   }
}

// ==================== NEW DIRECT STATUS UPDATE HANDLER ====================

private void handleDirectTaskStatusUpdate(long chatId, int messageId, int taskId, String newStatus, User currentUser) {
   try {
       ResponseEntity<ToDoItem> response = toDoItemService.getItemById(taskId);
       if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
           return;
       }
       
       ToDoItem task = response.getBody();
       
       // Verificar que la tarea pertenezca al usuario
       if (task.getAssignedTo() == null || !task.getAssignedTo().equals(currentUser.getID())) {
           EditMessageText editMessage = new EditMessageText();
           editMessage.setChatId(chatId);
           editMessage.setMessageId(messageId);
           editMessage.setText("❌ Esta tarea no está asignada a ti.");
           execute(editMessage);
           return;
       }
       
       task.setStatus(newStatus);
       if ("Completed".equals(newStatus)) {
           task.setDone(true);
       } else {
           task.setDone(false);
       }
       
       toDoItemService.updateToDoItem(taskId, task);
       
       // Mensaje de confirmación más elegante
       String statusEmoji = getStatusEmoji(newStatus);
       EditMessageText editMessage = new EditMessageText();
       editMessage.setChatId(chatId);
       editMessage.setMessageId(messageId);
       editMessage.setText("✅ <b>Estado actualizado</b>\n\n" + 
                          statusEmoji + " " + task.getDescription() + "\n" +
                          "🔄 Ahora: <b>" + newStatus + "</b>");
       editMessage.enableHtml(true);
       
       execute(editMessage);
       
       // Actualizar la vista de tareas después de un delay
       Thread.sleep(2000);
       deleteMessage(chatId, messageId);
       showDeveloperTasksWithButtons(chatId, currentUser);
       
   } catch (Exception e) {
       logger.error("Error updating task status directly", e);
   }
}

private void handleTaskCompletion(long chatId, int messageId, int taskId, User currentUser) {
   try {
       ResponseEntity<ToDoItem> response = toDoItemService.getItemById(taskId);
       if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
           return;
       }
       
       ToDoItem task = response.getBody();
       
       // Verificar que la tarea pertenezca al usuario
       if (task.getAssignedTo() == null || !task.getAssignedTo().equals(currentUser.getID())) {
           EditMessageText editMessage = new EditMessageText();
           editMessage.setChatId(chatId);
           editMessage.setMessageId(messageId);
           editMessage.setText("❌ Esta tarea no está asignada a ti.");
           execute(editMessage);
           return;
       }
       
       // Si la tarea tiene horas estimadas, pedir horas reales
       if (task.getEstimatedHours() != null) {
           Map<String, Object> data = temporaryData.getOrDefault(chatId, new HashMap<>());
           data.put("taskId", taskId);
           data.put("completionMessageId", messageId);
           temporaryData.put(chatId, data);
           
           chatState.put(chatId, STATE_COMPLETING_TASK_HOURS);
           
           EditMessageText editMessage = new EditMessageText();
           editMessage.setChatId(chatId);
           editMessage.setMessageId(messageId);
           editMessage.setText("🎯 <b>COMPLETANDO TAREA</b>\n\n" +
                              "📝 " + task.getDescription() + "\n" +
                              "⏱️ Tiempo estimado: " + task.getEstimatedHours() + " horas\n\n" +
                              "💭 <b>¿Cuántas horas trabajaste realmente?</b>\n" +
                              "Escribe el número de horas:");
           editMessage.enableHtml(true);
           execute(editMessage);
       } else {
           // Completar directamente si no tiene horas estimadas
           task.setStatus("Completed");
           task.setDone(true);
           toDoItemService.updateToDoItem(taskId, task);
           
           EditMessageText editMessage = new EditMessageText();
           editMessage.setChatId(chatId);
           editMessage.setMessageId(messageId);
           editMessage.setText("🎉 <b>¡TAREA COMPLETADA!</b>\n\n" +
                              "✅ " + task.getDescription() + "\n\n" +
                              "¡Excelente trabajo! 🚀");
           editMessage.enableHtml(true);
           execute(editMessage);
           
           // Actualizar vista después de un delay
           Thread.sleep(2500);
           deleteMessage(chatId, messageId);
           showDeveloperTasksWithButtons(chatId, currentUser);
       }
       
   } catch (Exception e) {
       logger.error("Error handling task completion", e);
   }
}

private void handleTaskStatusChange(long chatId, int messageId, int taskId, String newStatus, User currentUser) {
   try {
       ResponseEntity<ToDoItem> response = toDoItemService.getItemById(taskId);
       if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
           ToDoItem task = response.getBody();
           
           // Si cambia a Completed, pedir horas reales si tiene horas estimadas
           if ("Completed".equals(newStatus) && task.getEstimatedHours() != null && task.getActualHours() == null) {
               Map<String, Object> data = temporaryData.getOrDefault(chatId, new HashMap<>());
               data.put("taskId", taskId);
               data.put("newStatus", newStatus);
               data.put("completionMessageId", messageId);
               temporaryData.put(chatId, data);
               
               chatState.put(chatId, STATE_COMPLETING_TASK_HOURS);
               
               EditMessageText editMessage = new EditMessageText();
               editMessage.setChatId(chatId);
               editMessage.setMessageId(messageId);
               editMessage.setText("🎯 <b>COMPLETANDO TAREA</b>\n\n" +
                                  "📝 " + task.getDescription() + "\n" +
                                  "⏱️ Tiempo estimado: " + task.getEstimatedHours() + " horas\n\n" +
                                  "💭 <b>¿Cuántas horas trabajaste realmente?</b>\n" +
                                  "Escribe el número de horas:");
               editMessage.enableHtml(true);
               execute(editMessage);
               return;
           }
           
           task.setStatus(newStatus);
           if ("Completed".equals(newStatus)) {
               task.setDone(true);
           } else {
               task.setDone(false);
           }
           
           toDoItemService.updateToDoItem(taskId, task);
           
           String statusEmoji = getStatusEmoji(newStatus);
           EditMessageText editMessage = new EditMessageText();
           editMessage.setChatId(chatId);
           editMessage.setMessageId(messageId);
           editMessage.setText("✅ <b>Estado actualizado</b>\n\n" + 
                              statusEmoji + " " + task.getDescription() + "\n" +
                              "🔄 Nuevo estado: <b>" + newStatus + "</b>");
           editMessage.enableHtml(true);
           
           execute(editMessage);
           
           // Volver al menú después de un delay
           Thread.sleep(2500);
           deleteMessage(chatId, messageId);
           showMainMenu(chatId, currentUser);
       }
   } catch (Exception e) {
       logger.error("Error updating task status", e);
   }
}

private void handleTaskDeletion(long chatId, int messageId, int taskId, User currentUser) {
   try {
       toDoItemService.deleteToDoItem(taskId);
       
       EditMessageText editMessage = new EditMessageText();
       editMessage.setChatId(chatId);
       editMessage.setMessageId(messageId);
       editMessage.setText("✅ Tarea eliminada exitosamente.");
       
       execute(editMessage);
       
       // Volver al menú principal después de un delay
       Thread.sleep(2000);
       deleteMessage(chatId, messageId);
       showMainMenu(chatId, currentUser);
       
   } catch (Exception e) {
       logger.error("Error deleting task", e);
   }
}

// ==================== TEXT MESSAGE PROCESSING ====================

private void processAuthorizedRequest(Update update) {
   if (update.hasMessage() && update.getMessage().hasText()) {
       String messageTextFromTelegram = update.getMessage().getText();
       long chatId = update.getMessage().getChatId();
       User currentUser = authorizedUsers.get(chatId);
       
       // Obtener el estado actual
       String currentState = chatState.getOrDefault(chatId, STATE_NONE);
       Map<String, Object> data = temporaryData.getOrDefault(chatId, new HashMap<>());

       // Manejar cancelación en cualquier estado
       if (messageTextFromTelegram.equals("❌ Cancelar")) {
           chatState.put(chatId, STATE_NONE);
           temporaryData.put(chatId, new HashMap<>());
           showMainMenu(chatId, currentUser);
           return;
       }

       // Manejar estados de conversación
       switch (currentState) {
           case STATE_ADDING_NEW_TASK:
               handleAddingNewTaskState(chatId, messageTextFromTelegram, currentUser);
               return;
           case STATE_ADDING_TASK_ESTIMATED_HOURS:
               handleAddingTaskEstimatedHoursState(chatId, messageTextFromTelegram, currentUser, data);
               return;
           case STATE_COMPLETING_TASK_HOURS:
               handleCompletingTaskHoursState(chatId, messageTextFromTelegram, currentUser, data);
               return;
           case STATE_CHANGING_TASK_NAME:
               handleChangingTaskNameState(chatId, messageTextFromTelegram, currentUser, data);
               return;
           case STATE_SETTING_ESTIMATED_HOURS:
               handleSettingEstimatedHoursState(chatId, messageTextFromTelegram, currentUser, data);
               return;
           // Estados KPI
           case STATE_KPI_MENU:
               handleKpiMenuState(chatId, messageTextFromTelegram, currentUser, data);
               return;
           case STATE_KPI_SELECTING_SPRINT:
               handleKpiSelectingSprintState(chatId, messageTextFromTelegram, currentUser, data);
               return;
           case STATE_KPI_SELECTING_DEVELOPER:
               handleKpiSelectingDeveloperState(chatId, messageTextFromTelegram, currentUser, data);
               return;
       }

       // Procesar comandos básicos
       if (messageTextFromTelegram.equals(BotCommands.START_COMMAND.getCommand())) {
           showMainMenu(chatId, currentUser);
       } else if (messageTextFromTelegram.equals(BotCommands.HIDE_COMMAND.getCommand())) {
           BotHelper.sendMessageToTelegram(chatId, BotMessages.BYE.getMessage(), this);
       } else {
           // Respuesta por defecto con sugerencia de usar botones
           SendMessage message = new SendMessage();
           message.setChatId(chatId);
           message.setText("🤖 Te recomiendo usar los botones del menú para una mejor experiencia.\n\n¿Necesitas volver al menú principal?");
           
           InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup();
           List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
           
           List<InlineKeyboardButton> row = new ArrayList<>();
           InlineKeyboardButton menuBtn = new InlineKeyboardButton();
           menuBtn.setText("🏠 Menú Principal");
           menuBtn.setCallbackData("main_menu");
           row.add(menuBtn);
           keyboard.add(row);
           
           inlineKeyboard.setKeyboard(keyboard);
           message.setReplyMarkup(inlineKeyboard);
           
           try {
               execute(message);
           } catch (TelegramApiException e) {
               logger.error("Error sending default message", e);
           }
       }
   }
}

// ==================== STATE HANDLERS ====================

private void handleAddingNewTaskState(long chatId, String messageText, User currentUser) {
   Map<String, Object> data = temporaryData.getOrDefault(chatId, new HashMap<>());
   data.put("description", messageText);
   temporaryData.put(chatId, data);
   
   // Mostrar selección de prioridad con botones
   SendMessage message = new SendMessage();
   message.setChatId(chatId);
   message.setText("⚠️ <b>SELECCIONAR PRIORIDAD</b>\n\nSelecciona la prioridad para la tarea:\n\n📝 " + messageText);
   message.enableHtml(true);
   
   InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup();
   List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
   
   // Botones de prioridad
   List<InlineKeyboardButton> row1 = new ArrayList<>();
   InlineKeyboardButton lowBtn = new InlineKeyboardButton();
   lowBtn.setText("🟢 Low (Baja)");
   lowBtn.setCallbackData("priority:Low");
   row1.add(lowBtn);
   
   InlineKeyboardButton mediumBtn = new InlineKeyboardButton();
   mediumBtn.setText("🟡 Medium (Media)");
   mediumBtn.setCallbackData("priority:Medium");
   row1.add(mediumBtn);
   keyboard.add(row1);
   
   List<InlineKeyboardButton> row2 = new ArrayList<>();
   InlineKeyboardButton highBtn = new InlineKeyboardButton();
   highBtn.setText("🟠 High (Alta)");
   highBtn.setCallbackData("priority:High");
   row2.add(highBtn);
   
   InlineKeyboardButton criticalBtn = new InlineKeyboardButton();
   criticalBtn.setText("🔴 Critical (Crítica)");
   criticalBtn.setCallbackData("priority:Critical");
   row2.add(criticalBtn);
   keyboard.add(row2);
   
   // Botón de cancelar
   List<InlineKeyboardButton> cancelRow = new ArrayList<>();
   InlineKeyboardButton cancelBtn = new InlineKeyboardButton();
   cancelBtn.setText("❌ Cancelar");
   cancelBtn.setCallbackData("main_menu");
   cancelRow.add(cancelBtn);
   keyboard.add(cancelRow);
   
   inlineKeyboard.setKeyboard(keyboard);
   message.setReplyMarkup(inlineKeyboard);
   
   try {
       execute(message);
   } catch (TelegramApiException e) {
       logger.error("Error showing priority selection", e);
   }
}

private void handleAddingTaskEstimatedHoursState(long chatId, String messageText, User currentUser, Map<String, Object> data) {
   try {
       double hours = Double.parseDouble(messageText);
       
       if (hours <= 0) {
           SendMessage errorMessage = new SendMessage();
           errorMessage.setChatId(chatId);
           errorMessage.setText("❌ El número de horas debe ser mayor que cero. Por favor, intenta nuevamente:");
           execute(errorMessage);
           return;
       }
       
       data.put("estimatedHours", hours);
       temporaryData.put(chatId, data);
       
       // Si es developer, autoasignarse
       if ("Developer".equals(currentUser.getRole())) {
           data.put("assignedTo", currentUser.getID());
           createTaskWithSavedData(chatId, currentUser, data);
       } else {
           // Si es manager, mostrar desarrolladores con botones
           showDeveloperSelectionForTask(chatId, data);
       }
       
   } catch (NumberFormatException e) {
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

private void handleCompletingTaskHoursState(long chatId, String messageText, User currentUser, Map<String, Object> data) {
   try {
       double hours = Double.parseDouble(messageText);
       int taskId = (int) data.get("taskId");
       int completionMessageId = (int) data.getOrDefault("completionMessageId", 0);
       
       if (hours <= 0) {
           SendMessage errorMessage = new SendMessage();
           errorMessage.setChatId(chatId);
           errorMessage.setText("❌ El número de horas debe ser mayor que cero. Por favor, intenta nuevamente:");
           execute(errorMessage);
           return;
       }
       
       ResponseEntity<ToDoItem> response = toDoItemService.getItemById(taskId);
       if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
           ToDoItem task = response.getBody();
           
           task.setActualHours(hours);
           
           // Verificar si hay un estado pendiente de cambiar
           String newStatus = (String) data.get("newStatus");
           if (newStatus != null) {
               task.setStatus(newStatus);
               if ("Completed".equals(newStatus)) {
                   task.setDone(true);
               }
           } else {
               task.setStatus("Completed");
               task.setDone(true);
           }
           
           toDoItemService.updateToDoItem(taskId, task);
           
           // Crear mensaje de celebración más elaborado
           StringBuilder successMessage = new StringBuilder();
           successMessage.append("🎉 <b>¡TAREA COMPLETADA!</b>\n\n");
           successMessage.append("✅ ").append(task.getDescription()).append("\n\n");
           
           if (task.getEstimatedHours() != null) {
               successMessage.append("📊 <b>RESUMEN:</b>\n");
               successMessage.append("⏱️ Tiempo estimado: ").append(task.getEstimatedHours()).append("h\n");
               successMessage.append("⏱️ Tiempo real: ").append(hours).append("h\n");
               
               double diff = hours - task.getEstimatedHours();
               if (Math.abs(diff) < 0.01) {
                   successMessage.append("🎯 ¡Perfecto! Completada exactamente en el tiempo estimado.\n");
               } else if (diff > 0) {
                   successMessage.append("⚠️ Tomó ").append(String.format("%.1f", diff)).append(" horas extra.\n");
               } else {
                   successMessage.append("🚀 ¡Excelente! Terminaste ").append(String.format("%.1f", Math.abs(diff))).append(" horas antes.\n");
               }
               
               // Calcular eficiencia
               if (hours > 0) {
                   double efficiency = (task.getEstimatedHours() / hours) * 100;
                   String efficiencyEmoji = efficiency > 100 ? "🟢" : (efficiency >= 85 ? "🟡" : "🔴");
                   successMessage.append("📈 Eficiencia: ").append(efficiencyEmoji).append(" ")
                       .append(String.format("%.1f", efficiency)).append("%\n");
               }
           }
           
           successMessage.append("\n🏆 ¡Sigue así, gran trabajo!");
           
           SendMessage message = new SendMessage();
           message.setChatId(chatId);
           message.setText(successMessage.toString());
           message.enableHtml(true);
           
           InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup();
           List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
           
           List<InlineKeyboardButton> row = new ArrayList<>();
           InlineKeyboardButton menuBtn = new InlineKeyboardButton();
           menuBtn.setText("🏠 Menú Principal");
           menuBtn.setCallbackData("main_menu");
           row.add(menuBtn);
           
           InlineKeyboardButton tasksBtn = new InlineKeyboardButton();
           tasksBtn.setText("📋 Ver Mis Tareas");
           tasksBtn.setCallbackData("list_tasks");
           row.add(tasksBtn);
           keyboard.add(row);
           
           inlineKeyboard.setKeyboard(keyboard);
           message.setReplyMarkup(inlineKeyboard);
           
           execute(message);
           
           // Eliminar el mensaje anterior si existe
           if (completionMessageId > 0) {
               try {
                   deleteMessage(chatId, completionMessageId);
               } catch (Exception e) {
                   logger.warn("No se pudo eliminar mensaje de completación: " + e.getMessage());
               }
           }
       }
       
       chatState.put(chatId, STATE_NONE);
       temporaryData.put(chatId, new HashMap<>());
       
   } catch (NumberFormatException e) {
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

private void handleChangingTaskNameState(long chatId, String messageText, User currentUser, Map<String, Object> data) {
   int taskId = (int) data.get("taskId");
   int editMessageId = (int) data.get("editMessageId");
   
   try {
       ResponseEntity<ToDoItem> response = toDoItemService.getItemById(taskId);
       if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
           ToDoItem task = response.getBody();
           task.setDescription(messageText);
           toDoItemService.updateToDoItem(taskId, task);
           
           EditMessageText editMessage = new EditMessageText();
           editMessage.setChatId(chatId);
           editMessage.setMessageId(editMessageId);
           editMessage.setText("✅ Nombre actualizado exitosamente.\n\n📝 Nuevo nombre: " + messageText);
           
           InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup();
           List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
           
           List<InlineKeyboardButton> row = new ArrayList<>();
           InlineKeyboardButton menuBtn = new InlineKeyboardButton();
           menuBtn.setText("🏠 Menú Principal");
           menuBtn.setCallbackData("main_menu");
           row.add(menuBtn);
           keyboard.add(row);
           
           inlineKeyboard.setKeyboard(keyboard);
           editMessage.setReplyMarkup(inlineKeyboard);
           
           execute(editMessage);
       }
       
       chatState.put(chatId, STATE_NONE);
       temporaryData.put(chatId, new HashMap<>());
       
   } catch (Exception e) {
       logger.error("Error updating task description", e);
   }
}

private void handleSettingEstimatedHoursState(long chatId, String messageText, User currentUser, Map<String, Object> data) {
   try {
       double hours = Double.parseDouble(messageText);
       int taskId = (int) data.get("taskId");
       int editMessageId = (int) data.get("editMessageId");
       
       if (hours <= 0) {
           SendMessage errorMessage = new SendMessage();
           errorMessage.setChatId(chatId);
           errorMessage.setText("❌ El número de horas debe ser mayor que cero. Por favor, intenta nuevamente:");
           execute(errorMessage);
           return;
       }
       
       ResponseEntity<ToDoItem> response = toDoItemService.getItemById(taskId);
       if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
           ToDoItem task = response.getBody();
           task.setEstimatedHours(hours);
           toDoItemService.updateToDoItem(taskId, task);
           
           EditMessageText editMessage = new EditMessageText();
           editMessage.setChatId(chatId);
           editMessage.setMessageId(editMessageId);
           editMessage.setText("✅ Horas estimadas actualizadas exitosamente.\n\n⏱️ Horas estimadas: " + hours);
           
           InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup();
           List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
           
           List<InlineKeyboardButton> row = new ArrayList<>();
           InlineKeyboardButton menuBtn = new InlineKeyboardButton();
           menuBtn.setText("🏠 Menú Principal");
           menuBtn.setCallbackData("main_menu");
           row.add(menuBtn);
           keyboard.add(row);
           
           inlineKeyboard.setKeyboard(keyboard);
           editMessage.setReplyMarkup(inlineKeyboard);
           
           execute(editMessage);
       }
       
       chatState.put(chatId, STATE_NONE);
       temporaryData.put(chatId, new HashMap<>());
       
   } catch (NumberFormatException e) {
       SendMessage errorMessage = new SendMessage();
       errorMessage.setChatId(chatId);
       errorMessage.setText("❌ Por favor, ingresa un número válido para las horas:");
       
       try {
           execute(errorMessage);
       } catch (TelegramApiException ex) {
           logger.error("Error sending error message", ex);
       }
   } catch (Exception e) {
       logger.error("Error updating estimated hours", e);
   }
}

// ==================== TASK CREATION HELPER ====================

private void createTaskWithSavedData(long chatId, User currentUser, Map<String, Object> data) {
   try {
       String description = (String) data.get("description");
       String priority = (String) data.get("priority");
       Integer assignedTo = (Integer) data.get("assignedTo");
       Double estimatedHours = (Double) data.get("estimatedHours");
       Integer sprintId = (Integer) data.get("sprintId");
       
       if (estimatedHours != null && estimatedHours > 4.0) {
           // Subdividir tarea
           int numberOfSubtasks = (int) Math.ceil(estimatedHours / 4.0);
           double hoursPerSubtask = estimatedHours / numberOfSubtasks;
           
           StringBuilder resultMessage = new StringBuilder();
           resultMessage.append("⚠️ <b>TAREA SUBDIVIDIDA</b>\n\n");
           resultMessage.append("La tarea excede las 4 horas permitidas. Se ha dividido en ").append(numberOfSubtasks).append(" subtareas:\n\n");
           
           for (int i = 1; i <= numberOfSubtasks; i++) {
               ToDoItem subTask = new ToDoItem();
               subTask.setDescription(description + " (Parte " + i + " de " + numberOfSubtasks + ")");
               subTask.setCreation_ts(OffsetDateTime.now());
               subTask.setDone(false);
               subTask.setCreatedBy(currentUser.getID());
               subTask.setAssignedTo(assignedTo);
               subTask.setEstimatedHours(hoursPerSubtask);
               subTask.setStatus("Pending");
               subTask.setPriority(priority);
               subTask.setSprintId(sprintId);
               
               toDoItemService.addToDoItem(subTask);
               
               resultMessage.append("📌 ").append(subTask.getDescription()).append(" - ").append(String.format("%.2f", hoursPerSubtask)).append(" horas\n");
           }
           
           String developerName = "Sin asignar";
           if (assignedTo != null) {
               ResponseEntity<User> response = userService.getUserById(assignedTo);
               if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                   developerName = response.getBody().getName();
               }
           }
           
           String sprintName = "Sin Sprint";
           if (sprintId != null) {
               ResponseEntity<Sprint> sprintResponse = sprintService.getSprintById(sprintId);
               if (sprintResponse.getStatusCode() == HttpStatus.OK && sprintResponse.getBody() != null) {
                   sprintName = sprintResponse.getBody().getName();
               }
           }
           
           resultMessage.append("\n⚠️ Prioridad: ").append(priority).append("\n");
           resultMessage.append("👨‍💻 Asignadas a: ").append(developerName).append("\n");
           resultMessage.append("📋 Sprint: ").append(sprintName);
           
           SendMessage message = new SendMessage();
           message.setChatId(chatId);
           message.setText(resultMessage.toString());
           message.enableHtml(true);
           
           InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup();
           List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
           
           List<InlineKeyboardButton> row = new ArrayList<>();
           InlineKeyboardButton menuBtn = new InlineKeyboardButton();
           menuBtn.setText("🏠 Menú Principal");
           menuBtn.setCallbackData("main_menu");
           row.add(menuBtn);
           
           InlineKeyboardButton addMoreBtn = new InlineKeyboardButton();
           addMoreBtn.setText("➕ Crear Otra Tarea");
           addMoreBtn.setCallbackData("add_task");
           row.add(addMoreBtn);
           keyboard.add(row);
           
           inlineKeyboard.setKeyboard(keyboard);
           message.setReplyMarkup(inlineKeyboard);
           
           execute(message);
           
       } else {
           // Crear tarea normal
           ToDoItem newTask = new ToDoItem();
           newTask.setDescription(description);
           newTask.setCreation_ts(OffsetDateTime.now());
           newTask.setDone(false);
           newTask.setCreatedBy(currentUser.getID());
           newTask.setStatus("Pending");
           newTask.setPriority(priority);
           newTask.setAssignedTo(assignedTo);
           newTask.setEstimatedHours(estimatedHours);
           newTask.setSprintId(sprintId);
           
           toDoItemService.addToDoItem(newTask);
           
           String developerName = "Sin asignar";
           if (assignedTo != null) {
               ResponseEntity<User> response = userService.getUserById(assignedTo);
               if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                   developerName = response.getBody().getName();
               }
           }
           
           String sprintName = "Sin Sprint";
           if (sprintId != null) {
               ResponseEntity<Sprint> sprintResponse = sprintService.getSprintById(sprintId);
               if (sprintResponse.getStatusCode() == HttpStatus.OK && sprintResponse.getBody() != null) {
                   sprintName = sprintResponse.getBody().getName();
               }
           }
           
           StringBuilder successMessage = new StringBuilder();
           successMessage.append("✅ <b>TAREA CREADA</b>\n\n");
           successMessage.append("📌 ").append(description).append("\n");
           successMessage.append("🔄 Estado: Pending\n");
           successMessage.append("⚠️ Prioridad: ").append(priority).append("\n");
           successMessage.append("⏱️ Horas estimadas: ").append(estimatedHours).append("\n");
           successMessage.append("👨‍💻 Asignada a: ").append(developerName).append("\n");
           successMessage.append("📋 Sprint: ").append(sprintName);
           
           SendMessage message = new SendMessage();
           message.setChatId(chatId);
           message.setText(successMessage.toString());
           message.enableHtml(true);
           
           InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup();
           List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
           
           List<InlineKeyboardButton> row = new ArrayList<>();
           InlineKeyboardButton menuBtn = new InlineKeyboardButton();
           menuBtn.setText("🏠 Menú Principal");
           menuBtn.setCallbackData("main_menu");
           row.add(menuBtn);
           
           InlineKeyboardButton addMoreBtn = new InlineKeyboardButton();
           addMoreBtn.setText("➕ Crear Otra Tarea");
           addMoreBtn.setCallbackData("add_task");
           row.add(addMoreBtn);
           keyboard.add(row);
           
           inlineKeyboard.setKeyboard(keyboard);
           message.setReplyMarkup(inlineKeyboard);
           
           execute(message);
       }
       
       chatState.put(chatId, STATE_NONE);
       temporaryData.put(chatId, new HashMap<>());
       
   } catch (Exception e) {
       logger.error("Error creating new task", e);
       SendMessage errorMessage = new SendMessage();
       errorMessage.setChatId(chatId);
       errorMessage.setText("❌ Error al crear la tarea. Por favor, intenta nuevamente.");
       
       try {
           execute(errorMessage);
           showMainMenu(chatId, currentUser);
       } catch (TelegramApiException ex) {
           logger.error("Error sending error message", ex);
       }
   }
}

// ==================== KPI HANDLERS ====================

private void handleKpiMenuState(long chatId, String messageText, User currentUser, Map<String, Object> data) {
   SendMessage errorMessage = new SendMessage();
   errorMessage.setChatId(chatId);
   errorMessage.setText("🤖 Por favor, usa los botones para navegar por los KPIs.");
   
   try {
       execute(errorMessage);
       if (kpiController != null) {
           kpiController.showKpiMenuWithButtons(chatId, this, currentUser);
       }
   } catch (TelegramApiException e) {
       logger.error("Error sending KPI menu message", e);
   }
}

private void handleKpiSelectingSprintState(long chatId, String messageText, User currentUser, Map<String, Object> data) {
   if (kpiController == null) {
       SendMessage errorMessage = new SendMessage();
       errorMessage.setChatId(chatId);
       errorMessage.setText("❌ Error interno: No se pudo cargar el componente KPI.");
       try {
           execute(errorMessage);
           showMainMenu(chatId, currentUser);
       } catch (TelegramApiException e) {
           logger.error("Error enviando mensaje de error", e);
       }
       return;
   }
   
   if (messageText.equalsIgnoreCase("menu")) {
       chatState.put(chatId, STATE_NONE);
       showMainMenu(chatId, currentUser);
       return;
   }
   
   try {
       int sprintId = Integer.parseInt(messageText);
       String kpiType = (String) data.get("kpiType");
       
       switch (kpiType) {
           case "completedTasks":
               kpiController.showCompletedTasksBySprint(chatId, this, sprintId);
               break;
           case "teamSprint":
               kpiController.showTeamKpiBySprint(chatId, this, sprintId);
               break;
           case "personalSprint":
               int userId = (int) data.get("userId");
               kpiController.showPersonalKpiBySprint(chatId, this, userId, sprintId);
               break;
       }
       
       chatState.put(chatId, STATE_NONE);
       showMainMenu(chatId, currentUser);
       
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

private void handleKpiSelectingDeveloperState(long chatId, String messageText, User currentUser, Map<String, Object> data) {
   if (kpiController == null) {
       SendMessage errorMessage = new SendMessage();
       errorMessage.setChatId(chatId);
       errorMessage.setText("❌ Error interno: No se pudo cargar el componente KPI.");
       try {
           execute(errorMessage);
           showMainMenu(chatId, currentUser);
       } catch (TelegramApiException e) {
           logger.error("Error enviando mensaje de error", e);
       }
       return;
   }
   
   if (messageText.equalsIgnoreCase("menu")) {
       chatState.put(chatId, STATE_NONE);
       showMainMenu(chatId, currentUser);
       return;
   }
   
   try {
       int userId = Integer.parseInt(messageText);
       String kpiType = (String) data.get("kpiType");
       data.put("userId", userId);
       
       switch (kpiType) {
           case "personalSprint":
               chatState.put(chatId, STATE_KPI_SELECTING_SPRINT);
               kpiController.showSprintSelectionForKpiWithButtons(chatId, this, "personalSprint");
               break;
       }
       
   } catch (NumberFormatException e) {
       SendMessage errorMessage = new SendMessage();
       errorMessage.setChatId(chatId);
       errorMessage.setText("❌ Por favor, ingresa un número válido para el ID del desarrollador o 'menu' para volver.");
       
       try {
           execute(errorMessage);
       } catch (TelegramApiException ex) {
           logger.error("Error sending error message", ex);
       }
   }
}

private void handleKpiSprintSelection(long chatId, String kpiType, int sprintId, User currentUser) {
   if (kpiController == null) {
       return;
   }
   
   switch (kpiType) {
       case "completedTasks":
           kpiController.showCompletedTasksBySprint(chatId, this, sprintId);
           break;
       case "teamSprint":
           kpiController.showTeamKpiBySprint(chatId, this, sprintId);
           break;
   }
   
   // Volver al menú principal después de un delay
   try {
       Thread.sleep(2000);
       showMainMenu(chatId, currentUser);
   } catch (InterruptedException e) {
       Thread.currentThread().interrupt();
   }
}

private void handleKpiDeveloperSelection(long chatId, String kpiType, int developerId, User currentUser) {
   if (kpiController == null) {
       return;
   }
   
   // Mostrar selección de sprint para KPI personal
   kpiController.showSprintSelectionForPersonalKpi(chatId, this, developerId);
}

// ==================== UTILITY METHODS ====================

private void deleteMessage(long chatId, int messageId) {
   try {
       DeleteMessage deleteMessage = new DeleteMessage();
       deleteMessage.setChatId(chatId);
       deleteMessage.setMessageId(messageId);
       execute(deleteMessage);
   } catch (TelegramApiException e) {
       logger.warn("No se pudo eliminar el mensaje: " + e.getMessage());
   }
}

private String getStatusEmoji(String status) {
   switch (status) {
       case "Pending": return "⏳";
       case "In Progress": return "🔄";
       case "In Review": return "👁️";
       case "Completed": return "✅";
       default: return "📌";
   }
}

private String getPriorityEmoji(String priority) {
   switch (priority) {
       case "Low": return "🟢";
       case "Medium": return "🟡";
       case "High": return "🟠";
       case "Critical": return "🔴";
       default: return "⚪";
   }
}
}