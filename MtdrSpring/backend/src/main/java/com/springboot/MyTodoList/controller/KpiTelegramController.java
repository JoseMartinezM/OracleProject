// KpiTelegramController.java - Versión Corregida
package com.springboot.MyTodoList.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import com.springboot.MyTodoList.model.Sprint;
import com.springboot.MyTodoList.model.ToDoItem;
import com.springboot.MyTodoList.model.User;
import com.springboot.MyTodoList.service.SprintService;
import com.springboot.MyTodoList.service.ToDoItemService;
import com.springboot.MyTodoList.service.UserService;

import java.time.LocalDate;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;
import java.util.Locale;
import java.time.format.DateTimeFormatter;

// Esta clase contiene los métodos para mostrar reportes KPI en Telegram con interfaz mejorada
@Component
public class KpiTelegramController {
   
   private static final Logger logger = LoggerFactory.getLogger(KpiTelegramController.class);
   
   @Autowired
   private ToDoItemService toDoItemService;
   
   @Autowired
   private UserService userService;
   
   @Autowired
   private SprintService sprintService;
   
   // Método para mostrar el menú de reportes KPI con botones
   public void showKpiMenuWithButtons(long chatId, UserBotController botController, User currentUser) {
       SendMessage message = new SendMessage();
       message.setChatId(chatId);
       message.setText("📈 <b>REPORTES KPI</b>\n\nSelecciona el tipo de reporte que deseas ver:");
       message.enableHtml(true);
       
       InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup();
       List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
       
       // Botón para tareas completadas por sprint
       List<InlineKeyboardButton> row1 = new ArrayList<>();
       InlineKeyboardButton completedTasksBtn = new InlineKeyboardButton();
       completedTasksBtn.setText("📋 Tareas Completadas por Sprint");
       completedTasksBtn.setCallbackData("kpi_completed_tasks");
       row1.add(completedTasksBtn);
       keyboard.add(row1);
       
       // Botón para KPI de equipo por sprint
       List<InlineKeyboardButton> row2 = new ArrayList<>();
       InlineKeyboardButton teamSprintBtn = new InlineKeyboardButton();
       teamSprintBtn.setText("👥 KPI de Equipo por Sprint");
       teamSprintBtn.setCallbackData("kpi_team_sprint");
       row2.add(teamSprintBtn);
       keyboard.add(row2);
       
       // Botón para KPI personal por sprint
       List<InlineKeyboardButton> row3 = new ArrayList<>();
       InlineKeyboardButton personalSprintBtn = new InlineKeyboardButton();
       personalSprintBtn.setText("👤 KPI Personal por Sprint");
       personalSprintBtn.setCallbackData("kpi_personal_sprint");
       row3.add(personalSprintBtn);
       keyboard.add(row3);
       
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
           botController.execute(message);
       } catch (TelegramApiException e) {
           logger.error("Error sending KPI menu", e);
       }
   }
   
   // Método para mostrar selección de sprint con botones
   public void showSprintSelectionForKpiWithButtons(long chatId, UserBotController botController, String kpiType) {
       List<Sprint> sprints = sprintService.findAll();
       
       if (sprints.isEmpty()) {
           sendErrorMessage(chatId, "No hay sprints disponibles.", botController);
           return;
       }
       
       SendMessage message = new SendMessage();
       message.setChatId(chatId);
       message.setText("📋 <b>SELECCIONAR SPRINT</b>\n\nElige el sprint para el reporte KPI:");
       message.enableHtml(true);
       
       InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup();
       List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
       
       for (Sprint sprint : sprints) {
           List<InlineKeyboardButton> row = new ArrayList<>();
           InlineKeyboardButton button = new InlineKeyboardButton();
           button.setText(sprint.getName());
           button.setCallbackData("kpi_sprint:" + kpiType + ":" + sprint.getId());
           row.add(button);
           keyboard.add(row);
       }
       
       // Botón de regreso
       List<InlineKeyboardButton> backRow = new ArrayList<>();
       InlineKeyboardButton backBtn = new InlineKeyboardButton();
       backBtn.setText("🔙 Volver a KPIs");
       backBtn.setCallbackData("view_kpis");
       backRow.add(backBtn);
       keyboard.add(backRow);
       
       inlineKeyboard.setKeyboard(keyboard);
       message.setReplyMarkup(inlineKeyboard);
       
       try {
           botController.execute(message);
       } catch (TelegramApiException e) {
           logger.error("Error sending sprint selection", e);
       }
   }
   
   // Método para mostrar selección de desarrollador con botones
   public void showDeveloperSelectionForKpiWithButtons(long chatId, UserBotController botController, String kpiType) {
       List<User> developers = userService.findByRole("Developer");
       
       if (developers.isEmpty()) {
           sendErrorMessage(chatId, "No hay desarrolladores disponibles.", botController);
           return;
       }
       
       SendMessage message = new SendMessage();
       message.setChatId(chatId);
       message.setText("👨‍💻 <b>SELECCIONAR DESARROLLADOR</b>\n\nElige el desarrollador para el reporte KPI:");
       message.enableHtml(true);
       
       InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup();
       List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
       
       for (User developer : developers) {
           List<InlineKeyboardButton> row = new ArrayList<>();
           InlineKeyboardButton button = new InlineKeyboardButton();
           button.setText(developer.getName());
           button.setCallbackData("kpi_developer:" + kpiType + ":" + developer.getID());
           row.add(button);
           keyboard.add(row);
       }
       
       // Botón de regreso
       List<InlineKeyboardButton> backRow = new ArrayList<>();
       InlineKeyboardButton backBtn = new InlineKeyboardButton();
       backBtn.setText("🔙 Volver a KPIs");
       backBtn.setCallbackData("view_kpis");
       backRow.add(backBtn);
       keyboard.add(backRow);
       
       inlineKeyboard.setKeyboard(keyboard);
       message.setReplyMarkup(inlineKeyboard);
       
       try {
           botController.execute(message);
       } catch (TelegramApiException e) {
           logger.error("Error sending developer selection", e);
       }
   }
   
   // Método para mostrar selección de sprint para KPI personal (CORREGIDO)
   public void showSprintSelectionForPersonalKpi(long chatId, UserBotController botController, int developerId) {
       List<Sprint> sprints = sprintService.findAll();
       
       if (sprints.isEmpty()) {
           sendErrorMessage(chatId, "No hay sprints disponibles.", botController);
           return;
       }
       
       // Obtener nombre del desarrollador
       User developer = userService.getUserById(developerId).getBody();
       String developerName = developer != null ? developer.getName() : "Desarrollador";
       
       SendMessage message = new SendMessage();
       message.setChatId(chatId);
       message.setText("📋 <b>KPI PERSONAL - " + developerName + "</b>\n\nSelecciona el sprint:");
       message.enableHtml(true);
       
       InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup();
       List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
       
       for (Sprint sprint : sprints) {
           List<InlineKeyboardButton> row = new ArrayList<>();
           InlineKeyboardButton button = new InlineKeyboardButton();
           button.setText(sprint.getName());
           // CORREGIDO: Usar callback que existe en UserBotController
           button.setCallbackData("kpi_personal_final:" + developerId + ":" + sprint.getId());
           row.add(button);
           keyboard.add(row);
       }
       
       // Botón de regreso
       List<InlineKeyboardButton> backRow = new ArrayList<>();
       InlineKeyboardButton backBtn = new InlineKeyboardButton();
       backBtn.setText("🔙 Volver a KPIs");
       backBtn.setCallbackData("view_kpis");
       backRow.add(backBtn);
       keyboard.add(backRow);
       
       inlineKeyboard.setKeyboard(keyboard);
       message.setReplyMarkup(inlineKeyboard);
       
       try {
           botController.execute(message);
       } catch (TelegramApiException e) {
           logger.error("Error sending sprint selection for personal KPI", e);
       }
   }
   
   // 1.1 Lista de Tareas completadas en cada Sprint (SIN DUPLICACIÓN DE MENÚ)
   public void showCompletedTasksBySprint(long chatId, UserBotController botController, int sprintId) {
       // Obtener el sprint
       Sprint sprint = sprintService.getSprintById(sprintId).getBody();
       if (sprint == null) {
           sendErrorMessage(chatId, "Sprint no encontrado.", botController);
           return;
       }
       
       // Obtener tareas completadas para este sprint
       List<ToDoItem> completedTasks = toDoItemService.findBySprintId(sprintId)
           .stream()
           .filter(task -> "Completed".equals(task.getStatus()) || Boolean.TRUE.equals(task.isDone()))
           .collect(Collectors.toList());
       
       StringBuilder reportMessage = new StringBuilder();
       reportMessage.append("📋 <b>TAREAS COMPLETADAS</b>\n");
       reportMessage.append("🔸 <b>Sprint:</b> ").append(sprint.getName()).append("\n");
       reportMessage.append("📅 <b>Periodo:</b> ").append(sprint.getStartDate()).append(" - ").append(sprint.getEndDate()).append("\n\n");
       
       if (completedTasks.isEmpty()) {
           reportMessage.append("❌ No hay tareas completadas en este sprint.");
       } else {
           reportMessage.append("✅ <b>Total completadas:</b> ").append(completedTasks.size()).append("\n\n");
           
           // Resumen de horas y eficiencia
           double totalEstimatedHours = completedTasks.stream()
               .filter(t -> t.getEstimatedHours() != null)
               .mapToDouble(ToDoItem::getEstimatedHours)
               .sum();
           
           double totalActualHours = completedTasks.stream()
               .filter(t -> t.getActualHours() != null)
               .mapToDouble(ToDoItem::getActualHours)
               .sum();
           
           reportMessage.append("📊 <b>RESUMEN DE HORAS:</b>\n");
           reportMessage.append("⏱️ Estimadas: ").append(formatNumber(totalEstimatedHours)).append("h\n");
           reportMessage.append("⏱️ Reales: ").append(formatNumber(totalActualHours)).append("h\n");
           
           if (totalActualHours > 0) {
               double overallEfficiency = (totalEstimatedHours / totalActualHours) * 100;
               String efficiencyEmoji = overallEfficiency > 100 ? "🟢" : (overallEfficiency >= 85 ? "🟡" : "🔴");
               reportMessage.append("📈 Eficiencia: ").append(efficiencyEmoji).append(" ").append(formatNumber(overallEfficiency)).append("%\n\n");
           }
           
           // Detalles por desarrollador
           Map<Integer, List<ToDoItem>> tasksByDeveloper = completedTasks.stream()
               .filter(t -> t.getAssignedTo() != null)
               .collect(Collectors.groupingBy(ToDoItem::getAssignedTo));
           
           if (!tasksByDeveloper.isEmpty()) {
               reportMessage.append("👥 <b>POR DESARROLLADOR:</b>\n\n");
               
               for (Map.Entry<Integer, List<ToDoItem>> entry : tasksByDeveloper.entrySet()) {
                   User developer = userService.getUserById(entry.getKey()).getBody();
                   if (developer == null) continue;
                   
                   List<ToDoItem> devTasks = entry.getValue();
                   double devEstimated = devTasks.stream()
                       .filter(t -> t.getEstimatedHours() != null)
                       .mapToDouble(ToDoItem::getEstimatedHours)
                       .sum();
                   
                   double devActual = devTasks.stream()
                       .filter(t -> t.getActualHours() != null)
                       .mapToDouble(ToDoItem::getActualHours)
                       .sum();
                   
                   reportMessage.append("👨‍💻 <b>").append(developer.getName()).append("</b>\n");
                   reportMessage.append("   📝 Tareas: ").append(devTasks.size()).append("\n");
                   reportMessage.append("   ⏱️ Horas: ").append(formatNumber(devActual)).append("h\n");
                   
                   if (devActual > 0 && devEstimated > 0) {
                       double devEfficiency = (devEstimated / devActual) * 100;
                       String devEmoji = devEfficiency > 100 ? "🟢" : (devEfficiency >= 85 ? "🟡" : "🔴");
                       reportMessage.append("   📈 Eficiencia: ").append(devEmoji).append(" ").append(formatNumber(devEfficiency)).append("%\n");
                   }
                   reportMessage.append("\n");
               }
           }
           
           // Top 5 tareas más eficientes
           List<ToDoItem> efficientTasks = completedTasks.stream()
               .filter(t -> t.getEstimatedHours() != null && t.getActualHours() != null && t.getActualHours() > 0)
               .sorted((t1, t2) -> {
                   double eff1 = (t1.getEstimatedHours() / t1.getActualHours()) * 100;
                   double eff2 = (t2.getEstimatedHours() / t2.getActualHours()) * 100;
                   return Double.compare(eff2, eff1);
               })
               .limit(5)
               .collect(Collectors.toList());
           
           if (!efficientTasks.isEmpty()) {
               reportMessage.append("🏆 <b>TOP EFICIENCIA:</b>\n");
               for (int i = 0; i < efficientTasks.size(); i++) {
                   ToDoItem task = efficientTasks.get(i);
                   double efficiency = (task.getEstimatedHours() / task.getActualHours()) * 100;
                   String medal = i == 0 ? "🥇" : (i == 1 ? "🥈" : (i == 2 ? "🥉" : "🏅"));
                   
                   reportMessage.append(medal).append(" ").append(task.getDescription().length() > 30 ? 
                       task.getDescription().substring(0, 30) + "..." : task.getDescription())
                       .append(" (").append(formatNumber(efficiency)).append("%)\n");
               }
           }
       }
       
       // ENVIAR SOLO EL REPORTE SIN DUPLICAR MENÚ
       sendMessageOnly(chatId, reportMessage.toString(), botController);
   }
   
   // 1.2 Reporte KPI Horas trabajadas y Tareas completadas por EQUIPO por sprint (SIN DUPLICACIÓN)
   public void showTeamKpiBySprint(long chatId, UserBotController botController, int sprintId) {
       // Obtener el sprint
       Sprint sprint = sprintService.getSprintById(sprintId).getBody();
       if (sprint == null) {
           sendErrorMessage(chatId, "Sprint no encontrado.", botController);
           return;
       }
       
       List<ToDoItem> sprintTasks = toDoItemService.findBySprintId(sprintId);
       List<ToDoItem> completedTasks = sprintTasks.stream()
           .filter(task -> "Completed".equals(task.getStatus()) || Boolean.TRUE.equals(task.isDone()))
           .collect(Collectors.toList());
       
       StringBuilder reportMessage = new StringBuilder();
       reportMessage.append("👥 <b>KPI DE EQUIPO</b>\n");
       reportMessage.append("🔸 <b>Sprint:</b> ").append(sprint.getName()).append("\n");
       reportMessage.append("📅 <b>Periodo:</b> ").append(sprint.getStartDate()).append(" - ").append(sprint.getEndDate()).append("\n\n");
       
       // Métricas principales con emojis más informativos
       reportMessage.append("📊 <b>MÉTRICAS PRINCIPALES</b>\n");
       reportMessage.append("📋 Total tareas: ").append(sprintTasks.size()).append("\n");
       reportMessage.append("✅ Completadas: ").append(completedTasks.size()).append("\n");
       
       // Tasa de completado con indicador visual
       double completionRate = sprintTasks.isEmpty() ? 0 : 
           (double) completedTasks.size() / sprintTasks.size() * 100;
       String progressEmoji = completionRate >= 90 ? "🟢" : (completionRate >= 70 ? "🟡" : "🔴");
       reportMessage.append("🎯 Progreso: ").append(progressEmoji).append(" ").append(formatNumber(completionRate)).append("%\n\n");
       
       // Análisis de horas mejorado
       double totalEstimatedHours = sprintTasks.stream()
           .filter(t -> t.getEstimatedHours() != null)
           .mapToDouble(ToDoItem::getEstimatedHours)
           .sum();
       
       double totalActualHours = completedTasks.stream()
           .filter(t -> t.getActualHours() != null)
           .mapToDouble(ToDoItem::getActualHours)
           .sum();
       
       double completedEstimatedHours = completedTasks.stream()
           .filter(t -> t.getEstimatedHours() != null)
           .mapToDouble(ToDoItem::getEstimatedHours)
           .sum();
       
       double remainingHours = sprintTasks.stream()
           .filter(t -> !completedTasks.contains(t) && t.getEstimatedHours() != null)
           .mapToDouble(ToDoItem::getEstimatedHours)
           .sum();
       
       reportMessage.append("⏱️ <b>ANÁLISIS DE HORAS</b>\n");
       reportMessage.append("📈 Estimadas total: ").append(formatNumber(totalEstimatedHours)).append("h\n");
       reportMessage.append("✅ Estimadas completadas: ").append(formatNumber(completedEstimatedHours)).append("h\n");
       reportMessage.append("🔥 Trabajadas: ").append(formatNumber(totalActualHours)).append("h\n");
       reportMessage.append("⏳ Pendientes: ").append(formatNumber(remainingHours)).append("h\n");
       
       // Eficiencia del equipo con análisis
       if (totalActualHours > 0) {
           double efficiency = (completedEstimatedHours / totalActualHours) * 100;
           String efficiencyEmoji = efficiency > 100 ? "🟢" : (efficiency >= 85 ? "🟡" : "🔴");
           String interpretation = efficiency > 100 ? "Excelente!" : 
                                 (efficiency >= 85 ? "Buena" : "Necesita mejora");
           
           reportMessage.append("📈 Eficiencia: ").append(efficiencyEmoji).append(" ").append(formatNumber(efficiency))
               .append("% (").append(interpretation).append(")\n\n");
       }
       
       // Distribución por estado con porcentajes
       Map<String, Long> tasksByStatus = sprintTasks.stream()
           .collect(Collectors.groupingBy(
               t -> t.getStatus() == null ? "Sin estado" : t.getStatus(),
               Collectors.counting()
           ));
       
       reportMessage.append("📈 <b>DISTRIBUCIÓN POR ESTADO</b>\n");
       for (Map.Entry<String, Long> entry : tasksByStatus.entrySet()) {
           String statusEmoji = getStatusEmoji(entry.getKey());
           long count = entry.getValue();
           double percentage = sprintTasks.isEmpty() ? 0 : (double) count / sprintTasks.size() * 100;
           
           reportMessage.append(statusEmoji).append(" ").append(entry.getKey()).append(": ")
               .append(count).append(" (").append(formatNumber(percentage)).append("%)\n");
       }
       
       // Rendimiento del equipo
       reportMessage.append("\n🏆 <b>RENDIMIENTO DEL EQUIPO</b>\n");
       
       Map<Integer, List<ToDoItem>> tasksByDeveloper = sprintTasks.stream()
           .filter(t -> t.getAssignedTo() != null)
           .collect(Collectors.groupingBy(ToDoItem::getAssignedTo));
       
       if (!tasksByDeveloper.isEmpty()) {
           for (Map.Entry<Integer, List<ToDoItem>> entry : tasksByDeveloper.entrySet()) {
               User developer = userService.getUserById(entry.getKey()).getBody();
               if (developer == null) continue;
               
               List<ToDoItem> devTasks = entry.getValue();
               long devCompleted = devTasks.stream()
                   .filter(t -> "Completed".equals(t.getStatus()))
                   .count();
               
               double devCompletionRate = devTasks.isEmpty() ? 0 : (double) devCompleted / devTasks.size() * 100;
               String devEmoji = devCompletionRate >= 80 ? "🟢" : (devCompletionRate >= 60 ? "🟡" : "🔴");
               
               reportMessage.append("👨‍💻 ").append(developer.getName()).append(": ")
                   .append(devEmoji).append(" ").append(devCompleted).append("/").append(devTasks.size())
                   .append(" (").append(formatNumber(devCompletionRate)).append("%)\n");
           }
       }
       
       // ENVIAR SOLO EL REPORTE SIN DUPLICAR MENÚ
       sendMessageOnly(chatId, reportMessage.toString(), botController);
   }
   
   public void showPersonalKpiBySprint(long chatId, UserBotController botController, int userId, int sprintId) {
    // Obtener el usuario y el sprint
    User user = userService.getUserById(userId).getBody();
    Sprint sprint = sprintService.getSprintById(sprintId).getBody();
    
    if (user == null || sprint == null) {
        sendErrorMessage(chatId, "Usuario o sprint no encontrado.", botController);
        return;
    }
    
    // Obtener tareas del usuario en el sprint
    List<ToDoItem> userSprintTasks = toDoItemService.findBySprintId(sprintId).stream()
        .filter(task -> task.getAssignedTo() != null && task.getAssignedTo().equals(userId))
        .collect(Collectors.toList());
    
    List<ToDoItem> completedTasks = userSprintTasks.stream()
        .filter(task -> "Completed".equals(task.getStatus()) || Boolean.TRUE.equals(task.isDone()))
        .collect(Collectors.toList());
    
    StringBuilder reportMessage = new StringBuilder();
    reportMessage.append("👤 <b>KPI PERSONAL</b>\n");
    reportMessage.append("🔸 <b>Desarrollador:</b> ").append(user.getName()).append("\n");
    reportMessage.append("📋 <b>Sprint:</b> ").append(sprint.getName()).append("\n");
    reportMessage.append("📅 <b>Periodo:</b> ").append(sprint.getStartDate()).append(" - ").append(sprint.getEndDate()).append("\n\n");
    
    if (userSprintTasks.isEmpty()) {
        reportMessage.append("❌ No tienes tareas asignadas en este sprint.");
    } else {
        // Métricas personales
        reportMessage.append("📊 <b>TUS MÉTRICAS</b>\n");
        reportMessage.append("📋 Tareas asignadas: ").append(userSprintTasks.size()).append("\n");
        reportMessage.append("✅ Completadas: ").append(completedTasks.size()).append("\n");
        
        // Tasa de completado personal
        double completionRate = (double) completedTasks.size() / userSprintTasks.size() * 100;
        String progressEmoji = completionRate >= 90 ? "🟢" : (completionRate >= 70 ? "🟡" : "🔴");
        reportMessage.append("🎯 Tu progreso: ").append(progressEmoji).append(" ").append(formatNumber(completionRate)).append("%\n\n");
        
        // Análisis de horas personal
        double totalEstimatedHours = userSprintTasks.stream()
            .filter(t -> t.getEstimatedHours() != null)
            .mapToDouble(ToDoItem::getEstimatedHours)
            .sum();
        
        double totalActualHours = completedTasks.stream()
            .filter(t -> t.getActualHours() != null)
            .mapToDouble(ToDoItem::getActualHours)
            .sum();
        
        double completedEstimatedHours = completedTasks.stream()
            .filter(t -> t.getEstimatedHours() != null)
            .mapToDouble(ToDoItem::getEstimatedHours)
            .sum();
        
        double remainingHours = userSprintTasks.stream()
            .filter(t -> !completedTasks.contains(t) && t.getEstimatedHours() != null)
            .mapToDouble(ToDoItem::getEstimatedHours)
            .sum();
        
        reportMessage.append("⏱️ <b>TUS HORAS</b>\n");
        reportMessage.append("📈 Estimadas total: ").append(formatNumber(totalEstimatedHours)).append("h\n");
        reportMessage.append("✅ Estimadas completadas: ").append(formatNumber(completedEstimatedHours)).append("h\n");
        reportMessage.append("🔥 Trabajadas: ").append(formatNumber(totalActualHours)).append("h\n");
        reportMessage.append("⏳ Pendientes: ").append(formatNumber(remainingHours)).append("h\n");
        
        // Eficiencia personal
        if (totalActualHours > 0) {
            double efficiency = (completedEstimatedHours / totalActualHours) * 100;
            String efficiencyEmoji = efficiency > 100 ? "🟢" : (efficiency >= 85 ? "🟡" : "🔴");
            String interpretation = efficiency > 100 ? "¡Excelente!" : 
                                  (efficiency >= 85 ? "Buena" : "Puedes mejorar");
            
            reportMessage.append("📈 Tu eficiencia: ").append(efficiencyEmoji).append(" ").append(formatNumber(efficiency))
                .append("% (").append(interpretation).append(")\n\n");
        }
        
        // Comparación con el equipo
        List<ToDoItem> allSprintTasks = toDoItemService.findBySprintId(sprintId);
        List<ToDoItem> allCompleted = allSprintTasks.stream()
            .filter(t -> "Completed".equals(t.getStatus()))
            .collect(Collectors.toList());
        
        double teamCompletionRate = allSprintTasks.isEmpty() ? 0 : 
            (double) allCompleted.size() / allSprintTasks.size() * 100;
        
        reportMessage.append("📊 <b>VS EQUIPO</b>\n");
        reportMessage.append("👥 Progreso del equipo: ").append(formatNumber(teamCompletionRate)).append("%\n");
        
        String comparison = completionRate > teamCompletionRate ? "Por encima del promedio 🚀" :
                           completionRate == teamCompletionRate ? "En el promedio 👍" :
                           "Por debajo del promedio 💪";
        reportMessage.append("📈 Tu posición: ").append(comparison).append("\n\n");
        
        // Distribución personal por estado
        Map<String, Long> tasksByStatus = userSprintTasks.stream()
            .collect(Collectors.groupingBy(
                t -> t.getStatus() == null ? "Sin estado" : t.getStatus(),
                Collectors.counting()
            ));
        
        reportMessage.append("📈 <b>TUS TAREAS POR ESTADO</b>\n");
        for (Map.Entry<String, Long> entry : tasksByStatus.entrySet()) {
            String statusEmoji = getStatusEmoji(entry.getKey());
            long count = entry.getValue();
            double percentage = userSprintTasks.isEmpty() ? 0 : (double) count / userSprintTasks.size() * 100;
            
            reportMessage.append(statusEmoji).append(" ").append(entry.getKey()).append(": ")
                .append(count).append(" (").append(formatNumber(percentage)).append("%)\n");
        }
        
        // Tus mejores tareas (por eficiencia)
        List<ToDoItem> bestTasks = completedTasks.stream()
            .filter(t -> t.getEstimatedHours() != null && t.getActualHours() != null && t.getActualHours() > 0)
            .sorted((t1, t2) -> {
                double eff1 = (t1.getEstimatedHours() / t1.getActualHours()) * 100;
                double eff2 = (t2.getEstimatedHours() / t2.getActualHours()) * 100;
                return Double.compare(eff2, eff1);
            })
            .limit(3)
            .collect(Collectors.toList());
        
        if (!bestTasks.isEmpty()) {
            reportMessage.append("\n🏆 <b>TUS MEJORES TAREAS</b>\n");
            for (int i = 0; i < bestTasks.size(); i++) {
                ToDoItem task = bestTasks.get(i);
                double taskEfficiency = (task.getEstimatedHours() / task.getActualHours()) * 100;
                String medal = i == 0 ? "🥇" : (i == 1 ? "🥈" : "🥉");
                
                reportMessage.append(medal).append(" ").append(task.getDescription().length() > 25 ? 
                    task.getDescription().substring(0, 25) + "..." : task.getDescription())
                    .append(" (").append(formatNumber(taskEfficiency)).append("%)\n");
            }
        }
    }
    
    // ENVIAR EL REPORTE CON BOTONES DE NAVEGACIÓN
    sendMessageWithBackButton(chatId, reportMessage.toString(), botController);
 }

