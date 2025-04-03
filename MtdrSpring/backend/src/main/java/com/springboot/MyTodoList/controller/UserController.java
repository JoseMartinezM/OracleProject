package com.springboot.MyTodoList.controller;

import com.springboot.MyTodoList.model.User;
import com.springboot.MyTodoList.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class UserController {
    
    @Autowired
    private UserService userService;
    
    // Obtener todos los usuarios
    @GetMapping(value = "/users")
    public List<User> getAllUsers() {
        return userService.findAll();
    }
    
    // Obtener usuario por ID
    @GetMapping(value = "/users/{id}")
    public ResponseEntity<User> getUserById(@PathVariable int id) {
        try {
            ResponseEntity<User> responseEntity = userService.getUserById(id);
            return new ResponseEntity<>(responseEntity.getBody(), HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }
    
    // Obtener usuario por nombre de usuario
    @GetMapping(value = "/users/username/{username}")
    public ResponseEntity<User> getUserByUsername(@PathVariable String username) {
        try {
            ResponseEntity<User> responseEntity = userService.getUserByUsername(username);
            return new ResponseEntity<>(responseEntity.getBody(), HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }
    
    // Obtener usuario por número de teléfono
    @GetMapping(value = "/users/phone/{phone}")
    public ResponseEntity<User> getUserByPhone(@PathVariable String phone) {
        try {
            ResponseEntity<User> responseEntity = userService.getUserByPhone(phone);
            return new ResponseEntity<>(responseEntity.getBody(), HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }
    
    // Crear nuevo usuario
    @PostMapping(value = "/users")
    public ResponseEntity<Object> addUser(@RequestBody User user) throws Exception {
        User savedUser = userService.addUser(user);
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.set("location", "" + savedUser.getID());
        responseHeaders.set("Access-Control-Expose-Headers", "location");
        
        return ResponseEntity.ok()
                .headers(responseHeaders).build();
    }
    
    // Actualizar usuario existente
    @PutMapping(value = "/users/{id}")
    public ResponseEntity<User> updateUser(@RequestBody User user, @PathVariable int id) {
        try {
            User updatedUser = userService.updateUser(id, user);
            if (updatedUser != null) {
                return new ResponseEntity<>(updatedUser, HttpStatus.OK);
            } else {
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    // Eliminar usuario
    @DeleteMapping(value = "/users/{id}")
    public ResponseEntity<Boolean> deleteUser(@PathVariable("id") int id) {
        Boolean flag = false;
        try {
            flag = userService.deleteUser(id);
            return new ResponseEntity<>(flag, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(flag, HttpStatus.NOT_FOUND);
        }
    }
    
    // Verificar si el usuario es manager
    @GetMapping(value = "/users/{id}/isManager")
    public ResponseEntity<Boolean> isManager(@PathVariable int id) {
        try {
            boolean isManager = userService.isManager(id);
            return new ResponseEntity<>(isManager, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(false, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    // Obtener usuarios por rol
    @GetMapping(value = "/users/role/{role}")
    public List<User> getUsersByRole(@PathVariable String role) {
        return userService.findByRole(role);
    }
}