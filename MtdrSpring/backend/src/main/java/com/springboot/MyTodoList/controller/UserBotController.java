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

import com.springboot.MyTodoList.model.ToDoItem;
import com.springboot.MyTodoList.model.User;
import com.springboot.MyTodoList.service.ToDoItemService;
import com.springboot.MyTodoList.service.UserService;
import com.springboot.MyTodoList.util.BotCommands;
import com.springboot.MyTodoList.util.BotHelper;
import com.springboot.MyTodoList.util.BotLabels;
import com.springboot.MyTodoList.util.BotMessages;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class UserBotController extends TelegramLongPollingBot {

    private static final Logger logger = LoggerFactory.getLogger(UserBotController.class);
    private ToDoItemService toDoItemService;
    private UserService userService;
    private String botName;
    private Map<Long, User> authorizedUsers = new HashMap<>();

    public UserBotController(String botToken, String botName, ToDoItemService toDoItemService, UserService userService) {
        super(botToken);
        logger.info("Bot Token: " + botToken);
        logger.info("Bot name: " + botName);
        this.toDoItemService = toDoItemService;
        this.userService = userService;
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
        List<KeyboardRow> keyboard = new ArrayList<>();

        // first row
        KeyboardRow row = new KeyboardRow();
        row.add(BotLabels.LIST_ALL_ITEMS.getLabel());
        row.add(BotLabels.ADD_NEW_ITEM.getLabel());
        // Add the first row to the keyboard
        keyboard.add(row);

        // second row
        row = new KeyboardRow();
        row.add(BotLabels.SHOW_MAIN_SCREEN.getLabel());
        row.add(BotLabels.HIDE_MAIN_SCREEN.getLabel());
        keyboard.add(row);

        // Si el usuario es Manager, mostrar opciones adicionales
        if ("Manager".equals(user.getRole())) {
            row = new KeyboardRow();
            row.add("Ver Desarrolladores");
            row.add("Asignar Tarea");
            keyboard.add(row);
        }

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
                    || messageTextFromTelegram.equals(BotLabels.LIST_ALL_ITEMS.getLabel())
                    || messageTextFromTelegram.equals(BotLabels.MY_TODO_LIST.getLabel())) {
                
                showTodoList(chatId, currentUser);

            } else if (messageTextFromTelegram.equals(BotCommands.ADD_ITEM.getCommand())
                    || messageTextFromTelegram.equals(BotLabels.ADD_NEW_ITEM.getLabel())) {
                
                requestNewTodoItem(chatId);

            } else if (messageTextFromTelegram.equals("Ver Desarrolladores") && "Manager".equals(currentUser.getRole())) {
                showDevelopers(chatId);

            } else if (messageTextFromTelegram.equals("Asignar Tarea") && "Manager".equals(currentUser.getRole())) {
                startTaskAssignment(chatId);

            } else {
                // Asumir que es un nuevo ítem de tarea
                addNewTodoItem(messageTextFromTelegram, chatId, currentUser);
            }
        }
    }

    private void processDoneCommand(String messageText, long chatId) {
        String done = messageText.substring(0, messageText.indexOf(BotLabels.DASH.getLabel()));
        Integer id = Integer.valueOf(done);

        try {
            ResponseEntity<ToDoItem> response = toDoItemService.getItemById(id);
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                ToDoItem item = response.getBody();
                item.setDone(true);
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
            
            // Si es un developer, autoasignarse la tarea
            if ("Developer".equals(currentUser.getRole())) {
                newItem.setAssignedTo(currentUser.getID());
            }
            
            toDoItemService.addToDoItem(newItem);

            SendMessage messageToTelegram = new SendMessage();
            messageToTelegram.setChatId(chatId);
            messageToTelegram.setText(BotMessages.NEW_ITEM_ADDED.getMessage());

            execute(messageToTelegram);
        } catch (Exception e) {
            logger.error(e.getLocalizedMessage(), e);
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

    private void startTaskAssignment(long chatId) {
        BotHelper.sendMessageToTelegram(chatId, "Para asignar una tarea a un desarrollador, primero añade una nueva tarea usando el botón 'Add New Item'.", this);
    }

    @Override
    public String getBotUsername() {
        return botName;
    }
}