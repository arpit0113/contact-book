package com.exercise.contactBook.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class RoleDTO {
    private Long id;
    private String name;
    private boolean systemRole;
    private List<String> permissions;
    private long userCount;
}
