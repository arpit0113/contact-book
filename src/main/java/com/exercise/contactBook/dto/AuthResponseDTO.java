package com.exercise.contactBook.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AuthResponseDTO {
    private String status;
    private String accesstoken;
    private String refreshToken;
    private String message;
    private UserResponseDTO user;
}
