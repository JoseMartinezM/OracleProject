package com.springboot.MyTodoList.service;

import com.springboot.MyTodoList.model.Sprint;
import com.springboot.MyTodoList.model.ToDoItem;
import com.springboot.MyTodoList.model.User;
import com.springboot.MyTodoList.repository.SprintRepository;
import com.springboot.MyTodoList.repository.ToDoItemRepository;
import com.springboot.MyTodoList.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoField;
import java.time.temporal.WeekFields;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ReportService {

    @Autowired
    private ToDoItemRepository toDoItemRepository;
    
    @Autowired
    private SprintRepository sprintRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private SprintService sprintService;
    
    // 1.1 Lista de Tareas completadas en cada Sprint
    public List<Map<String, Object>> getCompletedTasksForSprint(int sprintId) {
        Optional<Sprint> sprintOpt = sprintRepository.findById(sprintId);
        if (!sprintOpt.isPresent()) {
            return new ArrayList<>();
        }
        
        List<ToDoItem> allTasks = toDoItemRepository.findAll();
        
        return allTasks.stream()
            .filter(task -> task.getSprintId() != null && task.getSprintId().equals(sprintId))
            .filter(task -> "Completed".equals(task.getStatus()) || Boolean.TRUE.equals(task.isDone()))
            .map(task -> {
                Map<String, Object> taskMap = new HashMap<>();
                taskMap.put("id", task.getID());
                taskMap.put("description", task.getDescription());
                taskMap.put("estimatedHours", task.getEstimatedHours() != null ? task.getEstimatedHours() : 0);
                taskMap.put("actualHours", task.getActualHours() != null ? task.getActualHours() : 0);
                
                // Get developer name
                if (task.getAssignedTo() != null) {
                    Optional<User> userOpt = userRepository.findById(task.getAssignedTo());
                    taskMap.put("developerName", userOpt.isPresent() ? userOpt.get().getName() : "Unknown");
                    taskMap.put("developerId", task.getAssignedTo());
                } else {
                    taskMap.put("developerName", "Unassigned");
                    taskMap.put("developerId", null);
                }
                
                taskMap.put("completionDate", task.getCreation_ts());
                return taskMap;
            })
            .collect(Collectors.toList());
    }
    
    // 1.2 Reporte KPI Horas trabajadas y Tareas completadas por EQUIPO por sprint
    public Map<String, Object> getTeamKpiForSprint(int sprintId) {
        Map<String, Object> result = new HashMap<>();
        
        Optional<Sprint> sprintOpt = sprintRepository.findById(sprintId);
        if (!sprintOpt.isPresent()) {
            return result;
        }
        
        Sprint sprint = sprintOpt.get();
        result.put("sprintId", sprintId);
        result.put("sprintName", sprint.getName());
        result.put("startDate", sprint.getStartDate());
        result.put("endDate", sprint.getEndDate());
        
        List<ToDoItem> sprintTasks = toDoItemRepository.findAll().stream()
            .filter(task -> task.getSprintId() != null && task.getSprintId().equals(sprintId))
            .collect(Collectors.toList());
        
        // Total estimated hours
        double totalEstimatedHours = sprintTasks.stream()
            .filter(task -> task.getEstimatedHours() != null)
            .mapToDouble(ToDoItem::getEstimatedHours)
            .sum();
        
        // Total actual hours
        double totalActualHours = sprintTasks.stream()
            .filter(task -> task.getActualHours() != null)
            .mapToDouble(ToDoItem::getActualHours)
            .sum();
        
        // Completed tasks
        List<ToDoItem> completedTasks = sprintTasks.stream()
            .filter(task -> "Completed".equals(task.getStatus()) || Boolean.TRUE.equals(task.isDone()))
            .collect(Collectors.toList());
        
        double completedEstimatedHours = completedTasks.stream()
            .filter(task -> task.getEstimatedHours() != null)
            .mapToDouble(ToDoItem::getEstimatedHours)
            .sum();
        
        double completedActualHours = completedTasks.stream()
            .filter(task -> task.getActualHours() != null)
            .mapToDouble(ToDoItem::getActualHours)
            .sum();
        
        // Efficiency calculation
        double efficiency = 0;
        if (completedActualHours > 0) {
            efficiency = (completedEstimatedHours / completedActualHours) * 100;
        }
        
        result.put("totalEstimatedHours", totalEstimatedHours);
        result.put("totalActualHours", totalActualHours);
        result.put("totalTasks", sprintTasks.size());
        result.put("completedTasks", completedTasks.size());
        result.put("completionRate", sprintTasks.size() > 0 ? 
                   (double) completedTasks.size() / sprintTasks.size() * 100 : 0);
        result.put("efficiency", efficiency);
        
        return result;
    }
    
    // 1.2 Reporte KPI Horas trabajadas y Tareas completadas por EQUIPO por semana
    public Map<String, Object> getTeamKpiForWeek(int year, int weekNumber) {
        Map<String, Object> result = new HashMap<>();
        
        // Set week dates
        LocalDate startOfWeek = LocalDate.now()
            .with(WeekFields.ISO.weekOfYear(), weekNumber)
            .with(WeekFields.ISO.dayOfWeek(), 1)
            .withYear(year);
        
        LocalDate endOfWeek = startOfWeek.plusDays(6);
        
        result.put("weekNumber", weekNumber);
        result.put("year", year);
        result.put("startDate", startOfWeek);
        result.put("endDate", endOfWeek);
        
        // Get all tasks
        List<ToDoItem> allTasks = toDoItemRepository.findAll();
        
        // Filter tasks with creation_ts within the week
        List<ToDoItem> weekTasks = allTasks.stream()
            .filter(task -> {
                if (task.getCreation_ts() == null) return false;
                
                LocalDate taskDate = task.getCreation_ts().toLocalDate();
                return !taskDate.isBefore(startOfWeek) && !taskDate.isAfter(endOfWeek);
            })
            .collect(Collectors.toList());
        
        // Total estimated hours
        double totalEstimatedHours = weekTasks.stream()
            .filter(task -> task.getEstimatedHours() != null)
            .mapToDouble(ToDoItem::getEstimatedHours)
            .sum();
        
        // Total actual hours
        double totalActualHours = weekTasks.stream()
            .filter(task -> task.getActualHours() != null)
            .mapToDouble(ToDoItem::getActualHours)
            .sum();
        
        // Completed tasks
        List<ToDoItem> completedTasks = weekTasks.stream()
            .filter(task -> "Completed".equals(task.getStatus()) || Boolean.TRUE.equals(task.isDone()))
            .collect(Collectors.toList());
        
        double completedEstimatedHours = completedTasks.stream()
            .filter(task -> task.getEstimatedHours() != null)
            .mapToDouble(ToDoItem::getEstimatedHours)
            .sum();
        
        double completedActualHours = completedTasks.stream()
            .filter(task -> task.getActualHours() != null)
            .mapToDouble(ToDoItem::getActualHours)
            .sum();
        
        // Efficiency calculation
        double efficiency = 0;
        if (completedActualHours > 0) {
            efficiency = (completedEstimatedHours / completedActualHours) * 100;
        }
        
        result.put("totalEstimatedHours", totalEstimatedHours);
        result.put("totalActualHours", totalActualHours);
        result.put("totalTasks", weekTasks.size());
        result.put("completedTasks", completedTasks.size());
        result.put("completionRate", weekTasks.size() > 0 ? 
                   (double) completedTasks.size() / weekTasks.size() * 100 : 0);
        result.put("efficiency", efficiency);
        
        return result;
    }
    
    // 1.3 Reporte KPI Horas trabajadas y Tareas completadas por PERSONA por sprint
    public Map<String, Object> getUserKpiForSprint(int userId, int sprintId) {
        Map<String, Object> result = new HashMap<>();
        
        Optional<User> userOpt = userRepository.findById(userId);
        Optional<Sprint> sprintOpt = sprintRepository.findById(sprintId);
        
        if (!userOpt.isPresent() || !sprintOpt.isPresent()) {
            return result;
        }
        
        User user = userOpt.get();
        Sprint sprint = sprintOpt.get();
        
        result.put("userId", userId);
        result.put("userName", user.getName());
        result.put("userRole", user.getRole());
        result.put("sprintId", sprintId);
        result.put("sprintName", sprint.getName());
        result.put("startDate", sprint.getStartDate());
        result.put("endDate", sprint.getEndDate());
        
        // Get all tasks assigned to this user in this sprint
        List<ToDoItem> userSprintTasks = toDoItemRepository.findAll().stream()
            .filter(task -> task.getAssignedTo() != null && task.getAssignedTo().equals(userId))
            .filter(task -> task.getSprintId() != null && task.getSprintId().equals(sprintId))
            .collect(Collectors.toList());
        
        // Total estimated hours
        double totalEstimatedHours = userSprintTasks.stream()
            .filter(task -> task.getEstimatedHours() != null)
            .mapToDouble(ToDoItem::getEstimatedHours)
            .sum();
        
        // Total actual hours
        double totalActualHours = userSprintTasks.stream()
            .filter(task -> task.getActualHours() != null)
            .mapToDouble(ToDoItem::getActualHours)
            .sum();
        
        // Completed tasks
        List<ToDoItem> completedTasks = userSprintTasks.stream()
            .filter(task -> "Completed".equals(task.getStatus()) || Boolean.TRUE.equals(task.isDone()))
            .collect(Collectors.toList());
        
        double completedEstimatedHours = completedTasks.stream()
            .filter(task -> task.getEstimatedHours() != null)
            .mapToDouble(ToDoItem::getEstimatedHours)
            .sum();
        
        double completedActualHours = completedTasks.stream()
            .filter(task -> task.getActualHours() != null)
            .mapToDouble(ToDoItem::getActualHours)
            .sum();
        
        // Efficiency calculation
        double efficiency = 0;
        if (completedActualHours > 0) {
            efficiency = (completedEstimatedHours / completedActualHours) * 100;
        }
        
        result.put("totalEstimatedHours", totalEstimatedHours);
        result.put("totalActualHours", totalActualHours);
        result.put("totalTasks", userSprintTasks.size());
        result.put("completedTasks", completedTasks.size());
        result.put("completionRate", userSprintTasks.size() > 0 ? 
                   (double) completedTasks.size() / userSprintTasks.size() * 100 : 0);
        result.put("efficiency", efficiency);
        
        // Add list of tasks for detailed view
        List<Map<String, Object>> tasksDetails = userSprintTasks.stream()
            .map(task -> {
                Map<String, Object> taskMap = new HashMap<>();
                taskMap.put("id", task.getID());
                taskMap.put("description", task.getDescription());
                taskMap.put("status", task.getStatus());
                taskMap.put("estimatedHours", task.getEstimatedHours() != null ? task.getEstimatedHours() : 0);
                taskMap.put("actualHours", task.getActualHours() != null ? task.getActualHours() : 0);
                taskMap.put("completed", "Completed".equals(task.getStatus()) || Boolean.TRUE.equals(task.isDone()));
                return taskMap;
            })
            .collect(Collectors.toList());
        
        result.put("tasks", tasksDetails);
        
        return result;
    }
    
    // 1.3 Reporte KPI Horas trabajadas y Tareas completadas por PERSONA por semana
    public Map<String, Object> getUserKpiForWeek(int userId, int year, int weekNumber) {
        Map<String, Object> result = new HashMap<>();
        
        Optional<User> userOpt = userRepository.findById(userId);
        if (!userOpt.isPresent()) {
            return result;
        }
        
        User user = userOpt.get();
        
        // Set week dates
        LocalDate startOfWeek = LocalDate.now()
            .with(WeekFields.ISO.weekOfYear(), weekNumber)
            .with(WeekFields.ISO.dayOfWeek(), 1)
            .withYear(year);
        
        LocalDate endOfWeek = startOfWeek.plusDays(6);
        
        result.put("userId", userId);
        result.put("userName", user.getName());
        result.put("userRole", user.getRole());
        result.put("weekNumber", weekNumber);
        result.put("year", year);
        result.put("startDate", startOfWeek);
        result.put("endDate", endOfWeek);
        
        // Get all tasks assigned to this user with creation_ts within the week
        List<ToDoItem> userWeekTasks = toDoItemRepository.findAll().stream()
            .filter(task -> task.getAssignedTo() != null && task.getAssignedTo().equals(userId))
            .filter(task -> {
                if (task.getCreation_ts() == null) return false;
                
                LocalDate taskDate = task.getCreation_ts().toLocalDate();
                return !taskDate.isBefore(startOfWeek) && !taskDate.isAfter(endOfWeek);
            })
            .collect(Collectors.toList());
        
        // Total estimated hours
        double totalEstimatedHours = userWeekTasks.stream()
            .filter(task -> task.getEstimatedHours() != null)
            .mapToDouble(ToDoItem::getEstimatedHours)
            .sum();
        
        // Total actual hours
        double totalActualHours = userWeekTasks.stream()
            .filter(task -> task.getActualHours() != null)
            .mapToDouble(ToDoItem::getActualHours)
            .sum();
        
        // Completed tasks
        List<ToDoItem> completedTasks = userWeekTasks.stream()
            .filter(task -> "Completed".equals(task.getStatus()) || Boolean.TRUE.equals(task.isDone()))
            .collect(Collectors.toList());
        
        double completedEstimatedHours = completedTasks.stream()
            .filter(task -> task.getEstimatedHours() != null)
            .mapToDouble(ToDoItem::getEstimatedHours)
            .sum();
        
        double completedActualHours = completedTasks.stream()
            .filter(task -> task.getActualHours() != null)
            .mapToDouble(ToDoItem::getActualHours)
            .sum();
        
        // Efficiency calculation
        double efficiency = 0;
        if (completedActualHours > 0) {
            efficiency = (completedEstimatedHours / completedActualHours) * 100;
        }
        
        result.put("totalEstimatedHours", totalEstimatedHours);
        result.put("totalActualHours", totalActualHours);
        result.put("totalTasks", userWeekTasks.size());
        result.put("completedTasks", completedTasks.size());
        result.put("completionRate", userWeekTasks.size() > 0 ? 
                   (double) completedTasks.size() / userWeekTasks.size() * 100 : 0);
        result.put("efficiency", efficiency);
        
        // Add list of tasks for detailed view
        List<Map<String, Object>> tasksDetails = userWeekTasks.stream()
            .map(task -> {
                Map<String, Object> taskMap = new HashMap<>();
                taskMap.put("id", task.getID());
                taskMap.put("description", task.getDescription());
                taskMap.put("status", task.getStatus());
                taskMap.put("estimatedHours", task.getEstimatedHours() != null ? task.getEstimatedHours() : 0);
                taskMap.put("actualHours", task.getActualHours() != null ? task.getActualHours() : 0);
                taskMap.put("completed", "Completed".equals(task.getStatus()) || Boolean.TRUE.equals(task.isDone()));
                
                // Get sprint info if available
                if (task.getSprintId() != null) {
                    Optional<Sprint> sprintOpt = sprintRepository.findById(task.getSprintId());
                    if (sprintOpt.isPresent()) {
                        taskMap.put("sprintName", sprintOpt.get().getName());
                        taskMap.put("sprintId", task.getSprintId());
                    }
                }
                
                return taskMap;
            })
            .collect(Collectors.toList());
        
        result.put("tasks", tasksDetails);
        
        return result;
    }
    
    // Obtener resumen de todos los sprints
    public List<Map<String, Object>> getSprintsSummary() {
        List<Sprint> allSprints = sprintRepository.findAll();
        
        return allSprints.stream()
            .map(sprint -> {
                Map<String, Object> sprintMap = new HashMap<>();
                sprintMap.put("id", sprint.getId());
                sprintMap.put("name", sprint.getName());
                sprintMap.put("startDate", sprint.getStartDate());
                sprintMap.put("endDate", sprint.getEndDate());
                sprintMap.put("status", sprint.getStatus());
                
                // Get tasks count for this sprint
                List<ToDoItem> sprintTasks = toDoItemRepository.findAll().stream()
                    .filter(task -> task.getSprintId() != null && task.getSprintId().equals(sprint.getId()))
                    .collect(Collectors.toList());
                
                int totalTasks = sprintTasks.size();
                int completedTasks = (int) sprintTasks.stream()
                    .filter(task -> "Completed".equals(task.getStatus()) || Boolean.TRUE.equals(task.isDone()))
                    .count();
                
                double totalEstimatedHours = sprintTasks.stream()
                    .filter(task -> task.getEstimatedHours() != null)
                    .mapToDouble(ToDoItem::getEstimatedHours)
                    .sum();
                
                double totalActualHours = sprintTasks.stream()
                    .filter(task -> task.getActualHours() != null)
                    .mapToDouble(ToDoItem::getActualHours)
                    .sum();
                
                sprintMap.put("totalTasks", totalTasks);
                sprintMap.put("completedTasks", completedTasks);
                sprintMap.put("completionRate", totalTasks > 0 ? (double) completedTasks / totalTasks * 100 : 0);
                sprintMap.put("totalEstimatedHours", totalEstimatedHours);
                sprintMap.put("totalActualHours", totalActualHours);
                
                return sprintMap;
            })
            .collect(Collectors.toList());
    }
    
    // Obtener resumen de todos los usuarios
    public List<Map<String, Object>> getUsersSummary() {
        List<User> developers = userRepository.findAll().stream()
            .filter(user -> "Developer".equals(user.getRole()))
            .collect(Collectors.toList());
        
        return developers.stream()
            .map(developer -> {
                Map<String, Object> userMap = new HashMap<>();
                userMap.put("id", developer.getID());
                userMap.put("name", developer.getName());
                
                // Get tasks for this developer
                List<ToDoItem> userTasks = toDoItemRepository.findAll().stream()
                    .filter(task -> task.getAssignedTo() != null && task.getAssignedTo().equals(developer.getID()))
                    .collect(Collectors.toList());
                
                int totalTasks = userTasks.size();
                int completedTasks = (int) userTasks.stream()
                    .filter(task -> "Completed".equals(task.getStatus()) || Boolean.TRUE.equals(task.isDone()))
                    .count();
                
                double totalEstimatedHours = userTasks.stream()
                    .filter(task -> task.getEstimatedHours() != null)
                    .mapToDouble(ToDoItem::getEstimatedHours)
                    .sum();
                
                double totalActualHours = userTasks.stream()
                    .filter(task -> task.getActualHours() != null)
                    .mapToDouble(ToDoItem::getActualHours)
                    .sum();
                
                userMap.put("totalTasks", totalTasks);
                userMap.put("completedTasks", completedTasks);
                userMap.put("completionRate", totalTasks > 0 ? (double) completedTasks / totalTasks * 100 : 0);
                userMap.put("totalEstimatedHours", totalEstimatedHours);
                userMap.put("totalActualHours", totalActualHours);
                
                // Efficiency calculation
                double completedEstimatedHours = userTasks.stream()
                    .filter(task -> "Completed".equals(task.getStatus()) || Boolean.TRUE.equals(task.isDone()))
                    .filter(task -> task.getEstimatedHours() != null)
                    .mapToDouble(ToDoItem::getEstimatedHours)
                    .sum();
                
                double completedActualHours = userTasks.stream()
                    .filter(task -> "Completed".equals(task.getStatus()) || Boolean.TRUE.equals(task.isDone()))
                    .filter(task -> task.getActualHours() != null)
                    .mapToDouble(ToDoItem::getActualHours)
                    .sum();
                
                double efficiency = 0;
                if (completedActualHours > 0) {
                    efficiency = (completedEstimatedHours / completedActualHours) * 100;
                }
                
                userMap.put("efficiency", efficiency);
                
                return userMap;
            })
            .collect(Collectors.toList());
    }
}