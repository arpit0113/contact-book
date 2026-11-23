package com.exercise.contactBook.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class OrgCreationRequestDTO {

    @NotBlank(message = "Admin name is required")
    private String adminName;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String adminEmail;

    private String orgName;

    public void setAdminEmail(String adminEmail){
        this.adminEmail = adminEmail == null ? null : adminEmail.trim().toLowerCase();
    }
}