// Métodos auxiliares mejorados
private String formatNumber(Double number) {
    if (number == null) return "0";
    return String.format("%.1f", number);
}

private String getStatusEmoji(String status) {
    if (status == null) return "❓";
        
    switch (status) {
        case "Pending": return "⏳";
        case "In Progress": return "🔄";
        case "In Review": return "👁️";
        case "Completed": return "✅";
        default: return "📌";
    }
}

// NUEVO MÉTODO: Enviar solo el mensaje sin botones adicionales
private void sendMessageOnly(long chatId, String text, UserBotController botController) {
    SendMessage message = new SendMessage();
    message.setChatId(chatId);
    message.setText(text);
    message.enableHtml(true);
    
    try {
        botController.execute(message);
    } catch (TelegramApiException e) {
        logger.error("Error sending KPI report", e);
    }
}

private void sendMessageWithBackButton(long chatId, String text, UserBotController botController) {
    SendMessage message = new SendMessage();
    message.setChatId(chatId);
    message.setText(text);
    message.enableHtml(true);
    
    InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup();
    List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
    
    List<InlineKeyboardButton> backRow = new ArrayList<>();
    InlineKeyboardButton backBtn = new InlineKeyboardButton();
    backBtn.setText("🔙 Volver a KPIs");
    backBtn.setCallbackData("view_kpis");
    backRow.add(backBtn);
    
    InlineKeyboardButton menuBtn = new InlineKeyboardButton();
    menuBtn.setText("🏠 Menú Principal");
    menuBtn.setCallbackData("main_menu");
    backRow.add(menuBtn);
    keyboard.add(backRow);
    
    inlineKeyboard.setKeyboard(keyboard);
    message.setReplyMarkup(inlineKeyboard);
    
    try {
        botController.execute(message);
    } catch (TelegramApiException e) {
        logger.error("Error sending message with back button", e);
    }
}

