package com.exercise.contactBook.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class OtpVerifyRequestDTO {

    @NotBlank(message = "request Id is required")
    private String requestedId;

    @NotBlank(message = "otp is required")
    private String otp;
}
