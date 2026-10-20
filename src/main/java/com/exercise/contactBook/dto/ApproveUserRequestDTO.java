package com.exercise.contactBook.dto;


import com.exercise.contactBook.enums.Role;
import jakarta.validation.constraints.NotNull;
import lombok.Data;


@Data
public class ApproveUserRequestDTO {
    @NotNull(message = "Role is required")
    private Long roleId;
}