private void sendMessage(long chatId, String text, UserBotController botController) {
    // Dividir el mensaje si es demasiado largo (límite de Telegram es 4096 caracteres)
    int maxLength = 4000; // Un poco menos que el límite para tener margen
    
    if (text.length() <= maxLength) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);
        message.enableHtml(true);
        
        try {
            botController.execute(message);
        } catch (TelegramApiException e) {
            logger.error("Error sending message", e);
        }
    } else {
        // Dividir el mensaje en partes
        List<String> parts = new ArrayList<>();
        int start = 0;
        
        while (start < text.length()) {
            int end = Math.min(start + maxLength, text.length());
            
            // Ajustar el final para no cortar en medio de una línea
            if (end < text.length()) {
                int lastNewline = text.lastIndexOf("\n", end);
                if (lastNewline > start) {
                    end = lastNewline + 1;
                }
            }
            
            parts.add(text.substring(start, end));
            start = end;
        }
        
        // Enviar cada parte
        for (int i = 0; i < parts.size(); i++) {
            String part = parts.get(i);
            String messageText = part;
            
            // Añadir indicador de parte para mensajes múltiples
            if (parts.size() > 1) {
                messageText = "<b>Parte " + (i+1) + " de " + parts.size() + ":</b>\n\n" + messageText;
            }
            
            SendMessage message = new SendMessage();
            message.setChatId(chatId);
            message.setText(messageText);
            message.enableHtml(true);
            
            try {
                botController.execute(message);
            } catch (TelegramApiException e) {
                logger.error("Error sending message part " + (i+1), e);
            }
        }
    }
}

