package com.springboot.MyTodoList.model;

import javax.persistence.*;

@Entity
@Table(name = "USERS", schema = "TODOUSER") // Explicit schema declaration
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID") // Match exact column name
    private int ID;
    
    @Column(name = "USERNAME", nullable = false, unique = true)
    private String username;
    
    @Column(name = "PASSWORD", nullable = false)
    private String password;
    
    @Column(name = "ROLE")
    private String role;
    
    @Column(name = "PHONE")
    private String phone;
    
    @Column(name = "NAME")
    private String name;

    public User() {
    }

    public User(int ID, String username, String password, String role, String phone, String name) {
        this.ID = ID;
        this.username = username;
        this.password = password;
        this.role = role;
        this.phone = phone;
        this.name = name;
    }

    public int getID() {
        return ID;
    }

    public void setID(int ID) {
        this.ID = ID;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "User{" +
                "ID=" + ID +
                ", username='" + username + '\'' +
                ", password='********'" +
                ", role='" + role + '\'' +
                ", phone='" + phone + '\'' +
                ", name='" + name + '\'' +
                '}';
    }
}