// KpiTelegramController.java
package com.springboot.MyTodoList.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
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

// Esta clase contiene los métodos para mostrar reportes KPI en Telegram
@Component
public class KpiTelegramController {
    
    private static final Logger logger = LoggerFactory.getLogger(KpiTelegramController.class);
    
    @Autowired
    private ToDoItemService toDoItemService;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private SprintService sprintService;
    
    // Método para mostrar el menú de reportes KPI
    public void showKpiMenu(long chatId, UserBotController botController, User currentUser) {
        StringBuilder menuMessage = new StringBuilder();
        menuMessage.append("📊 MENÚ DE REPORTES KPI:\n\n");
        menuMessage.append("Selecciona una opción:\n\n");
        menuMessage.append("1️⃣ Lista de Tareas Completadas por Sprint\n");
        menuMessage.append("2️⃣ KPI de Equipo por Sprint\n");
        menuMessage.append("3️⃣ KPI de Equipo por Semana\n");
        menuMessage.append("4️⃣ KPI Personal por Sprint\n");
        menuMessage.append("5️⃣ KPI Personal por Semana\n\n");
        menuMessage.append("🔙 Para volver al menú principal, escribe 'menu'");
        
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(menuMessage.toString());
        
        try {
            botController.execute(message);
        } catch (TelegramApiException e) {
            logger.error("Error sending KPI menu", e);
        }
    }
    
    // 1.1 Lista de Tareas completadas en cada Sprint
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
        reportMessage.append("📋 TAREAS COMPLETADAS EN SPRINT: ").append(sprint.getName()).append("\n\n");
        
        if (completedTasks.isEmpty()) {
            reportMessage.append("No hay tareas completadas en este sprint.");
        } else {
            // Mostrar resumen
            reportMessage.append("📅 Periodo: ").append(sprint.getStartDate()).append(" al ").append(sprint.getEndDate()).append("\n");
            reportMessage.append("✅ Total tareas completadas: ").append(completedTasks.size()).append("\n\n");
            
            // Detalles de cada tarea
            reportMessage.append("DETALLES DE TAREAS:\n\n");
            
            for (ToDoItem task : completedTasks) {
                reportMessage.append("📌 ").append(task.getDescription()).append("\n");
                
                // Obtener desarrollador
                String developerName = "Sin asignar";
                if (task.getAssignedTo() != null) {
                    User developer = userService.getUserById(task.getAssignedTo()).getBody();
                    if (developer != null) {
                        developerName = developer.getName();
                    }
                }
                
                reportMessage.append("  👨‍💻 Desarrollador: ").append(developerName).append("\n");
                reportMessage.append("  ⏱️ Horas estimadas: ").append(formatNumber(task.getEstimatedHours())).append("\n");
                reportMessage.append("  ⏱️ Horas reales: ").append(formatNumber(task.getActualHours())).append("\n");
                
                // Calcular eficiencia
                if (task.getEstimatedHours() != null && task.getActualHours() != null && task.getActualHours() > 0) {
                    double efficiency = (task.getEstimatedHours() / task.getActualHours()) * 100;
                    reportMessage.append("  📈 Eficiencia: ").append(formatNumber(efficiency)).append("%\n");
                }
                
                reportMessage.append("\n");
            }
            
            // Resumen de eficiencia y horas
            double totalEstimatedHours = completedTasks.stream()
                .filter(t -> t.getEstimatedHours() != null)
                .mapToDouble(ToDoItem::getEstimatedHours)
                .sum();
            
            double totalActualHours = completedTasks.stream()
                .filter(t -> t.getActualHours() != null)
                .mapToDouble(ToDoItem::getActualHours)
                .sum();
            
            reportMessage.append("RESUMEN DE HORAS:\n");
            reportMessage.append("⏱️ Total horas estimadas: ").append(formatNumber(totalEstimatedHours)).append("\n");
            reportMessage.append("⏱️ Total horas reales: ").append(formatNumber(totalActualHours)).append("\n");
            
            if (totalActualHours > 0) {
                double overallEfficiency = (totalEstimatedHours / totalActualHours) * 100;
                reportMessage.append("📈 Eficiencia global: ").append(formatNumber(overallEfficiency)).append("%\n");
            }
        }
        
