package com.exercise.contactBook.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ResendRequestDTO {

    @NotBlank private String requestId;
    @NotBlank @Email private String adminEmail;
}
