package com.springboot.MyTodoList.controller;

import com.springboot.MyTodoList.model.Sprint;
import com.springboot.MyTodoList.model.ToDoItem;
import com.springboot.MyTodoList.service.SprintService;
import com.springboot.MyTodoList.service.ToDoItemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class SprintController {

    @Autowired
    private SprintService sprintService;
    
    @Autowired
    private ToDoItemService toDoItemService;
    
    // Obtener todos los sprints
    @GetMapping("/sprints")
    public List<Sprint> getAllSprints() {
        return sprintService.findAll();
    }
    
    // Obtener sprint por ID
    @GetMapping("/sprints/{id}")
    public ResponseEntity<Sprint> getSprintById(@PathVariable int id) {
        try {
            ResponseEntity<Sprint> responseEntity = sprintService.getSprintById(id);
            return new ResponseEntity<>(responseEntity.getBody(), HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }
    
    // Crear nuevo sprint
    @PostMapping("/sprints")
    public ResponseEntity<Sprint> createSprint(@RequestBody Sprint sprint) {
        try {
            Sprint createdSprint = sprintService.createSprint(sprint);
            return new ResponseEntity<>(createdSprint, HttpStatus.CREATED);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    // Actualizar sprint existente
    @PutMapping("/sprints/{id}")
    public ResponseEntity<Sprint> updateSprint(@PathVariable int id, @RequestBody Sprint sprint) {
        try {
            Sprint updatedSprint = sprintService.updateSprint(id, sprint);
            if (updatedSprint != null) {
                return new ResponseEntity<>(updatedSprint, HttpStatus.OK);
            } else {
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    // Eliminar sprint
    @DeleteMapping("/sprints/{id}")
    public ResponseEntity<Boolean> deleteSprint(@PathVariable int id) {
        Boolean flag = false;
        try {
            flag = sprintService.deleteSprint(id);
            return new ResponseEntity<>(flag, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(flag, HttpStatus.NOT_FOUND);
        }
    }
    
    // Obtener sprints por estado
    @GetMapping("/sprints/status/{status}")
    public List<Sprint> getSprintsByStatus(@PathVariable String status) {
        return sprintService.findByStatus(status);
    }
    
    // Obtener sprints creados por un usuario específico
    @GetMapping("/sprints/created/{userId}")
    public List<Sprint> getSprintsByCreatedBy(@PathVariable Integer userId) {
        return sprintService.findByCreatedBy(userId);
    }
    
    // Obtener sprints activos
    @GetMapping("/sprints/active")
    public List<Sprint> getActiveSprints() {
        return sprintService.findActiveSprints();
    }
    
    // Actualizar estado de un sprint
    @PatchMapping("/sprints/{id}/status")
    public ResponseEntity<Sprint> updateSprintStatus(@PathVariable int id, @RequestBody Map<String, String> statusMap) {
        try {
            String status = statusMap.get("status");
            Sprint sprint = sprintService.updateStatus(id, status);
            if (sprint != null) {
                return new ResponseEntity<>(sprint, HttpStatus.OK);
            } else {
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    // Obtener todas las tareas de un sprint específico
    @GetMapping("/sprints/{id}/tasks")
    public List<ToDoItem> getSprintTasks(@PathVariable int id) {
        return toDoItemService.findBySprintId(id);
    }
}