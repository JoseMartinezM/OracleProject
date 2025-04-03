package com.springboot.MyTodoList.model;

import javax.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "TODOITEM")
public class ToDoItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    int ID;
    
    @Column(name = "DESCRIPTION", nullable = false, length = 4000)
    String description;
    
    @Column(name = "STEPS", length = 4000)
    String steps;
    
    @Column(name = "STATUS", length = 50)
    String status = "Pending";
    
    @Column(name = "PRIORITY", length = 20)
    String priority = "Medium";
    
    @Column(name = "ASSIGNED_TO")
    Integer assignedTo;
    
    @Column(name = "CREATED_BY")
    Integer createdBy;
    
    @Column(name = "IS_ARCHIVED")
    Integer isArchived = 0;
    
    @Column(name = "CREATION_TS")
    OffsetDateTime creation_ts;
    
    @Column(name = "DONE")
    Boolean done = false;
    
    // Nuevos campos
    @Column(name = "ESTIMATED_HOURS", precision = 5, scale = 2)
    Double estimatedHours;
    
    @Column(name = "ACTUAL_HOURS", precision = 5, scale = 2)
    Double actualHours;
    
    @Column(name = "SPRINT_ID")
    Integer sprintId;

    public ToDoItem() {
    }

    public ToDoItem(int ID, String description, String steps, String status, String priority, 
                    Integer assignedTo, Integer createdBy, Integer isArchived, 
                    OffsetDateTime creation_ts, Boolean done, Double estimatedHours, 
                    Double actualHours, Integer sprintId) {
        this.ID = ID;
        this.description = description;
        this.steps = steps;
        this.status = status;
        this.priority = priority;
        this.assignedTo = assignedTo;
        this.createdBy = createdBy;
        this.isArchived = isArchived;
        this.creation_ts = creation_ts;
        this.done = done;
        this.estimatedHours = estimatedHours;
        this.actualHours = actualHours;
        this.sprintId = sprintId;
    }

    public int getID() {
        return ID;
    }

    public void setID(int ID) {
        this.ID = ID;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSteps() {
        return steps;
    }

    public void setSteps(String steps) {
        this.steps = steps;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public Integer getAssignedTo() {
        return assignedTo;
    }

    public void setAssignedTo(Integer assignedTo) {
        this.assignedTo = assignedTo;
    }

    public Integer getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Integer createdBy) {
        this.createdBy = createdBy;
    }

    public Integer getIsArchived() {
        return isArchived;
    }

    public void setIsArchived(Integer isArchived) {
        this.isArchived = isArchived;
    }

    public OffsetDateTime getCreation_ts() {
        return creation_ts;
    }

    public void setCreation_ts(OffsetDateTime creation_ts) {
        this.creation_ts = creation_ts;
    }

    // Cambiado el método getter para Boolean
    public Boolean isDone() {
        return done;
    }

    public void setDone(Boolean done) {
        this.done = done;
    }
    
    // Getters y setters para los nuevos campos
    public Double getEstimatedHours() {
        return estimatedHours;
    }

    public void setEstimatedHours(Double estimatedHours) {
        this.estimatedHours = estimatedHours;
    }

    public Double getActualHours() {
        return actualHours;
    }

    public void setActualHours(Double actualHours) {
        this.actualHours = actualHours;
    }

    public Integer getSprintId() {
        return sprintId;
    }

    public void setSprintId(Integer sprintId) {
        this.sprintId = sprintId;
    }

    @Override
    public String toString() {
        return "ToDoItem{" +
                "ID=" + ID +
                ", description='" + description + '\'' +
                ", steps='" + steps + '\'' +
                ", status='" + status + '\'' +
                ", priority='" + priority + '\'' +
                ", assignedTo=" + assignedTo +
                ", createdBy=" + createdBy +
                ", isArchived=" + isArchived +
                ", creation_ts=" + creation_ts +
                ", done=" + done +
                ", estimatedHours=" + estimatedHours +
                ", actualHours=" + actualHours +
                ", sprintId=" + sprintId +
                '}';
    }
}