        sendMessage(chatId, reportMessage.toString(), botController);
    }
    
    // 1.2 Reporte KPI Horas trabajadas y Tareas completadas por EQUIPO por sprint
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
        reportMessage.append("📊 KPI DE EQUIPO - SPRINT: ").append(sprint.getName()).append("\n\n");
        reportMessage.append("📅 Periodo: ").append(sprint.getStartDate()).append(" al ").append(sprint.getEndDate()).append("\n\n");
        
        // Métricas generales
        reportMessage.append("MÉTRICAS GENERALES:\n");
        reportMessage.append("📋 Total tareas: ").append(sprintTasks.size()).append("\n");
        reportMessage.append("✅ Tareas completadas: ").append(completedTasks.size()).append("\n");
        
        // Tasa de completado
        double completionRate = sprintTasks.isEmpty() ? 0 : 
            (double) completedTasks.size() / sprintTasks.size() * 100;
        reportMessage.append("🎯 Tasa de completado: ").append(formatNumber(completionRate)).append("%\n\n");
        
        // Horas estimadas y reales
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
        
        reportMessage.append("ANÁLISIS DE HORAS:\n");
        reportMessage.append("⏱️ Horas estimadas (Total): ").append(formatNumber(totalEstimatedHours)).append("\n");
        reportMessage.append("⏱️ Horas estimadas (Completadas): ").append(formatNumber(completedEstimatedHours)).append("\n");
        reportMessage.append("⏱️ Horas reales trabajadas: ").append(formatNumber(totalActualHours)).append("\n");
        
        // Eficiencia
        if (totalActualHours > 0) {
            double efficiency = (completedEstimatedHours / totalActualHours) * 100;
            String efficiencyEmoji = efficiency > 100 ? "🟢" : (efficiency >= 85 ? "🟡" : "🔴");
            reportMessage.append("📈 Eficiencia del equipo: ").append(efficiencyEmoji).append(" ").append(formatNumber(efficiency)).append("%\n");
        }
        
        // Horas restantes
        double remainingHours = sprintTasks.stream()
            .filter(t -> !completedTasks.contains(t) && t.getEstimatedHours() != null)
            .mapToDouble(ToDoItem::getEstimatedHours)
            .sum();
        
        reportMessage.append("⏳ Horas pendientes estimadas: ").append(formatNumber(remainingHours)).append("\n\n");
        
        // Distribución por estado
        Map<String, Long> tasksByStatus = sprintTasks.stream()
            .collect(Collectors.groupingBy(
                t -> t.getStatus() == null ? "Sin estado" : t.getStatus(),
                Collectors.counting()
            ));
        
        reportMessage.append("DISTRIBUCIÓN POR ESTADO:\n");
        for (Map.Entry<String, Long> entry : tasksByStatus.entrySet()) {
            String statusEmoji = getStatusEmoji(entry.getKey());
            reportMessage.append(statusEmoji).append(" ").append(entry.getKey()).append(": ")
                .append(entry.getValue()).append(" tarea(s)\n");
        }
        
        sendMessage(chatId, reportMessage.toString(), botController);
    }
    
    // 1.2 Reporte KPI Horas trabajadas y Tareas completadas por EQUIPO por semana
    public void showTeamKpiByWeek(long chatId, UserBotController botController, int year, int weekNumber) {
        // Calcular fechas de la semana
        LocalDate startOfWeek = LocalDate.now()
            .withYear(year)
            .with(WeekFields.of(Locale.getDefault()).weekOfYear(), weekNumber)
            .with(WeekFields.of(Locale.getDefault()).dayOfWeek(), 1);
        
        LocalDate endOfWeek = startOfWeek.plusDays(6);
        
        // Obtener todas las tareas
        List<ToDoItem> allTasks = toDoItemService.findAll();
        
        // Filtrar tareas creadas o actualizadas en la semana especificada
        List<ToDoItem> weekTasks = allTasks.stream()
            .filter(task -> {
                if (task.getCreation_ts() == null) return false;
                
                LocalDate taskDate = task.getCreation_ts().toLocalDate();
                return !taskDate.isBefore(startOfWeek) && !taskDate.isAfter(endOfWeek);
            })
            .collect(Collectors.toList());
        
        List<ToDoItem> completedTasks = weekTasks.stream()
            .filter(task -> "Completed".equals(task.getStatus()) || Boolean.TRUE.equals(task.isDone()))
            .collect(Collectors.toList());
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        
        StringBuilder reportMessage = new StringBuilder();
        reportMessage.append("📊 KPI DE EQUIPO - SEMANA ").append(weekNumber).append(" DE ").append(year).append("\n\n");
        reportMessage.append("📅 Periodo: ").append(startOfWeek.format(formatter)).append(" al ")
            .append(endOfWeek.format(formatter)).append("\n\n");
        
        // Métricas generales
        reportMessage.append("MÉTRICAS GENERALES:\n");
        reportMessage.append("📋 Total tareas: ").append(weekTasks.size()).append("\n");
        reportMessage.append("✅ Tareas completadas: ").append(completedTasks.size()).append("\n");
        
        // Tasa de completado
        double completionRate = weekTasks.isEmpty() ? 0 : 
            (double) completedTasks.size() / weekTasks.size() * 100;
        reportMessage.append("🎯 Tasa de completado: ").append(formatNumber(completionRate)).append("%\n\n");
        
        // Horas estimadas y reales
        double totalEstimatedHours = weekTasks.stream()
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
        
        reportMessage.append("ANÁLISIS DE HORAS:\n");
        reportMessage.append("⏱️ Horas estimadas (Total): ").append(formatNumber(totalEstimatedHours)).append("\n");
        reportMessage.append("⏱️ Horas estimadas (Completadas): ").append(formatNumber(completedEstimatedHours)).append("\n");
        reportMessage.append("⏱️ Horas reales trabajadas: ").append(formatNumber(totalActualHours)).append("\n");
        
        // Eficiencia
        if (totalActualHours > 0) {
            double efficiency = (completedEstimatedHours / totalActualHours) * 100;
            String efficiencyEmoji = efficiency > 100 ? "🟢" : (efficiency >= 85 ? "🟡" : "🔴");
            reportMessage.append("📈 Eficiencia del equipo: ").append(efficiencyEmoji).append(" ").append(formatNumber(efficiency)).append("%\n");
        }
        
        // Horas restantes
        double remainingHours = weekTasks.stream()
            .filter(t -> !completedTasks.contains(t) && t.getEstimatedHours() != null)
            .mapToDouble(ToDoItem::getEstimatedHours)
            .sum();
        
        reportMessage.append("⏳ Horas pendientes estimadas: ").append(formatNumber(remainingHours)).append("\n\n");
        
        // Distribución por estado
        Map<String, Long> tasksByStatus = weekTasks.stream()
            .collect(Collectors.groupingBy(
                t -> t.getStatus() == null ? "Sin estado" : t.getStatus(),
                Collectors.counting()
            ));
        
        reportMessage.append("DISTRIBUCIÓN POR ESTADO:\n");
        for (Map.Entry<String, Long> entry : tasksByStatus.entrySet()) {
            String statusEmoji = getStatusEmoji(entry.getKey());
            reportMessage.append(statusEmoji).append(" ").append(entry.getKey()).append(": ")
                .append(entry.getValue()).append(" tarea(s)\n");
        }
        
        // Tareas por desarrollador
        Map<Integer, List<ToDoItem>> tasksByDeveloper = weekTasks.stream()
            .filter(t -> t.getAssignedTo() != null)
            .collect(Collectors.groupingBy(ToDoItem::getAssignedTo));
        
        if (!tasksByDeveloper.isEmpty()) {
            reportMessage.append("\nTAREAS POR DESARROLLADOR:\n");
            
            for (Map.Entry<Integer, List<ToDoItem>> entry : tasksByDeveloper.entrySet()) {
                User developer = userService.getUserById(entry.getKey()).getBody();
                if (developer == null) continue;
                
                int totalDeveloperTasks = entry.getValue().size();
                int completedDeveloperTasks = (int) entry.getValue().stream()
                    .filter(t -> "Completed".equals(t.getStatus()) || Boolean.TRUE.equals(t.isDone()))
                    .count();
                
                reportMessage.append("👨‍💻 ").append(developer.getName()).append(": ")
                    .append(completedDeveloperTasks).append("/").append(totalDeveloperTasks)
                    .append(" completadas\n");
            }
        }
        
        sendMessage(chatId, reportMessage.toString(), botController);
    }
    
    // 1.3 Reporte KPI Horas trabajadas y Tareas completadas por PERSONA por sprint
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
        reportMessage.append("👨‍💻 KPI PERSONAL - ").append(user.getName()).append("\n");
        reportMessage.append("📋 SPRINT: ").append(sprint.getName()).append("\n\n");
        reportMessage.append("📅 Periodo: ").append(sprint.getStartDate()).append(" al ").append(sprint.getEndDate()).append("\n\n");
        
        if (userSprintTasks.isEmpty()) {
            reportMessage.append("No tienes tareas asignadas en este sprint.");
        } else {
            // Métricas generales
            reportMessage.append("MÉTRICAS GENERALES:\n");
            reportMessage.append("📋 Total tareas asignadas: ").append(userSprintTasks.size()).append("\n");
            reportMessage.append("✅ Tareas completadas: ").append(completedTasks.size()).append("\n");
            
            // Tasa de completado
            double completionRate = (double) completedTasks.size() / userSprintTasks.size() * 100;
            reportMessage.append("🎯 Tasa de completado: ").append(formatNumber(completionRate)).append("%\n\n");
            
            // Horas estimadas y reales
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
            
            reportMessage.append("ANÁLISIS DE HORAS:\n");
            reportMessage.append("⏱️ Horas estimadas (Total): ").append(formatNumber(totalEstimatedHours)).append("\n");
            reportMessage.append("⏱️ Horas estimadas (Completadas): ").append(formatNumber(completedEstimatedHours)).append("\n");
            reportMessage.append("⏱️ Horas reales trabajadas: ").append(formatNumber(totalActualHours)).append("\n");
            
            // Eficiencia
            if (totalActualHours > 0) {
                double efficiency = (completedEstimatedHours / totalActualHours) * 100;
                String efficiencyEmoji = efficiency > 100 ? "🟢" : (efficiency >= 85 ? "🟡" : "🔴");
                reportMessage.append("📈 Eficiencia personal: ").append(efficiencyEmoji).append(" ").append(formatNumber(efficiency)).append("%\n\n");
            }
            
            // Horas restantes
            double remainingHours = userSprintTasks.stream()
                .filter(t -> !completedTasks.contains(t) && t.getEstimatedHours() != null)
                .mapToDouble(ToDoItem::getEstimatedHours)
                .sum();
            
            reportMessage.append("⏳ Horas pendientes estimadas: ").append(formatNumber(remainingHours)).append("\n\n");
            
            // Distribución por estado
            Map<String, Long> tasksByStatus = userSprintTasks.stream()
                .collect(Collectors.groupingBy(
                    t -> t.getStatus() == null ? "Sin estado" : t.getStatus(),
                    Collectors.counting()
                ));
            
            reportMessage.append("DISTRIBUCIÓN POR ESTADO:\n");
            for (Map.Entry<String, Long> entry : tasksByStatus.entrySet()) {
                String statusEmoji = getStatusEmoji(entry.getKey());
                reportMessage.append(statusEmoji).append(" ").append(entry.getKey()).append(": ")
                    .append(entry.getValue()).append(" tarea(s)\n");
            }
            
            // Listado de tareas detallado
            reportMessage.append("\nDETALLE DE TAREAS:\n\n");
            for (ToDoItem task : userSprintTasks) {
                String statusEmoji = getStatusEmoji(task.getStatus());
                reportMessage.append(statusEmoji).append(" ").append(task.getDescription()).append("\n");
                reportMessage.append("   🔄 Estado: ").append(task.getStatus()).append("\n");
                
                if (task.getEstimatedHours() != null) {
                    reportMessage.append("   ⏱️ Horas estimadas: ").append(formatNumber(task.getEstimatedHours())).append("\n");
                }
                
                if (task.getActualHours() != null) {
                    reportMessage.append("   ⏱️ Horas reales: ").append(formatNumber(task.getActualHours())).append("\n");
                    
                    if (task.getEstimatedHours() != null) {
                        double taskEfficiency = (task.getEstimatedHours() / task.getActualHours()) * 100;
                        reportMessage.append("   📈 Eficiencia: ").append(formatNumber(taskEfficiency)).append("%\n");
                    }
                }
                
                reportMessage.append("\n");
            }
        }
        
        sendMessage(chatId, reportMessage.toString(), botController);
    }
    
    // 1.3 Reporte KPI Horas trabajadas y Tareas completadas por PERSONA por semana
    public void showPersonalKpiByWeek(long chatId, UserBotController botController, int userId, int year, int weekNumber) {
        // Obtener el usuario
        User user = userService.getUserById(userId).getBody();
        if (user == null) {
            sendErrorMessage(chatId, "Usuario no encontrado.", botController);
            return;
        }
        
        // Calcular fechas de la semana
        LocalDate startOfWeek = LocalDate.now()
            .withYear(year)
            .with(WeekFields.of(Locale.getDefault()).weekOfYear(), weekNumber)
            .with(WeekFields.of(Locale.getDefault()).dayOfWeek(), 1);
        
        LocalDate endOfWeek = startOfWeek.plusDays(6);
        
        // Obtener todas las tareas del usuario
        List<ToDoItem> userTasks = toDoItemService.findByAssignedTo(userId);
        
        // Filtrar tareas creadas o actualizadas en la semana especificada
        List<ToDoItem> weekTasks = userTasks.stream()
            .filter(task -> {
                if (task.getCreation_ts() == null) return false;
                
                LocalDate taskDate = task.getCreation_ts().toLocalDate();
                return !taskDate.isBefore(startOfWeek) && !taskDate.isAfter(endOfWeek);
            })
            .collect(Collectors.toList());
        
        List<ToDoItem> completedTasks = weekTasks.stream()
            .filter(task -> "Completed".equals(task.getStatus()) || Boolean.TRUE.equals(task.isDone()))
            .collect(Collectors.toList());
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        
        StringBuilder reportMessage = new StringBuilder();
        reportMessage.append("👨‍💻 KPI PERSONAL - ").append(user.getName()).append("\n");
        reportMessage.append("📅 SEMANA ").append(weekNumber).append(" DE ").append(year).append("\n\n");
        reportMessage.append("📅 Periodo: ").append(startOfWeek.format(formatter)).append(" al ")
            .append(endOfWeek.format(formatter)).append("\n\n");
        
        if (weekTasks.isEmpty()) {
            reportMessage.append("No tienes tareas asignadas en esta semana.");
        } else {
            // Métricas generales
            reportMessage.append("MÉTRICAS GENERALES:\n");
            reportMessage.append("📋 Total tareas asignadas: ").append(weekTasks.size()).append("\n");
            reportMessage.append("✅ Tareas completadas: ").append(completedTasks.size()).append("\n");
            
            // Tasa de completado
            double completionRate = (double) completedTasks.size() / weekTasks.size() * 100;
            reportMessage.append("🎯 Tasa de completado: ").append(formatNumber(completionRate)).append("%\n\n");
            
            // Horas estimadas y reales
            double totalEstimatedHours = weekTasks.stream()
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
            
            reportMessage.append("ANÁLISIS DE HORAS:\n");
            reportMessage.append("⏱️ Horas estimadas (Total): ").append(formatNumber(totalEstimatedHours)).append("\n");
            reportMessage.append("⏱️ Horas estimadas (Completadas): ").append(formatNumber(completedEstimatedHours)).append("\n");
            reportMessage.append("⏱️ Horas reales trabajadas: ").append(formatNumber(totalActualHours)).append("\n");
            
            // Eficiencia
            if (totalActualHours > 0) {
                double efficiency = (completedEstimatedHours / totalActualHours) * 100;
                String efficiencyEmoji = efficiency > 100 ? "🟢" : (efficiency >= 85 ? "🟡" : "🔴");
                reportMessage.append("📈 Eficiencia personal: ").append(efficiencyEmoji).append(" ").append(formatNumber(efficiency)).append("%\n\n");
            }
            
            // Distribución por sprint
            Map<Integer, List<ToDoItem>> tasksBySprint = weekTasks.stream()
                .filter(t -> t.getSprintId() != null)
                .collect(Collectors.groupingBy(ToDoItem::getSprintId));
            
            if (!tasksBySprint.isEmpty()) {
                reportMessage.append("DISTRIBUCIÓN POR SPRINT:\n");
                
                for (Map.Entry<Integer, List<ToDoItem>> entry : tasksBySprint.entrySet()) {
                    Sprint sprint = sprintService.getSprintById(entry.getKey()).getBody();
                    if (sprint == null) continue;
                    
                    int totalSprintTasks = entry.getValue().size();
                    int completedSprintTasks = (int) entry.getValue().stream()
                        .filter(t -> "Completed".equals(t.getStatus()) || Boolean.TRUE.equals(t.isDone()))
                        .count();
                    
                    reportMessage.append("   📋 ").append(sprint.getName()).append(": ")
                        .append(completedSprintTasks).append("/").append(totalSprintTasks)
                        .append(" completadas\n");
                }
                reportMessage.append("\n");
                }
                
                // Distribución por estado
                Map<String, Long> tasksByStatus = weekTasks.stream()
                    .collect(Collectors.groupingBy(
                        t -> t.getStatus() == null ? "Sin estado" : t.getStatus(),
                        Collectors.counting()
                    ));
                
                reportMessage.append("DISTRIBUCIÓN POR ESTADO:\n");
                for (Map.Entry<String, Long> entry : tasksByStatus.entrySet()) {
                    String statusEmoji = getStatusEmoji(entry.getKey());
                    reportMessage.append(statusEmoji).append(" ").append(entry.getKey()).append(": ")
                        .append(entry.getValue()).append(" tarea(s)\n");
                }
                
                // Listado de tareas detallado
                reportMessage.append("\nDETALLE DE TAREAS:\n\n");
                for (ToDoItem task : weekTasks) {
                    String statusEmoji = getStatusEmoji(task.getStatus());
                    reportMessage.append(statusEmoji).append(" ").append(task.getDescription()).append("\n");
                    reportMessage.append("   🔄 Estado: ").append(task.getStatus()).append("\n");
                    
                    // Mostrar sprint si existe
                    if (task.getSprintId() != null) {
                        Sprint taskSprint = sprintService.getSprintById(task.getSprintId()).getBody();
                        if (taskSprint != null) {
                            reportMessage.append("   📋 Sprint: ").append(taskSprint.getName()).append("\n");
                        }
                    }
                    
                    if (task.getEstimatedHours() != null) {
                        reportMessage.append("   ⏱️ Horas estimadas: ").append(formatNumber(task.getEstimatedHours())).append("\n");
                    }
                    
                    if (task.getActualHours() != null) {
                        reportMessage.append("   ⏱️ Horas reales: ").append(formatNumber(task.getActualHours())).append("\n");
                        
                        if (task.getEstimatedHours() != null) {
                            double taskEfficiency = (task.getEstimatedHours() / task.getActualHours()) * 100;
                            reportMessage.append("   📈 Eficiencia: ").append(formatNumber(taskEfficiency)).append("%\n");
                        }
                    }
                    
                    reportMessage.append("\n");
                }
                }
                
                sendMessage(chatId, reportMessage.toString(), botController);
                }
                
                // Método para mostrar la lista de sprints para seleccionar
                public void showSprintSelectionForKpi(long chatId, UserBotController botController) {
                List<Sprint> sprints = sprintService.findAll();
                
                if (sprints.isEmpty()) {
                    sendErrorMessage(chatId, "No hay sprints disponibles.", botController);
                    return;
                }
                
                StringBuilder message = new StringBuilder();
                message.append("📋 SELECCIONE UN SPRINT PARA VER SUS KPIs:\n\n");
                
                for (Sprint sprint : sprints) {
                    message.append("ID: ").append(sprint.getId()).append(" - ");
                    message.append(sprint.getName()).append("\n");
                    message.append("   📅 Periodo: ").append(sprint.getStartDate()).append(" al ").append(sprint.getEndDate()).append("\n");
                    message.append("   🔄 Estado: ").append(sprint.getStatus()).append("\n\n");
                }
                
                message.append("Por favor, responde con el ID del sprint que deseas analizar.");
                
                sendMessage(chatId, message.toString(), botController);
                }
                
                // Método para mostrar la lista de desarrolladores para seleccionar
                public void showDeveloperSelectionForKpi(long chatId, UserBotController botController) {
                List<User> developers = userService.findByRole("Developer");
                
                if (developers.isEmpty()) {
                    sendErrorMessage(chatId, "No hay desarrolladores disponibles.", botController);
                    return;
                }
                
                StringBuilder message = new StringBuilder();
                message.append("👨‍💻 SELECCIONE UN DESARROLLADOR PARA VER SUS KPIs:\n\n");
                
                for (User developer : developers) {
                    message.append("ID: ").append(developer.getID()).append(" - ");
                    message.append(developer.getName()).append("\n");
                }
                
                message.append("\nPor favor, responde con el ID del desarrollador que deseas analizar.");
                
                sendMessage(chatId, message.toString(), botController);
                }
                
                // Método para solicitar el año y número de semana
                public void requestWeekSelection(long chatId, UserBotController botController) {
                LocalDate now = LocalDate.now();
                int currentYear = now.getYear();
                int currentWeek = now.get(WeekFields.of(Locale.getDefault()).weekOfYear());
                
                StringBuilder message = new StringBuilder();
                message.append("📅 SELECCIÓN DE SEMANA PARA ANÁLISIS KPI:\n\n");
                message.append("Año actual: ").append(currentYear).append("\n");
                message.append("Semana actual: ").append(currentWeek).append("\n\n");
                message.append("Por favor, ingresa el año y número de semana en formato 'YYYY-WW'.\n");
                message.append("Ejemplo: ").append(currentYear).append("-").append(currentWeek).append(" (semana actual)");
                
                sendMessage(chatId, message.toString(), botController);
                }
                
                // Métodos auxiliares
                private String formatNumber(Double number) {
                if (number == null) return "0";
                return String.format("%.2f", number);
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
                
                private void sendMessage(long chatId, String text, UserBotController botController) {
                // Dividir el mensaje si es demasiado largo (límite de Telegram es 4096 caracteres)
                int maxLength = 4000; // Un poco menos que el límite para tener margen
                
                if (text.length() <= maxLength) {
                    SendMessage message = new SendMessage();
                    message.setChatId(chatId);
                    message.setText(text);
                    
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
                            messageText = "Parte " + (i+1) + " de " + parts.size() + ":\n\n" + messageText;
                        }
                        
                        SendMessage message = new SendMessage();
                        message.setChatId(chatId);
                        message.setText(messageText);
                        
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
                message.setText("❌ Error: " + errorMessage);
                
                try {
                    botController.execute(message);
                } catch (TelegramApiException e) {
                    logger.error("Error sending error message", e);
                }
                }
                }