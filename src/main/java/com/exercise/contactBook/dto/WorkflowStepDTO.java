package com.exercise.contactBook.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class WorkflowStepDTO {
private int order;
private Long roleId;
private String roleName;
}
