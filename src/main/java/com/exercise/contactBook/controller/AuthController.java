package com.exercise.contactBook.controller;

import com.exercise.contactBook.dto.*;
import com.exercise.contactBook.enums.UserStatus;
import com.exercise.contactBook.exception.ResourceNotFoundException;
import com.exercise.contactBook.model.User;
import com.exercise.contactBook.service.RefreshTokenService;
import com.exercise.contactBook.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.DisabledException;
import org.springframework.web.bind.annotation.*;

import javax.naming.AuthenticationException;
import java.util.Map;

@RestController
@RequestMapping("/api/auth/user")
public class AuthController {

    private final UserService userService;
    private final RefreshTokenService refreshTokenService;


    public AuthController(UserService userService, RefreshTokenService refreshTokenService) {
        this.userService = userService;
        this.refreshTokenService = refreshTokenService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponseDTO> register(@Valid @RequestBody UserRequestDTO dto){
            return ResponseEntity.status(HttpStatus.CREATED).body(userService.registerUser(dto));

    }

    @PostMapping("/login")
    public ResponseEntity<TokenPairResponseDTO> login(@Valid @RequestBody LoginRequestDTO dto) {
        TokenPairResponseDTO tokens = userService.loginUser(dto);
        return ResponseEntity.ok(tokens);
    }


    @PostMapping("/refresh")
    public ResponseEntity<TokenPairResponseDTO> refresh(@Valid @RequestBody RefreshTokenRequestDTO dto){
        String newRefreshToken = refreshTokenService.rotate(dto.getRefreshToken());
        User user = refreshTokenService.getUserForToken(newRefreshToken);
        String newAccessToken = userService.generateToken(user);
        return ResponseEntity.ok(new TokenPairResponseDTO(newAccessToken, newRefreshToken));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshTokenRequestDTO dto){
        refreshTokenService.revoke(dto.getRefreshToken());
        return ResponseEntity.noContent().build();
    }
}

