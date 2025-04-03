package com.springboot.MyTodoList.controller;

import com.springboot.MyTodoList.model.ToDoItem;
import com.springboot.MyTodoList.service.ToDoItemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ToDoItemController {
    
    @Autowired
    private ToDoItemService toDoItemService;
    
    // Obtener todas las tareas activas
    @GetMapping(value = "/todolist")
    public List<ToDoItem> getAllToDoItems() {
        return toDoItemService.findAll();
    }
    
    // Obtener todas las tareas archivadas
    @GetMapping(value = "/todolist/archived")
    public List<ToDoItem> getAllArchivedToDoItems() {
        return toDoItemService.findAllArchived();
    }
    
    // Obtener tarea por ID
    @GetMapping(value = "/todolist/{id}")
    public ResponseEntity<ToDoItem> getToDoItemById(@PathVariable int id) {
        try {
            ResponseEntity<ToDoItem> responseEntity = toDoItemService.getItemById(id);
            return new ResponseEntity<>(responseEntity.getBody(), HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }
    
    // Crear nueva tarea
    @PostMapping(value = "/todolist")
    public ResponseEntity<Object> addToDoItem(@RequestBody ToDoItem todoItem) throws Exception {
        ToDoItem td = toDoItemService.addToDoItem(todoItem);
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.set("location", "" + td.getID());
        responseHeaders.set("Access-Control-Expose-Headers", "location");
        
        return ResponseEntity.ok()
                .headers(responseHeaders).build();
    }
    
    // Actualizar tarea existente
    @PutMapping(value = "/todolist/{id}")
    public ResponseEntity<ToDoItem> updateToDoItem(@RequestBody ToDoItem toDoItem, @PathVariable int id) {
        try {
            ToDoItem toDoItem1 = toDoItemService.updateToDoItem(id, toDoItem);
            if (toDoItem1 != null) {
                return new ResponseEntity<>(toDoItem1, HttpStatus.OK);
            } else {
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    // Eliminar tarea
    @DeleteMapping(value = "/todolist/{id}")
    public ResponseEntity<Boolean> deleteToDoItem(@PathVariable("id") int id) {
        Boolean flag = false;
        try {
            flag = toDoItemService.deleteToDoItem(id);
            return new ResponseEntity<>(flag, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(flag, HttpStatus.NOT_FOUND);
        }
    }
    
    // Archivar tarea
    @PutMapping(value = "/todolist/{id}/archive")
    public ResponseEntity<ToDoItem> archiveToDoItem(@PathVariable int id) {
        try {
            ToDoItem toDoItem = toDoItemService.archiveToDoItem(id);
            if (toDoItem != null) {
                return new ResponseEntity<>(toDoItem, HttpStatus.OK);
            } else {
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    // Actualizar estado de una tarea
    @PatchMapping(value = "/todolist/{id}/status")
    public ResponseEntity<ToDoItem> updateStatus(@PathVariable int id, @RequestBody Map<String, String> statusMap) {
        try {
            String status = statusMap.get("status");
            ToDoItem toDoItem = toDoItemService.updateStatus(id, status);
            if (toDoItem != null) {
                return new ResponseEntity<>(toDoItem, HttpStatus.OK);
            } else {
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    // Obtener tareas por prioridad
    @GetMapping(value = "/todolist/priority/{priority}")
    public List<ToDoItem> getToDoItemsByPriority(@PathVariable String priority) {
        return toDoItemService.findByPriority(priority);
    }
    
    // Obtener tareas por estado
    @GetMapping(value = "/todolist/status/{status}")
    public List<ToDoItem> getToDoItemsByStatus(@PathVariable String status) {
        return toDoItemService.findByStatus(status);
    }
    
    // Obtener tareas asignadas a un usuario específico
    @GetMapping(value = "/todolist/assigned/{userId}")
    public List<ToDoItem> getToDoItemsByAssignedTo(@PathVariable Integer userId) {
        return toDoItemService.findByAssignedTo(userId);
    }
    
    // Obtener tareas creadas por un manager específico
    @GetMapping(value = "/todolist/created/{managerId}")
    public List<ToDoItem> getToDoItemsByCreatedBy(@PathVariable Integer managerId) {
        return toDoItemService.findByCreatedBy(managerId);
    }
    
    // NUEVOS ENDPOINTS PARA SPRINT Y HORAS
    
    // Obtener tareas por Sprint ID
    @GetMapping(value = "/todolist/sprint/{sprintId}")
    public List<ToDoItem> getToDoItemsBySprintId(@PathVariable Integer sprintId) {
        return toDoItemService.findBySprintId(sprintId);
    }
    
    // Asignar una tarea a un sprint
    @PatchMapping(value = "/todolist/{id}/sprint")
    public ResponseEntity<ToDoItem> assignToSprint(@PathVariable int id, @RequestBody Map<String, Integer> sprintMap) {
        try {
            Integer sprintId = sprintMap.get("sprintId");
            ToDoItem toDoItem = toDoItemService.assignToSprint(id, sprintId);
            if (toDoItem != null) {
                return new ResponseEntity<>(toDoItem, HttpStatus.OK);
            } else {
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    // Actualizar horas estimadas
    @PatchMapping(value = "/todolist/{id}/estimated-hours")
    public ResponseEntity<ToDoItem> updateEstimatedHours(@PathVariable int id, @RequestBody Map<String, Double> hoursMap) {
        try {
            Double hours = hoursMap.get("hours");
            ToDoItem toDoItem = toDoItemService.updateEstimatedHours(id, hours);
            if (toDoItem != null) {
                return new ResponseEntity<>(toDoItem, HttpStatus.OK);
            } else {
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    // Actualizar horas reales trabajadas
    @PatchMapping(value = "/todolist/{id}/actual-hours")
    public ResponseEntity<ToDoItem> updateActualHours(@PathVariable int id, @RequestBody Map<String, Double> hoursMap) {
        try {
            Double hours = hoursMap.get("hours");
            ToDoItem toDoItem = toDoItemService.updateActualHours(id, hours);
            if (toDoItem != null) {
                return new ResponseEntity<>(toDoItem, HttpStatus.OK);
            } else {
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}