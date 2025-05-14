package com.springboot.MyTodoList.controller;

import com.springboot.MyTodoList.model.Sprint;
import com.springboot.MyTodoList.model.ToDoItem;
import com.springboot.MyTodoList.model.User;
import com.springboot.MyTodoList.service.ReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    @Autowired
    private ReportService reportService;
    
    // 1.1 Lista de Tareas completadas en cada Sprint
    @GetMapping("/sprint/{sprintId}/completed-tasks")
    public ResponseEntity<List<Map<String, Object>>> getCompletedTasksForSprint(@PathVariable int sprintId) {
        List<Map<String, Object>> completedTasks = reportService.getCompletedTasksForSprint(sprintId);
        return new ResponseEntity<>(completedTasks, HttpStatus.OK);
    }
    
    // 1.2 Reporte KPI Horas trabajadas y Tareas completadas por EQUIPO por sprint
    @GetMapping("/team/kpi/{sprintId}")
    public ResponseEntity<Map<String, Object>> getTeamKpiForSprint(@PathVariable int sprintId) {
        Map<String, Object> teamKpi = reportService.getTeamKpiForSprint(sprintId);
        return new ResponseEntity<>(teamKpi, HttpStatus.OK);
    }

    // 1.2 Reporte KPI Horas trabajadas y Tareas completadas por EQUIPO por semana
    @GetMapping("/team/kpi/weekly/{year}/{weekNumber}")
    public ResponseEntity<Map<String, Object>> getTeamKpiForWeek(
            @PathVariable int year, 
            @PathVariable int weekNumber) {
        Map<String, Object> teamKpi = reportService.getTeamKpiForWeek(year, weekNumber);
        return new ResponseEntity<>(teamKpi, HttpStatus.OK);
    }
    
    // 1.3 Reporte KPI Horas trabajadas y Tareas completadas por PERSONA por sprint
    @GetMapping("/user/{userId}/kpi/{sprintId}")
    public ResponseEntity<Map<String, Object>> getUserKpiForSprint(
            @PathVariable int userId, 
            @PathVariable int sprintId) {
        Map<String, Object> userKpi = reportService.getUserKpiForSprint(userId, sprintId);
        return new ResponseEntity<>(userKpi, HttpStatus.OK);
    }
    
    // 1.3 Reporte KPI Horas trabajadas y Tareas completadas por PERSONA por semana
    @GetMapping("/user/{userId}/kpi/weekly/{year}/{weekNumber}")
    public ResponseEntity<Map<String, Object>> getUserKpiForWeek(
            @PathVariable int userId, 
            @PathVariable int year, 
            @PathVariable int weekNumber) {
        Map<String, Object> userKpi = reportService.getUserKpiForWeek(userId, year, weekNumber);
        return new ResponseEntity<>(userKpi, HttpStatus.OK);
    }
    
    // Obtener datos resumen de todos los sprints
    @GetMapping("/sprints/summary")
    public ResponseEntity<List<Map<String, Object>>> getSprintsSummary() {
        List<Map<String, Object>> summary = reportService.getSprintsSummary();
        return new ResponseEntity<>(summary, HttpStatus.OK);
    }
    
    // Obtener datos resumen de todos los usuarios
    @GetMapping("/users/summary")
    public ResponseEntity<List<Map<String, Object>>> getUsersSummary() {
        List<Map<String, Object>> summary = reportService.getUsersSummary();
        return new ResponseEntity<>(summary, HttpStatus.OK);
    }

    
}