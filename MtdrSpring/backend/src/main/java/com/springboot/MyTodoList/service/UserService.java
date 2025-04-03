package com.springboot.MyTodoList.service;

import com.springboot.MyTodoList.model.User;
import com.springboot.MyTodoList.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;
    
    // Obtener todos los usuarios
    public List<User> findAll() {
        return userRepository.findAll();
    }
    
    // Obtener usuario por ID
    public ResponseEntity<User> getUserById(int id) {
        Optional<User> userData = userRepository.findById(id);
        if (userData.isPresent()) {
            return new ResponseEntity<>(userData.get(), HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }
    
    // Obtener usuario por nombre de usuario
    public ResponseEntity<User> getUserByUsername(String username) {
        Optional<User> userData = userRepository.findByUsername(username);
        if (userData.isPresent()) {
            return new ResponseEntity<>(userData.get(), HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }
    
    // Obtener usuario por número de teléfono
    public ResponseEntity<User> getUserByPhone(String phone) {
        Optional<User> userData = userRepository.findByPhone(phone);
        if (userData.isPresent()) {
            return new ResponseEntity<>(userData.get(), HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }
    
    // Añadir un nuevo usuario
    public User addUser(User user) {
        return userRepository.save(user);
    }

    // Eliminar un usuario
    public boolean deleteUser(int id) {
        try {
            userRepository.deleteById(id);
            return true;
        } catch(Exception e) {
            return false;
        }
    }
    
    // Actualizar un usuario existente
    public User updateUser(int id, User user) {
        Optional<User> userData = userRepository.findById(id);
        if(userData.isPresent()) {
            User existingUser = userData.get();
            existingUser.setID(id);
            
            if (user.getUsername() != null) {
                existingUser.setUsername(user.getUsername());
            }
            if (user.getPassword() != null) {
                existingUser.setPassword(user.getPassword());
            }
            if (user.getRole() != null) {
                existingUser.setRole(user.getRole());
            }
            if (user.getPhone() != null) {
                existingUser.setPhone(user.getPhone());
            }
            if (user.getName() != null) {
                existingUser.setName(user.getName());
            }
            
            return userRepository.save(existingUser);
        } else {
            return null;
        }
    }
    
    // Verificar si un usuario tiene el rol de Manager
    public boolean isManager(int id) {
        Optional<User> userData = userRepository.findById(id);
        return userData.isPresent() && "Manager".equals(userData.get().getRole());
    }
    
    // Obtener usuarios por rol
    public List<User> findByRole(String role) {
        List<User> users = userRepository.findAll();
        return users.stream()
            .filter(user -> user.getRole() != null && user.getRole().equals(role))
            .collect(java.util.stream.Collectors.toList());
    }
}