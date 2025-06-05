package com.springboot.MyTodoList.model;

import javax.persistence.*;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "SPRINT")
public class Sprint {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    
    @Column(name = "NAME", nullable = false, length = 100)
    private String name;
    
    @Column(name = "START_DATE", nullable = false)
    private LocalDate startDate;
    
    @Column(name = "END_DATE", nullable = false)
    private LocalDate endDate;
    
    @Column(name = "STATUS", length = 20)
    private String status = "PLANNED";
    
    @Column(name = "CREATED_BY")
    private Integer createdBy;
    
    @Column(name = "CREATION_TS")
    private OffsetDateTime creationTs;

    public Sprint() {
    }

    public Sprint(int id, String name, LocalDate startDate, LocalDate endDate, String status, 
                 Integer createdBy, OffsetDateTime creationTs) {
        this.id = id;
        this.name = name;
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = status;
        this.createdBy = createdBy;
        this.creationTs = creationTs;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Integer createdBy) {
        this.createdBy = createdBy;
    }

    public OffsetDateTime getCreationTs() {
        return creationTs;
    }

    public void setCreationTs(OffsetDateTime creationTs) {
        this.creationTs = creationTs;
    }

    @Override
    public String toString() {
        return "Sprint{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", startDate=" + startDate +
                ", endDate=" + endDate +
                ", status='" + status + '\'' +
                ", createdBy=" + createdBy +
                ", creationTs=" + creationTs +
                '}';
    }
}