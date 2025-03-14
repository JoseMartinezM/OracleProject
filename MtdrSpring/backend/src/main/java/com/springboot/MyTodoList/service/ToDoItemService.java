package com.springboot.MyTodoList.service;

import com.springboot.MyTodoList.model.ToDoItem;
import com.springboot.MyTodoList.repository.ToDoItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ToDoItemService {

    @Autowired
    private ToDoItemRepository toDoItemRepository;
    
    // Obtener todas las tareas activas (no archivadas)
    public List<ToDoItem> findAll() {
        List<ToDoItem> todoItems = toDoItemRepository.findAll();
        return todoItems.stream()
            .filter(item -> item.getIsArchived() == null || item.getIsArchived() == 0)
            .collect(Collectors.toList());
    }
    
    // Obtener todas las tareas archivadas
    public List<ToDoItem> findAllArchived() {
        List<ToDoItem> todoItems = toDoItemRepository.findAll();
        return todoItems.stream()
            .filter(item -> item.getIsArchived() != null && item.getIsArchived() == 1)
            .collect(Collectors.toList());
    }
    
    // Obtener tarea por ID
    public ResponseEntity<ToDoItem> getItemById(int id) {
        Optional<ToDoItem> todoData = toDoItemRepository.findById(id);
        if (todoData.isPresent()) {
            return new ResponseEntity<>(todoData.get(), HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }
    
    // Añadir una nueva tarea
    public ToDoItem addToDoItem(ToDoItem toDoItem) {
        return toDoItemRepository.save(toDoItem);
    }

    // Eliminar una tarea
    public boolean deleteToDoItem(int id) {
        try {
            toDoItemRepository.deleteById(id);
            return true;
        } catch(Exception e) {
            return false;
        }
    }
    
    // Actualizar una tarea existente
    public ToDoItem updateToDoItem(int id, ToDoItem td) {
        Optional<ToDoItem> toDoItemData = toDoItemRepository.findById(id);
        if(toDoItemData.isPresent()) {
            ToDoItem toDoItem = toDoItemData.get();
            toDoItem.setID(id);
            toDoItem.setDescription(td.getDescription());
            
            // Actualizar los nuevos campos
            if (td.getSteps() != null) {
                toDoItem.setSteps(td.getSteps());
            }
            if (td.getStatus() != null) {
                toDoItem.setStatus(td.getStatus());
            }
            if (td.getPriority() != null) {
                toDoItem.setPriority(td.getPriority());
            }
            if (td.getAssignedTo() != null) {
                toDoItem.setAssignedTo(td.getAssignedTo());
            }
            if (td.getCreatedBy() != null) {
                toDoItem.setCreatedBy(td.getCreatedBy());
            }
            if (td.getIsArchived() != null) {
                toDoItem.setIsArchived(td.getIsArchived());
            }
            if (td.getCreation_ts() != null) {
                toDoItem.setCreation_ts(td.getCreation_ts());
            }
            
            // Actualizado para manejar Boolean
            toDoItem.setDone(td.isDone());
            
            return toDoItemRepository.save(toDoItem);
        } else {
            return null;
        }
    }
    
    // Marcar una tarea como archivada
    public ToDoItem archiveToDoItem(int id) {
        Optional<ToDoItem> toDoItemData = toDoItemRepository.findById(id);
        if(toDoItemData.isPresent()) {
            ToDoItem toDoItem = toDoItemData.get();
            toDoItem.setIsArchived(1);
            return toDoItemRepository.save(toDoItem);
        } else {
            return null;
        }
    }
    
    // Actualizar el estado de una tarea
    public ToDoItem updateStatus(int id, String status) {
        Optional<ToDoItem> toDoItemData = toDoItemRepository.findById(id);
        if(toDoItemData.isPresent() && isValidStatus(status)) {
            ToDoItem toDoItem = toDoItemData.get();
            toDoItem.setStatus(status);
            
            // Si el estado es "Completed", marcar como done
            if ("Completed".equals(status)) {
                toDoItem.setDone(true);
            }
            
            return toDoItemRepository.save(toDoItem);
        } else {
            return null;
        }
    }
    
    // Validar que el estado sea uno de los permitidos
    private boolean isValidStatus(String status) {
        return status != null && (
            "Pending".equals(status) || 
            "In Progress".equals(status) || 
            "In Review".equals(status) || 
            "Completed".equals(status)
        );
    }
    
    // Obtener tareas por prioridad
    public List<ToDoItem> findByPriority(String priority) {
        List<ToDoItem> todoItems = toDoItemRepository.findAll();
        return todoItems.stream()
            .filter(item -> item.getPriority() != null && 
                           item.getPriority().equals(priority) && 
                           (item.getIsArchived() == null || item.getIsArchived() == 0))
            .collect(Collectors.toList());
    }
    
    // Obtener tareas por estado
    public List<ToDoItem> findByStatus(String status) {
        List<ToDoItem> todoItems = toDoItemRepository.findAll();
        return todoItems.stream()
            .filter(item -> item.getStatus() != null && 
                           item.getStatus().equals(status) && 
                           (item.getIsArchived() == null || item.getIsArchived() == 0))
            .collect(Collectors.toList());
    }
    
    // Obtener tareas asignadas a un usuario específico
    public List<ToDoItem> findByAssignedTo(Integer userId) {
        List<ToDoItem> todoItems = toDoItemRepository.findAll();
        return todoItems.stream()
            .filter(item -> item.getAssignedTo() != null && 
                           item.getAssignedTo().equals(userId) && 
                           (item.getIsArchived() == null || item.getIsArchived() == 0))
            .collect(Collectors.toList());
    }
    
    // Obtener tareas creadas por un manager específico
    public List<ToDoItem> findByCreatedBy(Integer managerId) {
        List<ToDoItem> todoItems = toDoItemRepository.findAll();
        return todoItems.stream()
            .filter(item -> item.getCreatedBy() != null && 
                           item.getCreatedBy().equals(managerId) && 
                           (item.getIsArchived() == null || item.getIsArchived() == 0))
            .collect(Collectors.toList());
    }
}