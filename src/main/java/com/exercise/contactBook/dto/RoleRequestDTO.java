package com.exercise.contactBook.dto;

import com.exercise.contactBook.enums.Permission;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Set;

@Data
public class RoleRequestDTO {

    @NotBlank(message = "Role name is required")
    @Size(max = 40, message = "Role name is too long")
    private String name;

    @NotNull(message = "Permissions are required")
    private Set<Permission> permissions;
}
