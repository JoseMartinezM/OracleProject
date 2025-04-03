package com.springboot.MyTodoList.service;

import com.springboot.MyTodoList.model.Sprint;
import com.springboot.MyTodoList.model.ToDoItem;
import com.springboot.MyTodoList.repository.SprintRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class SprintService {

    @Autowired
    private SprintRepository sprintRepository;
    
    @Autowired
    private ToDoItemService toDoItemService;
    
    // Obtener todos los sprints
    public List<Sprint> findAll() {
        return sprintRepository.findAll();
    }
    
    // Obtener sprint por ID
    public ResponseEntity<Sprint> getSprintById(int id) {
        Optional<Sprint> sprintData = sprintRepository.findById(id);
        if (sprintData.isPresent()) {
            return new ResponseEntity<>(sprintData.get(), HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }
    
    // Crear un nuevo sprint
    public Sprint createSprint(Sprint sprint) {
        // Establecer la fecha de creación si no se proporciona
        if (sprint.getCreationTs() == null) {
            sprint.setCreationTs(OffsetDateTime.now());
        }
        return sprintRepository.save(sprint);
    }
    
    // Actualizar un sprint existente
    public Sprint updateSprint(int id, Sprint sprint) {
        Optional<Sprint> sprintData = sprintRepository.findById(id);
        if (sprintData.isPresent()) {
            Sprint existingSprint = sprintData.get();
            
            // Actualizar campos
            if (sprint.getName() != null) {
                existingSprint.setName(sprint.getName());
            }
            if (sprint.getStartDate() != null) {
                existingSprint.setStartDate(sprint.getStartDate());
            }
            if (sprint.getEndDate() != null) {
                existingSprint.setEndDate(sprint.getEndDate());
            }
            if (sprint.getStatus() != null) {
                existingSprint.setStatus(sprint.getStatus());
            }
            
            return sprintRepository.save(existingSprint);
        } else {
            return null;
        }
    }
    
    // Eliminar un sprint
    public boolean deleteSprint(int id) {
        try {
            // Verificar si hay tareas asociadas a este sprint
            List<ToDoItem> sprintItems = toDoItemService.findBySprintId(id);
            if (!sprintItems.isEmpty()) {
                // Desasociar las tareas del sprint
                for (ToDoItem item : sprintItems) {
                    item.setSprintId(null);
                    toDoItemService.updateToDoItem(item.getID(), item);
                }
            }
            
            sprintRepository.deleteById(id);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    // Obtener sprints por estado
    public List<Sprint> findByStatus(String status) {
        List<Sprint> sprints = sprintRepository.findAll();
        return sprints.stream()
            .filter(sprint -> sprint.getStatus() != null && sprint.getStatus().equals(status))
            .collect(Collectors.toList());
    }
    
    // Obtener sprints creados por un usuario específico
    public List<Sprint> findByCreatedBy(Integer userId) {
        List<Sprint> sprints = sprintRepository.findAll();
        return sprints.stream()
            .filter(sprint -> sprint.getCreatedBy() != null && sprint.getCreatedBy().equals(userId))
            .collect(Collectors.toList());
    }
    
    // Obtener sprints activos (en curso)
    public List<Sprint> findActiveSprints() {
        LocalDate today = LocalDate.now();
        List<Sprint> sprints = sprintRepository.findAll();
        return sprints.stream()
            .filter(sprint -> 
                (!sprint.getStartDate().isAfter(today) && !sprint.getEndDate().isBefore(today)) ||
                "ACTIVE".equals(sprint.getStatus()))
            .collect(Collectors.toList());
    }
    
    // Cambiar el estado de un sprint
    public Sprint updateStatus(int id, String status) {
        Optional<Sprint> sprintData = sprintRepository.findById(id);
        if (sprintData.isPresent() && isValidStatus(status)) {
            Sprint sprint = sprintData.get();
            sprint.setStatus(status);
            return sprintRepository.save(sprint);
        } else {
            return null;
        }
    }
    
    // Validar que el estado sea uno de los permitidos
    private boolean isValidStatus(String status) {
        return status != null && (
            "PLANNED".equals(status) || 
            "ACTIVE".equals(status) || 
            "COMPLETED".equals(status) || 
            "CANCELLED".equals(status)
        );
    }
}