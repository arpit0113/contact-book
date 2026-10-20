package com.exercise.contactBook.controller;

import com.exercise.contactBook.dto.UserResponseDTO;
import com.exercise.contactBook.dto.UserUpdateRequestDTO;
import com.exercise.contactBook.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponseDTO> getUser(){
        return ResponseEntity.status(HttpStatus.OK)
                .body(userService.getCurrentUserProfile());
    }


    @PutMapping("/me")
    public ResponseEntity<UserResponseDTO> updateUser(@Valid @RequestBody UserUpdateRequestDTO dto){
        return ResponseEntity.status(HttpStatus.OK)
                .body(userService.updateCurrentUser(dto));
    }

    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteUser(){
        userService.deleteUser();
        return ResponseEntity.noContent().build();

    }


}
