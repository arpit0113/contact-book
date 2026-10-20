package com.exercise.contactBook.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class UserResponseDTO {
    private Long id;
    private String name;
    private String email;
    private String organizationName;
    private String role;
    private String status;
    private List<String> permissions = new ArrayList<>();
    private boolean needsApproval;
}