private void sendErrorMessage(long chatId, String errorMessage, UserBotController botController) {
    SendMessage message = new SendMessage();
    message.setChatId(chatId);
    message.setText("❌ <b>Error:</b> " + errorMessage);
    message.enableHtml(true);
    
    InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup();
    List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
    
    List<InlineKeyboardButton> backRow = new ArrayList<>();
    InlineKeyboardButton backBtn = new InlineKeyboardButton();
    backBtn.setText("🔙 Volver a KPIs");
    backBtn.setCallbackData("view_kpis");
    backRow.add(backBtn);
    keyboard.add(backRow);
    
    inlineKeyboard.setKeyboard(keyboard);
    message.setReplyMarkup(inlineKeyboard);
    
    try {
        botController.execute(message);
    } catch (TelegramApiException e) {
        logger.error("Error sending error message", e);
    }
}

// Métodos de compatibilidad (mantenidos para no romper el código existente)
@Deprecated
public void showKpiMenu(long chatId, UserBotController botController, User currentUser) {
    showKpiMenuWithButtons(chatId, botController, currentUser);
}

@Deprecated
public void showSprintSelectionForKpi(long chatId, UserBotController botController) {
    showSprintSelectionForKpiWithButtons(chatId, botController, "default");
}

@Deprecated
public void showDeveloperSelectionForKpi(long chatId, UserBotController botController) {
    showDeveloperSelectionForKpiWithButtons(chatId, botController, "default");
}

@Deprecated
public void requestWeekSelection(long chatId, UserBotController botController) {
    SendMessage message = new SendMessage();
    message.setChatId(chatId);
    message.setText("❌ <b>Función no disponible</b>\n\nLos reportes por semana han sido eliminados. Por favor, usa los reportes por Sprint.");
    message.enableHtml(true);
    
    InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup();
    List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
    
    List<InlineKeyboardButton> backRow = new ArrayList<>();
    InlineKeyboardButton backBtn = new InlineKeyboardButton();
    backBtn.setText("🔙 Volver a KPIs");
    backBtn.setCallbackData("view_kpis");
    backRow.add(backBtn);
    keyboard.add(backRow);
    
    inlineKeyboard.setKeyboard(keyboard);
    message.setReplyMarkup(inlineKeyboard);
    
    try {
        botController.execute(message);
    } catch (TelegramApiException e) {
        logger.error("Error sending deprecated week selection message", e);
    }
}
}